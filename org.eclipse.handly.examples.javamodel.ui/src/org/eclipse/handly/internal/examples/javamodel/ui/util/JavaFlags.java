/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ondrej Ilcik - adaptation (adapted from
 *         org.eclipse.jdt.internal.corext.util.JdtFlags)
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel.ui.util;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.examples.javamodel.IField;
import org.eclipse.handly.examples.javamodel.IMember;
import org.eclipse.handly.examples.javamodel.IMethod;
import org.eclipse.handly.examples.javamodel.IType;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.dom.Modifier;

/**
 * Utility class to detect element properties including implicit 
 * ones as defined by Java Language Spec. 
 * 
 * Adapted from <pre>org.eclipse.jdt.internal.corext.util.JdtFlags</pre>.
 */
public class JavaFlags
{
    private JavaFlags()
    {
    }

    public static final String VISIBILITY_STRING_PRIVATE = "private"; //$NON-NLS-1$
    public static final String VISIBILITY_STRING_PACKAGE = ""; //$NON-NLS-1$
    public static final String VISIBILITY_STRING_PROTECTED = "protected"; //$NON-NLS-1$
    public static final String VISIBILITY_STRING_PUBLIC = "public"; //$NON-NLS-1$

    public static final int VISIBILITY_CODE_INVALID = -1;

    public static boolean isAbstract(IMember member) throws CoreException
    {
        if (isInterfaceOrAnnotationMethod(member))
            return true;
        return Flags.isAbstract(member.getFlags());
    }

    public static boolean isDeprecated(IMember member) throws CoreException
    {
        return Flags.isDeprecated(member.getFlags());
    }

    public static boolean isFinal(IMember member) throws CoreException
    {
        if (isInterfaceOrAnnotationField(member))
            return true;
        if (isEnumConstant(member))
            return true;
        return Flags.isFinal(member.getFlags());
    }

    public static boolean isNative(IMember member) throws CoreException
    {
        return Flags.isNative(member.getFlags());
    }

    public static boolean isPackageVisible(IMember member) throws CoreException
    {
        return (!isPrivate(member) && !isProtected(member)
            && !isPublic(member));
    }

    public static boolean isPrivate(IMember member) throws CoreException
    {
        return Flags.isPrivate(member.getFlags());
    }

    public static boolean isProtected(IMember member) throws CoreException
    {
        return Flags.isProtected(member.getFlags());
    }

    public static boolean isPublic(IMember member) throws CoreException
    {
        if (isInterfaceOrAnnotationMember(member))
            return true;
        if (isEnumConstant(member))
            return true;
        return Flags.isPublic(member.getFlags());
    }

    public static boolean isStatic(IMember member) throws CoreException
    {
        if (isNestedInterfaceOrAnnotation(member))
            return true;
        if (member instanceof IMethod && isInterfaceOrAnnotationMember(member))
            return true;
        if (isEnumConstant(member))
            return true;
        return Flags.isStatic(member.getFlags());
    }

    public static boolean isStrictfp(IMember member) throws CoreException
    {
        return Flags.isStrictfp(member.getFlags());
    }

    public static boolean isSynchronized(IMember member) throws CoreException
    {
        return Flags.isSynchronized(member.getFlags());
    }

    public static boolean isSynthetic(IMember member) throws CoreException
    {
        return Flags.isSynthetic(member.getFlags());
    }

    public static boolean isAnnotation(IMember member) throws CoreException
    {
        return Flags.isAnnotation(member.getFlags());
    }

    public static boolean isEnum(IMember member) throws CoreException
    {
        return Flags.isEnum(member.getFlags());
    }

    public static boolean isVarargs(IMethod method) throws CoreException
    {
        return Flags.isVarargs(method.getFlags());
    }

    public static boolean isTransient(IMember member) throws CoreException
    {
        return Flags.isTransient(member.getFlags());
    }

    public static boolean isVolatile(IMember member) throws CoreException
    {
        return Flags.isVolatile(member.getFlags());
    }

    private static boolean isInterfaceOrAnnotationMethod(IMember member)
        throws CoreException
    {
        return (member instanceof IMethod)
            && (isInterfaceOrAnnotationMember(member));
    }

    private static boolean isInterfaceOrAnnotationField(IMember member)
        throws CoreException
    {
        return (member instanceof IType)
            && (isInterfaceOrAnnotationMember(member));
    }

    private static boolean isInterfaceOrAnnotationMember(IMember member)
        throws CoreException
    {
        return member.getDeclaringType() != null
            && (member.getDeclaringType().isInterface());
    }

    private static boolean isNestedInterfaceOrAnnotation(IMember member)
        throws CoreException
    {
        return (member instanceof IType) && member.getDeclaringType() != null
            && ((IType)member).isInterface();
    }

    private static boolean isEnumConstant(IMember member) throws CoreException
    {
        return (member instanceof IField) && isEnum(member);
    }

    public static int getVisibilityCode(IMember member) throws CoreException
    {
        if (isPublic(member))
            return Modifier.PUBLIC;
        else if (isProtected(member))
            return Modifier.PROTECTED;
        else if (isPackageVisible(member))
            return Modifier.NONE;
        else if (isPrivate(member))
            return Modifier.PRIVATE;
        Assert.isTrue(false);
        return VISIBILITY_CODE_INVALID;
    }

    public static String getVisibilityString(int visibilityCode)
    {
        if (Modifier.isPublic(visibilityCode))
            return VISIBILITY_STRING_PUBLIC;
        if (Modifier.isProtected(visibilityCode))
            return VISIBILITY_STRING_PROTECTED;
        if (Modifier.isPrivate(visibilityCode))
            return VISIBILITY_STRING_PRIVATE;
        return VISIBILITY_STRING_PACKAGE;
    }

    public static int getVisibilityCode(String visibilityString)
    {
        Assert.isNotNull(visibilityString);
        if (VISIBILITY_STRING_PACKAGE.equals(visibilityString))
            return 0;
        else if (VISIBILITY_STRING_PRIVATE.equals(visibilityString))
            return Modifier.PRIVATE;
        else if (VISIBILITY_STRING_PROTECTED.equals(visibilityString))
            return Modifier.PROTECTED;
        else if (VISIBILITY_STRING_PUBLIC.equals(visibilityString))
            return Modifier.PUBLIC;
        return VISIBILITY_CODE_INVALID;
    }

    public static void assertVisibility(int visibility)
    {
        Assert.isTrue(visibility == Modifier.PUBLIC
            || visibility == Modifier.PROTECTED || visibility == Modifier.NONE
            || visibility == Modifier.PRIVATE);
    }

    /**
     * Compares two visibilities.
     *
     * @param newVisibility the 'new' visibility
     * @param oldVisibility the 'old' visibility
     * @return <code>true</code> iff the 'new' visibility is strictly higher than the old visibility
     *
     * @see Modifier#PUBLIC
     * @see Modifier#PROTECTED
     * @see Modifier#NONE
     * @see Modifier#PRIVATE
     */
    public static boolean isHigherVisibility(int newVisibility,
        int oldVisibility)
    {
        assertVisibility(oldVisibility);
        assertVisibility(newVisibility);
        switch (oldVisibility)
        {
        case Modifier.PRIVATE:
            return newVisibility == Modifier.NONE
                || newVisibility == Modifier.PUBLIC
                || newVisibility == Modifier.PROTECTED;
        case Modifier.NONE:
            return newVisibility == Modifier.PUBLIC
                || newVisibility == Modifier.PROTECTED;

        case Modifier.PROTECTED:
            return newVisibility == Modifier.PUBLIC;

        case Modifier.PUBLIC:
            return false;
        default:
            Assert.isTrue(false);
            return false;
        }
    }

    public static int getLowerVisibility(int visibility1, int visibility2)
    {
        if (isHigherVisibility(visibility1, visibility2))
            return visibility2;
        else
            return visibility1;
    }

    public static int clearAccessModifiers(int flags)
    {
        return clearFlag(
            Modifier.PROTECTED | Modifier.PUBLIC | Modifier.PRIVATE, flags);
    }

    public static int clearFlag(int flag, int flags)
    {
        return flags & ~flag;
    }
}
