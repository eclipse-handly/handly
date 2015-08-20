/*******************************************************************************
 * Copyright (c) 2014, 2015 1C-Soft LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *     (inspired by Eclipse JDT work)
 *******************************************************************************/
package org.eclipse.handly.model.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.handly.internal.Activator;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.util.TextIndent;

/**
 * The root of the handle class hierarchy.
 *
 * @see IHandle
 */
public abstract class Handle
    extends PlatformObject
    implements IHandle
{
    protected static final Body NO_BODY = new Body();

    /**
     * The parent of the element.
     */
    protected final Handle parent;
    /**
     * The name of the element.
     */
    protected final String name;

    /**
     * Constructs a handle for an element with the given parent element
     * and the given name.
     *
     * @param parent the parent of the element,
     *  or <code>null</code> if the element has no parent
     * @param name the name of the element,
     *  or <code>null</code> if the element has no name
     */
    public Handle(Handle parent, String name)
    {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public final String getName()
    {
        return name;
    }

    /**
     * Returns {@link #parent}. Subclasses must honor this contract.
     */
    @Override
    public IHandle getParent()
    {
        return parent;
    }

    @Override
    public IHandle getRoot()
    {
        if (parent == null)
            return this;
        else
            return parent.getRoot();
    }

    @Override
    public <T extends IHandle> T getAncestor(Class<T> ancestorType)
    {
        if (parent == null)
            return null;
        if (ancestorType.isInstance(parent))
            return ancestorType.cast(parent);
        return parent.getAncestor(ancestorType);
    }

    @Override
    public IPath getPath()
    {
        IResource resource = getResource();
        if (resource != null)
            return resource.getFullPath();
        return Path.EMPTY;
    }

    @Override
    public boolean exists()
    {
        if (findBody() != null)
            return true;
        try
        {
            validateExistence();
            return true;
        }
        catch (CoreException e)
        {
            return false;
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Handle))
            return false;
        Handle other = (Handle)obj;
        if (!getElementType().equals(other.getElementType()))
            return false;
        if (parent == null)
        {
            if (other.parent != null)
                return false;
        }
        else if (!parent.equals(other.parent))
            return false;
        if (name == null)
        {
            if (other.name != null)
                return false;
        }
        else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (parent == null ? 0 : parent.hashCode());
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
    }

    @Override
    public IHandle[] getChildren() throws CoreException
    {
        return getBody().getChildren();
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

    /**
     * Returns the cached body for this element, or <code>null</code>
     * if none.
     *
     * @return the cached body for this element, or <code>null</code>
     *  if none
     */
    public Body findBody()
    {
        return getHandleManager().get(this);
    }

    /**
     * Returns the cached body for this element without disturbing
     * cache ordering, or <code>null</code> if none.
     *
     * @return the cached body for this element, or <code>null</code>
     *  if none
     */
    public Body peekAtBody()
    {
        return getHandleManager().peek(this);
    }

    /**
     * Closes this element, removing any previously registered handle/body
     * relationships for it and its existing descendants.
     *
     * @return <code>true</code> if this element was successfully closed;
     *  <code>false</code> if the current state of this element does not
     *  permit closing (e.g., a working copy)
     */
    public boolean close()
    {
        getHandleManager().removeBodyAndChildren(this);
        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        toString(TextIndent.with("  "), builder); //$NON-NLS-1$
        return builder.toString();
    }

    @Override
    public String toString(ToStringStyle style)
    {
        StringBuilder builder = new StringBuilder();
        if (style.getOptions().contains(ToStringStyle.Option.CHILDREN))
        {
            toString(style.getIndent(), builder);
        }
        else
        {
            toStringBody(style.getIndent(), builder, NO_BODY,
                true/*show resolved info*/);
            if (style.getOptions().contains(ToStringStyle.Option.ANCESTORS))
                toStringAncestors(builder);
        }
        return builder.toString();
    }

    /**
     * Debugging purposes
     */
    public String toDebugString()
    {
        StringBuilder builder = new StringBuilder();
        toStringBody(TextIndent.NONE, builder, NO_BODY,
            true/*show resolved info*/);
        return builder.toString();
    }

    /**
     * Debugging purposes
     */
    public String toStringWithAncestors()
    {
        return toStringWithAncestors(true/*show resolved info*/);
    }

    /**
     * Debugging purposes
     */
    public String toStringWithAncestors(boolean showResolvedInfo)
    {
        StringBuilder builder = new StringBuilder();
        toStringBody(TextIndent.NONE, builder, NO_BODY, showResolvedInfo);
        toStringAncestors(builder);
        return builder.toString();
    }

    /**
     * Debugging purposes
     */
    public Body toStringBody(TextIndent indent, StringBuilder builder)
    {
        Body body = peekAtBody();
        toStringBody(indent, builder, body, true/*show resolved info*/);
        return body;
    }

    /**
     * Debugging purposes
     */
    protected void toStringAncestors(StringBuilder builder)
    {
        if (parent != null && parent.getParent() != null)
        {
            builder.append(" [in "); //$NON-NLS-1$
            parent.toStringBody(TextIndent.NONE, builder, NO_BODY,
                false/*don't show resolved info*/);
            parent.toStringAncestors(builder);
            builder.append(']');
        }
    }

    /**
     * Debugging purposes
     */
    protected void toString(TextIndent indent, StringBuilder builder)
    {
        Body body = toStringBody(indent, builder);
        if (indent.getLevel() == 0)
        {
            toStringAncestors(builder);
        }
        toStringChildren(indent, builder, body);
    }

    /**
     * Debugging purposes
     */
    protected void toStringChildren(TextIndent indent, StringBuilder builder,
        Body body)
    {
        if (body == null)
            return;
        ToStringStyle childStyle = null;
        for (IHandle child : body.getChildren())
        {
            indent.appendLineSeparatorTo(builder);
            if (childStyle == null)
                childStyle = new ToStringStyle(indent.getIncreasedIndent(),
                    EnumSet.of(ToStringStyle.Option.CHILDREN));
            builder.append(child.toString(childStyle));
        }
    }

    /**
     * Debugging purposes
     */
    protected void toStringBody(TextIndent indent, StringBuilder builder,
        Body body, boolean showResolvedInfo)
    {
        indent.appendTo(builder);
        toStringName(builder);
        if (body == null)
        {
            builder.append(" (not open)"); //$NON-NLS-1$
        }
    }

    /**
     * Debugging purposes
     */
    protected void toStringName(StringBuilder builder)
    {
        builder.append(getName());
    }

    /**
     * Returns an opaque object representing the <i>type</i> of this element.
     * Equal elements must have equal types.
     *
     * @return the type of this element (never <code>null</code>)
     */
    protected Object getElementType()
    {
        return getClass();
    }

    /**
     * Returns the handle manager for this element. The manager must be shared
     * between all elements of a handle-based model. Typical implementations
     * would answer a model-specific singleton.
     *
     * @return the handle manager for this element (never <code>null</code>)
     */
    protected abstract HandleManager getHandleManager();

    /**
     * Validates if the element represented by the handle may be "opened",
     * i.e. begin existence in the model. For example, a necessary condition
     * for element existence might be that the underlying resource exists.
     *
     * @throws CoreException if the element may not exist
     */
    protected abstract void validateExistence() throws CoreException;

    /**
     * Initializes the given body based on the element's current contents.
     * Children are to be placed in the given <code>newElements</code> map
     * (note that this element has already been placed in the map).
     *
     * @param body a new, uninitialized body for this element
     *  (never <code>null</code>)
     * @param newElements a map containing handle/body relationships
     *  (never <code>null</code>)
     * @throws CoreException if an exception occurs while accessing
     *  the element's corresponding resource
     */
    protected abstract void buildStructure(Body body,
        Map<IHandle, Body> newElements) throws CoreException;

    /**
     * Returns the cached body for this element. If this element is not already
     * "open" (i.e. present in the body cache), it and all its parents are opened.
     *
     * @return the cached body for this element (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an
     *  exception occurs while accessing its corresponding resource
     */
    protected final Body getBody() throws CoreException
    {
        Body body = findBody();
        if (body != null)
            return body;
        return openWhenClosed(newBody());
    }

    /**
     * Returns a new, uninitialized body for this element, or <code>null</code>
     * if the body for the element is to be created by the openable parent.
     *
     * @return a new body for this element, or <code>null</code> if the body
     *  for the element is to be created by the openable parent
     */
    protected Body newBody()
    {
        return new Body();
    }

    /**
     * "Opens" this element that is known to be "closed" (absent in the body cache).
     * Automatically opens all openable parent elements that are not already open.
     * Returns the fully initialized body for this element.
     * <p>
     * Opening an element means creating and initializing its body and
     * putting the handle/body relationship into the body cache.
     * </p>
     *
     * @param body a new body to be initialized for this element, or
     *  <code>null</code> if the body is to be created by the openable parent
     * @return the fully initialized body for this element (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an
     *  exception occurs while accessing its corresponding resource
     */
    protected final Body openWhenClosed(Body body) throws CoreException
    {
        HandleManager handleManager = getHandleManager();
        boolean hadTemporaryCache = handleManager.hasTemporaryCache();
        try
        {
            Map<IHandle, Body> newElements = handleManager.getTemporaryCache();
            generateBodies(body, newElements);
            if (body == null)
            {
                body = newElements.get(this);
            }
            if (body == null)
            {
                // a source construct could not be opened
                throw new CoreException(Activator.createErrorStatus(
                    "The element does not exist: " + toString(), null)); //$NON-NLS-1$
            }
            if (!hadTemporaryCache)
            {
                handleManager.put(this, newElements);
            }
        }
        finally
        {
            if (!hadTemporaryCache)
            {
                handleManager.resetTemporaryCache();
            }
        }
        return body;
    }

    /**
     * Returns the first "openable" element in the parent hierarchy of
     * this element, or <code>null</code> if this element has no parent.
     * <p>
     * An openable element knows how to open itself on demand (i.e. build
     * its structure and properties and put it in the body cache). When opening
     * an element, all openable parent elements are automatically opened.
     * On the other hand, opening an element does not automatically open
     * any descendents which are themselves openable.
     * </p>
     *
     * @return the first "openable" element in the parent hierarchy of
     *  this element, or <code>null</code> if this element has no parent
     */
    protected Handle getOpenableParent()
    {
        return parent;
    }

    private void generateBodies(Body body, Map<IHandle, Body> newElements)
        throws CoreException
    {
        Handle openableParent = getOpenableParent();
        if (openableParent != null && openableParent.findBody() == null)
        {
            openableParent.generateBodies(openableParent.newBody(),
                newElements);
        }

        if (body != null)
        {
            validateExistence();

            // put the body before building the structure so that
            // questions to the handle behave as if the element existed
            newElements.put(this, body);

            try
            {
                buildStructure(body, newElements);
            }
            catch (CoreException e)
            {
                newElements.remove(this);
                throw e;
            }
        }
    }
}
