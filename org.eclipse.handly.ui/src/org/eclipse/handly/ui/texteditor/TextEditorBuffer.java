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
package org.eclipse.handly.ui.texteditor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.handly.buffer.BufferChangeOperation;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.buffer.IBufferChange;
import org.eclipse.handly.buffer.UiBufferChangeRunner;
import org.eclipse.handly.internal.ui.Activator;
import org.eclipse.handly.snapshot.DocumentSnapshot;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.util.UiSynchronizer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Implementation of {@link IBuffer} backed by an {@link ITextEditor}.
 * <p>
 * An instance of this class is safe for use by multiple threads,
 * provided that the underlying text editor's document is thread-safe.
 * However, certain operations can only be executed by the UI thread:
 * </p>
 * <ul>
 * <li><code>hasUnsavedChanges</code></li>
 * <li><code>mustSaveChanges</code></li>
 * <li><code>save</code></li>
 * </ul>
 */
public class TextEditorBuffer
    implements IBuffer
{
    private final UiSynchronizer uiSynchronizer;
    private final IEditorInput editorInput;
    private final IDocumentProvider documentProvider;
    private IDocument document;
    private int refCount = 1;

    /**
     * Creates a new buffer instance and connects it to the given text editor.
     * <p>
     * It is the client responsibility to {@link IBuffer#release() release}
     * the created buffer after it is no longer needed.
     * </p>
     * <p>
     * This constructor can only be executed by the UI thread.
     * </p>
     *
     * @param editor the text editor (not <code>null</code>)
     * @throws CoreException if the buffer could not be connected
     */
    public TextEditorBuffer(ITextEditor editor) throws CoreException
    {
        if (editor == null)
            throw new IllegalArgumentException();
        if ((this.uiSynchronizer = UiSynchronizer.getDefault()) == null)
            throw new AssertionError();
        if ((this.editorInput = editor.getEditorInput()) == null)
            throw new IllegalArgumentException();
        checkThread();
        if ((this.documentProvider = editor.getDocumentProvider()) == null)
            throw new IllegalArgumentException();
        documentProvider.connect(editorInput);
        if ((this.document = documentProvider.getDocument(editorInput)) == null)
            throw new AssertionError();
    }

    @Override
    public IDocument getDocument()
    {
        IDocument result = document;
        if (result == null)
            throw new IllegalStateException(
                "Attempt to access a disconnected TextEditorBuffer for " //$NON-NLS-1$
                    + editorInput);
        return result;
    }

    @Override
    public ISnapshot getSnapshot()
    {
        return new DocumentSnapshot(getDocument());
    }

    @Override
    public IBufferChange applyChange(IBufferChange change,
        IProgressMonitor monitor) throws CoreException
    {
        if (monitor == null)
            monitor = new NullProgressMonitor();
        try
        {
            UiBufferChangeRunner runner = new UiBufferChangeRunner(
                uiSynchronizer, new BufferChangeOperation(this, change));
            return runner.run(monitor);
        }
        catch (MalformedTreeException e)
        {
            throw new CoreException(Activator.createErrorStatus(e.getMessage(),
                e));
        }
        catch (BadLocationException e)
        {
            throw new CoreException(Activator.createErrorStatus(e.getMessage(),
                e));
        }
    }

    @Override
    public void setContents(String contents)
    {
        getDocument().set(contents);
    }

    @Override
    public String getContents()
    {
        return getDocument().get();
    }

    @Override
    public boolean hasUnsavedChanges()
    {
        checkThread();
        return documentProvider.canSaveDocument(editorInput);
    }

    @Override
    public boolean mustSaveChanges()
    {
        checkThread();
        return documentProvider.mustSaveDocument(editorInput);
    }

    @Override
    public void save(boolean overwrite, IProgressMonitor monitor)
        throws CoreException
    {
        checkThread();
        documentProvider.saveDocument(monitor, editorInput, getDocument(),
            overwrite);
    }

    @Override
    public synchronized void addRef()
    {
        ++refCount;
    }

    @Override
    public synchronized void release()
    {
        if (--refCount == 0 && document != null)
        {
            document = null;
            uiSynchronizer.asyncExec(new Runnable()
            {
                public void run()
                {
                    documentProvider.disconnect(editorInput);
                }
            });
        }
    }

    private void checkThread()
    {
        if (!Thread.currentThread().equals(uiSynchronizer.getThread()))
            throw new IllegalStateException(
                "Invalid thread access to TextEditorBuffer for " //$NON-NLS-1$
                    + editorInput);
    }
}
