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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.examples.javamodel.IJavaProject;
import org.eclipse.handly.examples.javamodel.IPackageFragmentRoot;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.handly.model.impl.Handle;
import org.eclipse.handly.model.impl.HandleManager;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

/**
 * Implementation of {@link IJavaProject}.
 */
public class JavaProject
    extends Handle
    implements IJavaProject
{
    private final IProject project;

    /**
     * Constructs a handle for a Java project with the given parent element 
     * and the given underlying workspace project.
     * 
     * @param parent the parent of the element (not <code>null</code>)
     * @param project the workspace project underlying the element 
     *  (not <code>null</code>)
     */
    public JavaProject(JavaModel parent, IProject project)
    {
        super(parent, project.getName());
        if (parent == null)
            throw new IllegalArgumentException();
        this.project = project;
    }

    @Override
    public JavaModel getParent()
    {
        return (JavaModel)parent;
    }

    @Override
    public IJavaModel getRoot()
    {
        return (IJavaModel)super.getRoot();
    }

    @Override
    public IProject getProject()
    {
        return project;
    }

    @Override
    public IResource getResource()
    {
        return project;
    }

    @Override
    public IPackageFragmentRoot getPackageFragmentRoot(IResource resource)
    {
        // In this example model, only folders that are direct children of the project
        // can be viewed as a package fragment root (representing a source folder)
        if (resource != null && resource.getType() == IResource.FOLDER
            && resource.getParent().equals(project))
        {
            return new PackageFragmentRoot(this, resource);
        }
        return null;
    }

    @Override
    public IPackageFragmentRoot[] getPackageFragmentRoots()
        throws CoreException
    {
        IHandle[] children = getChildren();
        int length = children.length;
        IPackageFragmentRoot[] result = new IPackageFragmentRoot[length];
        System.arraycopy(children, 0, result, 0, length);
        return result;
    }

    @Override
    public IResource[] getNonJavaResources() throws CoreException
    {
        return ((JavaProjectBody)getBody()).getNonJavaResources(this);
    }

    @Override
    protected HandleManager getHandleManager()
    {
        return JavaModelManager.INSTANCE.getHandleManager();
    }

    @Override
    protected void validateExistence() throws CoreException
    {
        if (!project.exists())
            throw new CoreException(Activator.createErrorStatus(
                MessageFormat.format(
                    "Project ''{0}'' does not exist in workspace", name), null));

        if (!project.isOpen())
            throw new CoreException(
                Activator.createErrorStatus(
                    MessageFormat.format("Project ''{0}'' is not open", name),
                    null));

        if (!project.hasNature(NATURE_ID))
            throw new CoreException(Activator.createErrorStatus(
                MessageFormat.format(
                    "Project ''{0}'' does not have the Java nature", name),
                null));
    }

    @Override
    protected void buildStructure(Body body, Map<IHandle, Body> newElements)
        throws CoreException
    {
        IClasspathEntry[] rawClasspath = getRawClasspath();
        List<IPackageFragmentRoot> roots =
            new ArrayList<IPackageFragmentRoot>();
        IPath projectPath = getPath();
        for (IClasspathEntry classpathEntry : rawClasspath)
        {
            IPackageFragmentRoot root = null;
            IPath entryPath = classpathEntry.getPath();
            int entryKind = classpathEntry.getEntryKind();
            // In this example model, only source folders that are
            // direct children of the project resource are represented
            // as package fragment roots
            if (entryKind == IClasspathEntry.CPE_SOURCE)
            {
                if (projectPath.isPrefixOf(entryPath)
                    && entryPath.segmentCount() == 2)
                {
                    IResource resource =
                        project.getParent().findMember(entryPath);
                    if (resource != null
                        && resource.getType() == IResource.FOLDER)
                    {
                        root = new PackageFragmentRoot(this, resource);
                    }
                }
            }
            if (root != null)
                roots.add(root);
        }
        body.setChildren(roots.toArray(new IHandle[roots.size()]));
    }

    @Override
    protected Body newBody()
    {
        return new JavaProjectBody();
    }

    IClasspathEntry[] getRawClasspath() throws CoreException
    {
        // TODO Need to cache the result
        return JavaCore.create(project).readRawClasspath();
    }

    String getOption(String optionName, boolean inheritJavaCoreOptions)
    {
        // Cheat and delegate directly to JDT
        return JavaCore.create(project).getOption(optionName,
            inheritJavaCoreOptions);
    }
}
