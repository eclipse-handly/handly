/*******************************************************************************
 * Copyright (c) 2015, 2016 Codasip Ltd and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ondrej Ilcik (Codasip) - initial API and implementation
 *    Vladimir Piskarev (1C) - ongoing maintenance
 *******************************************************************************/
package org.eclipse.handly.examples.jmodel.ui;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.examples.jmodel.ICompilationUnit;
import org.eclipse.handly.examples.jmodel.IJavaElement;
import org.eclipse.handly.examples.jmodel.IJavaModel;
import org.eclipse.handly.examples.jmodel.IJavaProject;
import org.eclipse.handly.examples.jmodel.IPackageFragment;
import org.eclipse.handly.examples.jmodel.IPackageFragmentRoot;
import org.eclipse.handly.examples.jmodel.JavaModelCore;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Common content provider for Java model.
 */
public class JavaModelContentProvider
    implements ITreeContentProvider
{
    protected static final Object[] NO_CHILDREN = new Object[0];

    @Override
    public Object[] getElements(Object inputElement)
    {
        return getChildren(inputElement);
    }

    @Override
    public Object[] getChildren(Object parentElement)
    {
        if (parentElement instanceof IJavaModel)
        {
            try
            {
                Object[] javaProjects =
                    ((IJavaModel)parentElement).getJavaProjects();
                Object[] nonJavaProjects =
                    ((IJavaModel)parentElement).getNonJavaProjects();
                return concat(javaProjects, nonJavaProjects);
            }
            catch (CoreException e)
            {
            }
        }
        if (parentElement instanceof IJavaProject)
        {
            try
            {
                Object[] children = ((IJavaElement)parentElement).getChildren();
                Object[] nonJavaResources =
                    ((IJavaProject)parentElement).getNonJavaResources();
                return concat(children, nonJavaResources);
            }
            catch (CoreException e)
            {
            }
        }
        if (parentElement instanceof IPackageFragmentRoot)
        {
            try
            {
                IPackageFragment[] children =
                    ((IPackageFragmentRoot)parentElement).getPackageFragments();
                Object[] nonJavaResources =
                    ((IPackageFragmentRoot)parentElement).getNonJavaResources();
                return concat(children, nonJavaResources);
            }
            catch (CoreException e)
            {
            }
        }
        if (parentElement instanceof IPackageFragment)
        {
            try
            {
                Object[] children =
                    ((IPackageFragment)parentElement).getCompilationUnits();
                Object[] nonJavaResources =
                    ((IPackageFragment)parentElement).getNonJavaResources();
                return concat(children, nonJavaResources);
            }
            catch (CoreException e)
            {
            }
        }
        if (parentElement instanceof ICompilationUnit)
        {
            try
            {
                return ((ICompilationUnit)parentElement).getTypes();
            }
            catch (CoreException e)
            {
            }
        }
        if (parentElement instanceof IJavaElement)
        {
            try
            {
                return ((IJavaElement)parentElement).getChildren();
            }
            catch (CoreException e)
            {
            }
        }
        if (parentElement instanceof IContainer)
        {
            try
            {
                return ((IContainer)parentElement).members();
            }
            catch (CoreException e)
            {
            }
        }
        return NO_CHILDREN;
    }

    @Override
    public Object getParent(Object element)
    {
        if (element instanceof IJavaElement)
            return ((IJavaElement)element).getParent();
        if (element instanceof IResource)
        {
            IContainer parent = ((IResource)element).getParent();
            IJavaElement javaParent = JavaModelCore.create(parent);
            if (javaParent != null && javaParent.exists())
                return javaParent;
            return parent;
        }
        return null;
    }

    @Override
    public boolean hasChildren(Object element)
    {
        return getChildren(element).length > 0;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
    }

    @Override
    public void dispose()
    {
    }

    private static Object[] concat(Object[] a, Object[] b)
    {
        Object[] c = new Object[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}
