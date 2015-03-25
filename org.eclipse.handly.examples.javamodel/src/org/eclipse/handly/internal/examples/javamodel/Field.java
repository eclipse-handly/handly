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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.examples.javamodel.IField;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.handly.model.impl.SourceElementBody;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.Signature;

/**
 * Implementation of {@link IField}.
 */
public class Field
    extends Member
    implements IField
{
    /**
     * Creates a handle for a field with the given parent element 
     * and the given name.
     * 
     * @param parent the parent of the element (not <code>null</code>)
     * @param name the name of the element (not <code>null</code>)
     */
    public Field(Type parent, String name)
    {
        super(parent, name);
        if (name == null)
            throw new IllegalArgumentException();
    }

    @Override
    public Type getParent()
    {
        return (Type)parent;
    }

    @Override
    public String getType() throws CoreException
    {
        return getSourceElementInfo().get(TYPE);
    }

    @Override
    public boolean isEnumConstant() throws CoreException
    {
        return Flags.isEnum(getFlags());
    }

    @Override
    protected void toStringBody(int tab, StringBuilder builder, Body body,
        boolean showResolvedInfo)
    {
        builder.append(tabString(tab));
        if (body != null)
        {
            SourceElementBody fieldBody = (SourceElementBody)body;
            String type = fieldBody.get(TYPE);
            builder.append(Signature.toString(type));
            builder.append(' ');
        }
        toStringName(builder);
        if (body == null)
        {
            builder.append(" (not open)"); //$NON-NLS-1$
        }
    }
}
