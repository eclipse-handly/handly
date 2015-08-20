/*******************************************************************************
 * Copyright (c) 2015 1C-Soft LLC.
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
import org.eclipse.handly.model.impl.Body;
import org.eclipse.handly.model.impl.SourceElementBody;
import org.eclipse.handly.util.TextIndent;
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
        return super.equals(obj) && Arrays.equals(parameterTypes,
            ((Method)obj).parameterTypes);
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

    @Override
    protected void toStringName(StringBuilder builder)
    {
        builder.append(getName());
        builder.append('(');
        String[] parameterTypes = getParameterTypes();
        int length = parameterTypes.length;
        for (int i = 0; i < length; i++)
        {
            String parameterType;
            try
            {
                parameterType = Signature.toString(parameterTypes[i]);
            }
            catch (IllegalArgumentException e)
            {
                // signature is malformed
                parameterType = "*** invalid signature: " + parameterTypes[i]; //$NON-NLS-1$
            }
            builder.append(parameterType);
            if (i < length - 1)
                builder.append(", "); //$NON-NLS-1$
        }
        builder.append(')');
        int occurenceCount = getOccurenceCount();
        if (occurenceCount > 1)
        {
            builder.append('#');
            builder.append(occurenceCount);
        }
    }

    @Override
    protected void toStringBody(TextIndent indent, StringBuilder builder,
        Body body, boolean showResolvedInfo)
    {
        indent.appendTo(builder);
        if (body != null && body != NO_BODY)
        {
            SourceElementBody methodBody = (SourceElementBody)body;
            String returnType = methodBody.get(RETURN_TYPE);
            if (returnType != null)
            {
                builder.append(Signature.toString(returnType));
                builder.append(' ');
            }
        }
        toStringName(builder);
        if (body == null)
        {
            builder.append(" (not open)"); //$NON-NLS-1$
        }
    }
}
