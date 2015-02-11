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
import org.eclipse.handly.examples.javamodel.IPackageFragment;
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
    public PackageFragmentRoot getPackageFragmentRoot(IResource resource)
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
    public IPackageFragment findPackageFragment(IResource resource)
    {
        // In this example model, only folders can correspond to a package fragment
        if (resource.getType() != IResource.FOLDER)
            return null;

        if (!exists())
            return null;

        IPath resourcePath = resource.getFullPath();
        try
        {
            IClasspathEntry[] rawClasspath = getRawClasspath();
            for (IClasspathEntry entry : rawClasspath)
            {
                int entryKind = entry.getEntryKind();

                // In this example model, only source folders are considered
                if (entryKind != IClasspathEntry.CPE_SOURCE)
                    continue;

                IPath entryPath = entry.getPath();
                if (entryPath.equals(resourcePath))
                {
                    PackageFragmentRoot root = getPackageFragmentRoot(resource);
                    if (root == null)
                        return null;
                    return root.getPackageFragment(""); //$NON-NLS-1$
                }
                else if (entryPath.isPrefixOf(resourcePath))
                {
                    IResource rootFolder;
                    if (entryPath.segmentCount() == 1)
                        rootFolder = project;
                    else
                        rootFolder = project.getParent().getFolder(entryPath);

                    PackageFragmentRoot root =
                        getPackageFragmentRoot(rootFolder);
                    if (root == null)
                        return null;

                    IPath packagePath =
                        resourcePath.removeFirstSegments(entryPath.segmentCount());

                    PackageFragment packageFragment =
                        root.getPackageFragment(packagePath.segments());
                    if (!packageFragment.isValidPackageName())
                        return null;

                    return packageFragment;
                }
            }
        }
        catch (CoreException e)
        {
            return null;
        }
        return null;
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
        for (IClasspathEntry entry : rawClasspath)
        {
            IPackageFragmentRoot root = null;
            IPath entryPath = entry.getPath();
            int entryKind = entry.getEntryKind();
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

    @SuppressWarnings("unchecked")
    Map<String, String> getOptions(boolean inheritJavaCoreOptions)
    {
        // Cheat and delegate directly to JDT
        return JavaCore.create(project).getOptions(inheritJavaCoreOptions);
    }
}
