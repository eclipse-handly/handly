/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ondrej Ilcik (Codasip) - adaptation (adapted from 
 *                  org.eclipse.jdt.internal.ui.filters.EmptyInnerPackageFilter)
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel.ui.filters;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.examples.javamodel.IPackageFragment;
import org.eclipse.handly.internal.examples.javamodel.ui.Activator;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Filters empty non-leaf package fragments.
 */
public class EmptyInnerPackageFilter
    extends ViewerFilter
{
    @Override
    public boolean select(Viewer viewer, Object parent, Object element)
    {
        if (element instanceof IPackageFragment)
        {
            try
            {
                IPackageFragment pkg = (IPackageFragment)element;
                if (pkg.isDefaultPackage())
                    return pkg.getChildren().length > 0;
                return !pkg.hasSubpackages() || pkg.getChildren().length > 0
                    || EmptyPackageFilter.hasUnfilteredResources(viewer, pkg);
            }
            catch (CoreException e)
            {
                Activator.log(e.getStatus());
                return false;
            }
        }

        return true;
    }
}
