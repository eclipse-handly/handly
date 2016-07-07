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
package org.eclipse.handly.document;

import java.util.concurrent.ExecutionException;

import org.eclipse.handly.snapshot.StaleSnapshotException;
import org.eclipse.handly.util.UiSynchronizer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Executes a document change in the UI thread.
 */
public final class UiDocumentChangeRunner
{
    private final UiSynchronizer synchronizer;
    private final DocumentChangeOperation operation;

    /**
     * Creates a new runner capable of executing the given document change
     * operation in the UI thread.
     *
     * @param synchronizer used to execute operation in the UI thread
     *  - must not be <code>null</code>
     * @param operation a document change operation
     *  - must not be <code>null</code>
     */
    public UiDocumentChangeRunner(UiSynchronizer synchronizer,
        DocumentChangeOperation operation)
    {
        if ((this.synchronizer = synchronizer) == null)
            throw new IllegalArgumentException();
        if ((this.operation = operation) == null)
            throw new IllegalArgumentException();
    }

    /**
     * Synchronously executes the change to the document in the UI thread.
     * Note that an update conflict may occur if the document's contents have
     * changed since the inception of the snapshot on which the change is based.
     * In that case, a {@link StaleSnapshotException} is thrown.
     *
     * @return undo change, if requested. Otherwise, <code>null</code>
     * @throws StaleSnapshotException if the document has changed
     *  since the inception of the snapshot on which the change is based
     * @throws MalformedTreeException if the change's edit tree isn't
     *  in a valid state
     * @throws BadLocationException if one of the edits in the tree
     *  can't be executed
     */
    public IDocumentChange run() throws BadLocationException
    {
        final Thread callerThread = Thread.currentThread();
        Thread synchronizerThread = synchronizer.getThread();
        if (callerThread.equals(synchronizerThread))
            return operation.execute();

        final IDocumentChange[] undoChange = new IDocumentChange[1];
        final Throwable[] exception = new Throwable[1];
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    undoChange[0] = operation.execute();
                }
                catch (Throwable e)
                {
                    exception[0] = e;
                }
            }
        };

        try
        {
            synchronizer.syncExec(runnable);
        }
        catch (ExecutionException e)
        {
            // using exception[0] is a more robust way of detecting execution exception
            // (org.eclipse.ui.internal.UISynchronizer.syncExec() doesn't always
            // propagate exceptions from the UI thread to the calling thread)
        }

        if (exception[0] != null)
        {
            Throwable e = exception[0];
            if (e instanceof RuntimeException)
                throw (RuntimeException)e;
            else if (e instanceof Error)
                throw (Error)e;
            else if (e instanceof BadLocationException)
                throw (BadLocationException)e;
            else
                throw new AssertionError(e);
        }

        return undoChange[0];
    }
}
