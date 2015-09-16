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
package org.eclipse.handly.internal.examples.javamodel.ui.filters;

import java.util.Objects;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.handly.examples.javamodel.IJavaProject;
import org.eclipse.handly.examples.javamodel.JavaModelCore;
import org.eclipse.handly.internal.examples.javamodel.JavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Filters out project's output folder.
 */
public class OutputFolderFilter
    extends ViewerFilter
{

    public OutputFolderFilter()
    {
    }

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element)
    {
        if (element instanceof IFolder)
        {
            IFolder folder = (IFolder)element;
            IProject proj = folder.getProject();
            try
            {
                if (!proj.hasNature(JavaCore.NATURE_ID))
                    return true;

                IJavaProject jProject =
                    JavaModelCore.create(folder.getProject());
                if (jProject == null || !jProject.exists())
                    return true;

                IPath outputLoc = ((JavaProject)jProject).getOutputLocation();
                if (Objects.equals(outputLoc, folder.getFullPath()))
                {
                    return false;
                }
            }
            catch (CoreException ex)
            {
                return true;
            }
        }
        return true;
    }
}
