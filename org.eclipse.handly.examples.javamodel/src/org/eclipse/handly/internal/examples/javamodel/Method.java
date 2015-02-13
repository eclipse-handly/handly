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

import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.examples.javamodel.IMethod;
import org.eclipse.jdt.core.Signature;

/**
 * Implementation of {@link IMethod}.
 */
public class Method
    extends Member
    implements IMethod
{
    private final String[] parameterTypes;

    /**
     * Creates a handle for a method with the given parent element, 
     * the given name, and the given parameter types.
     * 
     * @param parent the parent of the element (not <code>null</code>)
     * @param name the name of the element (not <code>null</code>)
     * @param parameterTypes the type signatures for the method parameters
     *  (not <code>null</code>)
     */
    public Method(Type parent, String name, String[] parameterTypes)
    {
        super(parent, name);
        if (name == null)
            throw new IllegalArgumentException();
        if (parameterTypes == null)
            throw new IllegalArgumentException();
        this.parameterTypes = parameterTypes;
    }

    @Override
    public Type getParent()
    {
        return (Type)parent;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Method))
            return false;
        return super.equals(obj)
            && Arrays.equals(parameterTypes, ((Method)obj).parameterTypes);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        for (String parameterType : parameterTypes)
            result = prime * result + parameterType.hashCode();
        return result;
    }

    @Override
    public String[] getParameterTypes()
    {
        return parameterTypes;
    }

    @Override
    public String[] getParameterNames() throws CoreException
    {
        String[] result = getSourceElementInfo().get(PARAMETER_NAMES);
        if (result == null)
            return NO_STRINGS;
        return result;
    }

    @Override
    public String getReturnType() throws CoreException
    {
        String result = getSourceElementInfo().get(RETURN_TYPE);
        if (result == null)
            return Signature.SIG_VOID;
        return result;
    }

    @Override
    public String[] getExceptionTypes() throws CoreException
    {
        String[] result = getSourceElementInfo().get(EXCEPTION_TYPES);
        if (result == null)
            return NO_STRINGS;
        return result;
    }

    @Override
    public String getSignature() throws CoreException
    {
        return Signature.createMethodSignature(parameterTypes, getReturnType());
    }

    @Override
    public boolean isConstructor() throws CoreException
    {
        Boolean result = getSourceElementInfo().get(IS_CONSTRUCTOR);
        if (result == null)
            return false;
        return result;
    }
}
