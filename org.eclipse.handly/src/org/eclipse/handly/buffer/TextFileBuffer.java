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
 * Implementation of {@link IBuffer} backed by an {@link ITextFileBuffer}.
 * <p>
 * An instance of this class is safe for use by multiple threads, provided that
 * the underlying <code>ITextFileBuffer</code> and its document are thread-safe.
 * </p>
 */
public class TextFileBuffer
    implements IBuffer
{
    private final IFile file;
    private final ITextFileBufferManager bufferManager;
    private ITextFileBuffer delegate;
    private int refCount = 1;

    /**
     * Creates a new buffer instance and connects it to the underlying {@link
     * ITextFileBuffer} for the given file.
     * <p>
     * It is the client responsibility to {@link IBuffer#release() release}
     * the created buffer after it is no longer needed.
     * </p>
     *
     * @param file the text file (not <code>null</code>)
     * @param bufferManager the manager of the underlying file buffer
     *  (not <code>null</code>)
     * @throws CoreException if the buffer could not be connected
     */
    public TextFileBuffer(IFile file, ITextFileBufferManager bufferManager)
        throws CoreException
    {
        if ((this.file = file) == null)
            throw new IllegalArgumentException();
        if ((this.bufferManager = bufferManager) == null)
            throw new IllegalArgumentException();
        bufferManager.connect(file.getFullPath(), LocationKind.IFILE, null);
        if ((this.delegate = bufferManager.getTextFileBuffer(file.getFullPath(),
            LocationKind.IFILE)) == null)
            throw new AssertionError();
    }

    @Override
    public IDocument getDocument()
    {
        return getDelegate().getDocument();
    }

    @Override
    public ISnapshot getSnapshot()
    {
        return new TextFileBufferSnapshot(getDelegate(), bufferManager);
    }

    @Override
    public IBufferChange applyChange(IBufferChange change,
        IProgressMonitor monitor) throws CoreException
    {
        if (monitor == null)
            monitor = new NullProgressMonitor();
        try
        {
            BufferChangeOperation operation = new BufferChangeOperation(this,
                change);
            if (!getDelegate().isSynchronizationContextRequested())
                return operation.execute(monitor);

            UiSynchronizer synchronizer = UiSynchronizer.getDefault();
            if (synchronizer == null)
                throw new AssertionError(
                    "Synchronization context is requested but UI is not running?"); //$NON-NLS-1$

            UiBufferChangeRunner runner = new UiBufferChangeRunner(synchronizer,
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
        return getDelegate().isDirty();
    }

    @Override
    public boolean mustSaveChanges()
    {
        ITextFileBuffer delegate = getDelegate();
        return delegate.isDirty() && !delegate.isShared();
    }

    @Override
    public void save(boolean overwrite, IProgressMonitor monitor)
        throws CoreException
    {
        getDelegate().commit(monitor, overwrite);
    }

    @Override
    public void addRef()
    {
        synchronized (this)
        {
            ++refCount;
        }
    }

    @Override
    public void release()
    {
        synchronized (this)
        {
            if (--refCount != 0)
                return;
            if (delegate == null)
                return;
            delegate = null;
        }
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

    /**
     * Returns the underlying {@link ITextFileBuffer}.
     *
     * @return the underlying <code>ITextFileBuffer</code>
     *  (never <code>null</code>)
     * @throws IllegalStateException if the buffer is disconnected
     */
    protected final ITextFileBuffer getDelegate()
    {
        ITextFileBuffer result = delegate;
        if (result == null)
            throw new IllegalStateException(
                "Attempt to access a disconnected TextFileBuffer for " //$NON-NLS-1$
                    + file.getFullPath());
        return result;
    }
}
