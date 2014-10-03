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
package org.eclipse.handly.buffer;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.handly.internal.Activator;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.snapshot.TextFileBufferSnapshot;
import org.eclipse.handly.util.UiSynchronizer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Implements {@link IBuffer} on top of a {@link ITextFileBuffer}.
 */
public class TextFileBuffer
    implements IDocumentBuffer
{
    private volatile boolean closed;
    protected final IFile file;
    protected final ITextFileBufferManager bufferManager;
    protected final ITextFileBuffer buffer;

    /**
     * Creates a new buffer instance and connects it to the underlying {@link 
     * ITextFileBuffer} for the given file. It is the client responsibility 
     * to {@link #dispose() close} the buffer after it is no longer needed.
     * 
     * @param file the text file (not <code>null</code>)
     * @param bufferManager the manager of the underlying file buffer
     *  (not <code>null</code>)
     * @param monitor a progress monitor to report progress, 
     *  or <code>null</code> if no progress reporting is desired
     * @throws CoreException if the buffer could not be connected
     */
    public TextFileBuffer(IFile file, ITextFileBufferManager bufferManager,
        IProgressMonitor monitor) throws CoreException
    {
        if ((this.file = file) == null)
            throw new IllegalArgumentException();
        if ((this.bufferManager = bufferManager) == null)
            throw new IllegalArgumentException();
        bufferManager.connect(file.getFullPath(), LocationKind.IFILE, monitor);
        buffer =
            bufferManager.getTextFileBuffer(file.getFullPath(),
                LocationKind.IFILE);
    }

    @Override
    public IDocument getDocument()
    {
        checkNotClosed();
        return buffer.getDocument();
    }

    @Override
    public ISnapshot getSnapshot()
    {
        checkNotClosed();
        return new TextFileBufferSnapshot(buffer, bufferManager);
    }

    @Override
    public IBufferChange applyChange(IBufferChange change,
        IProgressMonitor monitor) throws CoreException
    {
        checkNotClosed();
        if (monitor == null)
            monitor = new NullProgressMonitor();
        try
        {
            BufferChangeOperation operation = createChangeOperation(change);
            if (!buffer.isSynchronizationContextRequested())
                return operation.execute(monitor);

            final UiBufferChangeRunner runner =
                new UiBufferChangeRunner(UiSynchronizer.getInstance(),
                    operation);
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
        checkNotClosed();
        buffer.getDocument().set(contents);
    }

    @Override
    public boolean hasUnsavedChanges()
    {
        checkNotClosed();
        return buffer.isDirty();
    }

    @Override
    public boolean mustSaveChanges()
    {
        checkNotClosed();
        return buffer.isDirty() && !buffer.isShared();
    }

    @Override
    public void save(boolean overwrite, IProgressMonitor monitor)
        throws CoreException
    {
        checkNotClosed();
        buffer.commit(monitor, overwrite);
    }

    @Override
    public void dispose()
    {
        checkNotClosed();
        closed = true;
        try
        {
            bufferManager.disconnect(file.getFullPath(), LocationKind.IFILE,
                null);
        }
        catch (CoreException e)
        {
            Activator.log(e.getStatus());
        }
    }

    @Override
    public int hashCode()
    {
        return buffer.hashCode();
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
        TextFileBuffer other = (TextFileBuffer)obj;
        return (buffer == other.buffer);
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
