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
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.handly.model.IElementChangeListener;

/**
 * Represents the root Java element corresponding to the workspace. 
 * Since there is only one such root element, it is commonly referred to 
 * as <em>the</em> Java model element.
 */
public interface IJavaModel
    extends IJavaElement
{
    /**
     * Adds the given listener for changes to elements in the Java model. 
     * Has no effect if an identical listener is already registered. 
     * <p>
     * Once registered, a listener starts receiving notification of changes to
     * elements in the Java model. The listener continues to receive notifications 
     * until it is removed.
     * </p>
     *
     * @param listener the listener (not <code>null</code>)
     * @see #removeElementChangeListener(IElementChangeListener)
     */
    void addElementChangeListener(IElementChangeListener listener);

    /**
     * Removes the given element change listener.
     * Has no effect if an identical listener is not registered.
     *
     * @param listener the listener (not <code>null</code>)
     */
    void removeElementChangeListener(IElementChangeListener listener);

    /**
     * Returns the Java project with the given name. The given name must be 
     * a valid path segment as defined by {@link IPath#isValidSegment(String)}. 
     * This is a handle-only method. The project may or may not exist.
     *
     * @param name the name of the Java project (not <code>null</code>)
     * @return the Java project with the given name (never <code>null</code>)
     */
    IJavaProject getJavaProject(String name);

    /**
     * Returns the Java projects in this Java model.
     *
     * @return the Java projects in this Java model (never <code>null</code>)
     * @throws CoreException if this request fails
     */
    IJavaProject[] getJavaProjects() throws CoreException;

    /**
     * Returns the non-Java projects in the workspace. Non-Java projects
     * include all projects that are closed (even if they have the Java nature).
     *
     * @return the non-Java projects contained in the workspace
     *  (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    IProject[] getNonJavaProjects() throws CoreException;

    /**
     * Returns the workspace associated with this Java model.
     * This is a handle-only method.
     *
     * @return the workspace associated with this Java model
     *  (never <code>null</code>)
     */
    IWorkspace getWorkspace();
}
