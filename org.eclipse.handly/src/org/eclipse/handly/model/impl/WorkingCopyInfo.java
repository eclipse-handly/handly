/*******************************************************************************
 * Copyright (c) 2014, 2016 1C-Soft LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.model.impl;

import static org.eclipse.handly.context.Contexts.of;
import static org.eclipse.handly.model.Elements.FORCE_RECONCILING;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.util.Property;

/**
 * Holds information related to a working copy.
 * <p>
 * Concrete implementations of this abstract class are expected to be safe
 * for use by multiple threads.
 * </p>
 */
public abstract class WorkingCopyInfo
{
    private final IBuffer buffer;
    final InitTask initTask = new InitTask();
    SourceFile workingCopy;
    volatile boolean created; // whether wc was created (from the model POV)
    int refCount;

    /**
     * Constructs a new working copy info and associates it with the given
     * buffer; the buffer is NOT <code>addRef</code>'ed.
     *
     * @param buffer the working copy buffer (not <code>null</code>)
     */
    public WorkingCopyInfo(IBuffer buffer)
    {
        if ((this.buffer = buffer) == null)
            throw new IllegalArgumentException();
    }

    /**
     * Returns the buffer associated with this working copy info;
     * does NOT <code>addRef</code> the buffer.
     *
     * @return the working copy buffer (never <code>null</code>)
     */
    public final IBuffer getBuffer()
    {
        return buffer;
    }

    /**
     * Disposes of this working copy info. Does nothing if the working copy info
     * is already disposed.
     *
     * @throws IllegalStateException if the working copy info is still in use
     *  and cannot be disposed
     */
    public final void dispose()
    {
        synchronized (this)
        {
            if (refCount > 0)
                throw new IllegalStateException();
            if (refCount < 0)
                return; // already disposed
            refCount = -1;
        }
        onDispose();
    }

    /**
     * Returns whether this working copy info has been disposed.
     *
     * @return <code>true</code> if the working copy info has been disposed;
     *  <code>false</code> otherwise
     */
    public final boolean isDisposed()
    {
        synchronized (this)
        {
            return refCount < 0;
        }
    }

    /**
     * Initialization callback.
     */
    protected void onInit() throws CoreException
    {
        // does nothing: subclasses may override
    }

    /**
     * Disposal callback. Note that there is no guarantee that
     * <code>onInit()</code> has been called.
     */
    protected void onDispose()
    {
        // does nothing: subclasses may override
    }

    /**
     * Returns whether the working copy needs reconciling, i.e.
     * its buffer has been modified since the last time it was reconciled.
     *
     * @return <code>true</code> if the working copy needs reconciling,
     *  <code>false</code> otherwise
     */
    protected abstract boolean needsReconciling();

    /**
     * Makes the working copy consistent with its buffer by updating the
     * element's structure and properties as necessary.
     * <p>
     * Implementations are encouraged to support the following standard options,
     * which may be specified in the given context:
     * </p>
     * <ul>
     * {@link org.eclipse.handly.model.Elements#FORCE_RECONCILING
     * FORCE_RECONCILING} - Indicates whether reconciling has to be performed
     * even if the working copy is already consistent with its buffer.
     * </ul>
     * <p>
     * An implementation of this method is supposed to call {@link
     * #basicReconcile} with an appropriately augmented context while
     * providing the necessary synchronization guarantees.
     * </p>
     *
     * @param context the operation context (not <code>null</code>)
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired
     * @throws CoreException if the working copy cannot be reconciled
     * @throws OperationCanceledException if this method is canceled
     */
    protected abstract void reconcile(IContext context,
        IProgressMonitor monitor) throws CoreException;

    /**
     * Reconciles the working copy according to options specified in the given
     * context.
     * <p>
     * The following context options can influence whether the structure
     * of the working copy gets rebuilt:
     * </p>
     * <ul>
     * <li>
     * {@link #RECONCILING_FORCED} - Indicates whether reconciling was forced,
     * i.e. the working copy buffer has not been modified since the last time
     * it was reconciled. If <code>false</code> (the default), the structure
     * will be rebuilt.
     * </li>
     * </ul>
     * <p>
     * The following context options influence rebuilding of the structure
     * of the working copy and, if simultaneously present, must be mutually
     * consistent:
     * </p>
     * <ul>
     * <li>
     * {@link #SOURCE_AST} - Specifies the AST to use when building the structure.
     * The AST is safe to read in the dynamic context of this method call, but
     * must not be modified.
     * </li>
     * <li>
     * {@link #SOURCE_CONTENTS} - Specifies the source string to use when
     * building the structure.
     * </li>
     * </ul>
     * <p>
     * At least one of <code>SOURCE_AST</code> or <code>SOURCE_CONTENTS</code>
     * must have a non-null value in the given context.
     * </p>
     * <p>
     * The given context may provide additional data that this method can use,
     * including the following:
     * </p>
     * <ul>
     * <li>
     * {@link #SOURCE_SNAPSHOT} - Specifies the source snapshot from which
     * <code>SOURCE_AST</code> was created or <code>SOURCE_CONTENTS</code>
     * was obtained. The snapshot may expire.
     * </li>
     * </ul>
     * <p>
     * This method makes no guarantees about synchronization of reconcile
     * operations. Such guarantees must be provided by the {@link #reconcile}
     * method.
     * </p>
     *
     * @param context the operation context (not <code>null</code>)
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired
     * @throws CoreException if the working copy cannot be reconciled
     * @throws OperationCanceledException if this method is canceled
     */
    protected final void basicReconcile(IContext context,
        IProgressMonitor monitor) throws CoreException
    {
        Object ast = context.get(SOURCE_AST);
        if (ast == null)
            ast = workingCopy.hCreateAst(context.get(SOURCE_CONTENTS), context,
                monitor);
        workingCopy.hReconcileOperation().reconcile(ast, context, monitor);
    }

    /**
     * Specifies the source AST for reconciling.
     * @see #basicReconcile(IContext, IProgressMonitor)
     */
    protected static final Property<Object> SOURCE_AST = Property.get(
        "sourceAst", Object.class); //$NON-NLS-1$
    /**
     * Specifies the source string for reconciling.
     * @see #basicReconcile(IContext, IProgressMonitor)
     */
    protected static final Property<String> SOURCE_CONTENTS =
        SourceFile.SOURCE_CONTENTS;
    /**
     * Specifies the source snapshot for reconciling.
     * @see #basicReconcile(IContext, IProgressMonitor)
     */
    protected static final Property<ISnapshot> SOURCE_SNAPSHOT =
        SourceFile.SOURCE_SNAPSHOT;
    /**
     * Indicates whether reconciling was forced, i.e. the working copy buffer
     * has not been modified since the last time it was reconciled.
     * @see #basicReconcile(IContext, IProgressMonitor)
     */
    protected static final Property<Boolean> RECONCILING_FORCED =
        SourceFile.RECONCILING_FORCED;

    /**
     * Clients should not be exposed to working copy info if it has not been
     * initialized.
     *
     * @noreference For internal use only.
     */
    public final boolean isInitialized()
    {
        try
        {
            return initTask.wasSuccessful(0, TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException e)
        {
            throw new AssertionError(e);
        }
        catch (TimeoutException e)
        {
            return false;
        }
    }

    class InitTask
    {
        private IProgressMonitor monitor;
        private final FutureTask<?> futureTask = new FutureTask<Object>(
            new Callable<Object>()
            {
                @Override
                public Object call() throws CoreException
                {
                    run();
                    return null;
                }
            });

        void execute(IProgressMonitor monitor) throws CoreException
        {
            if (monitor == null)
                monitor = new NullProgressMonitor();
            this.monitor = monitor;
            futureTask.run();
            this.monitor = null;
            try
            {
                futureTask.get();
            }
            catch (InterruptedException e)
            {
                throw new AssertionError(e);
            }
            catch (ExecutionException e)
            {
                Throwable cause = e.getCause();
                if (cause instanceof CoreException)
                    throw (CoreException)cause;
                if (cause instanceof RuntimeException)
                    throw (RuntimeException)cause;
                if (cause instanceof Error)
                    throw (Error)cause;
                throw new AssertionError(cause); // should never happen
            }
        }

        boolean wasSuccessful(long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException
        {
            try
            {
                futureTask.get(timeout, unit);
                return true;
            }
            catch (ExecutionException e)
            {
                return false;
            }
        }

        private void run() throws CoreException
        {
            onInit();
            reconcile(of(FORCE_RECONCILING, true), monitor);
            if (!created)
            {
                throw new AssertionError(
                    "Working copy creation was not completed. Ill-behaved implementation of #reconcile?"); //$NON-NLS-1$
            }
        }
    }
}
