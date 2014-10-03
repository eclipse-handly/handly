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
package org.eclipse.handly.ui.texteditor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.handly.buffer.BufferChangeOperation;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.buffer.IBufferChange;
import org.eclipse.handly.buffer.IDocumentBuffer;
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
 * Implements {@link IBuffer} on top of a {@link ITextEditor}.
 */
public class TextEditorBuffer
    implements IDocumentBuffer
{
    private volatile boolean closed;
    protected final IEditorInput editorInput;
    protected final IDocumentProvider documentProvider;
    protected final IDocument document;

    /**
     * Creates a new buffer instance and connects it to the given text editor. 
     * It is the client responsibility to {@link #dispose() close} the buffer 
     * after it is no longer needed.
     * 
     * @param editor the text editor (not <code>null</code>)
     * @throws CoreException if the buffer could not be connected
     */
    public TextEditorBuffer(ITextEditor editor) throws CoreException
    {
        if (editor == null)
            throw new IllegalArgumentException();
        if ((this.editorInput = editor.getEditorInput()) == null)
            throw new IllegalArgumentException();
        if ((this.documentProvider = editor.getDocumentProvider()) == null)
            throw new IllegalArgumentException();
        documentProvider.connect(editorInput);
        document = documentProvider.getDocument(editorInput);
    }

    @Override
    public IDocument getDocument()
    {
        checkNotClosed();
        return document;
    }

    @Override
    public ISnapshot getSnapshot()
    {
        checkNotClosed();
        return new DocumentSnapshot(document);
    }

    @Override
    public IBufferChange applyChange(IBufferChange change, IProgressMonitor pm)
        throws CoreException
    {
        checkNotClosed();
        if (pm == null)
            pm = new NullProgressMonitor();
        try
        {
            final UiBufferChangeRunner runner =
                new UiBufferChangeRunner(UiSynchronizer.getInstance(),
                    createChangeOperation(change));
            return runner.run(pm);
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
        checkNotClosed();
        document.set(contents);
    }

    @Override
    public boolean hasUnsavedChanges()
    {
        checkNotClosed();
        return documentProvider.canSaveDocument(editorInput);
    }

    @Override
    public boolean mustSaveChanges()
    {
        checkNotClosed();
        return documentProvider.mustSaveDocument(editorInput);
    }

    @Override
    public void save(boolean overwrite, IProgressMonitor pm)
        throws CoreException
    {
        checkNotClosed();
        documentProvider.saveDocument(pm, editorInput, document, overwrite);
    }

    @Override
    public void dispose()
    {
        checkNotClosed();
        closed = true;
        documentProvider.disconnect(editorInput);
    }

    @Override
    public int hashCode()
    {
        return document.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TextEditorBuffer other = (TextEditorBuffer)obj;
        return (document == other.document);
    }

    protected boolean isClosed()
    {
        return closed;
    }

    protected void checkNotClosed()
    {
        if (isClosed())
            throw new IllegalStateException("the buffer has been closed"); //$NON-NLS-1$
    }

    protected BufferChangeOperation createChangeOperation(IBufferChange change)
    {
        return new BufferChangeOperation(this, change);
    }
}
