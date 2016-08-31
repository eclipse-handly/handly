/*******************************************************************************
 * Copyright (c) 2014 1C LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.examples.basic.ui.model;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.handly.internal.examples.basic.ui.model.FooModelManager;

/**
 * Facade to the Foo Model.
 */
public class FooModelCore
{
    /**
     * Returns the root Foo element.
     *
     * @return the root Foo element (never <code>null</code>)
     */
    public static IFooModel getFooModel()
    {
        return FooModelManager.INSTANCE.getModel();
    }

    /**
     * Returns the Foo file corresponding to the given file,
     * or <code>null</code> if unable to associate the given file
     * with a Foo file.
     *
     * @param file the given file (maybe <code>null</code>)
     * @return the Foo file corresponding to the given file,
     *  or <code>null</code> if unable to associate the given file
     *  with a Foo file
     */
    public static IFooFile create(IFile file)
    {
        if (file == null)
            return null;
        if (file.getParent().getType() != IResource.PROJECT)
            return null;
        IFooProject project = create(file.getProject());
        return project.getFooFile(file.getName());
    }

    /**
     * Returns the Foo project corresponding to the given project.
     * <p>
     * Note that no check is done at this time on the existence
     * or the nature of this project.
     * </p>
     *
     * @param project the given project (maybe <code>null</code>)
     * @return the Foo project corresponding to the given project,
     *  or <code>null</code> if the given project is <code>null</code>
     */
    public static IFooProject create(IProject project)
    {
        if (project == null)
            return null;
        return getFooModel().getFooProject(project.getName());
    }

    /**
     * Returns the Foo element corresponding to the given resource, or
     * <code>null</code> if unable to associate the given resource
     * with an element of the Foo Model.
     *
     * @param resource the given resource (maybe <code>null</code>)
     * @return the Foo element corresponding to the given resource, or
     *  <code>null</code> if unable to associate the given resource
     *  with an element of the Foo Model
     */
    public static IFooElement create(IResource resource)
    {
        if (resource == null)
            return null;
        int type = resource.getType();
        switch (type)
        {
        case IResource.PROJECT:
            return create((IProject)resource);
        case IResource.FILE:
            return create((IFile)resource);
        case IResource.ROOT:
            return getFooModel();
        default:
            return null;
        }
    }

    private FooModelCore()
    {
    }
}
