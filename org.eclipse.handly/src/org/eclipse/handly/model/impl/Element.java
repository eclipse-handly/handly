/*******************************************************************************
 * Copyright (c) 2014, 2016 1C-Soft LLC and others.
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

import static org.eclipse.handly.context.Contexts.EMPTY_CONTEXT;
import static org.eclipse.handly.context.Contexts.of;
import static org.eclipse.handly.context.Contexts.with;
import static org.eclipse.handly.util.ToStringOptions.FORMAT_STYLE;
import static org.eclipse.handly.util.ToStringOptions.INDENT_LEVEL;
import static org.eclipse.handly.util.ToStringOptions.INDENT_POLICY;
import static org.eclipse.handly.util.ToStringOptions.FormatStyle.FULL;
import static org.eclipse.handly.util.ToStringOptions.FormatStyle.LONG;
import static org.eclipse.handly.util.ToStringOptions.FormatStyle.MEDIUM;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.internal.Activator;
import org.eclipse.handly.model.Elements;
import org.eclipse.handly.model.IElement;
import org.eclipse.handly.util.IndentPolicy;
import org.eclipse.handly.util.Property;
import org.eclipse.handly.util.ToStringOptions.FormatStyle;

/**
 * The root of the element class hierarchy.
 * <p>
 * Note that despite having a dependency on {@link IResource} this class can
 * safely be used even when <code>org.eclipse.core.resources</code> bundle is
 * not available. This is based on the "outward impression" of late resolution
 * of symbolic references a JVM must provide according to the JVMS.
 * </p>
 *
 * @see IElement
 */
public abstract class Element
    extends PlatformObject
    implements IElementImpl
{
    private final Element parent;
    private final String name;

    /**
     * Constructs a handle for an element with the given parent element
     * and the given name.
     *
     * @param parent the parent of the element,
     *  or <code>null</code> if the element has no parent
     * @param name the name of the element,
     *  or <code>null</code> if the element has no name
     */
    public Element(Element parent, String name)
    {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Element))
            return false;
        Element other = (Element)obj;
        if (!hElementType().equals(other.hElementType()))
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
    public String toString()
    {
        return hToString(EMPTY_CONTEXT);
    }

    @Override
    public final String hName()
    {
        return name;
    }

    @Override
    public final Element hParent()
    {
        return parent;
    }

    @Override
    public boolean hExists()
    {
        if (hFindBody() != null)
            return true;
        if (parent != null && !parent.hExists())
            return false;
        try
        {
            hValidateExistence(EMPTY_CONTEXT);
            return true;
        }
        catch (CoreException e)
        {
            return false;
        }
    }

    @Override
    public IElement[] hChildren() throws CoreException
    {
        return hChildren(hBody());
    }

    /**
     * Returns the cached body for this element, or <code>null</code>
     * if none.
     *
     * @return the cached body for this element, or <code>null</code>
     *  if none
     */
    public Object hFindBody()
    {
        return hElementManager().get(this);
    }

    /**
     * Returns the cached body for this element without disturbing
     * cache ordering, or <code>null</code> if none.
     *
     * @return the cached body for this element, or <code>null</code>
     *  if none
     */
    public Object hPeekAtBody()
    {
        return hElementManager().peek(this);
    }

    /**
     * Closes this element, removing any previously registered handle/body
     * relationships for it and its existing descendants.
     *
     * @return <code>true</code> if this element was successfully closed;
     *  <code>false</code> if the current state of this element does not
     *  permit closing (e.g., a working copy)
     */
    public final boolean hClose()
    {
        return hClose(true);
    }

    @Override
    public String hToString(IContext context)
    {
        StringBuilder builder = new StringBuilder();
        IndentPolicy indentPolicy = context.getOrDefault(INDENT_POLICY);
        int indentLevel = context.getOrDefault(INDENT_LEVEL);
        FormatStyle style = context.getOrDefault(FORMAT_STYLE);
        if (style == FULL || style == LONG)
        {
            Object body = hPeekAtBody();
            indentPolicy.appendIndent(builder, indentLevel);
            hToStringBody(builder, body, context);
            if (style == FULL)
                hToStringAncestors(builder, context);
            if (body != null && hChildren(body).length > 0)
            {
                indentPolicy.appendLine(builder);
                hToStringChildren(builder, body, with(of(FORMAT_STYLE, LONG),
                    of(INDENT_LEVEL, indentLevel + 1), context));
            }
        }
        else
        {
            indentPolicy.appendIndent(builder, indentLevel);
            hToStringBody(builder, NO_BODY, context);
            if (style == MEDIUM)
                hToStringAncestors(builder, context);
        }
        return builder.toString();
    }

    /**
     * Debugging purposes.
     */
    protected void hToStringAncestors(StringBuilder builder, IContext context)
    {
        if (parent != null && parent.hParent() != null)
        {
            builder.append(" [in "); //$NON-NLS-1$
            parent.hToStringBody(builder, NO_BODY, context);
            parent.hToStringAncestors(builder, context);
            builder.append(']');
        }
    }

    /**
     * Debugging purposes.
     */
    protected void hToStringChildren(StringBuilder builder, Object body,
        IContext context)
    {
        if (body == null)
            return;
        IndentPolicy indentPolicy = context.getOrDefault(INDENT_POLICY);
        IElement[] children = hChildren(body);
        for (int i = 0; i < children.length; i++)
        {
            if (i > 0)
                indentPolicy.appendLine(builder);
            builder.append(Elements.toString(children[i], context));
        }
    }

    /**
     * Special-purpose value for the <code>body</code> argument of the
     * {@link #hToStringBody(StringBuilder, Object, IContext) hToStringBody}
     * method. Indicates that information about the body is not relevant.
     */
    protected static final Object NO_BODY = new Object();

    /**
     * Debugging purposes.
     */
    protected void hToStringBody(StringBuilder builder, Object body,
        IContext context)
    {
        hToStringName(builder, context);
        if (body == null)
            builder.append(" (not open)"); //$NON-NLS-1$
    }

    /**
     * Debugging purposes.
     */
    protected void hToStringName(StringBuilder builder, IContext context)
    {
        builder.append(name);
    }

    /**
     * Returns an opaque object representing the <i>type</i> of this element.
     * Equal elements must have equal types.
     *
     * @return the type of this element (never <code>null</code>)
     */
    protected Object hElementType()
    {
        return getClass();
    }

    /**
     * Returns the element manager for this element. The manager must be shared
     * between all elements of a Handly-based model. Typical implementations
     * would answer a model-specific singleton.
     *
     * @return the element manager for this element (never <code>null</code>)
     */
    protected abstract ElementManager hElementManager();

    /**
     * Validates if the element represented by the handle may be "opened",
     * i.e. begin existence in the model. For example, a necessary condition
     * for element existence might be that the underlying resource exists.
     * <p>
     * Note that ancestor elements may or may not exist. This method need not
     * explicitly verify their existence.
     * </p>
     *
     * @param context the operation context (never <code>null</code>)
     * @throws CoreException if this element shall not exist
     */
    protected abstract void hValidateExistence(IContext context)
        throws CoreException;

    /**
     * A map containing handle/body relationships.
     * @see #hBuildStructure(IContext, IProgressMonitor)
     */
    protected static final Property<Map<IElement, Object>> NEW_ELEMENTS =
        new Property<Map<IElement, Object>>("newElements") //$NON-NLS-1$
        {};

    /**
     * Creates and initializes bodies for this element and for each non-{@link
     * #hIsOpenable() openable} child element (and their non-openable children,
     * recursively). Uses the {@link #NEW_ELEMENTS} map in the given context
     * to associate the created bodies their respective elements.
     *
     * @param context the operation context (never <code>null</code>)
     * @param monitor a progress monitor (never <code>null</code>)
     * @throws CoreException if an exception occurs while accessing
     *  the element's corresponding resource
     * @throws OperationCanceledException if this method is canceled
     */
    protected abstract void hBuildStructure(IContext context,
        IProgressMonitor monitor) throws CoreException;

    /**
     * Returns the cached body for this element. If this element is not
     * already present in the body cache, its body will be created,
     * initialized, and put in the cache.
     *
     * @return the cached body for this element (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an
     *  exception occurs while accessing its corresponding resource
     */
    protected final Object hBody() throws CoreException
    {
        return hBody(EMPTY_CONTEXT, null);
    }

    /**
     * Returns the cached body for this element. If this element is not
     * already present in the body cache, its body will be created,
     * initialized, and put in the cache.
     *
     * @param context the operation context (not <code>null</code>)
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired
     * @return the cached body for this element (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an
     *  exception occurs while accessing its corresponding resource
     * @throws OperationCanceledException if this method is canceled
     */
    protected final Object hBody(IContext context, IProgressMonitor monitor)
        throws CoreException
    {
        Object body = hFindBody();
        if (body != null)
            return body;
        return hOpen(context, monitor);
    }

    /**
     * Given a body, returns the immediate children of this element.
     *
     * @param body the body corresponding to this element
     *  (never <code>null</code>)
     * @return the immediate children of this element (not <code>null</code>)
     */
    protected IElement[] hChildren(Object body)
    {
        return ((Body)body).getChildren();
    }

    /**
     * Indicates whether to forcibly reopen this element if it is already open
     * (i.e. already present in the body cache).
     */
    static final Property<Boolean> FORCE_OPEN = Property.get("forceOpen", //$NON-NLS-1$
        Boolean.class).withDefault(false);

    /**
     * Creates and initializes bodies for this element, its ancestors and its
     * children as necessary and then atomically puts them into the body cache.
     * Returns the cached body for this element.
     * <p>
     * The following context options influence the operation's behavior:
     * </p>
     * <ul>
     * <li>
     * {@link #FORCE_OPEN} - Indicates whether to forcibly reopen this element
     * if it is already open (i.e. already present in the body cache).
     * </li>
     * </ul>
     *
     * @param context the operation context (not <code>null</code>)
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired
     * @return the cached body for this element (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an
     *  exception occurs while accessing its corresponding resource
     * @throws OperationCanceledException if this method is canceled
     */
    final Object hOpen(IContext context, IProgressMonitor monitor)
        throws CoreException
    {
        if (monitor == null)
            monitor = new NullProgressMonitor();
        ElementManager elementManager = hElementManager();
        boolean hadTemporaryCache = elementManager.hasTemporaryCache();
        try
        {
            Map<IElement, Object> newElements =
                elementManager.getTemporaryCache();
            hGenerateBodies(with(of(NEW_ELEMENTS, newElements), context),
                monitor);
            Object body = newElements.get(this);
            if (body == null)
            {
                // the openable parent did not create a body for this element
                throw new CoreException(Activator.createErrorStatus(
                    "The element does not exist: " + toString(), null)); //$NON-NLS-1$
            }
            if (monitor.isCanceled())
                throw new OperationCanceledException();
            if (!hadTemporaryCache)
            {
                if (context.getOrDefault(FORCE_OPEN))
                    elementManager.put(this, newElements);
                else
                {
                    Object existingBody = elementManager.putIfAbsent(this,
                        newElements);
                    if (existingBody != null)
                        body = existingBody;
                }
            }
            return body;
        }
        finally
        {
            if (!hadTemporaryCache)
            {
                elementManager.resetTemporaryCache();
            }
        }
    }

    /**
     * Returns whether this element is "openable".
     * <p>
     * An openable element knows how to open itself on demand (i.e. initialize
     * its body and put it in the body cache). When opening an element, it is
     * ensured that all openable parent elements are open. On the other hand,
     * opening an element should open only those child elements that are not
     * openable: all other children will open themselves on demand.
     * </p>
     * <p>
     * This implementation returns <code>true</code>. Non-openable elements
     * must override this method and return <code>false</code>.
     * </p>
     *
     * @return <code>true</code> if this element is openable,
     *  <code>false</code> otherwise
     */
    protected boolean hIsOpenable()
    {
        return true;
    }

    /**
     * Returns the innermost {@link #hIsOpenable() openable} element
     * in the parent chain of this element, or <code>null</code>
     * if this element has no openable parent.
     *
     * @return the innermost openable element in the parent chain of this
     *  element, or <code>null</code> if this element has no openable parent
     */
    protected final Element hOpenableParent()
    {
        Element result = parent;
        while (result != null && !result.hIsOpenable())
            result = result.parent;
        return result;
    }

    /**
     * Creates and initializes bodies for ancestors of this element
     * as necessary and and puts them into the {@link #NEW_ELEMENTS} map
     * available in the given context.
     *
     * @param context the operation context (never <code>null</code>)
     * @param monitor a progress monitor (never <code>null</code>)
     * @throws CoreException if an ancestor does not exist or if an
     *  exception occurs while accessing its corresponding resource
     * @throws OperationCanceledException if this method is canceled
     */
    protected void hGenerateAncestorBodies(IContext context,
        IProgressMonitor monitor) throws CoreException
    {
        Element openableParent = hOpenableParent();
        if (openableParent != null && openableParent.hFindBody() == null)
            openableParent.hGenerateBodies(context, monitor);
    }

    /**
     * Creates and initializes bodies for this element, its ancestors
     * and its children as necessary and puts them into the {@link
     * #NEW_ELEMENTS} map available in the given context.
     *
     * @param context the operation context (never <code>null</code>)
     * @param monitor a progress monitor (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an
     *  exception occurs while accessing its corresponding resource
     * @throws OperationCanceledException if this method is canceled
     */
    protected final void hGenerateBodies(IContext context,
        IProgressMonitor monitor) throws CoreException
    {
        monitor.beginTask("", 2); //$NON-NLS-1$
        try
        {
            hGenerateAncestorBodies(context, new SubProgressMonitor(monitor,
                1));

            if (hIsOpenable())
            {
                hValidateExistence(context);

                if (monitor.isCanceled())
                    throw new OperationCanceledException();

                hBuildStructure(context, new SubProgressMonitor(monitor, 1));

                Object body = context.get(NEW_ELEMENTS).get(this);
                if (body == null)
                {
                    throw new AssertionError(MessageFormat.format(
                        "No body for {0}. Incorrect {1}#hBuildStructure implementation?", //$NON-NLS-1$
                        toString(), getClass().getSimpleName()));
                }
            }
        }
        finally
        {
            monitor.done();
        }
    }

    /**
     * Closes this element, removing any previously registered handle/body
     * relationships for it and its existing descendants.
     *
     * @param external <code>false</code> if this is a recursive call,
     *  <code>true</code> otherwise
     * @return <code>true</code> if this element was successfully closed;
     *  <code>false</code> if the current state of this element does not
     *  permit closing (e.g., a working copy)
     */
    protected boolean hClose(boolean external)
    {
        if (external && !hIsOpenable())
            return false;
        hElementManager().remove(this);
        return true;
    }

    /**
     * The cached body for this element is going to be removed from the cache.
     * Do any necessary cleanup.
     *
     * @param body the cached body for this element (never <code>null</code>)
     */
    protected void hRemoving(Object body)
    {
        // Does nothing. Subclasses may override
    }
}
