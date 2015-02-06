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
package org.eclipse.handly.examples.javamodel;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * A Java project represents a view of a project resource in terms of Java
 * elements. Each Java project has a classpath, defining which folders
 * contain source code, etc.
 * <p>
 * The children of a Java project are the package fragment roots that are
 * defined by the classpath. They appear in the order they are defined
 * by the classpath.
 * </p>
 */
public interface IJavaProject
    extends IJavaElement
{
    /**
     * The identifier for the Java nature.
     */
    String NATURE_ID = "org.eclipse.jdt.core.javanature"; //$NON-NLS-1$

    /**
     * Returns the <code>IProject</code> on which this <code>IJavaProject</code>
     * was created. This is handle-only method.
     *
     * @return the <code>IProject</code> on which this <code>IJavaProject</code>
     *  was created (never <code>null</code>)
     */
    IProject getProject();

    /**
     * Returns a package fragment root for the given resource.
     * This is a handle-only method. The underlying resource may
     * or may not exist.
     *
     * @param resource the given resource (not <code>null</code>)
     * @return a package fragment root for the given resource,
     *  or <code>null</code> if unable to associate the given resource
     *  with a package fragment root
     */
    IPackageFragmentRoot getPackageFragmentRoot(IResource resource);

    /**
     * Returns the package fragment roots contained in this project.
     * The package fragment roots appear in the order they are defined
     * by the classpath. This is equivalent to <code>getChildren()</code>.
     *
     * @return the package fragment roots contained in this project
     *  (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    IPackageFragmentRoot[] getPackageFragmentRoots() throws CoreException;

    /**
     * Returns the non-Java resources directly contained in this project.
     * Non-Java resources include other files and folders located in the
     * project not accounted for by any of its package fragment roots.
     *
     * @return the non-Java resources (<code>IFile</code>s and/or
     *  <code>IFolder</code>s) directly contained in this project
     *  (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    IResource[] getNonJavaResources() throws CoreException;

    /**
     * Returns the package fragment on this project's classpath
     * the given resource corresponds to, or <code>null</code> if none.
     *
     * @param resource the given resource (not <code>null</code>)
     * @return the package fragment on this project's classpath
      * the given resource corresponds to, or <code>null</code> if none
     */
    IPackageFragment findPackageFragment(IResource resource);
}
