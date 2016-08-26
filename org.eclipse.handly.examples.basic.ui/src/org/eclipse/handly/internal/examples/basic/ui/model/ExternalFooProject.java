/*******************************************************************************
 * Copyright (c) 2015, 2016 1C-Soft LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.basic.ui.model;

import java.net.URI;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.internal.examples.basic.ui.Activator;

/**
 * Bridges the gap between Foo files external to the workspace and the Foo model.
 * Each external Foo file is parented by its own external Foo project. External
 * Foo projects have a {@link ExternalFooProject#EXTERNAL_PROJECT_NAME special
 * name} and never exist.
 */
public class ExternalFooProject
    extends FooProject
{
    /**
     * The name reserved for external Foo projects.
     */
    public static final String EXTERNAL_PROJECT_NAME = " ";

    /**
     * Constructs a handle for external Foo project. The constructed instance
     * is equal only to itself.
     */
    public ExternalFooProject()
    {
        super(FooModelManager.INSTANCE.getFooModel(),
            ResourcesPlugin.getWorkspace().getRoot().getProject(
                EXTERNAL_PROJECT_NAME));
    }

    @Override
    public boolean equals(Object o)
    {
        return this == o;
    }

    @Override
    public void create(URI location, IProgressMonitor monitor)
        throws CoreException
    {
        throw new CoreException(Activator.createErrorStatus(
            "Cannot create external project", null));
    }

    @Override
    protected void hValidateExistence(IContext context) throws CoreException
    {
        throw new CoreException(Activator.createErrorStatus(
            "External project never exists", null));
    }
}
