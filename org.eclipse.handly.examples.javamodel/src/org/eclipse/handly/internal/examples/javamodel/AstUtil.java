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

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.WildcardType;

/**
 * AST utilities.
 */
class AstUtil
{
    /**
     * Converts the given type names to type signatures.
     */
    static String[] toTypeSignatures(List<? extends Name> names)
    {
        String[] result = new String[names.size()];
        int i = 0;
        for (Name name : names)
        {
            result[i++] =
                Signature.createTypeSignature(name.getFullyQualifiedName(),
                    false);
        }
        return result;
    }

    /**
     * Returns the signature of the given type.
     * @see Signature
     */
    static String getSignature(Type type)
    {
        StringBuilder builder = new StringBuilder();
        appendTypeName(type, builder);
        return Signature.createTypeSignature(builder.toString(), false);
    }

    /**
     * Returns the signatures of the given types.
     * @see Signature
     */
    static String[] getSignatures(List<? extends Type> types)
    {
        String[] result = new String[types.size()];
        int i = 0;
        for (Type type : types)
            result[i++] = getSignature(type);
        return result;
    }

    private static void appendTypeName(Type type, StringBuilder builder)
    {
        if (type instanceof ArrayType)
        {
            ArrayType arrayType = (ArrayType)type;
            appendTypeName(arrayType.getElementType(), builder);
            for (int i = 0; i < arrayType.getDimensions(); i++)
            {
                builder.append('[');
                builder.append(']');
            }
        }
        else if (type instanceof ParameterizedType)
        {
            ParameterizedType parameterizedType = (ParameterizedType)type;
            appendTypeName(parameterizedType.getType(), builder);
            builder.append('<');
            @SuppressWarnings("unchecked")
            Iterator<Type> iterator =
                parameterizedType.typeArguments().iterator();
            boolean isFirst = true;
            while (iterator.hasNext())
            {
                if (!isFirst)
                    builder.append(',');
                else
                    isFirst = false;
                Type typeArgument = iterator.next();
                appendTypeName(typeArgument, builder);
            }
            builder.append('>');
        }
        else if (type instanceof PrimitiveType)
        {
            builder.append(((PrimitiveType)type).getPrimitiveTypeCode().toString());
        }
        else if (type instanceof QualifiedType)
        {
            QualifiedType qualifiedType = (QualifiedType)type;
            appendTypeName(qualifiedType.getQualifier(), builder);
            builder.append('.');
            builder.append(qualifiedType.getName().getIdentifier());
        }
        else if (type instanceof SimpleType)
        {
            builder.append(((SimpleType)type).getName().getFullyQualifiedName());
        }
        else if (type instanceof WildcardType)
        {
            builder.append('?');
            WildcardType wildcardType = (WildcardType)type;
            Type bound = wildcardType.getBound();
            if (bound == null)
                return;
            if (wildcardType.isUpperBound())
                builder.append(" extends "); //$NON-NLS-1$
            else
                builder.append(" super "); //$NON-NLS-1$
            appendTypeName(bound, builder);
        }
    }

    private AstUtil()
    {
    }
}
