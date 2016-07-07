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
package org.eclipse.handly.xtext.ui.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.handly.document.DocumentChange;
import org.eclipse.handly.document.DocumentChangeOperation;
import org.eclipse.handly.document.IDocumentChange;
import org.eclipse.handly.document.UiDocumentChangeRunner;
import org.eclipse.handly.internal.xtext.ui.Activator;
import org.eclipse.handly.snapshot.DocumentSnapshot;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.snapshot.NonExpiringSnapshot;
import org.eclipse.handly.util.UiSynchronizer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.service.OperationCanceledError;
import org.eclipse.xtext.ui.editor.DirtyStateEditorSupport;
import org.eclipse.xtext.ui.editor.model.DocumentTokenSource;
import org.eclipse.xtext.ui.editor.model.IXtextDocumentContentObserver.Processor;
import org.eclipse.xtext.ui.editor.model.XtextDocument;
import org.eclipse.xtext.ui.editor.model.edit.ITextEditComposer;
import org.eclipse.xtext.ui.editor.reconciler.ReplaceRegion;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import com.google.common.base.Throwables;
import com.google.inject.Inject;

/**
 * Extends {@link XtextDocument} for Handly reconciling story.
 * Implements {@link IHandlyXtextDocument}.
 * <p>
 * Bind this class in place of the default <code>XtextDocument</code> if you
 * have {@link HandlyXtextEditorCallback} configured. Note that if you bind
 * this class, you should also bind other classes pertaining to Handly/Xtext
 * integration:
 * </p>
 * <pre>
 * public Class&lt;? extends XtextDocument&gt; bindXtextDocument() {
 *     return HandlyXtextDocument.class;
 * }
 *
 * public Class&lt;? extends IReconciler&gt; bindIReconciler() {
 *     return HandlyXtextReconciler.class;
 * }
 *
 * public Class&lt;? extends DirtyStateEditorSupport&gt; bindDirtyStateEditorSupport() {
 *     return HandlyDirtyStateEditorSupport.class; // or its subclass
 * }</pre>
 *
 * @noextend This class is not intended to be extended by clients.
 */
public class HandlyXtextDocument
    extends XtextDocument
    implements IHandlyXtextDocument
{
    private final static IUnitOfWork.Void<XtextResource> NO_OP =
        new IUnitOfWork.Void<XtextResource>()
        {
            public void process(XtextResource resource)
            {
            }
        };

    private final BooleanThreadLocal hasTopLevelModification =
        new BooleanThreadLocal();

    private ITextEditComposer composer2; // unfortunately had to duplicate
    private volatile NonExpiringSnapshot reconciledSnapshot;
    private volatile boolean reconcilingWasCanceled;
    private final ThreadLocal<IProgressMonitor> reconcilingMonitor =
        new ThreadLocal<IProgressMonitor>();
    private final ListenerList reconcilingListeners = new ListenerList(
        ListenerList.IDENTITY);
    private final DocumentListener selfListener = new DocumentListener();
    private PendingChange pendingChange;
    private final Object pendingChangeLock = new Object();
    private DirtyStateEditorSupport dirtyStateEditorSupport;

    @Inject
    public HandlyXtextDocument(DocumentTokenSource tokenSource,
        ITextEditComposer composer)
    {
        super(tokenSource, composer);
        this.composer2 = composer;
        addReconcilingListener(new PostReconcileProcessor());
    }

    @Override
    public void setInput(XtextResource resource)
    {
        if (resource == null)
            throw new IllegalArgumentException();
        super.setInput(resource);
        reconciledSnapshot = getNonExpiringSnapshot(); // initial snapshot
        addDocumentListener(selfListener);
    }

    @Override
    public void disposeInput()
    {
        super.disposeInput(); // sets resource to null under the document's write lock
        // After this, T2MReconcilingUnitOfWork and M2TReconcilingUnitOfWork for the document
        // are guaranteed to be passed null resource, which effectively causes them to become noop.

        removeDocumentListener(selfListener);
        getAndResetPendingChange();
        reconciledSnapshot = null;
        reconcilingListeners.clear();
    }

    @Override
    public ISnapshot getSnapshot()
    {
        return new DocumentSnapshot(this);
    }

    @Override
    public ISnapshot getReconciledSnapshot()
    {
        NonExpiringSnapshot reconciledSnapshot = this.reconciledSnapshot;
        if (reconciledSnapshot == null)
            return null;
        return reconciledSnapshot.getWrappedSnapshot();
    }

    public void addReconcilingListener(IReconcilingListener listener)
    {
        reconcilingListeners.add(listener);
    }

    public void removeReconcilingListener(IReconcilingListener listener)
    {
        reconcilingListeners.remove(listener);
    }

    @Override
    public boolean needsReconciling()
    {
        ISnapshot reconciledSnapshot = getReconciledSnapshot();
        if (reconciledSnapshot == null)
            return false;
        return !getSnapshot().isEqualTo(reconciledSnapshot)
            || reconcilingWasCanceled;
    }

    @Override
    public void reconcile(boolean force, IProgressMonitor monitor)
    {
        reconcilingMonitor.set(monitor);
        try
        {
            if (!force)
            {
                readOnly(NO_OP);
            }
            else
            {
                T2MReconcilingUnitOfWork reconcilingUnitOfWork =
                    new T2MReconcilingUnitOfWork(true);
                internalModify(reconcilingUnitOfWork);
            }

            if (monitor != null && monitor.isCanceled())
                throw new OperationCanceledException();
        }
        finally
        {
            reconcilingMonitor.remove();
        }
    }

    /**
     * Re-parses the resource so it becomes reconciled with the document contents.
     * Does nothing if already reconciled. <b>For internal use only.</b>
     *
     * @param processor the processor to execute the reconciling unit of work
     *  (not <code>null</code>)
     * @return <code>true</code> if the document had any changes to be reconciled,
     *   <code>false</code> otherwise
     */
    public boolean reconcile(Processor processor)
    {
        T2MReconcilingUnitOfWork reconcilingUnitOfWork =
            new T2MReconcilingUnitOfWork(false);
        return processor.process(reconcilingUnitOfWork);
    }

    @Override
    public <T> T modify(IUnitOfWork<T, XtextResource> work)
    {
        if (!hasTopLevelModification.get()) // wrap top-level modification (only)
            work = new M2TReconcilingUnitOfWork<T>(work);
        return internalModify(work);
    }

    @Override
    public IDocumentChange applyChange(IDocumentChange change)
        throws BadLocationException
    {
        DocumentChangeOperation operation = new DocumentChangeOperation(this,
            change);
        UiDocumentChangeRunner runner = new UiDocumentChangeRunner(
            UiSynchronizer.getDefault(), operation);
        return runner.run();
    }

    void setDirtyStateEditorSupport(
        DirtyStateEditorSupport dirtyStateEditorSupport)
    {
        this.dirtyStateEditorSupport = dirtyStateEditorSupport;
    }

    private PendingChange getAndResetPendingChange()
    {
        final PendingChange result;
        synchronized (pendingChangeLock)
        {
            result = pendingChange;
            pendingChange = null;
        }
        return result;
    }

    private void handleDocumentChanged(DocumentEvent event)
    {
        synchronized (pendingChangeLock)
        {
            if (pendingChange == null)
            {
                pendingChange = new PendingChange();
            }
            pendingChange.add(event);
        }
    }

    private NonExpiringSnapshot getNonExpiringSnapshot()
    {
        return new NonExpiringSnapshot(this);
    }

    /*
     * Called just after a reconciling operation has been performed. Informs
     * that the document's XtextResource contents is based on the given snapshot.
     * Notifies reconciling listeners (if any). Should only be called
     * in the dynamic context of {@link XtextDocument#internalModify}.
     *
     * @param resource the reconciled resource (never <code>null</code>)
     * @param snapshot the reconciled snapshot (never <code>null</code>)
     * @param forced whether reconciling was forced, i.e. the document has not
     *  changed since it was reconciled the last time
     * @param monitor a progress monitor (never <code>null</code>)
     * @throws OperationCanceledException if this method is canceled
     */
    private void reconciled(final XtextResource resource,
        final NonExpiringSnapshot snapshot, final boolean forced,
        final IProgressMonitor monitor)
    {
        Object[] listeners = reconcilingListeners.getListeners();
        monitor.beginTask("", listeners.length); //$NON-NLS-1$
        try
        {
            for (final Object listener : listeners)
            {
                if (monitor.isCanceled())
                    break;

                SafeRunner.run(new ISafeRunnable()
                {
                    @Override
                    public void run() throws Exception
                    {
                        ((IReconcilingListener)listener).reconciled(resource,
                            snapshot, forced, new SubProgressMonitor(monitor,
                                1));
                    }

                    @Override
                    public void handleException(Throwable exception)
                    {
                    }
                });
            }
            if (monitor.isCanceled())
                throw new OperationCanceledException();
        }
        finally
        {
            monitor.done();
        }
    }

    private void internalReconcile(XtextResource resource)
    {
        reconcile(new InternalProcessor(resource));
    }

    private IProgressMonitor getReconcilingMonitor()
    {
        IProgressMonitor monitor = reconcilingMonitor.get();
        if (monitor == null)
            monitor = new NullProgressMonitor();
        return monitor;
    }

    /**
     * Document reconciling listener protocol.
     */
    public interface IReconcilingListener
    {
        /**
         * Called just after a reconciling operation has been performed. Informs
         * that the given resource contents is based on the given snapshot.
         * <p>
         * Implementations of this method must not modify the resource and
         * must not keep any references to it. The resource is safe to read
         * in the dynamic context of the method call. The resource has bindings
         * to text positions in the given snapshot.
         * </p>
         * <p>
         * An exception thrown by this method will be logged (except for
         * OperationCanceledException) and suppressed.
         * </p>
         *
         * @param resource the reconciled resource (never <code>null</code>)
         * @param snapshot the reconciled snapshot (never <code>null</code>)
         * @param forced whether reconciling was forced, i.e. the document
         *  has not changed since it was reconciled the last time
         * @param monitor a progress monitor (never <code>null</code>)
         * @throws Exception if a problem occurred while running this method
         * @throws OperationCanceledException if this method is canceled
         */
        void reconciled(XtextResource resource, NonExpiringSnapshot snapshot,
            boolean forced, IProgressMonitor monitor) throws Exception;
    }

    private class DocumentListener
        implements IDocumentListener
    {
        @Override
        public void documentAboutToBeChanged(DocumentEvent event)
        {
        }

        @Override
        public void documentChanged(DocumentEvent event)
        {
            handleDocumentChanged(event);
        }
    }

    private class PendingChange
    {
        private NonExpiringSnapshot snapshotToReconcile;
        private ReplaceRegion replaceRegionToReconcile;
        private long modificationStamp;

        public NonExpiringSnapshot getSnapshotToReconcile()
        {
            return snapshotToReconcile;
        }

        public ReplaceRegion getReplaceRegionToReconcile()
        {
            return replaceRegionToReconcile;
        }

        public long getModificationStamp()
        {
            return modificationStamp;
        }

        /**
         * Should be called immediately <b>after</b> document change.
         *
         * @param event describes how the document changed
         */
        public void add(DocumentEvent event)
        {
            snapshotToReconcile = getNonExpiringSnapshot();
            ReplaceRegion replaceRegion = new ReplaceRegion(event.getOffset(),
                event.getLength(), event.getText());
            if (replaceRegionToReconcile == null)
                replaceRegionToReconcile = replaceRegion;
            else
                replaceRegionToReconcile.mergeWith(replaceRegion,
                    snapshotToReconcile.getContents());
            modificationStamp = event.getModificationStamp();
        }
    }

    /*
     * Should only be called when the document's write lock is held.
     */
    private class T2MReconcilingUnitOfWork
        implements IUnitOfWork<Boolean, XtextResource>
    {
        private final boolean force;

        public T2MReconcilingUnitOfWork(boolean force)
        {
            this.force = force;
        }

        /*
         * This method uses a thread-local reconciling monitor to support
         * cancellation and report progress. If this method is canceled,
         * it does NOT throw OperationCanceledException. Rather, it
         * makes a note that reconciling was canceled and returns.
         */
        @Override
        public Boolean exec(final XtextResource resource) throws Exception
        {
            final IProgressMonitor monitor = getReconcilingMonitor();
            monitor.beginTask("", 2); //$NON-NLS-1$
            try
            {
                if (resource == null) // input not set or already disposed
                {
                    if (force)
                        throw new NoXtextResourceException(
                            HandlyXtextDocument.this);
                    return false;
                }

                if (monitor.isCanceled())
                {
                    reconcilingWasCanceled = true;
                    return false;
                }

                PendingChange change = getAndResetPendingChange();
                if (change == null)
                {
                    if (force || reconcilingWasCanceled)
                    {
                        final NonExpiringSnapshot snapshot = reconciledSnapshot;
                        if (force) // no need to reparse -- just update internal state
                            resource.update(0, 0, ""); //$NON-NLS-1$
                        SafeRunner.run(new ISafeRunnable()
                        {
                            @Override
                            public void run() throws Exception
                            {
                                reconciled(resource, snapshot,
                                    !reconcilingWasCanceled,
                                    new SubProgressMonitor(monitor, 2));
                            }

                            @Override
                            public void handleException(Throwable exception)
                            {
                                // the exception is suppressed, including OCE
                            }
                        });
                        reconcilingWasCanceled = monitor.isCanceled();
                    }
                    return false;
                }
                else
                {
                    final NonExpiringSnapshot snapshot =
                        change.getSnapshotToReconcile();
                    ReplaceRegion replaceRegion =
                        change.getReplaceRegionToReconcile();
                    long modificationStamp = change.getModificationStamp();
                    try
                    {
                        resource.update(replaceRegion.getOffset(),
                            replaceRegion.getLength(), replaceRegion.getText());
                        resource.setModificationStamp(modificationStamp);
                    }
                    catch (Exception e)
                    {
                        // partial parsing failed - performing full reparse
                        Activator.log(Activator.createErrorStatus(
                            e.getMessage(), e));
                        try
                        {
                            resource.reparse(snapshot.getContents());
                            resource.setModificationStamp(modificationStamp);
                        }
                        catch (Exception e2)
                        {
                            // full parsing also failed - restore state
                            Activator.log(Activator.createErrorStatus(
                                e2.getMessage(), e2));
                            resource.reparse(reconciledSnapshot.getContents());
                            throw e2;
                        }
                    }
                    SafeRunner.run(new ISafeRunnable()
                    {
                        @Override
                        public void run() throws Exception
                        {
                            monitor.worked(1);
                            reconciled(resource, snapshot, false,
                                new SubProgressMonitor(monitor, 1));
                        }

                        @Override
                        public void handleException(Throwable exception)
                        {
                            // the exception is suppressed, including OCE
                        }
                    });
                    reconcilingWasCanceled = monitor.isCanceled();
                    reconciledSnapshot = snapshot;
                    return true;
                }
            }
            finally
            {
                monitor.done();
            }
        }
    }

    /*
     * Should only be called when the document's write lock is held.
     */
    private class M2TReconcilingUnitOfWork<T>
        implements IUnitOfWork<T, XtextResource>
    {
        private final IUnitOfWork<T, XtextResource> work;
        private ISnapshot baseSnapshot; // snapshot before resource modifications

        public M2TReconcilingUnitOfWork(IUnitOfWork<T, XtextResource> work)
        {
            this.work = work;
        }

        @Override
        public T exec(XtextResource resource) throws Exception
        {
            if (resource == null) // input not set or already disposed
                return work.exec(resource);

            hasTopLevelModification.set(true);
            try
            {
                internalReconcile(resource); // ensure a fresh base snapshot
                baseSnapshot = getReconciledSnapshot();
                T result;
                try
                {
                    // resolve all proxies before model modification
                    // (otherwise, proxy resolution might throw exceptions
                    // due to inconsistency between 'changed' model and
                    // 'old' proxy URIs)
                    EcoreUtil2.resolveAll(resource, CancelIndicator.NullImpl);

                    composer2.beginRecording(resource);
                    result = work.exec(resource);
                    TextEdit edit = composer2.endRecording();
                    if (edit != null)
                    {
                        DocumentChange change = new DocumentChange(edit);
                        change.setBase(baseSnapshot);
                        if (work instanceof IUndoableUnitOfWork)
                            change.setStyle(IDocumentChange.CREATE_UNDO);
                        else
                            change.setStyle(IDocumentChange.NONE);

                        IDocumentChange undoChange = applyChange(change);

                        if (work instanceof IUndoableUnitOfWork)
                            ((IUndoableUnitOfWork<T, XtextResource>)work).acceptUndoChange(
                                undoChange);
                    }
                }
                catch (Exception e)
                {
                    // modification failed - restore state
                    Activator.log(Activator.createErrorStatus(e.getMessage(),
                        e));
                    resource.reparse(reconciledSnapshot.getContents());
                    throw e;
                }
                internalReconcile(resource); // reconcile resource with changed document
                return result;
            }
            finally
            {
                hasTopLevelModification.remove();
            }
        }
    }

    private static class BooleanThreadLocal
        extends ThreadLocal<Boolean>
    {
        @Override
        protected Boolean initialValue()
        {
            return Boolean.FALSE;
        }
    }

    // Special processor for #internalReconcile
    private static class InternalProcessor
        implements Processor
    {
        private final XtextResource resource;

        public InternalProcessor(XtextResource resource)
        {
            this.resource = resource;
        }

        @Override
        public <T> T process(IUnitOfWork<T, XtextResource> transaction)
        {
            try
            {
                return transaction.exec(resource);
            }
            catch (RuntimeException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new WrappedException(e);
            }
        }
    }

    // Initially copied from XtextDocumentReconcileStrategy#postParse
    private class PostReconcileProcessor
        implements IReconcilingListener
    {
        @Override
        public void reconciled(XtextResource resource,
            NonExpiringSnapshot snapshot, boolean forced,
            final IProgressMonitor monitor) throws Exception
        {
            CancelIndicator cancelIndicator = new CancelIndicator()
            {
                @Override
                public boolean isCanceled()
                {
                    return monitor.isCanceled();
                }
            };
            monitor.beginTask("", 1); //$NON-NLS-1$
            try
            {
                EcoreUtil2.resolveLazyCrossReferences(resource,
                    cancelIndicator);
                if (dirtyStateEditorSupport != null && !monitor.isCanceled())
                {
                    dirtyStateEditorSupport.announceDirtyState(resource);
                }
            }
            catch (Throwable t)
            {
                resource.getCache().clear(resource);

                if (!(t instanceof OperationCanceledError))
                    Throwables.propagate(t);
            }
            finally
            {
                monitor.done();
            }
        }
    }
}
