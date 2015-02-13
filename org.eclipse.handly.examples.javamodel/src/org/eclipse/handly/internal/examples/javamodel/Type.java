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
import org.eclipse.handly.examples.javamodel.IMethod;
import org.eclipse.handly.examples.javamodel.IType;
import org.eclipse.handly.model.impl.Handle;
import org.eclipse.jdt.core.Flags;

/**
 * Implementation of {@link IType}.
 */
public class Type
    extends Member
    implements IType
{
    /**
     * Creates a handle for a type with the given parent element 
     * and the given name.
     * 
     * @param parent the parent of the element (not <code>null</code>)
     * @param name the name of the element (not <code>null</code>)
     */
    public Type(Handle parent, String name)
    {
        super(parent, name);
        if (name == null)
            throw new IllegalArgumentException();
    }

    @Override
    public IField getField(String name)
    {
        return new Field(this, name);
    }

    @Override
    public IField[] getFields() throws CoreException
    {
        return getChildren(IField.class);
    }

    @Override
    public IMethod getMethod(String name, String[] parameterTypes)
    {
        return new Method(this, name, parameterTypes);
    }

    @Override
    public IMethod[] getMethods() throws CoreException
    {
        return getChildren(IMethod.class);
    }

    @Override
    public IType getType(String name)
    {
        return new Type(this, name);
    }

    @Override
    public IType[] getTypes() throws CoreException
    {
        return getChildren(IType.class);
    }

    @Override
    public String getSuperclassType() throws CoreException
    {
        return getSourceElementInfo().get(SUPERCLASS_TYPE);
    }

    @Override
    public String[] getSuperInterfaceTypes() throws CoreException
    {
        String[] result = getSourceElementInfo().get(SUPER_INTERFACE_TYPES);
        if (result == null)
            return NO_STRINGS;
        return result;
    }

    @Override
    public boolean isClass() throws CoreException
    {
        int flags = getFlags();
        return !(Flags.isEnum(flags) || Flags.isInterface(flags) || Flags.isAnnotation(flags));
    }

    @Override
    public boolean isEnum() throws CoreException
    {
        return Flags.isEnum(getFlags());
    }

    @Override
    public boolean isInterface() throws CoreException
    {
        int flags = getFlags();
        return Flags.isInterface(flags);
    }

    @Override
    public boolean isAnnotation() throws CoreException
    {
        int flags = getFlags();
        return Flags.isInterface(flags) && Flags.isAnnotation(flags);
    }

    @Override
    public boolean isMember()
    {
        return getDeclaringType() != null;
    }
}
