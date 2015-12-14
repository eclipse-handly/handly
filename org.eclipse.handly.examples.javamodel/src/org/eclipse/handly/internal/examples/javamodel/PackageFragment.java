/*******************************************************************************
 * Copyright (c) 2015 1C-Soft LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *     Ondrej Ilcik (Codasip)
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.handly.examples.javamodel.ICompilationUnit;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.examples.javamodel.IPackageFragment;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.handly.model.impl.Handle;
import org.eclipse.handly.model.impl.HandleManager;
import org.eclipse.handly.util.IndentationPolicy;
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

    @Override
    public ICompilationUnit getCompilationUnit(String name)
    {
        if (name == null)
            throw new IllegalArgumentException();
        return new CompilationUnit(this, ((IContainer)getResource()).getFile(
            new Path(name)), null);
    }

    @Override
    public ICompilationUnit[] getCompilationUnits() throws CoreException
    {
        IHandle[] children = getChildren();
        int length = children.length;
        ICompilationUnit[] result = new ICompilationUnit[length];
        System.arraycopy(children, 0, result, 0, length);
        return result;
    }

    @Override
    public Object[] getNonJavaResources() throws CoreException
    {
        // Note: non-java resources of the default package are assigned
        // to the package fragment root.
        if (isDefaultPackage())
            return PackageFragmentBody.NO_NON_JAVA_RESOURCES;
        else
            return ((PackageFragmentBody)getBody()).getNonJavaResources(this);
    }

    @Override
    public boolean isDefaultPackage()
    {
        return name.isEmpty();
    }

    @Override
    public boolean hasSubpackages() throws CoreException
    {
        IPackageFragment[] packages = getParent().getChildren(
            IPackageFragment.class);
        int namesLength = simpleNames.length;
        nextPackage: for (IPackageFragment pkg : packages)
        {
            String[] otherNames = ((PackageFragment)pkg).simpleNames;
            if (otherNames.length <= namesLength)
                continue nextPackage;
            for (int i = 0; i < namesLength; i++)
            {
                if (!simpleNames[i].equals(otherNames[i]))
                    continue nextPackage;
            }
            return true;
        }
        return false;
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

    boolean isValidPackageName()
    {
        JavaProject javaProject = getParent().getParent();
        String sourceLevel = javaProject.getOption(JavaCore.COMPILER_SOURCE,
            true);
        String complianceLevel = javaProject.getOption(
            JavaCore.COMPILER_COMPLIANCE, true);
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
    protected Body newBody()
    {
        return new PackageFragmentBody();
    }

    @Override
    protected void buildStructure(Body body, Map<IHandle, Body> newElements,
        IProgressMonitor monitor) throws CoreException
    {
        HashSet<ICompilationUnit> children = new HashSet<ICompilationUnit>();
        IResource[] members = ((IContainer)getResource()).members();
        if (members.length > 0)
        {
            JavaProject javaProject = getAncestor(JavaProject.class);
            String sourceLevel = javaProject.getOption(JavaCore.COMPILER_SOURCE,
                true);
            String complianceLevel = javaProject.getOption(
                JavaCore.COMPILER_COMPLIANCE, true);
            for (IResource member : members)
            {
                if (member instanceof IFile)
                {
                    if (JavaConventions.validateCompilationUnitName(
                        member.getName(), sourceLevel,
                        complianceLevel).getSeverity() != IStatus.ERROR)
                    {
                        children.add(new CompilationUnit(this, (IFile)member,
                            null));
                    }
                }
            }
        }
        body.setChildren(children.toArray(new IHandle[children.size()]));
    }

    @Override
    protected void toStringName(StringBuilder builder)
    {
        if (isDefaultPackage())
            builder.append("<default>"); //$NON-NLS-1$
        else
            super.toStringName(builder);
    }

    @Override
    protected void toStringBody(IndentationPolicy indentationPolicy,
        int indentationLevel, StringBuilder builder, Body body,
        boolean showResolvedInfo)
    {
        super.toStringBody(indentationPolicy, indentationLevel, builder, body,
            showResolvedInfo);
        if (body != null && indentationLevel > 0)
            builder.append(" (...)"); //$NON-NLS-1$
    }

    @Override
    protected void toStringChildren(IndentationPolicy indentationPolicy,
        int indentationLevel, StringBuilder builder, Body body)
    {
        if (indentationLevel == 0)
            super.toStringChildren(indentationPolicy, indentationLevel, builder,
                body);
    }
}
