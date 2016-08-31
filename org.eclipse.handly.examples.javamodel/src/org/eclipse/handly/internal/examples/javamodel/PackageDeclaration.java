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
package org.eclipse.handly.internal.examples.javamodel;

import org.eclipse.handly.context.IContext;
import org.eclipse.handly.examples.javamodel.IPackageDeclaration;
import org.eclipse.handly.model.impl.SourceConstruct;

/**
 * Implementation of {@link IPackageDeclaration}.
 */
public class PackageDeclaration
    extends SourceConstruct
    implements IPackageDeclaration, IJavaElementInternal
{
    /**
     * Creates a handle for a package declaration with the given parent element
     * and the given name.
     *
     * @param parent the parent of the element (not <code>null</code>)
     * @param name the name of the element (not <code>null</code>)
     */
    public PackageDeclaration(CompilationUnit parent, String name)
    {
        super(parent, name);
        if (name == null)
            throw new IllegalArgumentException();
    }

    @Override
    protected void hToStringName(StringBuilder builder, IContext context)
    {
        builder.append("package "); //$NON-NLS-1$
        super.hToStringName(builder, context);
    }
}
