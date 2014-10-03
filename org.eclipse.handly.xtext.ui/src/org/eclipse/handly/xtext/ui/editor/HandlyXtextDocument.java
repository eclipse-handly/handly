/*******************************************************************************
 * Copyright (c) 2014 1C LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.xtext.ui.editor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.xtext.resource.DerivedStateAwareResource;
import org.eclipse.xtext.resource.IBatchLinkableResource;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.DirtyStateEditorSupport;
import org.eclipse.xtext.ui.editor.model.DocumentTokenSource;
import org.eclipse.xtext.ui.editor.model.IXtextDocumentContentObserver.Processor;
import org.eclipse.xtext.ui.editor.model.IXtextModelListener;
import org.eclipse.xtext.ui.editor.model.XtextDocument;
import org.eclipse.xtext.ui.editor.model.edit.ITextEditComposer;
import org.eclipse.xtext.ui.editor.reconciler.CancelIndicatorBasedProgressMonitor;
import org.eclipse.xtext.ui.editor.reconciler.ReplaceRegion;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import com.google.inject.Inject;

/**
 * Extends {@link XtextDocument} for our reconciling story.
 * Implements {@link IHandlyXtextDocument}.
 *
 * @noextend This class is not intended to be extended by clients.
 */
public class HandlyXtextDocument
    extends XtextDocument
    implements IHandlyXtextDocument
{
    private final BooleanThreadLocal hasTopLevelModification =
        new BooleanThreadLocal();

    private ITextEditComposer composer2; // unfortunately had to duplicate
    private final List<IXtextModelListener> modelListeners2 =
        new ArrayList<IXtextModelListener>(); // unfortunately had to duplicate
    private volatile NonExpiringSnapshot reconciledSnapshot;
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
    }

    @Override
    public void setInput(XtextResource resource)
    {
        super.setInput(resource);
        reconciledSnapshot = getNonExpiringSnapshot(); // initial snapshot
        addDocumentListener(selfListener);
    }

    @Override
    public void disposeInput()
    {
        removeDocumentListener(selfListener);
        getAndResetPendingChange();
        reconciledSnapshot = null;
        reconcilingListeners.clear();
        synchronized (modelListeners2)
        {
            modelListeners2.clear(); // fixes issue with holding XtextEditor from disposed document (see DefaultFoldingStructureProvider)
        }
        Job validationJob = getValidationJob();
        if (validationJob != null)
            validationJob.cancel();
        setValidationJob(null);
        detachResource(); // unfortunately can't just set resource to null
    }

    @Override
    public ISnapshot getSnapshot()
    {
        return new DocumentSnapshot(this);
    }

    @Override
    public ISnapshot getReconciledSnapshot()
    {
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
    public void addModelListener(IXtextModelListener listener)
    {
        Assert.isNotNull(listener);
        synchronized (modelListeners2)
        {
            if (modelListeners2.contains(listener))
                return;
            if (listener instanceof DirtyStateEditorSupport)
                modelListeners2.add(0, listener); // DirtyStateEditorSupport must be the first ModelChangelistener to be called, since it updates the resource descriptions used by subsequent listeners
            else
                modelListeners2.add(listener);
        }
    }

    @Override
    public void removeModelListener(IXtextModelListener listener)
    {
        Assert.isNotNull(listener);
        synchronized (modelListeners2)
        {
            modelListeners2.remove(listener);
        }
    }

    @Override
    public boolean needsReconciling()
    {
        return !getSnapshot().isEqualTo(getReconciledSnapshot());
    }

    @Override
    public boolean reconcile(boolean force)
    {
        return reconcile(force, null);
    }

    /**
     * Reparses the resource so it becomes reconciled with the document contents.
     * Does nothing if already reconciled and <code>force == false</code>.
     * For internal use only.
     *
     * @param force indicates whether reconciling has to be performed
     *  even if it is not {@link #needsReconciling() needed}
     * @param processor the processor to execute the reconciling unit of work
     *  or <code>null</code> if no special processor is needed
     * @return <code>true</code> if the document had any changes to be reconciled,
     *   <code>false</code> otherwise
     */
    public boolean reconcile(boolean force, Processor processor)
    {
        T2MReconcilingUnitOfWork reconcilingUnitOfWork =
            new T2MReconcilingUnitOfWork(force);
        if (processor != null)
            return processor.process(reconcilingUnitOfWork);
        else
            return internalModify(reconcilingUnitOfWork);
    }

    @Override
    public <T> T readOnly(IUnitOfWork<T, XtextResource> work)
    {
        return super.readOnly(work);
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
        DocumentChangeOperation operation =
            new DocumentChangeOperation(this, change);
        UiDocumentChangeRunner runner =
            new UiDocumentChangeRunner(UiSynchronizer.getInstance(), operation);
        return runner.run();
    }

    @Override
    protected void notifyModelListeners(XtextResource resource)
    {
        List<IXtextModelListener> modelListenersCopy;
        synchronized (modelListeners2)
        {
            modelListenersCopy =
                new ArrayList<IXtextModelListener>(modelListeners2);
        }
        for (IXtextModelListener listener : modelListenersCopy)
        {
            try
            {
                listener.modelChanged(resource);
            }
            catch (Exception e)
            {
                Activator.log(Activator.createErrorStatus(
                    "Error in IXtextModelListener", e)); //$NON-NLS-1$
            }
        }
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
     * Notifies reconciling listerners (if any). Should only be called
     * in the dynamic context of {@link XtextDocument#internalModify}.
     *
     * @param resource the reconciled resource - must not be <code>null</code>
     * @param snapshot the reconciled snapshot - must not be <code>null</code>
     * @param forced whether reconciling was forced, i.e. the document has not
     *  changed since it was reconciled the last time
     */
    private void reconciled(final XtextResource resource,
        final NonExpiringSnapshot snapshot, final boolean forced)
    {
        postProcess(resource, new CancelIndicatorBasedProgressMonitor(
            getOutdatedStateCancelIndicator(resource)));
        Object[] listeners = reconcilingListeners.getListeners();
        for (final Object listener : listeners)
        {
            SafeRunner.run(new ISafeRunnable()
            {
                @Override
                public void run() throws Exception
                {
                    ((IReconcilingListener)listener).reconciled(resource,
                        snapshot, forced);
                }

                @Override
                public void handleException(Throwable exception)
                {
                }
            });
        }
        reconciledSnapshot = snapshot;
    }

    // initially copied from XtextDocumentReconcileStrategy#postParse
    private void postProcess(XtextResource resource,
        final IProgressMonitor monitor)
    {
        if (dirtyStateEditorSupport != null)
            dirtyStateEditorSupport.announceDirtyState(resource);
        CancelIndicator cancelIndicator = new CancelIndicator()
        {
            public boolean isCanceled()
            {
                return monitor.isCanceled();
            }
        };
        try
        {
            if (resource instanceof DerivedStateAwareResource)
                ((DerivedStateAwareResource)resource).installDerivedState(false);
            if (resource instanceof IBatchLinkableResource)
            {
                ((IBatchLinkableResource)resource).linkBatched(cancelIndicator);
            }
        }
        catch (OperationCanceledException e)
        {
            resource.getCache().clear(resource);
        }
        catch (RuntimeException e)
        {
            Activator.log(Activator.createErrorStatus(
                "Error post-processing resource", e)); //$NON-NLS-1$
        }
    }

    private void internalReconcile(XtextResource resource, boolean force)
        throws Exception
    {
        reconcile(force, new InternalProcessor(resource));
    }

    private void detachResource()
    {
        internalModify(new IUnitOfWork.Void<XtextResource>()
        {
            @Override
            public void process(XtextResource resource) throws Exception
            {
                clearInternalState(resource);
                resource.getResourceSet().getResources().clear();
            }
        });
    }

    private static void clearInternalState(XtextResource resource)
        throws Exception
    {
        // Ensure that the resource's derived state (if any) is discarded
        // and that clear doesn't make derived state to be installed
        Field isUpdating = XtextResource.class.getDeclaredField("isUpdating"); //$NON-NLS-1$
        isUpdating.setAccessible(true);
        Method clearInternalState =
            XtextResource.class.getDeclaredMethod("clearInternalState"); //$NON-NLS-1$
        clearInternalState.setAccessible(true);
        try
        {
            isUpdating.set(resource, true);
            clearInternalState.invoke(resource);
        }
        finally
        {
            isUpdating.set(resource, false);
        }
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
         * An exception thrown by this method will be logged and suppressed.
         * </p>
         *
         * @param resource the reconciled resource - must not be <code>null</code>
         * @param snapshot the reconciled snapshot - must not be <code>null</code>
         * @param forced whether reconciling was forced, i.e. the document
         *  has not changed since it was reconciled the last time
         */
        void reconciled(XtextResource resource, NonExpiringSnapshot snapshot,
            boolean forced);
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
            ReplaceRegion replaceRegion =
                new ReplaceRegion(event.getOffset(), event.getLength(),
                    event.getText());
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

        @Override
        public Boolean exec(XtextResource resource) throws Exception
        {
            PendingChange change = getAndResetPendingChange();
            if (change == null)
            {
                if (force) // reconciling is forced
                {
                    NonExpiringSnapshot snapshot = reconciledSnapshot;
                    // no need to reparse -- just update internal state
                    resource.update(0, 0, ""); //$NON-NLS-1$
                    reconciled(resource, snapshot, true);
                }
                return false;
            }
            else
            {
                NonExpiringSnapshot snapshot = change.getSnapshotToReconcile();
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
                    Activator.log(Activator.createErrorStatus(e.getMessage(), e));
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
                reconciled(resource, snapshot, false);
                return true;
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
            hasTopLevelModification.set(true);
            try
            {
                internalReconcile(resource, false); // ensure a fresh base snapshot
                baseSnapshot = getReconciledSnapshot();
                T result;
                try
                {
                    // resolve all proxies before model modification
                    // (otherwise, proxy resolution might throw exceptions
                    // due to inconsistency between 'changed' model and
                    // 'old' proxy URIs)
                    EcoreUtil2.resolveLazyCrossReferences(resource,
                        CancelIndicator.NullImpl);

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
                            ((IUndoableUnitOfWork<T, XtextResource>)work).acceptUndoChange(undoChange);
                    }
                }
                catch (Exception e)
                {
                    // modification failed - restore state
                    Activator.log(Activator.createErrorStatus(e.getMessage(), e));
                    resource.reparse(reconciledSnapshot.getContents());
                    throw e;
                }
                internalReconcile(resource, false); // reconcile resource with changed document
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
}
