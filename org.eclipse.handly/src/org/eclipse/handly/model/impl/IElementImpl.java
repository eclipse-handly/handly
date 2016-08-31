/*******************************************************************************
 * Copyright (c) 2014, 2016 1C-Soft LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.model.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.model.Elements;
import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.IModel;

/**
 * All {@link IElement}s must implement this interface.
 */
public interface IElementImpl
    extends IElement
{
    /**
     * Returns the name of this element, or <code>null</code>
     * if this element has no name. This is a handle-only method.
     *
     * @return the element name, or <code>null</code> if this element has no name
     */
    String hName();

    /**
     * Returns the element directly containing this element,
     * or <code>null</code> if this element has no parent.
     * This is a handle-only method.
     *
     * @return the parent element, or <code>null</code> if this element has
     *  no parent
     */
    IElement hParent();

    /**
     * Returns the root element containing this element.
     * Returns this element if it has no parent.
     * This is a handle-only method.
     *
     * @return the root element (never <code>null</code>)
     */
    default IElement hRoot()
    {
        IElement parent = hParent();
        if (parent == null)
            return this;
        else
            return Elements.getRoot(parent);
    }

    /**
     * Returns the closest ancestor of this element that has the given type.
     * Returns <code>null</code> if no such ancestor can be found.
     * This is a handle-only method.
     *
     * @param ancestorType the given type (not <code>null</code>)
     * @return the closest ancestor of this element that has the given type,
     *  or <code>null</code> if no such ancestor can be found
     */
    default <T> T hAncestor(Class<T> ancestorType)
    {
        IElement parent = hParent();
        if (parent == null)
            return null;
        if (ancestorType.isInstance(parent))
            return ancestorType.cast(parent);
        return Elements.getAncestor(parent, ancestorType);
    }

    /**
     * Returns the model that owns this element. This is a handle-only method.
     * <p>
     * Note that the relationship between an element and its owing model does
     * not change over the lifetime of an element.
     * </p>
     *
     * @param element not <code>null</code>
     * @return the element's model (never <code>null</code>)
     * @throws IllegalStateException if the model is no longer accessible
     */
    IModel hModel();

    /**
     * Returns the innermost resource enclosing this element, or <code>null</code>
     * if this element is not enclosed in a workspace resource.
     * This is a handle-only method.
     * <p>
     * Note that it is safe to call this method and test the return value
     * for <code>null</code> even when <code>org.eclipse.core.resources</code>
     * bundle is not available.
     * </p>
     *
     * @return the innermost resource enclosing this element, or <code>null</code>
     *  if this element is not enclosed in a workspace resource
     */
    IResource hResource();

    /**
     * Returns the path to the innermost resource enclosing this element.
     * If this element is enclosed in a workspace resource, the path returned
     * is the full, absolute path to the underlying resource, relative to
     * the workspace. Otherwise, the path returned is the absolute path to
     * a file or to a folder in the file system.
     * This is a handle-only method.
     *
     * @return the path to the innermost resource enclosing this element
     *  (never <code>null</code>)
     */
    default IPath hPath()
    {
        IResource resource = hResource();
        if (resource != null)
            return resource.getFullPath();
        throw new AssertionError(
            "Please override the default implementation of this method"); //$NON-NLS-1$
    }

    /**
     * Returns whether this element exists in the model.
     * <p>
     * Handles may or may not be backed by an actual element. Handles that are
     * backed by an actual element are said to "exist". It is always the case
     * that if an element exists, then its parent also exists (provided
     * it has one) and includes that element as one of its children.
     * It is therefore possible to navigate to any existing element
     * from the root element along a chain of existing elements.
     * </p>
     *
     * @return <code>true</code> if this element exists in the model, and
     *  <code>false</code> if this element does not exist
     */
    boolean hExists();

    /**
     * Returns the immediate children of this element. Unless otherwise specified
     * by the implementing element, the children are in no particular order.
     *
     * @return the immediate children of this element (never <code>null</code>).
     *  Clients <b>must not</b> modify the returned array.
     * @throws CoreException if this element does not exist or if an
     *  exception occurs while accessing its corresponding resource
     */
    IElement[] hChildren() throws CoreException;

    /**
     * Returns the immediate children of this element that have the given type.
     * Unless otherwise specified by the implementing element, the children
     * are in no particular order.
     *
     * @param childType the given type (not <code>null</code>)
     * @return the immediate children of this element that have the given type
     *  (never <code>null</code>). Clients <b>must not</b> modify the returned
     *  array.
     * @throws CoreException if this element does not exist or if an
     *  exception occurs while accessing its corresponding resource
     */
    default <T> T[] hChildren(Class<T> childType) throws CoreException
    {
        IElement[] children = hChildren();
        List<T> list = new ArrayList<T>(children.length);
        for (IElement child : children)
        {
            if (childType.isInstance(child))
                list.add(childType.cast(child));
        }
        @SuppressWarnings("unchecked")
        T[] result = (T[])Array.newInstance(childType, list.size());
        return list.toArray(result);
    }

    /**
     * Debugging purposes. Returns a string representation of this element.
     * Note that the format options specified in the given context serve as
     * a hint that implementations may or may not fully support.
     * <p>
     * Implementations are advised to support common hints defined in
     * {@link org.eclipse.handly.util.ToStringOptions ToStringOptions} and
     * interpret the format style as follows:
     * </p>
     * <ul>
     * <li>{@link org.eclipse.handly.util.ToStringOptions.FormatStyle#FULL FULL}
     * - A full representation that lists ancestors and children.</li>
     * <li>{@link org.eclipse.handly.util.ToStringOptions.FormatStyle#LONG LONG}
     * - A long representation that lists children but not ancestors.</li>
     * <li>{@link org.eclipse.handly.util.ToStringOptions.FormatStyle#MEDIUM MEDIUM}
     * - A compact representation that lists ancestors but not children.</li>
     * <li>{@link org.eclipse.handly.util.ToStringOptions.FormatStyle#SHORT SHORT}
     * - A minimal representation that does not list ancestors or children.</li>
     * </ul>
     *
     * @param context not <code>null</code>
     * @return a string representation of this element (never <code>null</code>)
     */
    String hToString(IContext context);
}
