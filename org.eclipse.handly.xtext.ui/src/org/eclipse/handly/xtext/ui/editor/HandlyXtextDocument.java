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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EObject;
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
import org.eclipse.xtext.parser.antlr.IReferableElementsUnloader;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.model.DocumentTokenSource;
import org.eclipse.xtext.ui.editor.model.IXtextDocumentContentObserver.Processor;
import org.eclipse.xtext.ui.editor.model.IXtextModelListener;
import org.eclipse.xtext.ui.editor.model.XtextDocument;
import org.eclipse.xtext.ui.editor.model.edit.ITextEditComposer;
import org.eclipse.xtext.ui.editor.reconciler.ReplaceRegion;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import com.google.inject.Inject;

/**
 * Extends {@link XtextDocument} for our reconciling story. 
 * Implements {@link IHandlyXtextDocument}.
 * 
 * @noextend This class is not indended to be extended by clients.
 */
public class HandlyXtextDocument
    extends XtextDocument
    implements IHandlyXtextDocument
{
    private final BooleanThreadLocal hasTopLevelModification =
        new BooleanThreadLocal();

    private ITextEditComposer composer2; // unfortunately had to duplicate
    private final ListenerList modelListeners2 = new ListenerList(
        ListenerList.IDENTITY); // unfortunately had to duplicate
    private NonExpiringSnapshot reconciledSnapshot;
    private final ListenerList reconcilingListeners = new ListenerList(
        ListenerList.IDENTITY);
    private final DocumentListener selfListener = new DocumentListener();
    private PendingChange pendingChange;
    private final Object pendingChangeLock = new Object();
    private HandlyXtextDocumentLocker locker;

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
        modelListeners2.clear(); // fixes issue with holding XtextEditor from disposed document (see DefaultFoldingStructureProvider)
        locker.dispose();
        setValidationJob(null);
        detachResource(); // unfortunately can't just set resource to null
        super.disposeInput();
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
    public void addModificationListener(IModificationListener listener)
    {
        locker.addModificationListener(listener);
    }

    @Override
    public void removeModificationListener(IModificationListener listener)
    {
        locker.removeModificationListener(listener);
    }

    @Override
    public void addModelListener(IXtextModelListener listener)
    {
        modelListeners2.add(listener);
    }

    @Override
    public void removeModelListener(IXtextModelListener listener)
    {
        modelListeners2.remove(listener);
    }

    @Override
    public boolean needsReconciling()
    {
        return hasPendingChange();
    }

    @Override
    public void reconcile(boolean force)
    {
        reconcile(force, null);
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
     */
    public void reconcile(boolean force, Processor processor)
    {
        if (!hasPendingChange() && !force)
            return;

        T2MReconcilingUnitOfWork reconcilingUnitOfWork =
            new T2MReconcilingUnitOfWork(force);
        if (processor != null)
            processor.process(reconcilingUnitOfWork);
        else
            internalModify(reconcilingUnitOfWork);
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
            new UiDocumentChangeRunner(UiSynchronizer.DEFAULT, operation);
        return runner.run();
    }

    @Override
    protected XtextDocumentLocker createDocumentLocker()
    {
        return (locker = new HandlyXtextDocumentLocker());
    }

    @Override
    protected void notifyModelListeners(XtextResource resource)
    {
        Object[] listeners = modelListeners2.getListeners();
        for (int i = 0; i < listeners.length; i++)
        {
            ((IXtextModelListener)listeners[i]).modelChanged(resource);
        }
    }

    private boolean hasPendingChange()
    {
        synchronized (pendingChangeLock)
        {
            return pendingChange != null;
        }
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
        try
        {
            return new NonExpiringSnapshot(this);
        }
        catch (CoreException e) // cannot happen
        {
            throw new AssertionError(e);
        }
    }

    /*
     * Called just after a reconciling operation has been performed. Informs 
     * that the document's XtextResource contents is based on the given snapshot. 
     * Notifies reconciling listerners (if any). Should only be called 
     * in the dynamic context of {@link XtextDocument#internalModify}. 
     *
     * @param snapshot the reconciled snapshot - must not be <code>null</code>
     * @param forced whether reconciling was forced, i.e. the document has not 
     *  changed since it was reconciled the last time
     */
    private void reconciled(final NonExpiringSnapshot snapshot,
        final boolean forced)
    {
        reconciledSnapshot = snapshot;

        locker.notify(new IUnitOfWork.Void<XtextResource>()
        {
            @Override
            public void process(final XtextResource resource) throws Exception
            {
                Object[] listeners = reconcilingListeners.getListeners();
                for (final Object listener : listeners)
                {
                    SafeRunner.run(new ISafeRunnable()
                    {
                        @Override
                        public void run() throws Exception
                        {
                            ((IReconcilingListener)listener).reconciled(
                                resource, snapshot, forced);
                        }

                        @Override
                        public void handleException(Throwable exception)
                        {
                        }
                    });
                }
            }
        });
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
                IReferableElementsUnloader unloader = resource.getUnloader();
                if (unloader != null)
                {
                    for (EObject content : resource.getContents())
                    {
                        unloader.unloadRoot(content);
                    }
                }
                resource.getContents().clear();
                resource.getErrors().clear();
                resource.getWarnings().clear();
                resource.setParseResult(null);
                resource.getResourceSet().getResources().clear();
            }
        });
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

        public NonExpiringSnapshot getSnapshotToReconcile()
        {
            return snapshotToReconcile;
        }

        public ReplaceRegion getReplaceRegionToReconcile()
        {
            return replaceRegionToReconcile;
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
        }
    }

    /*
     * Should only be called when the document's write lock is held.
     */
    private class T2MReconcilingUnitOfWork
        implements IUnitOfWork<Object, XtextResource>
    {
        private final boolean force;

        public T2MReconcilingUnitOfWork(boolean force)
        {
            this.force = force;
        }

        @Override
        public Object exec(XtextResource resource) throws Exception
        {
            PendingChange change = getAndResetPendingChange();
            if (change == null)
            {
                if (force) // reconciling is forced
                {
                    NonExpiringSnapshot snapshot = reconciledSnapshot;
                    resource.reparse(snapshot.getContents());
                    reconciled(snapshot, true);
                }
            }
            else
            {
                NonExpiringSnapshot snapshot = change.getSnapshotToReconcile();
                ReplaceRegion replaceRegion =
                    change.getReplaceRegionToReconcile();
                boolean reconciled = false;
                try
                {
                    resource.update(replaceRegion.getOffset(),
                        replaceRegion.getLength(), replaceRegion.getText());
                    reconciled = true;
                }
                catch (Exception e)
                {
                    // partial parsing failed - performing full reparse
                    Activator.log(Activator.createErrorStatus(e.getMessage(), e));
                    try
                    {
                        resource.reparse(snapshot.getContents());
                        reconciled = true;
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
                if (reconciled)
                    reconciled(snapshot, false);
            }
            return null;
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

    private class HandlyXtextDocumentLocker
        extends XtextDocumentLocker
    {
        private final ListenerList modificationListeners = new ListenerList(
            ListenerList.IDENTITY);

        /** 
         * Expects that the current thread holds one or more write locks 
         * and no read locks. Downgrades the write lock to the read lock, 
         * executes the given unit of work, then regains the write lock. 
         * Note that while regaining the write lock (i.e. after the read 
         * lock has been released and before the write lock is obtained) 
         * the resource may get modified.
         */
        public void notify(IUnitOfWork.Void<XtextResource> notifyingUoW)
        {
            int writeHoldCount = downgradeWriteLock();
            try
            {
                try
                {
                    notifyingUoW.exec(getState());
                }
                catch (RuntimeException e)
                {
                    throw e;
                }
                catch (Exception e)
                {
                    throw new WrappedException(e);
                }
                finally
                {
                    Job validationJob = getValidationJob();
                    if (validationJob != null)
                    {
                        validationJob.cancel();
                    }
                }
            }
            finally
            {
                regainWriteLock(writeHoldCount);
            }
        }

        @Override
        public <T> T modify(IUnitOfWork<T, XtextResource> work)
        {
            aboutToModify(work);
            return super.modify(work);
        }

        public void addModificationListener(IModificationListener listener)
        {
            modificationListeners.add(listener);
        }

        public void removeModificationListener(IModificationListener listener)
        {
            modificationListeners.remove(listener);
        }

        public void dispose()
        {
            modificationListeners.clear();
        }

        @Override
        protected void beforeReadOnly(XtextResource resource,
            IUnitOfWork<?, XtextResource> work)
        {
            if (rwLock.getReadHoldCount() == 1
                && !rwLock.isWriteLockedByCurrentThread())
            {
                if (!(work instanceof IStraightReadingUnitOfWork))
                    updateContentBeforeRead(); // reconcile
            }
        }

        private int downgradeWriteLock()
        {
            if (rwLock.getWriteHoldCount() == 0)
                throw new IllegalStateException();
            if (rwLock.getReadHoldCount() > 0)
                throw new IllegalStateException();
            readLock.lock();
            int count = rwLock.getWriteHoldCount();
            for (int i = 0; i < count; i++)
                writeLock.unlock();
            return count;
        }

        private void regainWriteLock(int count)
        {
            if (count <= 0)
                throw new IllegalArgumentException();
            if (rwLock.getWriteHoldCount() > 0)
                throw new IllegalStateException();
            if (rwLock.getReadHoldCount() != 1)
                throw new IllegalStateException();
            readLock.unlock();
            // -----> NOTE: resource is unlocked and may get modified here
            for (int i = 0; i < count; i++)
                writeLock.lock();
        }

        private void aboutToModify(final IUnitOfWork<?, XtextResource> work)
        {
            Object[] listeners = modificationListeners.getListeners();
            for (final Object listener : listeners)
            {
                SafeRunner.run(new ISafeRunnable()
                {
                    @Override
                    public void run() throws Exception
                    {
                        ((IModificationListener)listener).aboutToModify(work);
                    }

                    @Override
                    public void handleException(Throwable exception)
                    {
                    }
                });
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
