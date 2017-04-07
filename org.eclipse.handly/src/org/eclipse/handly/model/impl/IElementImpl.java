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

import static org.eclipse.handly.context.Contexts.of;
import static org.eclipse.handly.context.Contexts.with;
import static org.eclipse.handly.util.ToStringOptions.FORMAT_STYLE;

import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.model.Elements;
import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.IModel;
import org.eclipse.handly.util.ToStringOptions.FormatStyle;

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
     * @return the element's model (never <code>null</code>)
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
     * Returns a file system location for this element. The resulting URI is
     * suitable to passing to <code>EFS.getStore(URI)</code>. Returns
     * <code>null</code> if no location can be determined.
     *
     * @return a file system location for this element,
     *  or <code>null</code> if no location can be determined
     */
    default URI hLocationURI()
    {
        IResource resource = hResource();
        if (resource != null)
            return resource.getLocationURI();
        return null;
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
     * Returns a string representation of this element in a form suitable for
     * debugging purposes. Clients can influence the result with format options
     * specified in the given context; unrecognized options are ignored and
     * an empty context is permitted.
     * <p>
     * Implementations are advised to support common options defined in
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

    /**
     * Returns a string representation of this element in a form suitable for
     * displaying to the user, e.g. in message dialogs. Clients can influence
     * the result with format options specified in the given context;
     * unrecognized options are ignored and an empty context is permitted.
     * <p>
     * Implementations are encouraged to support common options defined in
     * {@link org.eclipse.handly.util.ToStringOptions ToStringOptions} and may
     * interpret the format style as they see fit in a way that is specific to
     * the model. No hard rules apply, but usually the string representation
     * does not list the element's children regardless of the format style, and
     * a {@link org.eclipse.handly.util.ToStringOptions.FormatStyle#FULL FULL}
     * representation fully identifies the element within the model.
     * </p>
     *
     * @param context not <code>null</code>
     * @return a string representation of this element (never <code>null</code>)
     */
    default String hToDisplayString(IContext context)
    {
        FormatStyle style = context.getOrDefault(FORMAT_STYLE);
        if (style != FormatStyle.SHORT && style != FormatStyle.MEDIUM)
            context = with(of(FORMAT_STYLE, FormatStyle.MEDIUM), context);
        return hToString(context);
    }
}