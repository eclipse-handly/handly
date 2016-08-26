/*******************************************************************************
 * Copyright (c) 2015, 2016 1C-Soft LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.adapter;

import static org.eclipse.handly.model.Elements.CREATE_BUFFER;
import static org.eclipse.handly.model.Elements.FORCE_RECONCILING;

import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.buffer.TextFileBuffer;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.model.impl.ISourceFileImpl;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Adapts a compilation unit to <code>ISourceFile</code>.
 */
class JavaSourceFile
    extends JavaSourceElement
    implements ISourceFileImpl
{
    /**
     * Constructs a <code>JavaSourceFile</code> for the given compilation unit.
     *
     * @param compilationUnit not <code>null</code>
     */
    public JavaSourceFile(ICompilationUnit compilationUnit)
    {
        super(compilationUnit);
    }

    /**
     * Returns the underlying compilation unit.
     *
     * @return the underlying compilation unit (not <code>null</code>)
     */
    public ICompilationUnit getCompilationUnit()
    {
        return (ICompilationUnit)getJavaElement();
    }

    @Override
    public IFile hFile()
    {
        return (IFile)getCompilationUnit().getResource();
    }

    @Override
    public boolean hIsWorkingCopy()
    {
        return getCompilationUnit().isWorkingCopy();
    }

    @Override
    public boolean hNeedsReconciling()
    {
        try
        {
            return !getCompilationUnit().isConsistent();
        }
        catch (JavaModelException e)
        {
            Activator.log(e.getStatus());
            return false;
        }
    }

    @Override
    public void hReconcile(IContext context, IProgressMonitor monitor)
        throws CoreException
    {
        getCompilationUnit().reconcile(ICompilationUnit.NO_AST,
            context.getOrDefault(FORCE_RECONCILING), null/*use primary owner*/,
            monitor);
    }

    @Override
    public IBuffer hBuffer(IContext context, IProgressMonitor monitor)
        throws CoreException
    {
        IFile file = hFile();
        if (file == null)
            throw new AssertionError("No underlying IFile for " + toString());
        if (!context.getOrDefault(CREATE_BUFFER)
            && ITextFileBufferManager.DEFAULT.getTextFileBuffer(
                file.getFullPath(), LocationKind.IFILE) == null)
        {
            return null;
        }
        return new TextFileBuffer(file, ITextFileBufferManager.DEFAULT);
    }
}
