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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.jdt.core.IClasspathEntry;

/**
 * <code>Body</code> extension for the Java project.
 */
public class JavaProjectBody
    extends Body
{
    private volatile IResource[] nonJavaResources;

    public IResource[] getNonJavaResources(JavaProject javaProject)
        throws CoreException
    {
        IResource[] nonJavaResources = this.nonJavaResources;
        if (nonJavaResources == null)
        {
            nonJavaResources = computeNonJavaResources(javaProject);
            this.nonJavaResources = nonJavaResources;
        }
        return nonJavaResources;
    }

    void setNonJavaResources(IResource[] resources)
    {
        this.nonJavaResources = resources;
    }

    private IResource[] computeNonJavaResources(JavaProject javaProject)
        throws CoreException
    {
        ArrayList<IResource> result = new ArrayList<IResource>();
        IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
        IResource[] members = javaProject.getProject().members();
        for (IResource member : members)
        {
            // In this example model, only source folders that are
            // direct children of the project resource are represented
            // as package fragment roots, and thus must not be included
            // in non-java resources
            if (!ClasspathUtil.isSourceFolder(member, rawClasspath))
                result.add(member);
        }
        return result.toArray(new IResource[result.size()]);
    }
}
