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
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.examples.javamodel.IPackageFragmentRoot;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.handly.model.impl.Handle;
import org.eclipse.handly.model.impl.HandleManager;
import org.eclipse.jdt.core.IClasspathEntry;

/**
 * Implementation of {@link IPackageFragmentRoot}.
 */
public class PackageFragmentRoot
    extends Handle
    implements IPackageFragmentRoot
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
        return (JavaProject)parent;
    }

    @Override
    public IJavaModel getRoot()
    {
        return (IJavaModel)super.getRoot();
    }

    @Override
    public IResource getResource()
    {
        return resource;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof PackageFragmentRoot))
            return false;
        PackageFragmentRoot other = (PackageFragmentRoot)o;
        return resource.equals(other.resource) && parent.equals(other.parent);
    }

    @Override
    public int hashCode()
    {
        return resource.hashCode();
    }

    @Override
    protected HandleManager getHandleManager()
    {
        return JavaModelManager.INSTANCE.getHandleManager();
    }

    @Override
    protected void validateExistence() throws CoreException
    {
        validateOnClasspath();

        if (!resource.exists())
            throw new CoreException(Activator.createErrorStatus(
                MessageFormat.format("Resource ''{0}'' does not exist",
                    getPath()), null));
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
    protected void buildStructure(Body body, Map<IHandle, Body> newElements)
        throws CoreException
    {
        // TODO Auto-generated method stub
    }
}
