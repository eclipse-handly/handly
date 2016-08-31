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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.handly.examples.javamodel.IPackageFragment;
import org.eclipse.handly.examples.javamodel.IPackageFragmentRoot;
import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.handly.model.impl.Element;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;

/**
 * Implementation of {@link IPackageFragmentRoot}.
 */
public class PackageFragmentRoot
    extends Element
    implements IPackageFragmentRoot, IJavaElementInternal
{
    private final IResource resource;

    /**
     * Constructs a package fragment root with the given parent element
     * and the given underlying resource.
     *
     * @param parent the parent of the element (not <code>null</code>)
     * @param resource the resource underlying the element (not <code>null</code>)
     */
    public PackageFragmentRoot(JavaProject parent, IResource resource)
    {
        super(parent, resource.getName());
        if (parent == null)
            throw new IllegalArgumentException();
        this.resource = resource;
    }

    @Override
    public JavaProject getParent()
    {
        return (JavaProject)hParent();
    }

    @Override
    public PackageFragment getPackageFragment(String packageName)
    {
        return new PackageFragment(this, packageName);
    }

    PackageFragment getPackageFragment(String[] simpleNames)
    {
        return new PackageFragment(this, simpleNames);
    }

    @Override
    public IPackageFragment[] getPackageFragments() throws CoreException
    {
        IElement[] children = getChildren();
        int length = children.length;
        IPackageFragment[] result = new IPackageFragment[length];
        System.arraycopy(children, 0, result, 0, length);
        return result;
    }

    @Override
    public Object[] getNonJavaResources() throws CoreException
    {
        return ((PackageFragmentRootBody)hBody()).getNonJavaResources(this);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof PackageFragmentRoot))
            return false;
        PackageFragmentRoot other = (PackageFragmentRoot)o;
        return resource.equals(other.resource) && getParent().equals(
            other.getParent());
    }

    @Override
    public int hashCode()
    {
        return resource.hashCode();
    }

    @Override
    public IResource hResource()
    {
        return resource;
    }

    @Override
    protected void hValidateExistence() throws CoreException
    {
        validateOnClasspath();

        if (!resource.exists())
            throw new CoreException(Activator.createErrorStatus(
                MessageFormat.format("Resource ''{0}'' does not exist",
                    resource.getFullPath()), null));
    }

    protected void validateOnClasspath() throws CoreException
    {
        IClasspathEntry[] rawClasspath = getParent().getRawClasspath();
        if (!ClasspathUtil.isSourceFolder(resource, rawClasspath))
            throw new CoreException(Activator.createErrorStatus(
                MessageFormat.format("Not a source folder: {0}", getPath()),
                null));
    }

    @Override
    protected Object hNewBody()
    {
        return new PackageFragmentRootBody();
    }

    @Override
    protected void hBuildStructure(Object body,
        Map<IElement, Object> newElements, IProgressMonitor monitor)
        throws CoreException
    {
        if (resource.getType() == IResource.FOLDER
            || resource.getType() == IResource.PROJECT)
        {
            IContainer rootFolder = (IContainer)resource;
            ArrayList<IPackageFragment> children = new ArrayList<>();
            computeFolderChildren(rootFolder, Path.EMPTY, children);
            ((Body)body).setChildren(children.toArray(Body.NO_CHILDREN));
        }
    }

    private void computeFolderChildren(IContainer folder, IPath packagePath,
        ArrayList<IPackageFragment> children) throws CoreException
    {
        children.add(new PackageFragment(this, packagePath.segments()));

        IResource[] members = folder.members();
        if (members.length > 0)
        {
            JavaProject javaProject = getParent();
            String sourceLevel = javaProject.getOption(JavaCore.COMPILER_SOURCE,
                true);
            String complianceLevel = javaProject.getOption(
                JavaCore.COMPILER_COMPLIANCE, true);
            for (IResource member : members)
            {
                if (member instanceof IFolder)
                {
                    String memberName = member.getName();
                    if (JavaConventions.validateIdentifier(memberName,
                        sourceLevel,
                        complianceLevel).getSeverity() != IStatus.ERROR)
                    {
                        computeFolderChildren((IFolder)member,
                            packagePath.append(memberName), children);
                    }
                }
            }
        }
    }
}
