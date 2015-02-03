/*******************************************************************************
 * Copyright (c) 2015 1C LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.model.impl.Body;

/**
 * <code>Body</code> extension for the Java model.
 */
public class JavaModelBody
    extends Body
{
    private volatile IProject[] nonJavaProjects;

    public IProject[] getNonJavaProjects(JavaModel javaModel)
        throws CoreException
    {
        IProject[] nonJavaProjects = this.nonJavaProjects;
        if (nonJavaProjects == null)
        {
            nonJavaProjects = computeNonJavaProjects(javaModel);
            this.nonJavaProjects = nonJavaProjects;
        }
        return nonJavaProjects;
    }

    void setNonJavaProjects(IProject[] projects)
    {
        this.nonJavaProjects = projects;
    }

    private IProject[] computeNonJavaProjects(JavaModel javaModel)
        throws CoreException
    {
        ArrayList<IProject> result = new ArrayList<IProject>();
        IProject[] projects = javaModel.getWorkspace().getRoot().getProjects();
        for (IProject project : projects)
        {
            if (!project.isOpen() || !project.hasNature(JavaProject.NATURE_ID))
                result.add(project);
        }
        return result.toArray(new IProject[result.size()]);
    }
}
