/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ondrej Ilcik (Codasip) - adaptation (adopted from
 *         org.eclipse.jdt.ui.JavaElementComparator)
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel.ui;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.examples.javamodel.ICompilationUnit;
import org.eclipse.handly.examples.javamodel.IField;
import org.eclipse.handly.examples.javamodel.IImportContainer;
import org.eclipse.handly.examples.javamodel.IImportDeclaration;
import org.eclipse.handly.examples.javamodel.IJavaElement;
import org.eclipse.handly.examples.javamodel.IJavaProject;
import org.eclipse.handly.examples.javamodel.IMember;
import org.eclipse.handly.examples.javamodel.IMethod;
import org.eclipse.handly.examples.javamodel.IPackageDeclaration;
import org.eclipse.handly.examples.javamodel.IPackageFragment;
import org.eclipse.handly.examples.javamodel.IPackageFragmentRoot;
import org.eclipse.handly.examples.javamodel.IType;
import org.eclipse.handly.internal.examples.javamodel.ui.preferences.MembersOrderPreferenceCache;
import org.eclipse.handly.internal.examples.javamodel.ui.util.JavaFlags;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

/**
 * Viewer comparator for Java elements. 
 */
public class JavaElementComparator
    extends ViewerComparator
{
    private static final int PROJECTS = 1;
    private static final int PACKAGEFRAGMENTROOTS = 2;
    private static final int PACKAGEFRAGMENT = 3;

    private static final int COMPILATIONUNITS = 4;

    private static final int RESOURCEFOLDERS = 7;
    private static final int RESOURCES = 8;

    private static final int PACKAGE_DECL = 10;
    private static final int IMPORT_CONTAINER = 11;
    private static final int IMPORT_DECLARATION = 12;

    // Includes all categories ordered using the OutlineSortOrderPage:
    // types, initializers, methods & fields
    private static final int MEMBERSOFFSET = 15;

    private static final int JAVAELEMENTS = 50;
    private static final int OTHERS = 51;
    private MembersOrderPreferenceCache memberOrderCache;

    {
        memberOrderCache = Activator.getMemberOrderPreferenceCache();
    }

    @Override
    public int category(Object element)
    {
        try
        {
            if (element instanceof IJavaElement)
            {
                if (element instanceof IMethod)
                {
                    IMethod method = (IMethod)element;
                    if (method.isConstructor())
                    {
                        return getMemberCategory(
                            MembersOrderPreferenceCache.CONSTRUCTORS_INDEX);
                    }
                    int flags = method.getFlags();
                    if (Flags.isStatic(flags))
                        return getMemberCategory(
                            MembersOrderPreferenceCache.STATIC_METHODS_INDEX);
                    else
                        return getMemberCategory(
                            MembersOrderPreferenceCache.METHOD_INDEX);
                }
                else if (element instanceof IField)
                {
                    int flags = ((IField)element).getFlags();
                    if (Flags.isEnum(flags))
                    {
                        return getMemberCategory(
                            MembersOrderPreferenceCache.ENUM_CONSTANTS_INDEX);
                    }
                    if (Flags.isStatic(flags))
                        return getMemberCategory(
                            MembersOrderPreferenceCache.STATIC_FIELDS_INDEX);
                    else
                        return getMemberCategory(
                            MembersOrderPreferenceCache.FIELDS_INDEX);
                }
                else if (element instanceof IType)
                {
                    return getMemberCategory(
                        MembersOrderPreferenceCache.TYPE_INDEX);
                }
                else if (element instanceof IPackageDeclaration)
                {
                    return PACKAGE_DECL;
                }
                else if (element instanceof IImportContainer)
                {
                    return IMPORT_CONTAINER;
                }
                else if (element instanceof IImportDeclaration)
                {
                    return IMPORT_DECLARATION;
                }
                else if (element instanceof IPackageFragment)
                {
                    return PACKAGEFRAGMENT;
                }
                else if (element instanceof IPackageFragmentRoot)
                {
                    return PACKAGEFRAGMENTROOTS;
                }
                else if (element instanceof IJavaProject)
                {
                    return PROJECTS;
                }
                else if (element instanceof ICompilationUnit)
                {
                    return COMPILATIONUNITS;
                }
                return JAVAELEMENTS;
            }
            else if (element instanceof IFile)
            {
                return RESOURCES;
            }
            else if (element instanceof IProject)
            {
                return PROJECTS;
            }
            else if (element instanceof IContainer)
            {
                return RESOURCEFOLDERS;
            }
        }
        catch (CoreException e)
        {
            Activator.log(e.getStatus());
        }
        return OTHERS;
    }

    @Override
    public int compare(Viewer viewer, Object e1, Object e2)
    {
        if (e1 instanceof IMember)
        {
            if (memberOrderCache.isSortByVisibility())
            {
                try
                {
                    int flags1 = JavaFlags.getVisibilityCode((IMember)e1);
                    int flags2 = JavaFlags.getVisibilityCode((IMember)e2);
                    int vis = memberOrderCache.getVisibilityIndex(flags1)
                        - memberOrderCache.getVisibilityIndex(flags2);
                    if (vis != 0)
                    {
                        return vis;
                    }
                }
                catch (CoreException e)
                {
                    Activator.log(e.getStatus());
                }
            }
        }

        return super.compare(viewer, e1, e2);
    }

    private int getMemberCategory(int kind)
    {
        int offset = memberOrderCache.getCategoryIndex(kind);
        return offset + MEMBERSOFFSET;
    }
}
