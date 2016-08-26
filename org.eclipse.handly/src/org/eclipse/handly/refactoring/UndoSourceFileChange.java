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
package org.eclipse.handly.refactoring;

import static org.eclipse.handly.context.Contexts.EMPTY_CONTEXT;
import static org.eclipse.handly.model.Elements.exists;
import static org.eclipse.handly.model.Elements.getPath;
import static org.eclipse.handly.model.Elements.getBuffer;
import static org.eclipse.handly.model.Elements.getFile;

import java.text.MessageFormat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.buffer.IBufferChange;
import org.eclipse.handly.internal.Activator;
import org.eclipse.handly.model.ISourceFile;
import org.eclipse.handly.snapshot.StaleSnapshotException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

class UndoSourceFileChange
    extends Change
{
    private final String name;
    private final ISourceFile sourceFile;
    private final IBufferChange undoChange;
    private boolean existed;

    public UndoSourceFileChange(String name, ISourceFile sourceFile,
        IBufferChange undoChange)
    {
        if ((this.name = name) == null)
            throw new IllegalArgumentException();
        if ((this.sourceFile = sourceFile) == null)
            throw new IllegalArgumentException();
        if ((this.undoChange = undoChange) == null)
            throw new IllegalArgumentException();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void initializeValidationData(IProgressMonitor pm)
    {
        existed = exists(sourceFile);
    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException,
        OperationCanceledException
    {
        RefactoringStatus result = new RefactoringStatus();

        if (!existed) // got deleted/moved during refactoring
        {
            if (exists(sourceFile))
            {
                result.addFatalError(MessageFormat.format(
                    Messages.UndoSourceFileChange_Should_not_exist__0, getPath(
                        sourceFile).makeRelative()));
            }
            return result; // let the delete/move undo change handle the rest
        }
        else if (!exists(sourceFile))
        {
            result.addFatalError(MessageFormat.format(
                Messages.UndoSourceFileChange_Should_exist__0, getPath(
                    sourceFile).makeRelative()));
            return result;
        }

        if (undoChange.getBase() == null)
            return result; // OK

        try (IBuffer buffer = getBuffer(sourceFile, EMPTY_CONTEXT, pm))
        {
            if (!undoChange.getBase().isEqualTo(buffer.getSnapshot()))
            {
                result.addFatalError(MessageFormat.format(
                    Messages.UndoSourceFileChange_Cannot_undo_stale_change__0,
                    getPath(sourceFile).makeRelative()));
            }
        }
        return result;
    }

    @Override
    public Change perform(IProgressMonitor pm) throws CoreException
    {
        pm.beginTask("", 2); //$NON-NLS-1$
        try (
            IBuffer buffer = getBuffer(sourceFile, EMPTY_CONTEXT,
                new SubProgressMonitor(pm, 1)))
        {
            IBufferChange redoChange;

            try
            {
                redoChange = buffer.applyChange(undoChange,
                    new SubProgressMonitor(pm, 1));
            }
            catch (StaleSnapshotException e)
            {
                throw new CoreException(Activator.createErrorStatus(
                    MessageFormat.format(
                        Messages.UndoSourceFileChange_Cannot_undo_stale_change__0,
                        getPath(sourceFile).makeRelative()), e));
            }

            return new UndoSourceFileChange(getName(), sourceFile, redoChange);
        }
        finally
        {
            pm.done();
        }
    }

    @Override
    public Object getModifiedElement()
    {
        return sourceFile;
    }

    @Override
    public Object[] getAffectedObjects()
    {
        IFile file = getFile(sourceFile);
        if (file == null)
            return null;
        return new Object[] { file };
    }
}
