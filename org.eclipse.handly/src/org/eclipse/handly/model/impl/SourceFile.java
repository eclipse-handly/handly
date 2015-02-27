/*******************************************************************************
 * Copyright (c) 2014, 2015 1C LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.model.impl;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.buffer.TextFileBuffer;
import org.eclipse.handly.internal.Activator;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.ISourceFile;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.snapshot.ISnapshotProvider;
import org.eclipse.handly.snapshot.NonExpiringSnapshot;
import org.eclipse.handly.snapshot.TextFileSnapshot;
import org.eclipse.handly.util.TextRange;

/**
 * Represents a source file.
 */
public abstract class SourceFile
    extends Module
    implements ISourceFile
{
    private static final ThreadLocal<AstHolder> AST_HOLDER =
        new ThreadLocal<AstHolder>();
    private static final ThreadLocal<WcCreation> WC_CREATION =
        new ThreadLocal<WcCreation>();

    /**
     * The underlying workspace file.
     */
    protected final IFile file;

    /**
     * Constructs a handle for a source file with the given parent element and 
     * the given underlying workspace file.
     * 
     * @param parent the parent of the element (not <code>null</code>)
     * @param file the workspace file underlying the element (not <code>null</code>)
     */
    public SourceFile(Handle parent, IFile file)
    {
        super(parent, file.getName());
        this.file = file;
    }

    @Override
    public final IResource getResource()
    {
        return file;
    }

    @Override
    public final IFile getFile()
    {
        return file;
    }

    @Override
    public final SourceFile getSourceFile()
    {
        return this;
    }

    @Override
    public final IBuffer openBuffer(boolean create, IProgressMonitor monitor)
        throws CoreException
    {
        WorkingCopyInfo info = getWorkingCopyInfo();
        if (info == null)
        {
            if (!create
                && ITextFileBufferManager.DEFAULT.getTextFileBuffer(
                    file.getFullPath(), LocationKind.IFILE) == null)
            {
                return null;
            }
            return new TextFileBuffer(file, ITextFileBufferManager.DEFAULT,
                monitor);
        }
        else
        {
            try
            {
                IWorkingCopyBuffer buffer = info.getBuffer();
                buffer.addRef();
                return buffer;
            }
            finally
            {
                discardWorkingCopyInfo();
            }
        }
    }

    @Override
    public final IBuffer openBuffer(IProgressMonitor monitor)
        throws CoreException
    {
        return openBuffer(true, monitor);
    }

    /**
     * Switches this source file into a working copy. Switching to working copy 
     * means that the source file's structure and properties shall no longer 
     * correspond to the underlying resource contents and shall no longer 
     * be updated by a resource delta processor. Instead, those structure and 
     * properties can be explicitly {@link #reconcile(boolean, IProgressMonitor) 
     * reconciled} with the current contents of the working copy's buffer.
     * <p>
     * If this source file was already in working copy mode, an internal counter 
     * is incremented and no other action is taken on this source file. 
     * To bring this source file back into the original mode (where it reflects 
     * the underlying resource), <code>discardWorkingCopy()</code> must be called 
     * as many times as <code>becomeWorkingCopy</code>.
     * </p>
     * <p>
     * If this source file was already in working copy mode, but was associated 
     * with a different buffer, an <code>IllegalStateException</code> is thrown 
     * and the internal counter is NOT incremented.
     * </p>
     * 
     * @param buffer the working copy buffer to be associated with 
     *  this source file (not <code>null</code>). The buffer will be 
     *  <code>addRef</code>'ed if this method succeeds, and will NOT be 
     *  <code>addRef</code>'ed if this method fails with an exception
     * @param monitor a progress monitor, or <code>null</code> 
     *  if progress reporting is not desired
     * @throws CoreException if the working copy cannot be created 
     * @see {@link #discardWorkingCopy()}
     */
    public final void becomeWorkingCopy(IWorkingCopyBuffer buffer,
        IProgressMonitor monitor) throws CoreException
    {
        if (createWorkingCopyInfo(buffer))
        {
            boolean success = false;
            try
            {
                WC_CREATION.set(WcCreation.IN_PROGRESS);
                reconcile(false, monitor);
                success = true;
            }
            finally
            {
                WcCreation creationState = WC_CREATION.get();
                WC_CREATION.set(null);
                if (!success)
                {
                    if (discardWorkingCopyInfo()
                        && creationState == WcCreation.FINISHED)
                    {
                        workingCopyModeChanged();
                    }
                }
            }
        }
    }

    /**
     * Switches this source file from working copy mode back to its original mode.
     * Has no effect if this source file was not in working copy mode.
     * <p>
     * If <code>becomeWorkingCopy</code> method was called several times 
     * on this source file, <code>discardWorkingCopy()</code> must be called 
     * exactly as many times before it switches back to the original mode.
     * </p>
     *
     * @return <code>true</code> if this source file was switched from 
     *  working copy mode back to its original mode, <code>false</code> 
     *  otherwise
     * @see #becomeWorkingCopy(IWorkingCopyBuffer, IProgressMonitor)
     */
    public final boolean discardWorkingCopy()
    {
        if (discardWorkingCopyInfo())
        {
            workingCopyModeChanged();
            return true;
        }
        return false;
    }

    @Override
    public final boolean isWorkingCopy()
    {
        return peekAtWorkingCopyInfo() != null;
    }

    @Override
    public final boolean needsReconciling()
    {
        WorkingCopyInfo info = getWorkingCopyInfo();
        if (info == null)
            return false;
        else
        {
            try
            {
                return info.getBuffer().needsReconciling();
            }
            finally
            {
                discardWorkingCopy();
            }
        }
    }

    @Override
    public final void reconcile(boolean force, IProgressMonitor monitor)
        throws CoreException
    {
        WorkingCopyInfo info = getWorkingCopyInfo();
        if (info == null)
            return; // not a working copy
        else
        {
            try
            {
                if (monitor == null)
                    monitor = new NullProgressMonitor();

                info.getBuffer().reconcile(force, monitor);
            }
            finally
            {
                discardWorkingCopyInfo();
            }
        }
    }

    @Override
    public final boolean close()
    {
        if (isWorkingCopy())
            return false; // must not close a working copy

        return super.close();
    }

    /**
     * Internal API. This method should be used only from {@link IWorkingCopyBuffer} 
     * implementations. Other clients are not intended to invoke this method. 
     * Subclasses may override.
     * 
     * @return a {@link ReconcileOperation} (never <code>null</code>)
     */
    public ReconcileOperation getReconcileOperation()
    {
        return new ReconcileOperation();
    }

    /**
     * Notifies about a working copy mode change: either the source file 
     * became a working copy or reverted back from the working copy mode.
     */
    protected void workingCopyModeChanged()
    {
        // subclasses might fire an appropriate event, etc.
    }

    @Override
    protected SourceElementBody newBody()
    {
        return new SourceElementBody();
    }

    @Override
    protected void validateExistence() throws CoreException
    {
        if (!isWorkingCopy())
        {
            if (!file.exists())
                throw new CoreException(Activator.createErrorStatus(
                    MessageFormat.format(
                        Messages.SourceFile_File_does_not_exist__0,
                        file.getFullPath().makeRelative()), null));
        }
    }

    @Override
    protected final void buildStructure(Body body,
        Map<IHandle, Body> newElements) throws CoreException
    {
        AstHolder astHolder = AST_HOLDER.get();

        if (astHolder == null) // not a working copy
        {
            // NOTE: AST is created from the underlying file contents, 
            // not from the buffer contents, since source files that are not 
            // working copies must reflect the structure of the underlying file
            NonExpiringSnapshot snapshot =
                new NonExpiringSnapshot(new ISnapshotProvider()
                {
                    @Override
                    public ISnapshot getSnapshot()
                    {
                        return new TextFileSnapshot(getFile());
                    }
                });
            Object ast = createStructuralAst(snapshot.getContents());
            astHolder = new AstHolder(ast, snapshot);
        }

        SourceElementBody thisBody = (SourceElementBody)body;
        String source = astHolder.snapshot.getContents();
        ISnapshot snapshot = astHolder.snapshot.getWrappedSnapshot();

        buildStructure(thisBody, newElements, astHolder.ast, source);

        thisBody.setFullRange(new TextRange(0, source.length()));
        setSnapshot(thisBody, snapshot, newElements);
    }

    /**
     * Returns a new AST created from the given source string. The AST may 
     * contain just enough information for computing this source file's 
     * structure and properties as well as of all of its descendant elements.
     *
     * @param source the source string to parse (not <code>null</code>)
     * @return the (possibly abridged) AST created from the given source string
     *  (never <code>null</code>)
     * @throws CoreException
     */
    protected abstract Object createStructuralAst(String source)
        throws CoreException;

    /**
     * Initializes the given body based on the given AST and the given source 
     * string from which the AST was created. The AST should contain enough 
     * information for computing this source file's structure and properties 
     * as well as of all of its descendant elements. The descendants are to be 
     * placed in the given <code>newElements</code> map (note that this element 
     * has already been placed in the map).
     * <p>
     * The AST is safe to read in the dynamic context of the method call, 
     * but must not be modified. Implementations must not keep references 
     * to any part of the AST or the source string outside the dynamic scope 
     * of the invocation of this method.
     * </p>
     *
     * @param body a new, uninitialized body for this element 
     *  (never <code>null</code>)
     * @param newElements a map containing handle/body relationships 
     *  (never <code>null</code>)
     * @param ast the AST created from the given source string 
     *  (not <code>null</code>) 
     * @param source the source string from which the given AST was created 
     *  (not <code>null</code>)
     */
    protected abstract void buildStructure(SourceElementBody body,
        Map<IHandle, Body> newElements, Object ast, String source);

    /**
     * Returns whether the structure should be rebuilt when reconciling 
     * is forced (i.e. the working copy buffer has not been modified 
     * since the last time it was reconciled). Default implementation 
     * returns <code>false</code> since typically the structure remains 
     * the same if reconciling is forced. Subclasses may override.
     *
     * @return <code>true</code> if the structure should be rebuilt 
     *  when reconciling is forced, <code>false</code> otherwise
     */
    protected boolean shouldRebuildStructureIfForced()
    {
        return false;
    }

    private static void setSnapshot(SourceElementBody body, ISnapshot snapshot,
        Map<IHandle, Body> newElements)
    {
        body.setSnapshot(snapshot);
        for (IHandle child : body.getChildren())
        {
            setSnapshot((SourceElementBody)newElements.get(child), snapshot,
                newElements);
        }
    }

    private boolean createWorkingCopyInfo(IWorkingCopyBuffer buffer)
    {
        return getHandleManager().createWorkingCopyInfo(this, buffer);
    }

    private WorkingCopyInfo getWorkingCopyInfo()
    {
        return getHandleManager().getWorkingCopyInfo(this);
    }

    private WorkingCopyInfo peekAtWorkingCopyInfo()
    {
        return getHandleManager().peekAtWorkingCopyInfo(this);
    }

    private boolean discardWorkingCopyInfo()
    {
        return getHandleManager().discardWorkingCopyInfo(this);
    }

    /**
     * Part of the internal API intended for use in {@link IWorkingCopyBuffer} 
     * implementations. May be subclassed to augment default behavior, 
     * e.g. to send out a delta notification indicating the nature of the 
     * change of the working copy since the last time it was reconciled.
     */
    public class ReconcileOperation
    {
        /**
         * Use {@link SourceFile#getReconcileOperation()} for obtaining 
         * an instance.
         */
        protected ReconcileOperation()
        {
        }

        /**
         * Reconciles this working copy according to the given AST and the given 
         * non-expiring snapshot on which the AST is based. The AST should contain 
         * enough information for computing this working copy's structure and 
         * properties as well as of all its descendant elements.
         * <p>
         * The AST is safe to read in the dynamic context of the method call, 
         * but must not be modified. Implementations must not keep references 
         * to any part of the AST or of the snapshot outside the dynamic scope 
         * of the invocation of this method.
         * </p>
         * <p>
         * Subclasses may override this method, but must call the super 
         * implementation.
         * </p>
         *
         * @param ast the working copy AST based on the given snapshot 
         *  (not <code>null</code>) 
         * @param snapshot the non-expiring snapshot on which the given AST 
         *  is based (not <code>null</code>)
         * @param forced indicates whether reconciling was forced, i.e. 
         *  the working copy buffer has not been modified since the last time 
         *  it was reconciled
         * @throws CoreException if the working copy cannot be reconciled
         */
        public void reconcile(Object ast, NonExpiringSnapshot snapshot,
            boolean forced) throws CoreException
        {
            if (!forced || shouldRebuildStructureIfForced())
            {
                if (AST_HOLDER.get() != null)
                    throw new AssertionError(); // should never happen
                AST_HOLDER.set(new AstHolder(ast, snapshot));
                try
                {
                    openWhenClosed(newBody());
                }
                finally
                {
                    AST_HOLDER.set(null);
                }
            }

            if (WC_CREATION.get() == WcCreation.IN_PROGRESS)
            {
                workingCopyModeChanged(); // notify about wc creation
                WC_CREATION.set(WcCreation.FINISHED);
            }
        }
    }

    private static enum WcCreation
    {
        IN_PROGRESS,
        FINISHED
    }

    private static class AstHolder
    {
        public final Object ast;
        public final NonExpiringSnapshot snapshot;

        public AstHolder(Object ast, NonExpiringSnapshot snapshot)
        {
            this.ast = ast;
            this.snapshot = snapshot;
        }
    }
}
