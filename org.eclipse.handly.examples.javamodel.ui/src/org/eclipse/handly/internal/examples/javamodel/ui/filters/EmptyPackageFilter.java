/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ondrej Ilcik (Codasip) - Handly model adaptation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel.ui.filters;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.examples.javamodel.IPackageFragment;
import org.eclipse.handly.internal.examples.javamodel.ui.Activator;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Filters out all empty package fragments.
 */
public class EmptyPackageFilter
    extends ViewerFilter
{
    @Override
    public boolean select(Viewer viewer, Object parent, Object element)
    {
        if (element instanceof IPackageFragment)
        {
            IPackageFragment pkg = (IPackageFragment)element;
            try
            {
                return pkg.getChildren().length > 0
                    || hasUnfilteredResources(viewer, pkg);
            }
            catch (CoreException e)
            {
                Activator.log(e.getStatus());
                ;
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether the given package has unfiltered resources.
     *
     * @param viewer the viewer
     * @param pkg the package
     * @return <code>true</code> if the package has unfiltered resources
     * @throws CoreException 
     * @since 3.4.1
     */
    static boolean hasUnfilteredResources(Viewer viewer, IPackageFragment pkg)
        throws CoreException
    {
        Object[] resources = pkg.getNonJavaResources();
        int length = resources.length;
        if (length == 0)
            return false;

        if (!(viewer instanceof StructuredViewer))
            return true;

        ViewerFilter[] filters = ((StructuredViewer)viewer).getFilters();
        resourceLoop: for (int i = 0; i < length; i++)
        {
            for (int j = 0; j < filters.length; j++)
            {
                if (!filters[j].select(viewer, pkg, resources[i]))
                    continue resourceLoop;
            }
            return true;

        }
        return false;
    }
}
