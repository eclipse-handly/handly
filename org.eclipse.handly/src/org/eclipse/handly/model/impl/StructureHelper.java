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
import static org.eclipse.handly.util.ToStringOptions.FORMAT_STYLE;
import static org.eclipse.handly.util.ToStringOptions.FormatStyle.MEDIUM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.model.Elements;
import org.eclipse.handly.model.IElement;

/**
 * A helper class for building the structure of {@link Element#hIsOpenable()
 * openable} elements that have non-openable children. Typically, this class
 * is utilized for building the structure of source files.
 * <p>
 * The structure is being created as calls to {@link #addChild(Body, IElement,
 * Object)} are made on the helper. Make sure to complete initialization of each
 * parent body with a call to {@link #complete(Body)}.
 * </p>
 * <p>
 * Clients can use this class as it stands or subclass it
 * as circumstances warrant.
 * </p>
 *
 * @see Element#hBuildStructure(IContext, IProgressMonitor)
 */
public class StructureHelper
{
    private final Map<IElement, Object> newElements;

    /*
     * Map from body to list of child elements.
     */
    private final Map<Body, List<IElement>> children = new HashMap<>();

    /**
     * Constructs a new structure helper that will populate the given map
     * with new handle/body relationships as calls to {@link #addChild} are
     * made on the helper.
     *
     * @param newElements the map to populate with structure elements
     *  (not <code>null</code>)
     */
    public StructureHelper(Map<IElement, Object> newElements)
    {
        if (newElements == null)
            throw new IllegalArgumentException();

        this.newElements = newElements;
    }

    /**
     * Remembers the given element as a child of the yet-to-be-{@link
     * #complete(Body) completed} parent body and establishes an association
     * between the child handle and the child body, resolving {@link
     * #resolveDuplicates(IElement) duplicates} along the way.
     *
     * @param parentBody the body of the parent element (not <code>null</code>)
     * @param child the handle for the child element (not <code>null</code>)
     * @param childBody the body for the child element, or <code>null</code>
     *  if no body is to be associated with the child element (e.g. if the
     *  child is an {@link Element#hIsOpenable() openable} element)
     */
    public void addChild(Body parentBody, IElement child, Object childBody)
    {
        if (parentBody == null)
            throw new IllegalArgumentException();
        if (child == null)
            throw new IllegalArgumentException();
        if (childBody != null)
        {
            resolveDuplicates(child);
            if (newElements.containsKey(child))
                throw new AssertionError(
                    "Attempt to add an already present element: " //$NON-NLS-1$
                        + Elements.toString(child, of(FORMAT_STYLE, MEDIUM)));
            newElements.put(child, childBody);
        }
        List<IElement> childrenList = children.get(parentBody);
        if (childrenList == null)
            children.put(parentBody, childrenList = new ArrayList<IElement>());
        childrenList.add(child);
    }

    /**
     * Completes initialization of the given body. In particular, initializes it
     * with a list of elements previously {@link #addChild remembered} as children
     * of the body.
     *
     * @param body the given body (not <code>null</code>)
     */
    public void complete(Body body)
    {
        if (body == null)
            throw new IllegalArgumentException();

        List<IElement> childrenList = children.remove(body);
        body.setChildren(childrenList == null ? Body.NO_CHILDREN
            : childrenList.toArray(Body.NO_CHILDREN));
    }

    /**
     * Allows to make distinctions among elements which would otherwise be equal.
     * <p>
     * If the given element is a <code>SourceConstruct</code> which is equal to
     * an already {@link #addChild added} element, this implementation increments
     * its {@link SourceConstruct#hOccurrenceCount() occurrence count} until
     * it is no longer equal to any previously added element.
     * </p>
     *
     * @param element the given element (never <code>null</code>)
     */
    protected void resolveDuplicates(IElement element)
    {
        if (!(element instanceof SourceConstruct))
            return;
        SourceConstruct sc = (SourceConstruct)element;
        while (newElements.containsKey(sc))
            sc.hIncrementOccurrenceCount();
    }
}
