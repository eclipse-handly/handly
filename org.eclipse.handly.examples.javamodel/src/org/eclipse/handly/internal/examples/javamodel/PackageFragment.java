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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.examples.javamodel.IPackageFragment;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.handly.model.impl.Handle;
import org.eclipse.handly.model.impl.HandleManager;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;

/**
 * Implementation of {@link IPackageFragment}
 */
public class PackageFragment
    extends Handle
    implements IPackageFragment
{
    private final String[] simpleNames;

    /**
     * Constructs a package fragment with the given parent element 
     * and the given name.
     *
     * @param parent the parent of the element (not <code>null</code>)
     * @param name the name of the element (not <code>null</code>) 
     */
    public PackageFragment(PackageFragmentRoot parent, String name)
    {
        super(parent, name);
        if (parent == null)
            throw new IllegalArgumentException();
        if (name == null)
            throw new IllegalArgumentException();
        simpleNames = Signature.getSimpleNames(name);
    }

    PackageFragment(PackageFragmentRoot parent, String[] simpleNames)
    {
        super(parent, Signature.toQualifiedName(simpleNames));
        if (parent == null)
            throw new IllegalArgumentException();
        this.simpleNames = simpleNames;
    }

    @Override
    public PackageFragmentRoot getParent()
    {
        return (PackageFragmentRoot)parent;
    }

    @Override
    public IJavaModel getRoot()
    {
        return (IJavaModel)super.getRoot();
    }

    @Override
    public IResource getResource()
    {
        if (simpleNames.length == 0)
            return parent.getResource();
        IPath path = Path.EMPTY;
        for (String simpleName : simpleNames)
            path = path.append(simpleName);
        return ((IContainer)parent.getResource()).getFolder(path);
    }

    public boolean isValidPackageName()
    {
        JavaProject javaProject = getParent().getParent();
        String sourceLevel =
            javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
        String complianceLevel =
            javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);
        for (String simpleName : simpleNames)
        {
            if (JavaConventions.validateIdentifier(simpleName, sourceLevel,
                complianceLevel).getSeverity() == IStatus.ERROR)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    protected HandleManager getHandleManager()
    {
        return JavaModelManager.INSTANCE.getHandleManager();
    }

    @Override
    protected void validateExistence() throws CoreException
    {
        if (!isValidPackageName())
            throw new CoreException(Activator.createErrorStatus(
                MessageFormat.format("Invalid Java package name: {0}", name),
                null));

        IResource resource = getResource();
        if (resource != null && !resource.isAccessible())
            throw new CoreException(Activator.createErrorStatus(
                MessageFormat.format("Resource ''{0}'' is not accessible",
                    resource.getFullPath()), null));
    }

    @Override
    protected void buildStructure(Body body, Map<IHandle, Body> newElements)
        throws CoreException
    {
        // TODO Auto-generated method stub
    }
}
