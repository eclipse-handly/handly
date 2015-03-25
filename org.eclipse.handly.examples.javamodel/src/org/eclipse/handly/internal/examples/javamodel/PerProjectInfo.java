/*******************************************************************************
 * Copyright (c) 2015 1C-Soft LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

/**
 * Holds cached per-project info that should be accessible
 * without opening a Java element.
 */
public class PerProjectInfo
{
    private final IProject project;
    private volatile IClasspathEntry[] rawClasspath;
    private volatile IPath outputLocation;

    /**
     * Creates a new per-project info for the given project.
     * 
     * @param project not <code>null</code>
     */
    PerProjectInfo(IProject project)
    {
        if (project == null)
            throw new IllegalArgumentException();
        this.project = project;
    }

    /**
     * Returns the per-project info project.
     * 
     * @return the per-project info project (never <code>null</code>)
     */
    public IProject getProject()
    {
        return project;
    }

    /**
     * Returns the cached raw classpath for the project,
     * as a list of classpath entries.
     *
     * @return the cached raw classpath for the project,
     *  as a list of classpath entries (never <code>null</code>)
     * @see IClasspathEntry
     */
    public IClasspathEntry[] getRawClasspath()
    {
        IClasspathEntry[] rawClasspath = this.rawClasspath;
        if (rawClasspath == null)
        {
            rawClasspath = JavaCore.create(project).readRawClasspath();
            this.rawClasspath = rawClasspath;
        }
        return rawClasspath;
    }

    /**
     * Returns the cached output location for the project,
     * as a workspace-relative absolute path.
     * <p>
     * The output location is where class files are generated
     * (and resource files, copied).
     * </p>
     *
     * @return the cached output location for the project,
     *  as a workspace-relative absolute path (never <code>null</code>)
     */
    public IPath getOutputLocation()
    {
        IPath outputLocation = this.outputLocation;
        if (outputLocation == null)
        {
            outputLocation = JavaCore.create(project).readOutputLocation();
            if (outputLocation == null)
            {
                // default output location (the project bin folder)
                outputLocation = project.getFullPath().append("bin"); //$NON-NLS-1$
            }
            this.outputLocation = outputLocation;
        }
        return outputLocation;
    }

    void setRawClasspath(IClasspathEntry[] rawClasspath, IPath outputLocation)
    {
        this.rawClasspath = rawClasspath;
        this.outputLocation = outputLocation;
    }
}
