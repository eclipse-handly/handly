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
package org.eclipse.handly.model.impl;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.context.IContext;

/**
 * A simple source file for tests.
 * Test clients can instantiate this class directly or subclass it.
 */
class SimpleSourceFile
    extends SourceFile
{
    /**
     * Constructs a handle for a source file with the given parent element and
     * the given underlying workspace file.
     *
     * @param parent the parent of the element,
     *  or <code>null</code> if the element has no parent
     * @param file the workspace file underlying the element (not <code>null</code>)
     */
    public SimpleSourceFile(Element parent, IFile file)
    {
        super(parent, file);
    }

    @Override
    protected Object hCreateAst(String source, IContext context,
        IProgressMonitor monitor) throws CoreException
    {
        return new Object();
    }

    @Override
    protected void hBuildStructure(Object ast, IContext context,
        IProgressMonitor monitor)
    {
    }

    @Override
    protected ElementManager hElementManager()
    {
        throw new UnsupportedOperationException();
    }
}
