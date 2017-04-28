/*******************************************************************************
 * Copyright (c) 2014, 2017 1C-Soft LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.model.impl;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.IModel;

/**
 * A simple element for tests.
 * Test clients can instantiate this class directly or subclass it.
 */
class SimpleElement
    extends Element
{
    private final IModel model;

    /**
     * Constructs a handle for an element with the given parent element
     * and the given name.
     *
     * @param parent the parent of the element,
     *  or <code>null</code> if the element has no parent
     * @param name the name of the element
     * @param model the model the element belongs to
     */
    public SimpleElement(IElement parent, String name, IModel model)
    {
        super(parent, name);
        this.model = model;
    }

    /**
     * Returns a child element with the given name.
     * This is a handle-only method.
     *
     * @param name the name of the element
     * @return the child element with the given name
     */
    public SimpleElement getChild(String name)
    {
        return new SimpleElement(this, name, model);
    }

    @Override
    public IModel hModel()
    {
        return model;
    }

    @Override
    public IResource hResource()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public IModelManager hModelManager()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void hValidateExistence(IContext context) throws CoreException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void hBuildStructure(IContext context, IProgressMonitor monitor)
        throws CoreException
    {
        throw new UnsupportedOperationException();
    }
}
