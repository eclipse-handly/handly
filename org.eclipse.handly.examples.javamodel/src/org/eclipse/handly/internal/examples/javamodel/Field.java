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
import org.eclipse.jdt.core.Flags;

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
}
