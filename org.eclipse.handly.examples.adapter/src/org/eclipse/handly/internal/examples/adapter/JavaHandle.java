/*******************************************************************************
 * Copyright (c) 2015 1C-Soft LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.adapter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceReference;

/**
 * Adapts a Java element to <code>IHandle</code>.
 */
public class JavaHandle
    extends PlatformObject
    implements IHandle
{
    private final IJavaElement javaElement;

    /**
     * Returns <code>IHandle</code> corresponding to the given Java element.
     *
     * @param javaElement may be <code>null</code>
     * @return <code>IHandle</code> corresponding to the given Java element,
     *  or <code>null</code> if none
     */
    public static IHandle create(IJavaElement javaElement)
    {
        if (javaElement == null)
            return null;
        if (javaElement instanceof ICompilationUnit)
            return new JavaSourceFile((ICompilationUnit)javaElement);
        if (javaElement instanceof IClassFile)
            return new JavaSourceElement(javaElement);
        if (javaElement instanceof ISourceReference)
            return new JavaSourceConstruct(javaElement);
        return new JavaHandle(javaElement);
    }

    /**
     * Constructs a <code>JavaHandle</code> for the given Java element.
     *
     * @param javaElement not <code>null</code>
     */
    JavaHandle(IJavaElement javaElement)
    {
        if (javaElement == null)
            throw new IllegalArgumentException();
        this.javaElement = javaElement;
    }

    /**
     * Returns the underlying Java element.
     *
     * @return the underlying Java element (never <code>null</code>)
     */
    public IJavaElement getJavaElement()
    {
        return javaElement;
    }

    @Override
    public String getName()
    {
        return javaElement.getElementName();
    }

    @Override
    public IHandle getParent()
    {
        return create(javaElement.getParent());
    }

    @Override
    public IHandle getRoot()
    {
        return create(javaElement.getJavaModel());
    }

    @Override
    public <T extends IHandle> T getAncestor(Class<T> ancestorType)
    {
        IHandle parent = getParent();
        if (parent == null)
            return null;
        if (ancestorType.isInstance(parent))
            return ancestorType.cast(parent);
        return parent.getAncestor(ancestorType);
    }

    @Override
    public IResource getResource()
    {
        return javaElement.getResource();
    }

    @Override
    public IPath getPath()
    {
        return javaElement.getPath();
    }

    @Override
    public boolean exists()
    {
        return javaElement.exists();
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
        JavaHandle other = (JavaHandle)obj;
        return javaElement.equals(other.javaElement);
    }

    @Override
    public int hashCode()
    {
        return javaElement.hashCode();
    }

    @Override
    public String toString()
    {
        return javaElement.toString();
    }

    @Override
    public String toString(ToStringStyle style)
    {
        return toString();
    }

    @Override
    public IHandle[] getChildren() throws CoreException
    {
        if (!(javaElement instanceof IParent))
            return Body.NO_CHILDREN;
        IJavaElement[] children = ((IParent)javaElement).getChildren();
        ArrayList<IHandle> result = new ArrayList<IHandle>(children.length);
        for (IJavaElement child : children)
        {
            IHandle handle = create(child);
            if (handle != null)
                result.add(handle);
        }
        return result.toArray(Body.NO_CHILDREN);
    }

    @Override
    public <T extends IHandle> T[] getChildren(Class<T> childType)
        throws CoreException
    {
        IHandle[] children = getChildren();
        List<T> list = new ArrayList<T>(children.length);
        for (IHandle child : children)
        {
            if (childType.isInstance(child))
                list.add(childType.cast(child));
        }
        @SuppressWarnings("unchecked")
        T[] result = (T[])Array.newInstance(childType, list.size());
        return list.toArray(result);
    }
}
