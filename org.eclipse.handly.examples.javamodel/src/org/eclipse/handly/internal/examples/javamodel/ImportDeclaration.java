/*******************************************************************************
 * Copyright (c) 2015 1C LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel;

import org.eclipse.handly.examples.javamodel.IImportDeclaration;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.model.impl.HandleManager;
import org.eclipse.handly.model.impl.SourceConstruct;

/**
 * Implementation of {@link IImportDeclaration}.
 */
public class ImportDeclaration
    extends SourceConstruct
    implements IImportDeclaration
{
    /**
     * Creates a handle for an import declaration with the given parent element 
     * and the given name.
     * 
     * @param parent the parent of the element (not <code>null</code>)
     * @param name the name of the element (not <code>null</code>)
     */
    public ImportDeclaration(ImportContainer parent, String name)
    {
        super(parent, name);
        if (name == null)
            throw new IllegalArgumentException();
    }

    @Override
    public ImportContainer getParent()
    {
        return (ImportContainer)parent;
    }

    @Override
    public IJavaModel getRoot()
    {
        return (IJavaModel)super.getRoot();
    }

    @Override
    protected HandleManager getHandleManager()
    {
        return JavaModelManager.INSTANCE.getHandleManager();
    }

    @Override
    protected void toStringName(StringBuilder builder)
    {
        builder.append("import "); //$NON-NLS-1$
        super.toStringName(builder);
    }
}
