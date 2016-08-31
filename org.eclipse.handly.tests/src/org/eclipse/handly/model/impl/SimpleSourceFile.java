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

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.IModel;

/**
 * A simple source file for tests.
 * Test clients can instantiate this class directly or subclass it.
 */
class SimpleSourceFile
    extends SourceFile
{
    private final IModel model;

    /**
     * Constructs a handle for a source file with the given parent element and
     * the given underlying workspace file.
     *
     * @param parent the parent of the element,
     *  or <code>null</code> if the element has no parent
     * @param file the workspace file underlying the element (not <code>null</code>)
     * @param model the model the element belongs to
     */
    public SimpleSourceFile(Element parent, IFile file, IModel model)
    {
        super(parent, file);
        this.model = model;
    }

    @Override
    public IModel hModel()
    {
        return model;
    }

    @Override
    public IModelManager hModelManager()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object hCreateStructuralAst(String source,
        IProgressMonitor monitor) throws CoreException
    {
        return null;
    }

    @Override
    protected void hBuildStructure(SourceElementBody body,
        Map<IElement, Object> newElements, Object ast, String source,
        IProgressMonitor monitor)
    {
    }
}
