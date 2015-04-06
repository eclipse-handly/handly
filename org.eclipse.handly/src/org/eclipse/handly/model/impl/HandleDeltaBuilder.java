/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Vladimir Piskarev (1C) - adaptation
 *******************************************************************************/
package org.eclipse.handly.model.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.IHandleDelta;

/**
 * Builds a delta tree between the version of an element at the time the 
 * builder was created and the current version of the element. Performs 
 * this operation by locally caching the contents of the element when the 
 * builder is created. When the method <code>buildDeltas()</code> is called, 
 * it creates a delta over the cached contents and the new contents.
 * <p>
 * Adapted from <code>org.eclipse.jdt.internal.core.JavaElementDeltaBuilder</code>.
 * </p>
 * 
 * @see HandleDelta
 */
public class HandleDeltaBuilder
{
    private final IHandle element;

    /*
     * The maximum depth in the element children we should look into
     */
    private final int maxDepth;

    /*
     * The old handle to body relationships
     */
    private Map<IHandle, Body> oldBodies;

    private Map<IHandle, ListItem> oldPositions;
    private Map<IHandle, ListItem> newPositions;
    private Set<IHandle> added;
    private Set<IHandle> removed;

    private HandleDelta delta;

    /**
     * Constructs a delta builder on the given element 
     * looking as deep as necessary.
     * 
     * @param element the tracked element (not <code>null</code>)
     */
    public HandleDeltaBuilder(IHandle element)
    {
        this(element, Integer.MAX_VALUE);
    }

    /**
     * Constructs a delta builder on the given element 
     * looking only <code>maxDepth</code> levels deep.
     * 
     * @param element the tracked element (not <code>null</code>)
     * @param maxDepth the maximum depth in the element children we should look into
     */
    public HandleDeltaBuilder(IHandle element, int maxDepth)
    {
        if (element == null)
            throw new IllegalArgumentException();

        this.element = element;
        this.maxDepth = maxDepth;
        initialize();
        recordBody(element, 0);
    }

    /**
     * Builds the delta tree between the old content of the element and its 
     * new content. This method may only be called once on a given builder.
     */
    public void buildDelta()
    {
        delta = newDelta(element);
        recordNewPositions(element, 0);
        findAdditions(element, 0);
        findDeletions();
        findChangesInPositioning(element, 0);
        trimDelta(delta);
    }

    /**
     * Returns the built delta, or <code>null</code> if the delta has not been built. 
     * Returns an empty delta if the element has not changed.
     * 
     * @return the built delta, or <code>null</code> if the delta has not been built
     */
    public HandleDelta getDelta()
    {
        return delta;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Built delta:\n"); //$NON-NLS-1$
        builder.append(delta == null ? "<null>" : delta.toString()); //$NON-NLS-1$
        return builder.toString();
    }

    /**
     * Returns a new, initially empty delta for the given element.
     *
     * @param element the element that this delta describes a change to
     *  (not <code>null</code>)
     * @return a new, initially empty delta for the given element
     *  (never <code>null</code>)
     */
    protected HandleDelta newDelta(IHandle element)
    {
        return new HandleDelta(element);
    }

    /**
     * Finds whether the given element has had a content change.
     * <p>
     * Implementations can compare the given bodies (excepting children) 
     * and if there are differences, insert an appropriate change delta 
     * (such as <code>F_CONTENT</code>) for the given element into the delta 
     * tree being built. Implementations should not take the element's 
     * children into account.
     * </p>
     * 
     * @param oldBody the old version of the element's body (never <code>null</code>)
     * @param newBody the new version of the element's body (never <code>null</code>)
     * @param element the element whose bodies are to be compared (never <code>null</code>)
     */
    protected void findContentChange(Body newBody, Body oldBody, IHandle element)
    {
        newBody.findContentChange(oldBody, element, delta);
    }

    private void initialize()
    {
        oldBodies = new HashMap<IHandle, Body>(20);
        oldPositions = new HashMap<IHandle, ListItem>(20);
        newPositions = new HashMap<IHandle, ListItem>(20);
        oldPositions.put(element, new ListItem(null, null));
        newPositions.put(element, new ListItem(null, null));
        added = new HashSet<IHandle>(5);
        removed = new HashSet<IHandle>(5);
    }

    /*
     * Records the given element's body and the bodies for its children.
     */
    private void recordBody(IHandle element, int depth)
    {
        if (depth >= maxDepth)
            return;

        Body body;
        try
        {
            body = ((Handle)element).getBody();
        }
        catch (CoreException e)
        {
            return;
        }

        oldBodies.put(element, body);

        IHandle[] children = body.getChildren();
        if (children != null)
        {
            insertPositions(children, false);
            for (IHandle child : children)
            {
                recordBody(child, depth + 1);
            }
        }
    }

    /*
     * Fills the newPositions map with the new position information.
     */
    private void recordNewPositions(IHandle newElement, int depth)
    {
        if (depth >= maxDepth)
            return;

        Body body;
        try
        {
            body = ((Handle)newElement).getBody();
        }
        catch (CoreException e)
        {
            return;
        }

        IHandle[] children = body.getChildren();
        if (children != null)
        {
            insertPositions(children, true);
            for (IHandle child : children)
            {
                recordNewPositions(child, depth + 1);
            }
        }
    }

    /*
     * Inserts position information for the elements 
     * into the new or old positions map.
     */
    private void insertPositions(IHandle[] elements, boolean isNew)
    {
        int length = elements.length;
        IHandle previous = null, current = null, next =
            (length > 0) ? elements[0] : null;
        for (int i = 0; i < length; i++)
        {
            previous = current;
            current = next;
            next = (i + 1 < length) ? elements[i + 1] : null;
            if (isNew)
                newPositions.put(current, new ListItem(previous, next));
            else
                oldPositions.put(current, new ListItem(previous, next));
        }
    }

    /*
     * Finds elements which have been added or changed.
     */
    private void findAdditions(IHandle newElement, int depth)
    {
        Body oldBody = getOldBody(newElement);
        if (oldBody == null && depth < maxDepth)
        {
            delta.insertAdded(newElement);
            added(newElement);
        }
        else
        {
            removeOldBody(newElement);
        }

        if (depth >= maxDepth)
        {
            // mark element as changed
            delta.insertChanged(newElement, IHandleDelta.F_CONTENT);
            return;
        }

        if (oldBody != null)
        {
            Body newBody;
            try
            {
                newBody = ((Handle)newElement).getBody();
            }
            catch (CoreException e)
            {
                return;
            }

            findContentChange(newBody, oldBody, newElement);

            IHandle[] children = newBody.getChildren();
            if (children != null)
            {
                for (IHandle child : children)
                {
                    findAdditions(child, depth + 1);
                }
            }
        }
    }

    /*
     * Adds removed deltas for any handles left in the 'oldBodies' map.
     */
    private void findDeletions()
    {
        Iterator<IHandle> iter = oldBodies.keySet().iterator();
        while (iter.hasNext())
        {
            IHandle element = iter.next();
            delta.insertRemoved(element);
            removed(element);
        }
    }

    /*
     * Looks for changed positioning of elements.
     */
    private void findChangesInPositioning(IHandle element, int depth)
    {
        if (depth >= maxDepth || added.contains(element)
            || removed.contains(element))
            return;

        if (!isPositionedCorrectly(element))
        {
            delta.insertChanged(element, IHandleDelta.F_REORDER);
        }

        Body body;
        try
        {
            body = ((Handle)element).getBody();
        }
        catch (CoreException e)
        {
            return;
        }

        IHandle[] children = body.getChildren();
        if (children != null)
        {
            for (IHandle child : children)
            {
                findChangesInPositioning(child, depth + 1);
            }
        }
    }

    /*
     * Trims deletion deltas to only report the highest level of deletion.
     */
    private static void trimDelta(HandleDelta delta)
    {
        if (delta.getKind() == IHandleDelta.REMOVED)
        {
            delta.clearAffectedChildren();
        }
        else
        {
            IHandleDelta[] children = delta.getAffectedChildren();
            for (IHandleDelta child : children)
            {
                trimDelta((HandleDelta)child);
            }
        }
    }

    /*
     * Repairs the positioning information after an element has been added.
     */
    private void added(IHandle element)
    {
        added.add(element);
        ListItem current = getNewPosition(element);
        ListItem previous = null, next = null;
        if (current.previous != null)
            previous = getNewPosition(current.previous);
        if (current.next != null)
            next = getNewPosition(current.next);
        if (previous != null)
            previous.next = current.next;
        if (next != null)
            next.previous = current.previous;
    }

    /*
     * Repairs the positioning information after an element has been removed.
     */
    private void removed(IHandle element)
    {
        removed.add(element);
        ListItem current = getOldPosition(element);
        ListItem previous = null, next = null;
        if (current.previous != null)
            previous = getOldPosition(current.previous);
        if (current.next != null)
            next = getOldPosition(current.next);
        if (previous != null)
            previous.next = current.next;
        if (next != null)
            next.previous = current.previous;

    }

    /*
     * Returns whether the element's position has not changed.
     */
    private boolean isPositionedCorrectly(IHandle element)
    {
        ListItem oldListItem = getOldPosition(element);
        if (oldListItem == null)
            return false;

        ListItem newListItem = getNewPosition(element);
        if (newListItem == null)
            return false;

        IHandle oldPrevious = oldListItem.previous;
        IHandle newPrevious = newListItem.previous;
        if (oldPrevious == null)
            return (newPrevious == null);
        else
            return oldPrevious.equals(newPrevious);
    }

    private Body getOldBody(IHandle element)
    {
        return oldBodies.get(element);
    }

    private void removeOldBody(IHandle element)
    {
        oldBodies.remove(element);
    }

    private ListItem getOldPosition(IHandle element)
    {
        return oldPositions.get(element);
    }

    private ListItem getNewPosition(IHandle element)
    {
        return newPositions.get(element);
    }

    /*
     * Doubly linked list item
     */
    private static class ListItem
    {
        public IHandle previous;
        public IHandle next;

        public ListItem(IHandle previous, IHandle next)
        {
            this.previous = previous;
            this.next = next;
        }
    }
}
