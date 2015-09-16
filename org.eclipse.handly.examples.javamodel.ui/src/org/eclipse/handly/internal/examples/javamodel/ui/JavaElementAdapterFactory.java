/*******************************************************************************
 * Copyright (c) 2015 Codasip Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ondrej Ilcik (Codasip) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.handly.examples.javamodel.ICompilationUnit;
import org.eclipse.handly.examples.javamodel.IJavaElement;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.examples.javamodel.IJavaProject;
import org.eclipse.handly.examples.javamodel.IPackageFragment;
import org.eclipse.handly.examples.javamodel.IPackageFragmentRoot;
import org.eclipse.handly.examples.javamodel.IType;
import org.eclipse.ui.IContainmentAdapter;
import org.eclipse.ui.IPersistableElement;

/**
 * Adapts Java elements to various other types.
 * 
 * @author Ondrej Ilcik
 *
 */
public class JavaElementAdapterFactory
    implements IAdapterFactory
{
    private static Class<?>[] ADAPTER_LIST = new Class[] { IResource.class,
        IPersistableElement.class, IContainmentAdapter.class };

    private static IContainmentAdapter containmentAdapter =
        new JavaElementContainmentAdapter();

    public Class<?>[] getAdapterList()
    {
        return ADAPTER_LIST;
    }

    public Object getAdapter(Object element,
        @SuppressWarnings("rawtypes") Class adapterType)
    {
        IJavaElement java = (IJavaElement)element;
        if (IResource.class.equals(adapterType))
        {
            return getResource(java);
        }
        else if (IPersistableElement.class.equals(adapterType))
        {
            return new PersistableJavaElementFactory(java);
        }
        else if (IContainmentAdapter.class.equals(adapterType))
        {
            return containmentAdapter;
        }

        return null;
    }

    private IResource getResource(IJavaElement element)
    {
        if (element instanceof IType)
        {
            // top level types behave like the CU
            IJavaElement parent = element.getParent();
            if (parent instanceof ICompilationUnit)
            {
                return ((ICompilationUnit)parent).getResource();
            }
        }
        else if (element instanceof ICompilationUnit)
        {
            return ((ICompilationUnit)element).getResource();
        }
        else if (element instanceof IPackageFragment
            || element instanceof IPackageFragmentRoot
            || element instanceof IJavaProject || element instanceof IJavaModel)
        {
            return element.getResource();
        }

        return null;
    }
}
