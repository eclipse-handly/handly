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
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.examples.javamodel.IJavaElement;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.examples.javamodel.IJavaProject;
import org.eclipse.handly.model.IElementChangeListener;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.handly.model.impl.Handle;
import org.eclipse.handly.model.impl.HandleManager;

/**
 * Implementation of {@link IJavaModel}. The Java model maintains a cache of
 * {@link IJavaProject}s in a workspace. A Java model is specific to a workspace.
 */
public class JavaModel
    extends Handle
    implements IJavaModel
{
    private final IWorkspace workspace;

    /**
     * Constructs a new Java model on the given workspace.
     * 
     * @param workspace the workspace underlying the Java model
     *  (not <code>null</code>)
     */
    public JavaModel(IWorkspace workspace)
    {
        super(null, "Java Model"); //$NON-NLS-1$
        if (workspace == null)
            throw new IllegalArgumentException();
        this.workspace = workspace;
    }

    @Override
    public IJavaElement getParent()
    {
        return null;
    }

    @Override
    public IJavaModel getRoot()
    {
        return this;
    }

    @Override
    public void addElementChangeListener(IElementChangeListener listener)
    {
        JavaModelManager.INSTANCE.addElementChangeListener(listener);
    }

    @Override
    public void removeElementChangeListener(IElementChangeListener listener)
    {
        JavaModelManager.INSTANCE.removeElementChangeListener(listener);
    }

    @Override
    public IJavaProject getJavaProject(String name)
    {
        return new JavaProject(this, workspace.getRoot().getProject(name));
    }

    @Override
    public IJavaProject[] getJavaProjects() throws CoreException
    {
        IHandle[] children = getChildren();
        int length = children.length;
        IJavaProject[] result = new IJavaProject[length];
        System.arraycopy(children, 0, result, 0, length);
        return result;
    }

    @Override
    public IProject[] getNonJavaProjects() throws CoreException
    {
        return ((JavaModelBody)getBody()).getNonJavaProjects(this);
    }

    @Override
    public IWorkspace getWorkspace()
    {
        return workspace;
    }

    @Override
    public IResource getResource()
    {
        return workspace.getRoot();
    }

    @Override
    public boolean exists()
    {
        return true; // always exists
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JavaModel other = (JavaModel)obj;
        if (!workspace.equals(other.workspace))
            return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        return workspace.hashCode();
    }

    @Override
    protected HandleManager getHandleManager()
    {
        return JavaModelManager.INSTANCE.getHandleManager();
    }

    @Override
    protected void validateExistence() throws CoreException
    {
        // always exists
    }

    @Override
    protected void buildStructure(Body body, Map<IHandle, Body> newElements)
        throws CoreException
    {
        IProject[] projects = workspace.getRoot().getProjects();
        List<IJavaProject> javaProjects =
            new ArrayList<IJavaProject>(projects.length);
        for (IProject project : projects)
        {
            if (project.isOpen() && project.hasNature(IJavaProject.NATURE_ID))
            {
                javaProjects.add(new JavaProject(this, project));
            }
        }
        body.setChildren(javaProjects.toArray(new IHandle[javaProjects.size()]));
    }

    @Override
    protected Body newBody()
    {
        return new JavaModelBody();
    }
}
