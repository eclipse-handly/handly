/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ondrej Ilcik (Codasip) - Adaptation (adapted from 
 *         org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider)
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.handly.examples.javamodel.ICompilationUnit;
import org.eclipse.handly.examples.javamodel.IField;
import org.eclipse.handly.examples.javamodel.IImportContainer;
import org.eclipse.handly.examples.javamodel.IImportDeclaration;
import org.eclipse.handly.examples.javamodel.IJavaElement;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.examples.javamodel.IJavaProject;
import org.eclipse.handly.examples.javamodel.IMember;
import org.eclipse.handly.examples.javamodel.IMethod;
import org.eclipse.handly.examples.javamodel.IPackageDeclaration;
import org.eclipse.handly.examples.javamodel.IPackageFragment;
import org.eclipse.handly.examples.javamodel.IPackageFragmentRoot;
import org.eclipse.handly.examples.javamodel.IType;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Default strategy of the Java plugin for the construction of Java element icons.
 *
 */
public class JavaElementImageProvider
{
    public Image getImage(Object element) throws CoreException
    {
        ImageDescriptor descriptor = getDescriptor(element);
        if (descriptor == null)
            return null;
        return getRegistry().get(descriptor);
    }

    private ImageDescriptor getDescriptor(Object element) throws CoreException
    {
        if (element instanceof IJavaElement)
        {
            return getJavaImageDescriptor((IJavaElement)element);
        }
        else if (element instanceof IFile)
        {
            IFile file = (IFile)element;
            if (JavaCore.isJavaLikeFileName(file.getName()))
            {
                return getCUResourceImageDescriptor(file);
            }
            return getWorkbenchImageDescriptor(file);
        }
        else if (element instanceof IAdaptable)
        {
            return getWorkbenchImageDescriptor((IAdaptable)element);
        }

        return null;
    }

    private ImageDescriptor getWorkbenchImageDescriptor(IAdaptable adaptable)
    {
        IWorkbenchAdapter wbAdapter =
            (IWorkbenchAdapter)adaptable.getAdapter(IWorkbenchAdapter.class);
        if (wbAdapter == null)
        {
            return null;
        }
        return wbAdapter.getImageDescriptor(adaptable);
    }

    /**
     * Gets an image descriptor for a Java source file which is not on the project build path.
     * 
     * @param file
     * @return descriptor
     */
    public ImageDescriptor getCUResourceImageDescriptor(IFile file)
    {
        return Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
            IJavaImages.IMG_OBJS_CUNIT_RESOURCE);
    }

    public ImageDescriptor getJavaImageDescriptor(IJavaElement element)
        throws CoreException
    {
        ImageDescriptor baseDesc = getBaseImageDescriptor(element);
        if (baseDesc != null)
        {
            int adornmentFlags = computeJavaAdornmentFlags(element);
            return new JavaElementImageDescriptor(baseDesc, adornmentFlags);
        }
        return null;
    }

    /**
     * Returns an image descriptor for a java element. This is the base image, no overlays.
     * @param element the element
     * @param renderFlags the image flags
     * @return returns the image descriptor
     * @throws CoreException 
     */
    public ImageDescriptor getBaseImageDescriptor(IJavaElement element)
        throws CoreException
    {
        if (element instanceof IMethod)
        {
            IMethod method = (IMethod)element;
            IType declType = method.getDeclaringType();
            int flags = method.getFlags();
            if (declType.isEnum() && isDefaultFlag(flags)
                && method.isConstructor())
                return JavaUI.getSharedImages().getImageDescriptor(
                    ISharedImages.IMG_OBJS_PRIVATE);
            return getMethodImageDescriptor(
                declType.isInterface() || declType.isAnnotation(), flags);
        }
        else if (element instanceof IField)
        {
            IMember member = (IMember)element;
            IType declType = member.getDeclaringType();
            return getFieldImageDescriptor(
                declType.isInterface() || declType.isAnnotation(),
                member.getFlags());
        }
        else if (element instanceof IPackageDeclaration)
        {
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_PACKDECL);
        }
        else if (element instanceof IImportDeclaration)
        {
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_IMPDECL);
        }
        else if (element instanceof IImportContainer)
        {
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_IMPCONT);
        }
        else if (element instanceof IType)
        {
            IType type = (IType)element;

            IType declType = type.getDeclaringType();
            boolean isInner = declType != null;
            boolean isInInterfaceOrAnnotation =
                isInner && (declType.isInterface() || declType.isAnnotation());
            return getTypeImageDescriptor(isInner, isInInterfaceOrAnnotation,
                type.getFlags());
        }
        else if (element instanceof IPackageFragmentRoot)
        {
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_PACKFRAG_ROOT);
        }
        else if (element instanceof IPackageFragment)
        {
            return getPackageFragmentIcon(element);
        }
        else if (element instanceof ICompilationUnit)
        {
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_CUNIT);
        }
        else if (element instanceof IJavaProject)
        {
            IJavaProject jp = (IJavaProject)element;
            if (jp.getProject().isOpen())
            {
                IProject project = jp.getProject();
                IWorkbenchAdapter adapter =
                    (IWorkbenchAdapter)project.getAdapter(
                        IWorkbenchAdapter.class);
                if (adapter != null)
                {
                    ImageDescriptor result =
                        adapter.getImageDescriptor(project);
                    if (result != null)
                        return result;
                }
                return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
                    IDE.SharedImages.IMG_OBJ_PROJECT);
            }
            return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
                IDE.SharedImages.IMG_OBJ_PROJECT_CLOSED);
        }
        else if (element instanceof IJavaModel)
        {
            return Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                IJavaImages.IMG_OBJS_JAVA_MODEL);
        }
        else
        {
            IWorkbenchAdapter wbAdapter =
                (IWorkbenchAdapter)element.getAdapter(IWorkbenchAdapter.class);
            if (wbAdapter != null)
            {
                ImageDescriptor imageDescriptor =
                    wbAdapter.getImageDescriptor(element);
                if (imageDescriptor != null)
                {
                    return imageDescriptor;
                }
            }
            return null;
        }
    }

    private int computeJavaAdornmentFlags(IJavaElement element)
        throws CoreException
    {
        int flags = 0;
        try
        {
            if (element instanceof IMember)
            {
                IMember member = (IMember)element;

                int modifiers = member.getFlags();
                if (Flags.isAbstract(modifiers) && confirmAbstract(member))
                    flags |= JavaElementImageDescriptor.ABSTRACT;
                if (Flags.isFinal(modifiers)
                    || isInterfaceOrAnnotationField(member)
                    || isEnumConstant(member, modifiers))
                    flags |= JavaElementImageDescriptor.FINAL;
                if (Flags.isStatic(modifiers)
                    || isInterfaceOrAnnotationFieldOrType(member)
                    || isEnumConstant(member, modifiers))
                    flags |= JavaElementImageDescriptor.STATIC;

                if (element instanceof IMethod)
                {
                    if (((IMethod)element).isConstructor())
                        flags |= JavaElementImageDescriptor.CONSTRUCTOR;
                    if (Flags.isSynchronized(modifiers)) // collides with 'super' flag
                        flags |= JavaElementImageDescriptor.SYNCHRONIZED;
                    if (Flags.isNative(modifiers))
                        flags |= JavaElementImageDescriptor.NATIVE;
                }

                if (element instanceof IField)
                {
                    if (Flags.isVolatile(modifiers))
                        flags |= JavaElementImageDescriptor.VOLATILE;
                    if (Flags.isTransient(modifiers))
                        flags |= JavaElementImageDescriptor.TRANSIENT;
                }
            }
        }
        catch (JavaModelException e)
        {
            // do nothing. Can't compute runnable adornment or get flags
            Activator.log(e.getStatus());
        }
        return flags;
    }

    private ImageDescriptorRegistry getRegistry()
    {
        return Activator.getImageDescriptorRegistry();
    }

    private static boolean isDefaultFlag(int flags)
    {
        return !Flags.isPublic(flags) && !Flags.isProtected(flags)
            && !Flags.isPrivate(flags);
    }

    public static ImageDescriptor getMethodImageDescriptor(
        boolean isInterfaceOrAnnotation, int flags)
    {
        if (Flags.isPublic(flags) || isInterfaceOrAnnotation)
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_PUBLIC);
        if (Flags.isProtected(flags))
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_PROTECTED);
        if (Flags.isPrivate(flags))
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_PRIVATE);

        return JavaUI.getSharedImages().getImageDescriptor(
            ISharedImages.IMG_OBJS_DEFAULT);
    }

    public static ImageDescriptor getFieldImageDescriptor(
        boolean isInInterfaceOrAnnotation, int flags)
    {
        if (Flags.isPublic(flags) || isInInterfaceOrAnnotation
            || Flags.isEnum(flags))
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_FIELD_PUBLIC);
        if (Flags.isProtected(flags))
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_FIELD_PROTECTED);
        if (Flags.isPrivate(flags))
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_FIELD_PRIVATE);

        return JavaUI.getSharedImages().getImageDescriptor(
            ISharedImages.IMG_FIELD_PRIVATE);
    }

    private ImageDescriptor getPackageFragmentIcon(IJavaElement element)
        throws JavaModelException
    {
        IPackageFragment fragment = (IPackageFragment)element;
        boolean containsJavaElements = false;
        boolean containsNonJavaElements = false;
        try
        {
            containsJavaElements = fragment.getCompilationUnits().length > 0;
            containsNonJavaElements = fragment.getNonJavaResources().length > 0;
        }
        catch (CoreException e)
        {
            // log and assume no children
            Activator.log(e.getStatus());
        }
        if (!containsJavaElements && containsNonJavaElements)
            return Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                IJavaImages.IMG_OBJS_EMPTY_PACK_RESOURCE);
        else if (!containsJavaElements)
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_EMPTY_PACKAGE);
        return JavaUI.getSharedImages().getImageDescriptor(
            ISharedImages.IMG_OBJS_PACKAGE);
    }

    public static ImageDescriptor getTypeImageDescriptor(boolean isInner,
        boolean isInInterfaceOrAnnotation, int flags)
    {
        if (Flags.isEnum(flags))
        {
            if (isInner)
            {
                return getInnerEnumImageDescriptor(isInInterfaceOrAnnotation,
                    flags);
            }
            return getEnumImageDescriptor(flags);
        }
        else if (Flags.isAnnotation(flags))
        {
            if (isInner)
            {
                return getInnerAnnotationImageDescriptor(
                    isInInterfaceOrAnnotation, flags);
            }
            return getAnnotationImageDescriptor(flags);
        }
        else if (Flags.isInterface(flags))
        {
            if (isInner)
            {
                return getInnerInterfaceImageDescriptor(
                    isInInterfaceOrAnnotation, flags);
            }
            return getInterfaceImageDescriptor(flags);
        }
        else
        {
            if (isInner)
            {
                return getInnerClassImageDescriptor(isInInterfaceOrAnnotation,
                    flags);
            }
            return getClassImageDescriptor(flags);
        }
    }

    private static ImageDescriptor getClassImageDescriptor(int flags)
    {
        if (Flags.isPublic(flags) || Flags.isProtected(flags)
            || Flags.isPrivate(flags))
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_CLASS);
        else
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_CLASS_DEFAULT);
    }

    private static ImageDescriptor getInnerClassImageDescriptor(
        boolean isInInterfaceOrAnnotation, int flags)
    {
        if (Flags.isPublic(flags) || isInInterfaceOrAnnotation)
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_INNER_CLASS_PUBLIC);
        else if (Flags.isPrivate(flags))
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_INNER_CLASS_PRIVATE);
        else if (Flags.isProtected(flags))
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_INNER_CLASS_PROTECTED);
        else
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_INNER_CLASS_DEFAULT);
    }

    private static ImageDescriptor getInnerEnumImageDescriptor(
        boolean isInInterfaceOrAnnotation, int flags)
    {
        if (Flags.isPublic(flags) || isInInterfaceOrAnnotation)
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_ENUM);
        else if (Flags.isPrivate(flags))
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_ENUM_PRIVATE);
        else if (Flags.isProtected(flags))
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_ENUM_PROTECTED);
        else
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_ENUM_DEFAULT);
    }

    private static ImageDescriptor getEnumImageDescriptor(int flags)
    {
        if (Flags.isPublic(flags) || Flags.isProtected(flags)
            || Flags.isPrivate(flags))
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_ENUM);
        else
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_ENUM_DEFAULT);
    }

    private static ImageDescriptor getAnnotationImageDescriptor(int flags)
    {
        if (Flags.isPublic(flags) || Flags.isProtected(flags)
            || Flags.isPrivate(flags))
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_ANNOTATION);
        else
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_ANNOTATION_DEFAULT);
    }

    private static ImageDescriptor getInnerAnnotationImageDescriptor(
        boolean isInInterfaceOrAnnotation, int flags)
    {
        if (Flags.isPublic(flags) || isInInterfaceOrAnnotation)
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_ANNOTATION);
        else if (Flags.isPrivate(flags))
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_ANNOTATION_PRIVATE);
        else if (Flags.isProtected(flags))
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_ANNOTATION_PROTECTED);
        else
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_ANNOTATION_DEFAULT);
    }

    private static ImageDescriptor getInterfaceImageDescriptor(int flags)
    {
        if (Flags.isPublic(flags) || Flags.isProtected(flags)
            || Flags.isPrivate(flags))
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_INTERFACE);
        else
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_INTERFACE_DEFAULT);
    }

    private static ImageDescriptor getInnerInterfaceImageDescriptor(
        boolean isInInterfaceOrAnnotation, int flags)
    {
        if (Flags.isPublic(flags) || isInInterfaceOrAnnotation)
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_INNER_INTERFACE_PUBLIC);
        else if (Flags.isPrivate(flags))
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_INNER_INTERFACE_PRIVATE);
        else if (Flags.isProtected(flags))
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_INNER_INTERFACE_PROTECTED);
        else
            return JavaUI.getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_INTERFACE_DEFAULT);
    }

    private static boolean confirmAbstract(IMember element) throws CoreException
    {
        if (element instanceof IType)
        {
            return !((IType)element).isInterface();
        }
        return !element.getDeclaringType().isInterface();
    }

    private static boolean isEnumConstant(IMember element, int modifiers)
    {
        if (element instanceof IField)
        {
            return Flags.isEnum(modifiers);
        }
        return false;
    }

    private static boolean isInterfaceOrAnnotationField(IMember element)
        throws CoreException
    {
        // always show the final symbol on interface fields
        if (element instanceof IField)
        {
            return element.getDeclaringType().isInterface();
        }
        return false;
    }

    private static boolean isInterfaceOrAnnotationFieldOrType(IMember element)
        throws CoreException
    {
        // always show the static symbol on interface fields and types
        if (element instanceof IField)
        {
            return element.getDeclaringType().isInterface();
        }
        else if (element instanceof IType && element.getDeclaringType() != null)
        {
            return element.getDeclaringType().isInterface();
        }
        return false;
    }
}
