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

import org.eclipse.handly.document.IDocumentChange;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.snapshot.ISnapshotProvider;
import org.eclipse.handly.snapshot.StaleSnapshotException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

/**
 * Handly extension interface for {@link IXtextDocument}.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IHandlyXtextDocument
    extends IXtextDocument, ISnapshotProvider
{
    /**
     * Returns whether the document has changed since the last time it was 
     * reconciled.
     * 
     * @return <code>true</code> if the document has changed since it was 
     *  last reconciled or if can't tell for sure, <code>false</code> otherwise
     */
    boolean needsReconciling();

    /**
     * Reparses the resource so it becomes reconciled with the document contents. 
     * Does nothing if already reconciled and <code>force == false</code>.
     *
     * @param force indicates whether reconciling has to be performed 
     *  even if it is not {@link #needsReconciling() needed}
     */
    void reconcile(boolean force);

    /**
     * Returns the snapshot from which the document's resource was parsed 
     * in the last {@link #reconcile(boolean) reconcile} operation. 
     * Note that this snapshot may turn out to be stale. 
     * 
     * @return the last reconciled snapshot (never <code>null</code>)
     */
    ISnapshot getReconciledSnapshot();

    /**
     * Executes the given unit of work under the (shared) read lock. 
     * The unit of work <b>must not</b> modify the resource or return 
     * any references to it. Read-only units of work may nest into each other. 
     * <p>
     * While inside the dynamic context of the unit of work, 
     * the resource contents is guaranteed to be based on the {@link 
     * #getReconciledSnapshot() last reconciled snapshot}. Usually, 
     * there is no need for clients to do a {@link #reconcile(boolean) reconcile} 
     * before calling this method to ensure a fresh snapshot. Reconciling 
     * will happen automatically before the top-level unit of work is run.
     * </p>
     * 
     * @param work a read-only unit of work - must not be <code>null</code>
     * @return the unit of work's result - may be <code>null</code>. 
     *  Must not contain any references to the resource or its contents 
     *  (both semantic objects and parse tree nodes)
     */
    @Override
    <T> T readOnly(IUnitOfWork<T, XtextResource> work);

    /**
     * Executes the given unit of work under the (exclusive) write lock. 
     * The unit of work may modify the resource but <b>must not</b> return 
     * any references to it.
     * <p>
     * Automatically updates the document contents so it is reconciled with 
     * the resource modifications performed by the unit of work. 
     * Units of work may be nested - changes are only applied to the document 
     * after successful completion of the top-level modification, 
     * i.e. when all the work is done. If the top-level unit of work is 
     * {@link IUndoableUnitOfWork undoable}, it will be {@link 
     * IUndoableUnitOfWork#acceptUndoChange(IDocumentChange) notified} 
     * of an undo change that can be later {@link #applyChange(IDocumentChange) 
     * applied} to the document to revert modifications made by 
     * the whole transaction.
     * </p>
     * <p>
     * Before the top-level unit of work is run, the resource contents 
     * is guaranteed to be based on the {@link #getReconciledSnapshot() 
     * last reconciled snapshot}. That snapshot is regarded as the <i>base</i> 
     * snapshot for the whole transaction, i.e. if it turns out to be stale 
     * when changes are to be applied to the document, this method will throw 
     * {@link StaleSnapshotException}. Usually, there is no need for clients 
     * to do a {@link #reconcile(boolean) reconcile} before calling this method 
     * to ensure a fresh snapshot. Reconciling will happen automatically 
     * before the top-level unit of work is run.
     * </p>
     * 
     * @param work a modifying unit of work - must not be <code>null</code> 
     * @return the unit of work's result - may be <code>null</code>. 
     *  Must not contain any references to the resource or its contents 
     *  (both semantic objects and parse tree nodes)
     * @throws StaleSnapshotException if the document's contents have changed 
     *  since the inception of the transaction's base snapshot
     */
    @Override
    <T> T modify(IUnitOfWork<T, XtextResource> work);

    /**
     * Registers the modification listener with the document. 
     * Does nothing if the given listener is already registered.
     *
     * @param listener must not be <code>null</code>
     * 
     * @deprecated This method will be removed in the Handly 0.2 release.
     */
    void addModificationListener(IModificationListener listener);

    /**
     * Removes the listener from the document's list of modification listeners.
     * If the listener is not registered with the document nothing happens.
     *
     * @param listener must not be <code>null</code>
     * 
     * @deprecated This method will be removed in the Handly 0.2 release.
     */
    void removeModificationListener(IModificationListener listener);

    /**
     * Applies the given change to the document. Note that an update conflict 
     * may occur if the document's contents have changed since the inception 
     * of the snapshot on which the change is based. In that case, a 
     * {@link StaleSnapshotException} is thrown. 
     *
     * @param change a document change - must not be <code>null</code>
     * @return undo change, if requested. Otherwise, <code>null</code>
     * @throws StaleSnapshotException if the document's contents have changed 
     *  since the inception of the snapshot on which the change is based
     * @throws MalformedTreeException if the change's edit tree isn't 
     *  in a valid state
     * @throws BadLocationException if one of the edits in the tree 
     *  can't be executed 
     */
    IDocumentChange applyChange(IDocumentChange change)
        throws BadLocationException;

    /**
     * An undoable <b>top-level</b> unit of work will be {@link 
     * #acceptUndoChange(IDocumentChange) notified} of an undo change 
     * just after modifications made by the whole transaction 
     * have been applied to the document. This change can be later 
     * {@link IHandlyXtextDocument#applyChange(IDocumentChange) used} 
     * to revert those modifications. 
     */
    interface IUndoableUnitOfWork<R, P>
        extends IUnitOfWork<R, P>
    {
        /**
         * Notification of an undo change.
         *
         * @param undoChange never <code>null</code>
         */
        void acceptUndoChange(IDocumentChange undoChange);
    }

    /**
     * Marker interface for read-only units of work which don't require 
     * reconciling before execution.
     * 
     * @deprecated This interface will be removed in the Handly 0.2 release.
     */
    interface IStraightReadingUnitOfWork<R, P>
        extends IUnitOfWork<R, P>
    {
    }

    /**
     * Document modification listener protocol. Clients should extend from 
     * {@link NullImpl} rather than implementing this interface directly.
     * 
     * @deprecated This interface will be removed in the Handly 0.2 release.
     */
    interface IModificationListener
    {
        /**
         * Called before the given modification unit of work is going to be 
         * performed. In particular, called before a write lock is obtained.
         */
        void aboutToModify(IUnitOfWork<?, XtextResource> work);

        /**
         * Implements all methods of {@link IModificationListener} as no-op.
         */
        class NullImpl
            implements IModificationListener
        {
            @Override
            public void aboutToModify(IUnitOfWork<?, XtextResource> work)
            {
            }
        }
    }
}
