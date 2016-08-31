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
package org.eclipse.handly.internal.examples.javamodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.examples.javamodel.IJavaProject;
import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.IElementChangeListener;
import org.eclipse.handly.model.IModel;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.handly.model.impl.Element;

/**
 * Implementation of {@link IJavaModel}. The Java model maintains a cache of
 * {@link IJavaProject}s in a workspace. A Java model is specific to a workspace.
 */
public class JavaModel
    extends Element
    implements IJavaModel, IJavaElementInternal, IModel
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
        super(null, null);
        if (workspace == null)
            throw new IllegalArgumentException();
        this.workspace = workspace;
    }

    @Override
    public int getApiLevel()
    {
        return ApiLevel.CURRENT;
    }

    @Override
    public IContext getModelContext()
    {
        return JavaModelManager.INSTANCE.getModelContext();
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
        IElement[] children = getChildren();
        int length = children.length;
        IJavaProject[] result = new IJavaProject[length];
        System.arraycopy(children, 0, result, 0, length);
        return result;
    }

    @Override
    public IProject[] getNonJavaProjects() throws CoreException
    {
        return ((JavaModelBody)hBody()).getNonJavaProjects(this);
    }

    @Override
    public IWorkspace getWorkspace()
    {
        return workspace;
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
    public IResource hResource()
    {
        return workspace.getRoot();
    }

    @Override
    public boolean hExists()
    {
        return true; // always exists
    }

    @Override
    protected void hValidateExistence() throws CoreException
    {
        // always exists
    }

    @Override
    protected void hBuildStructure(Object body,
        Map<IElement, Object> newElements, IProgressMonitor monitor)
        throws CoreException
    {
        IProject[] projects = workspace.getRoot().getProjects();
        List<IJavaProject> javaProjects = new ArrayList<>(projects.length);
        for (IProject project : projects)
        {
            if (project.isOpen() && project.hasNature(IJavaProject.NATURE_ID))
            {
                javaProjects.add(new JavaProject(this, project));
            }
        }
        ((Body)body).setChildren(javaProjects.toArray(Body.NO_CHILDREN));
    }

    @Override
    protected Object hNewBody()
    {
        return new JavaModelBody();
    }

    @Override
    protected void hToStringName(StringBuilder builder, IContext context)
    {
        builder.append("Java Model"); //$NON-NLS-1$
    }
}
