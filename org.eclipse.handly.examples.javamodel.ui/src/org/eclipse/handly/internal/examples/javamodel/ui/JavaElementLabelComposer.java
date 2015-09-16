/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Guven Demir <guven.internet+eclipse@gmail.com> - [package explorer] 
 *          Alternative package name shortening: abbreviation - 
 *          https://bugs.eclipse.org/bugs/show_bug.cgi?id=299514
 *     Ondrej Ilcik (Codasip) - adaptation (adapted from 
 *          org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabelComposer)
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel.ui;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.examples.javamodel.IJavaElement;
import org.eclipse.handly.examples.javamodel.IMethod;
import org.eclipse.handly.examples.javamodel.IPackageFragment;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;

/**
 * Label composer for Java elements.
 */
public class JavaElementLabelComposer
{
    private static final Styler DECORATIONS_STYLE =
        StyledString.DECORATIONS_STYLER;

    private final Object buffer;

    /**
     * Constructs new string-based label composer.
     * 
     * @param sb string builder to fill
     */
    public static JavaElementLabelComposer create(StringBuilder sb)
    {
        Assert.isNotNull(sb);
        return new JavaElementLabelComposer(sb);
    }

    /**
     * Constructs new {@link StyledString}-based label composer.
     * 
     * @param ss styled string to fill 
     */
    public static JavaElementLabelComposer create(StyledString ss)
    {
        Assert.isNotNull(ss);
        return new JavaElementLabelComposer(ss);
    }

    private JavaElementLabelComposer(Object buffer)
    {
        this.buffer = buffer;
    }

    /**
     * Appends label for the specified Java element.
     * 
     * @param element Java element
     * @param flags options
     * @throws CoreException 
     */
    public void appendElementLabel(IJavaElement element) throws CoreException
    {
        if (element instanceof IMethod)
        {
            appendMethodLabel((IMethod)element);
        }
        else if (element instanceof IPackageFragment)
        {
            appendPackageFragmentLabel((IPackageFragment)element);
        }
        else
        {
            append(element.getName());
        }
    }

    /**
     * Appends the label for a method. Considers the M_* flags.
     *
     * @param method the element to render
     * @param flags the rendering flags. Flags with names starting with 'M_' are considered.
     */
    public void appendMethodLabel(IMethod method)
    {
        append(method.getName());

        try
        {
            // parameters
            append('(');
            String[] paramTypes = method.getParameterTypes();
            for (int i = 0; i < paramTypes.length; i++)
            {
                if (i > 0)
                    append(JavaElementLabels.COMMA_STRING);

                String simpleName = getSimpleTypeName(method, paramTypes[i]);
                append(simpleName);
            }
            append(')');

            // exceptions
            String[] exceptionTypes = method.getExceptionTypes();
            if (exceptionTypes.length > 0)
            {
                append(" throws "); //$NON-NLS-1$
                for (int i = 0; i < exceptionTypes.length; i++)
                {
                    if (i > 0)
                        append(JavaElementLabels.COMMA_STRING);

                    String simpleName =
                        getSimpleTypeName(method, exceptionTypes[i]);
                    append(simpleName);
                }
            }

            // return type
            if (!method.isConstructor())
            {
                int offset = getLength();
                append(JavaElementLabels.DECL_STRING);
                appendTypeSignatureLabel(method, method.getReturnType());
                setStyle(offset, getLength() - offset, DECORATIONS_STYLE);
            }
        }
        catch (CoreException e)
        {
            Activator.log(e.getStatus());
        }
    }

    /**
     * Appends the label for a package fragment.
     *
     * @param pack the element to render
     */
    private void appendPackageFragmentLabel(IPackageFragment pack)
    {
        if (pack.isDefaultPackage())
        {
            append(JavaElementLabels.DEFAULT_PACKAGE);
        }
        else
        {
            append(pack.getName());
        }
    }

    /**
     * Appends signature type label of the specified Java element.
     * 
     * @param enclosingElement
     * @param typeSig
     * @param flags
     */
    private void appendTypeSignatureLabel(IJavaElement enclosingElement,
        String typeSig)
    {
        int sigKind = Signature.getTypeSignatureKind(typeSig);
        switch (sigKind)
        {
        case Signature.BASE_TYPE_SIGNATURE:
            append(Signature.toString(typeSig));
            break;
        case Signature.ARRAY_TYPE_SIGNATURE:
            appendTypeSignatureLabel(enclosingElement,
                Signature.getElementType(typeSig));
            for (int dim = Signature.getArrayCount(typeSig); dim > 0; dim--)
            {
                append('[').append(']');
            }
            break;
        case Signature.CLASS_TYPE_SIGNATURE:
            String baseType = getSimpleTypeName(enclosingElement, typeSig);
            append(baseType);

            String[] typeArguments = Signature.getTypeArguments(typeSig);
            appendTypeArgumentSignaturesLabel(enclosingElement, typeArguments);
            break;
        case Signature.TYPE_VARIABLE_SIGNATURE:
            append(getSimpleTypeName(enclosingElement, typeSig));
            break;
        case Signature.WILDCARD_TYPE_SIGNATURE:
            char ch = typeSig.charAt(0);
            if (ch == Signature.C_STAR)
            { //workaround for bug 85713
                append('?');
            }
            else
            {
                if (ch == Signature.C_EXTENDS)
                {
                    append("? extends "); //$NON-NLS-1$
                    appendTypeSignatureLabel(enclosingElement,
                        typeSig.substring(1));
                }
                else if (ch == Signature.C_SUPER)
                {
                    append("? super "); //$NON-NLS-1$
                    appendTypeSignatureLabel(enclosingElement,
                        typeSig.substring(1));
                }
            }
            break;
        case Signature.CAPTURE_TYPE_SIGNATURE:
            appendTypeSignatureLabel(enclosingElement, typeSig.substring(1));
            break;
        case Signature.INTERSECTION_TYPE_SIGNATURE:
            String[] typeBounds = Signature.getIntersectionTypeBounds(typeSig);
            appendTypeBoundsSignaturesLabel(enclosingElement, typeBounds);
            break;
        default:
            // unknown
        }
    }

    private void appendTypeArgumentSignaturesLabel(
        IJavaElement enclosingElement, String[] typeArgsSig)
    {
        if (typeArgsSig.length > 0)
        {
            append("<");
            for (int i = 0; i < typeArgsSig.length; i++)
            {
                if (i > 0)
                {
                    append(JavaElementLabels.COMMA_STRING);
                }
                appendTypeSignatureLabel(enclosingElement, typeArgsSig[i]);
            }
            append(">");
        }
    }

    private void appendTypeBoundsSignaturesLabel(IJavaElement enclosingElement,
        String[] typeArgsSig)
    {
        for (int i = 0; i < typeArgsSig.length; i++)
        {
            if (i > 0)
            {
                append(" | "); //$NON-NLS-1$
            }
            appendTypeSignatureLabel(enclosingElement, typeArgsSig[i]);
        }
    }

    /**
     * Converts signature to a simple name.
     * 
     * @param enclosingElement
     * @param typeSig
     * @return
     */
    protected String getSimpleTypeName(IJavaElement enclosingElement,
        String typeSig)
    {
        return Signature.getSimpleName(
            Signature.toString(Signature.getTypeErasure(typeSig)));
    }

    /**
     * Appends the string representation of the given character to the buffer.
     *
     * @param ch the character to append
     * @return a reference to this object
     */
    private JavaElementLabelComposer append(char ch)
    {
        if (buffer instanceof StringBuilder)
        {
            ((StringBuilder)buffer).append(ch);
        }
        else
        {
            ((StyledString)buffer).append(ch);
        }

        return this;
    }

    /**
     * Appends the given string to the buffer.
     *
     * @param string the string to append
     * @return a reference to this object
     */
    private JavaElementLabelComposer append(String string)
    {
        if (buffer instanceof StringBuilder)
        {
            ((StringBuilder)buffer).append(string);
        }
        else
        {
            ((StyledString)buffer).append(string);
        }

        return this;
    }

    /**
     * Sets a styler to use for the given source range.
     * 
     * @see StyledString#setStyle(int, int, Styler)
     */
    private void setStyle(int offset, int length, Styler styler)
    {
        // only used for styled strings
        if (buffer instanceof StyledString)
        {
            ((StyledString)buffer).setStyle(offset, length, styler);
        }
    }

    /**
     * Gets current length of the buffer.
     * 
     * @return
     */
    private int getLength()
    {
        if (buffer instanceof StringBuilder)
        {
            return ((StringBuilder)buffer).length();
        }
        else
        {
            return ((StyledString)buffer).length();
        }
    }
}
