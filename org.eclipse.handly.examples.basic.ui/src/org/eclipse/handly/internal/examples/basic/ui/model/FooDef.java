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
package org.eclipse.handly.internal.examples.basic.ui.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.examples.basic.ui.model.IFooDef;
import org.eclipse.handly.model.impl.SourceConstruct;

/**
 * Represents a function defined in a Foo file.
 */
public class FooDef
    extends SourceConstruct
    implements IFooDef, IFooElementInternal
{
    private final int arity;

    /**
     * Creates a handle for a function with the given parent element,
     * the given name, and the given arity.
     *
     * @param parent the parent of the element (not <code>null</code>)
     * @param name the name of the element (not <code>null</code>)
     * @param arity the arity of the function
     */
    public FooDef(FooFile parent, String name, int arity)
    {
        super(parent, name);
        if (name == null)
            throw new IllegalArgumentException();
        this.arity = arity;
    }

    @Override
    public int getArity()
    {
        return arity;
    }

    @Override
    public String[] getParameterNames() throws CoreException
    {
        return getSourceElementInfo().get(PARAMETER_NAMES);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof FooDef))
            return false;
        return super.equals(obj) && arity == ((FooDef)obj).arity;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + arity;
        return result;
    }

    protected void hToStringName(StringBuilder builder, IContext context)
    {
        super.hToStringName(builder, context);
        builder.append('/');
        builder.append(arity);
    }
}
