/*******************************************************************************
 * Copyright (c) 2014 1C LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.model.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.handly.model.IHandle;

/**
 * A useful superclass for structure builders of "terminal openables"
 * such as source files.
 * <p>
 * Subclasses provide a client API of the structure builder. 
 * Subclasses call {@link #addChild(Body, IHandle, Body)} and 
 * {@link #complete(Body)} to build the structure.
 * </p>
 * 
 * @see Handle#buildStructure(Body, Map)
 */
public class StructureHelper
{
    private final Map<IHandle, Body> newElements;

    /*
     * Map from body to list of handles representing the children of the given body
     */
    private final Map<Body, List<IHandle>> children =
        new HashMap<Body, List<IHandle>>();

    /**
     * Constructs a new structure helper.
     * 
     * @param newElements the map to populate with structure elements 
     *  (not <code>null</code>)
     */
    protected StructureHelper(Map<IHandle, Body> newElements)
    {
        if (newElements == null)
            throw new IllegalArgumentException();

        this.newElements = newElements;
    }

    /**
     * Remembers the given handle as a child of the given parent body. 
     * Puts the given handle/body pair to the structure elements map, 
     * resolving duplicates along the way.
     *
     * @param parentBody the body of the parent element (not <code>null</code>)
     * @param handle the handle of the child element (not <code>null</code>)
     * @param body the body of the child element (not <code>null</code>)
     */
    protected void addChild(Body parentBody, IHandle handle, Body body)
    {
        if (parentBody == null)
            throw new IllegalArgumentException();
        if (handle == null)
            throw new IllegalArgumentException();
        if (body == null)
            throw new IllegalArgumentException();

        if (handle instanceof SourceConstruct)
            resolveDuplicates((SourceConstruct)handle);
        newElements.put(handle, body);
        List<IHandle> childrenList = children.get(parentBody);
        if (childrenList == null)
            children.put(parentBody, childrenList = new ArrayList<IHandle>());
        childrenList.add(handle);
    }

    /**
     * Completes the given body. In particular, sets the body's children to the 
     * handles previously remembered by {@link #addChild(Body, IHandle, Body)}.
     *
     * @param body the body to complete (not <code>null</code>)
     */
    protected void complete(Body body)
    {
        if (body == null)
            throw new IllegalArgumentException();

        List<IHandle> childrenList = children.remove(body);
        body.setChildren(childrenList == null ? Body.NO_CHILDREN
            : childrenList.toArray(new IHandle[childrenList.size()]));
    }

    /**
     * Returns whether the given string is <code>null</code> or empty.
     * 
     * @param s the given string
     * @return <code>true</code> if the string is <code>null</code> or empty 
     */
    protected static boolean isEmpty(String s)
    {
        return s == null || s.isEmpty();
    }

    private void resolveDuplicates(SourceConstruct element)
    {
        while (newElements.containsKey(element))
            element.incrementOccurenceCount();
    }
}
