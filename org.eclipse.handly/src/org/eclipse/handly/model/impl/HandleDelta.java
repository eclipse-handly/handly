/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.IHandleDelta;
import org.eclipse.handly.model.ISourceFile;

/**
 * To create a delta tree, use the <code>insertXXX</code> methods on a root delta.
 * <p>
 * Adapted from <code>org.eclipse.jdt.internal.core.JavaElementDelta</code>.
 * </p>
 * 
 * @see IHandleDelta
 */
public class HandleDelta
    implements IHandleDelta
{
    protected static final IHandleDelta[] EMPTY_HANDLE_DELTAS =
        new IHandleDelta[0];
    protected static final IMarkerDelta[] EMPTY_MARKER_DELTAS =
        new IMarkerDelta[0];
    protected static final IResourceDelta[] EMPTY_RESOURCE_DELTAS =
        new IResourceDelta[0];

    protected int kind;
    protected int flags;
    protected final IHandle element;
    protected IHandle movedFromElement;
    protected IHandle movedToElement;
    protected IHandleDelta[] affectedChildren = EMPTY_HANDLE_DELTAS;

    /**
     * On-demand index into <code>affectedChildren</code>
     * @see #needsChildIndex()
     * @see #indexOfChild(Key)
     */
    protected Map<Key, Integer> childIndex;

    protected IMarkerDelta[] markerDeltas;
    protected IResourceDelta[] resourceDeltas;
    protected int resourceDeltasCounter;

    /**
     * Constructs an initially empty delta for the given element. 
     * 
     * @param element the element that this delta describes a change to 
     *  (not <code>null</code>)
     */
    public HandleDelta(IHandle element)
    {
        if (element == null)
            throw new IllegalArgumentException();
        this.element = element;
    }

    @Override
    public IHandle getElement()
    {
        return element;
    }

    @Override
    public final int getKind()
    {
        return kind;
    }

    @Override
    public final int getFlags()
    {
        return flags;
    }

    @Override
    public IHandleDelta[] getAffectedChildren()
    {
        return affectedChildren;
    }

    @Override
    public IHandleDelta[] getAddedChildren()
    {
        return getChildrenOfType(ADDED);
    }

    @Override
    public IHandleDelta[] getRemovedChildren()
    {
        return getChildrenOfType(REMOVED);
    }

    @Override
    public IHandleDelta[] getChangedChildren()
    {
        return getChildrenOfType(CHANGED);
    }

    @Override
    public IHandle getMovedFromElement()
    {
        return movedFromElement;
    }

    @Override
    public IHandle getMovedToElement()
    {
        return movedToElement;
    }

    @Override
    public IMarkerDelta[] getMarkerDeltas()
    {
        if (markerDeltas == null)
            return EMPTY_MARKER_DELTAS;
        return markerDeltas;
    }

    @Override
    public IResourceDelta[] getResourceDeltas()
    {
        if (resourceDeltas == null)
            return EMPTY_RESOURCE_DELTAS;
        if (resourceDeltas.length != resourceDeltasCounter)
        {
            System.arraycopy(resourceDeltas, 0, resourceDeltas =
                new IResourceDelta[resourceDeltasCounter], 0,
                resourceDeltasCounter);
        }
        return resourceDeltas;
    }

    /**
     * @return <code>true</code> if the delta is empty, 
     *  i.e. it represents an unchanged element
     */
    public boolean isEmpty()
    {
        return kind == 0;
    }

    /**
     * Same as <code>{@link #insertAdded(IHandle, int) insertAdded}(element, 0)</code>
     * 
     * @param element the added element (not <code>null</code>)
     * @return this delta object (never <code>null</code>)
     */
    public final HandleDelta insertAdded(IHandle element)
    {
        return insertAdded(element, 0);
    }

    /**
     * Inserts the nested deltas resulting from an add operation. 
     * The constructor should be used to create the root delta 
     * and then the add operation should call this method.
     * 
     * @param element the added element (not <code>null</code>)
     * @param flags change flags
     * @return this delta object (never <code>null</code>)
     */
    public HandleDelta insertAdded(IHandle element, int flags)
    {
        insert(newAdded(element, flags));
        return this;
    }

    /**
     * Same as <code>{@link #insertRemoved(IHandle, int) insertRemoved}(element, 0)</code>
     * 
     * @param element the removed element (not <code>null</code>)
     * @return this delta object (never <code>null</code>)
     */
    public final HandleDelta insertRemoved(IHandle element)
    {
        return insertRemoved(element, 0);
    }

    /**
     * Inserts the nested deltas resulting from a delete operation. 
     * The constructor should be used to create the root delta 
     * and then the delete operation should call this method.
     * 
     * @param element the removed element (not <code>null</code>)
     * @param flags change flags
     * @return this delta object (never <code>null</code>)
     */
    public HandleDelta insertRemoved(IHandle element, int flags)
    {
        HandleDelta delta = newDelta(element);
        delta.flags = flags;
        insert(delta);
        HandleDelta actualDelta = getDeltaFor(element);
        if (actualDelta != null)
        {
            actualDelta.kind = REMOVED;
            actualDelta.flags = flags;
            actualDelta.clearAffectedChildren();
        }
        return this;
    }

    /**
     * Inserts the nested deltas resulting from a change operation. 
     * The constructor should be used to create the root delta 
     * and then the change operation should call this method.
     * 
     * @param element the changed element (not <code>null</code>)
     * @param flags change flags
     * @return this delta object (never <code>null</code>)
     */
    public HandleDelta insertChanged(IHandle element, int flags)
    {
        insert(newChanged(element, flags));
        return this;
    }

    /**
     * Inserts the nested deltas resulting from a move operation. 
     * The constructor should be used to create the root delta 
     * and then the move operation should call this method.
     * 
     * @param movedFromElement the element before it was moved to its current 
     *  location (not <code>null</code>)
     * @param movedToElement the element in its new location (not <code>null</code>)
     * @return this delta object (never <code>null</code>)
     */
    public HandleDelta insertMovedFrom(IHandle movedFromElement,
        IHandle movedToElement)
    {
        insert(newMovedFrom(movedFromElement, movedToElement));
        return this;
    }

    /**
     * Inserts the nested deltas resulting from a move operation. 
     * The constructor should be used to create the root delta 
     * and then the move operation should call this method.
     * 
     * @param movedToElement the element in its new location (not <code>null</code>)
     * @param movedFromElement the element before it was moved to its current 
     *  location (not <code>null</code>)
     * @return this delta object (never <code>null</code>)
     */
    public HandleDelta insertMovedTo(IHandle movedToElement,
        IHandle movedFromElement)
    {
        insert(newMovedTo(movedToElement, movedFromElement));
        return this;
    }

    /**
     * Creates the delta tree for the given delta, and then inserts the tree 
     * as an affected child of this node.
     * 
     * @param delta the delta to insert (not <code>null</code>)
     */
    public void insert(HandleDelta delta)
    {
        HandleDelta childDelta = createDeltaTree(delta);
        if (!equalsAndSameParent(delta.getElement(), getElement()))
        {
            addAffectedChild(childDelta);
        }
    }

    /**
     * Creates the nested deltas based on the given delta and the root of 
     * this delta tree. Returns the root of the created delta tree. 
     * 
     * @param delta the delta to create the delta tree for (not <code>null</code>)
     * @return the root of the created delta tree (never <code>null</code>)
     */
    public HandleDelta createDeltaTree(HandleDelta delta)
    {
        if (delta == null)
            throw new IllegalArgumentException();

        HandleDelta childDelta = delta;
        List<IHandle> ancestors = getAncestors(delta.getElement());
        if (ancestors == null)
        {
            if (equalsAndSameParent(delta.getElement(), getElement()))
            {
                // the element being changed is the root element
                kind = delta.kind;
                flags = delta.flags;
                movedToElement = delta.movedToElement;
                movedFromElement = delta.movedFromElement;
            }
        }
        else
        {
            for (int i = 0, size = ancestors.size(); i < size; i++)
            {
                IHandle ancestor = ancestors.get(i);
                HandleDelta ancestorDelta = newDelta(ancestor);
                ancestorDelta.addAffectedChild(childDelta);
                childDelta = ancestorDelta;
            }
        }
        return childDelta;
    }

    /**
     * Adds the child delta to the collection of affected children. 
     * If the child is already in the collection, walks down the hierarchy.
     * 
     * @param child the child delta to add (not <code>null</code>)
     */
    public void addAffectedChild(HandleDelta child)
    {
        if (child == null)
            throw new IllegalArgumentException();

        switch (kind)
        {
        case ADDED:
        case REMOVED:
            // no need to add a child if this parent is added or removed
            return;
        case CHANGED:
            flags |= F_CHILDREN;
            break;
        default:
            kind = CHANGED;
            flags |= F_CHILDREN;
        }

        // if a child delta is added to a source file delta, 
        // it's a fine grained delta
        if (element instanceof ISourceFile)
        {
            flags |= F_FINE_GRAINED;
        }

        Key key = new Key(child.getElement());
        Integer index = indexOfChild(key);
        if (index == null) // new affected child
        {
            addNewChild(child);
        }
        else
        {
            HandleDelta existingChild = (HandleDelta)affectedChildren[index];
            switch (existingChild.getKind())
            {
            case ADDED:
                switch (child.getKind())
                {
                case ADDED: // child was added then added -> it is added
                case CHANGED: // child was added then changed -> it is added
                    return;
                case REMOVED: // child was added then removed -> noop
                    removeExistingChild(key, index);
                    return;
                }
                break;
            case REMOVED:
                switch (child.getKind())
                {
                case ADDED: // child was removed then added -> it is changed
                    child.kind = CHANGED;
                    affectedChildren[index] = child;
                    return;
                case CHANGED: // child was removed then changed -> it is removed
                case REMOVED: // child was removed then removed -> it is removed
                    return;
                }
                break;
            case CHANGED:
                switch (child.getKind())
                {
                case ADDED: // child was changed then added -> it is added
                case REMOVED: // child was changed then removed -> it is removed
                    affectedChildren[index] = child;
                    return;
                case CHANGED: // child was changed then changed -> it is changed
                    IHandleDelta[] children = child.getAffectedChildren();
                    for (int i = 0; i < children.length; i++)
                    {
                        HandleDelta childsChild = (HandleDelta)children[i];
                        existingChild.addAffectedChild(childsChild);
                    }

                    // update flags
                    boolean childHadContentFlag =
                        (child.flags & F_CONTENT) != 0;
                    boolean existingChildHadChildrenFlag =
                        (existingChild.flags & F_CHILDREN) != 0;
                    existingChild.flags |= child.flags;

                    // remove F_CONTENT if existing child had F_CHILDREN flag set
                    // (case of fine grained delta (existing child) and 
                    // delta coming from DeltaProcessor (child))
                    if (childHadContentFlag && existingChildHadChildrenFlag)
                    {
                        existingChild.flags &= ~F_CONTENT;
                    }

                    // add marker deltas if needed
                    if (child.markerDeltas != null)
                    {
                        if (existingChild.markerDeltas != null)
                            throw new AssertionError(
                                "Merge of marker deltas is not supported"); //$NON-NLS-1$

                        existingChild.markerDeltas = child.markerDeltas;
                    }

                    // add resource deltas if needed
                    if (child.resourceDeltas != null)
                    {
                        if (existingChild.resourceDeltas != null)
                            throw new AssertionError(
                                "Merge of resource deltas is not supported"); //$NON-NLS-1$

                        existingChild.resourceDeltas = child.resourceDeltas;
                        existingChild.resourceDeltasCounter =
                            child.resourceDeltasCounter;
                    }

                    return;
                }
                break;
            default:
                // unknown -> existing child becomes the child with the existing child's flags
                int flags = existingChild.getFlags();
                affectedChildren[index] = child;
                child.flags |= flags;
            }
        }
    }

    /**
     * Removes the child delta from the collection of affected children.
     * 
     * @param child the child delta to remove (not <code>null</code>)
     */
    public void removeAffectedChild(HandleDelta child)
    {
        if (child == null)
            throw new IllegalArgumentException();

        if (affectedChildren.length == 0)
            return;

        Key key = new Key(child.getElement());
        Integer index = indexOfChild(key);
        if (index != null)
        {
            removeExistingChild(key, index);
        }
    }

    /**
     * Clears the collection of affected children.
     */
    public void clearAffectedChildren()
    {
        affectedChildren = EMPTY_HANDLE_DELTAS;
        childIndex = null;
    }

    /**
     * Returns the delta for the given element in the delta tree, 
     * or <code>null</code> if no delta for the given element is found.
     * 
     * @param element the element to search delta for or <code>null</code>
     * @return the delta for the given element, or <code>null</code> if not found
     */
    public HandleDelta getDeltaFor(IHandle element)
    {
        if (element == null)
            return null;
        if (equalsAndSameParent(getElement(), element))
            return this;
        return findDescendant(new Key(element));
    }

    /**
     * Sets the marker deltas.
     *
     * @param markerDeltas the marker deltas to set (not <code>null</code>, 
     *  not empty)
     */
    public void setMarkerDeltas(IMarkerDelta[] markerDeltas)
    {
        if (markerDeltas == null || markerDeltas.length == 0)
            throw new IllegalArgumentException();

        switch (kind)
        {
        case ADDED:
        case REMOVED:
            return;
        case CHANGED:
            flags |= F_MARKERS;
            break;
        default:
            kind = CHANGED;
            flags |= F_MARKERS;
        }
        this.markerDeltas = markerDeltas;
    }

    /**
     * Adds the child resource delta to the collection of resource deltas.
     * 
     * @param child the resource delta to add (not <code>null</code>)
     */
    public void addResourceDelta(IResourceDelta child)
    {
        if (child == null)
            throw new IllegalArgumentException();

        switch (kind)
        {
        case ADDED:
        case REMOVED:
            // no need to add a child if this parent is added or removed
            return;
        case CHANGED:
            flags |= F_CONTENT;
            break;
        default:
            kind = CHANGED;
            flags |= F_CONTENT;
        }
        if (resourceDeltas == null)
        {
            resourceDeltas = new IResourceDelta[5];
            resourceDeltas[resourceDeltasCounter++] = child;
            return;
        }
        if (resourceDeltas.length == resourceDeltasCounter)
        {
            // need a resize
            System.arraycopy(resourceDeltas, 0, (resourceDeltas =
                new IResourceDelta[resourceDeltasCounter * 2]), 0,
                resourceDeltasCounter);
        }
        resourceDeltas[resourceDeltasCounter++] = child;
    }

    @Override
    public String toString()
    {
        return toDebugString(0);
    }

    /** 
     * Returns a string representation of this delta structure 
     * suitable for debugging purposes.
     */
    public String toDebugString(int depth)
    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < depth; i++)
        {
            builder.append('\t');
        }
        builder.append(((Handle)getElement()).toDebugString());
        toDebugString(builder);
        IHandleDelta[] children = getAffectedChildren();
        if (children != null)
        {
            for (int i = 0; i < children.length; ++i)
            {
                builder.append("\n"); //$NON-NLS-1$
                builder.append(((HandleDelta)children[i]).toDebugString(depth + 1));
            }
        }
        for (int i = 0; i < resourceDeltasCounter; i++)
        {
            builder.append("\n");//$NON-NLS-1$
            for (int j = 0; j < depth + 1; j++)
            {
                builder.append('\t');
            }
            IResourceDelta resourceDelta = resourceDeltas[i];
            builder.append(resourceDelta.toString());
            builder.append("["); //$NON-NLS-1$
            switch (resourceDelta.getKind())
            {
            case IResourceDelta.ADDED:
                builder.append('+');
                break;
            case IResourceDelta.REMOVED:
                builder.append('-');
                break;
            case IResourceDelta.CHANGED:
                builder.append('*');
                break;
            default:
                builder.append('?');
                break;
            }
            builder.append("]"); //$NON-NLS-1$
        }
        return builder.toString();
    }

    /**
     * Debugging purposes
     */
    protected void toDebugString(StringBuilder builder)
    {
        builder.append("["); //$NON-NLS-1$
        switch (getKind())
        {
        case IHandleDelta.ADDED:
            builder.append('+');
            break;
        case IHandleDelta.REMOVED:
            builder.append('-');
            break;
        case IHandleDelta.CHANGED:
            builder.append('*');
            break;
        default:
            builder.append('?');
            break;
        }
        builder.append("]: {"); //$NON-NLS-1$
        toDebugString(builder, getFlags());
        builder.append("}"); //$NON-NLS-1$
    }

    /**
     * Debugging purposes
     */
    protected boolean toDebugString(StringBuilder builder, int flags)
    {
        boolean prev = false;
        if ((flags & IHandleDelta.F_CHILDREN) != 0)
        {
            if (prev)
                builder.append(" | "); //$NON-NLS-1$
            builder.append("CHILDREN"); //$NON-NLS-1$
            prev = true;
        }
        if ((flags & IHandleDelta.F_CONTENT) != 0)
        {
            if (prev)
                builder.append(" | "); //$NON-NLS-1$
            builder.append("CONTENT"); //$NON-NLS-1$
            prev = true;
        }
        if ((flags & IHandleDelta.F_MOVED_FROM) != 0)
        {
            if (prev)
                builder.append(" | "); //$NON-NLS-1$
            builder.append("MOVED_FROM(" + ((Handle)getMovedFromElement()).toStringWithAncestors() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
            prev = true;
        }
        if ((flags & IHandleDelta.F_MOVED_TO) != 0)
        {
            if (prev)
                builder.append(" | "); //$NON-NLS-1$
            builder.append("MOVED_TO(" + ((Handle)getMovedToElement()).toStringWithAncestors() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
            prev = true;
        }
        if ((flags & IHandleDelta.F_REORDER) != 0)
        {
            if (prev)
                builder.append(" | "); //$NON-NLS-1$
            builder.append("REORDERED"); //$NON-NLS-1$
            prev = true;
        }
        if ((flags & IHandleDelta.F_FINE_GRAINED) != 0)
        {
            if (prev)
                builder.append(" | "); //$NON-NLS-1$
            builder.append("FINE GRAINED"); //$NON-NLS-1$
            prev = true;
        }
        if ((flags & IHandleDelta.F_OPEN) != 0)
        {
            if (prev)
                builder.append(" | "); //$NON-NLS-1$
            builder.append("OPEN"); //$NON-NLS-1$
            prev = true;
        }
        if ((flags & IHandleDelta.F_DESCRIPTION) != 0)
        {
            if (prev)
                builder.append(" | "); //$NON-NLS-1$
            builder.append("DESCRIPTION"); //$NON-NLS-1$
            prev = true;
        }
        if ((flags & IHandleDelta.F_WORKING_COPY) != 0)
        {
            if (prev)
                builder.append(" | "); //$NON-NLS-1$
            builder.append("WORKING COPY"); //$NON-NLS-1$
            prev = true;
        }
        if ((flags & IHandleDelta.F_UNDERLYING_RESOURCE) != 0)
        {
            if (prev)
                builder.append(" | "); //$NON-NLS-1$
            builder.append("UNDERLYING_RESOURCE"); //$NON-NLS-1$
            prev = true;
        }
        if ((flags & IHandleDelta.F_MARKERS) != 0)
        {
            if (prev)
                builder.append(" | "); //$NON-NLS-1$
            builder.append("MARKERS"); //$NON-NLS-1$
            prev = true;
        }
        if ((flags & IHandleDelta.F_SYNC) != 0)
        {
            if (prev)
                builder.append(" | "); //$NON-NLS-1$
            builder.append("SYNC"); //$NON-NLS-1$
            prev = true;
        }
        return prev;
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
     * Returns a new <code>ADDED</code> delta for the given element.
     * 
     * @param element the element that this delta describes a change to 
     *  (not <code>null</code>)
     * @param flags the change flags
     * @return a new <code>ADDED</code> delta for the given element
     *  (never <code>null</code>)
     */
    protected HandleDelta newAdded(IHandle element, int flags)
    {
        HandleDelta delta = newDelta(element);
        delta.kind = ADDED;
        delta.flags = flags;
        return delta;
    }

    /**
     * Returns a new <code>REMOVED</code> delta for the given element.
     * 
     * @param element the element that this delta describes a change to 
     *  (not <code>null</code>)
     * @param flags the change flags
     * @return a new <code>REMOVED</code> delta for the given element
     *  (never <code>null</code>)
     */
    protected HandleDelta newRemoved(IHandle element, int flags)
    {
        HandleDelta delta = newDelta(element);
        delta.kind = REMOVED;
        delta.flags = flags;
        return delta;
    }

    /**
     * Returns a new <code>CHANGED</code> delta for the given element.
     * 
     * @param element the element that this delta describes a change to 
     *  (not <code>null</code>)
     * @param flags the change flags
     * @return a new <code>CHANGED</code> delta for the given element
     *  (never <code>null</code>)
     */
    protected HandleDelta newChanged(IHandle element, int flags)
    {
        HandleDelta delta = newDelta(element);
        delta.kind = CHANGED;
        delta.flags = flags;
        return delta;
    }

    /**
     * Returns a new "moved from" (<code>REMOVED</code>) delta for the 
     * given element.
     *
     * @param movedFromElement the element before it was moved to its current 
     *  location (not <code>null</code>)
     * @param movedToElement the element in its new location (not <code>null</code>)
     * @return a new "moved from" (<code>REMOVED</code>) delta
     *  (never <code>null</code>)
     */
    protected HandleDelta newMovedFrom(IHandle movedFromElement,
        IHandle movedToElement)
    {
        HandleDelta delta = newRemoved(movedFromElement, F_MOVED_TO);
        if (movedToElement == null)
            throw new IllegalArgumentException();
        delta.movedToElement = movedToElement;
        return delta;
    }

    /**
     * Returns a new "moved to" (<code>ADDED</code>) delta for the given element.
     *
     * @param movedToElement the element in its new location (not <code>null</code>)
     * @param movedFromElement the element before it was moved to its current 
     *  location (not <code>null</code>)
     * @return a new "moved to" (<code>ADDED</code>) delta
     *  (never <code>null</code>)
     */
    protected HandleDelta newMovedTo(IHandle movedToElement,
        IHandle movedFromElement)
    {
        HandleDelta delta = newAdded(movedToElement, F_MOVED_FROM);
        if (movedFromElement == null)
            throw new IllegalArgumentException();
        delta.movedFromElement = movedFromElement;
        return delta;
    }

    /**
     * Returns a collection of all the parents of the given element up to (but 
     * not including) the root of this tree in bottom-up order. If the given 
     * element is not a descendant of the root of this tree, <code>null</code> 
     * is returned.
     */
    protected final List<IHandle> getAncestors(IHandle child)
    {
        IHandle parent = child.getParent();
        if (parent == null)
            return null;

        ArrayList<IHandle> parents = new ArrayList<IHandle>();
        while (!parent.equals(getElement()))
        {
            parents.add(parent);
            parent = parent.getParent();
            if (parent == null)
            {
                return null;
            }
        }
        parents.trimToSize();
        return parents;
    }

    /**
     * @param type one of {@link IHandleDelta#ADDED ADDED},
     *  {@link IHandleDelta#REMOVED REMOVED}, or
     *  {@link IHandleDelta#CHANGED CHANGED}
     * @return deltas for the affected children of the given type
     *  (never <code>null</code>)
     */
    protected final IHandleDelta[] getChildrenOfType(int type)
    {
        int length = affectedChildren.length;
        if (length == 0)
            return EMPTY_HANDLE_DELTAS;

        ArrayList<IHandleDelta> children = new ArrayList<IHandleDelta>(length);
        for (int i = 0; i < length; i++)
        {
            if (affectedChildren[i].getKind() == type)
                children.add(affectedChildren[i]);
        }

        IHandleDelta[] childrenOfType = new IHandleDelta[children.size()];
        children.toArray(childrenOfType);
        return childrenOfType;
    }

    /**
     * Returns the descendant delta for the given key in the delta tree, 
     * or <code>null</code> if no delta for the given key is found.
     * 
     * @param key the key to search delta for (not <code>null</code>)
     * @return the delta for the given key, or <code>null</code> if not found
     */
    protected final HandleDelta findDescendant(Key key)
    {
        if (affectedChildren.length == 0)
            return null;
        Integer index = indexOfChild(key);
        if (index != null)
            return (HandleDelta)affectedChildren[index];
        for (IHandleDelta child : affectedChildren)
        {
            HandleDelta delta = ((HandleDelta)child).findDescendant(key);
            if (delta != null)
                return delta;
        }
        return null;
    }

    /**
     * Returns the index of the delta in the collection of affected children,
     * or <code>null</code> if the child delta for the given key is not found. 
     *
     * @param key the key to search child delta for (not <code>null</code>)
     * @return the index of the child delta for the given key,
     *  or <code>null</code> if not found
     */
    protected Integer indexOfChild(Key key)
    {
        int length = affectedChildren.length;
        if (!needsChildIndex())
        {
            for (int i = 0; i < length; i++)
            {
                if (equalsAndSameParent(key.getElement(),
                    affectedChildren[i].getElement()))
                {
                    return i;
                }
            }
            return null;
        }
        if (childIndex == null)
        {
            childIndex = new HashMap<Key, Integer>();
            for (int i = 0; i < length; i++)
            {
                childIndex.put(new Key(affectedChildren[i].getElement()), i);
            }
        }
        return childIndex.get(key);
    }

    /**
     * Adds the new child delta to the collection of affected children.
     * 
     * @param child the child delta to add (not <code>null</code>)
     */
    protected void addNewChild(HandleDelta child)
    {
        affectedChildren = growAndAddToArray(affectedChildren, child);
        if (childIndex != null)
        {
            childIndex.put(new Key(child.getElement()),
                affectedChildren.length - 1);
        }
    }

    /**
     * Removes the existing child delta from the collection of affected children.
     * 
     * @param key
     *  the child delta key (not <code>null</code>)
     * @param index
     *  the index of the child delta in the collection of affected children
     */
    protected void removeExistingChild(Key key, int index)
    {
        affectedChildren = removeAndShrinkArray(affectedChildren, index);
        if (childIndex != null)
        {
            if (!needsChildIndex())
                childIndex = null;
            else
            {
                childIndex.remove(key);
                for (int i = index; i < affectedChildren.length; i++)
                {
                    childIndex.put(new Key(affectedChildren[i].getElement()), i);
                }
            }
        }
    }

    /**
     * Returns whether {@link #childIndex} needs to be used for child lookup.
     * <p>
     * Default implementation answers <code>true</code> iff
     * <code>affectedChildren.length >= 3</code>.
     * </p>
     */
    protected boolean needsChildIndex()
    {
        return affectedChildren.length >= 3;
    }

    /**
     * Returns whether the two elements are equal and have the same parent.
     */
    protected static boolean equalsAndSameParent(IHandle e1, IHandle e2)
    {
        if (!e1.equals(e2))
            return false;

        IHandle parent1 = e1.getParent();
        IHandle parent2 = e2.getParent();
        if (parent1 == null)
        {
            if (parent2 != null)
                return false;
        }
        else
        {
            if (!parent1.equals(parent2))
                return false;
        }
        return true;
    }

    /**
     * Adds the new element to a new array that contains all of the elements 
     * of the old array. Returns the new array.
     */
    protected static IHandleDelta[] growAndAddToArray(IHandleDelta[] array,
        IHandleDelta addition)
    {
        IHandleDelta[] old = array;
        array = new IHandleDelta[old.length + 1];
        System.arraycopy(old, 0, array, 0, old.length);
        array[old.length] = addition;
        return array;
    }

    /**
     * Removes the element from the array. Returns the new array which has shrunk.
     */
    protected static IHandleDelta[] removeAndShrinkArray(IHandleDelta[] old,
        int index)
    {
        IHandleDelta[] array = new IHandleDelta[old.length - 1];
        if (index > 0)
            System.arraycopy(old, 0, array, 0, index);
        int rest = old.length - index - 1;
        if (rest > 0)
            System.arraycopy(old, index + 1, array, index, rest);
        return array;
    }

    /**
     * The delta key.
     */
    protected static class Key
    {
        private final IHandle element;

        /**
         * Constructs a new delta key for the given element. 
         *
         * @param element an {@link IHandle} (not <code>null</code>)
         */
        public Key(IHandle element)
        {
            if (element == null)
                throw new IllegalArgumentException();
            this.element = element;
        }

        /**
         * @return the element of the key (never <code>null</code>)
         */
        public IHandle getElement()
        {
            return element;
        }

        @Override
        public int hashCode()
        {
            return element.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof Key))
                return false;
            return equalsAndSameParent(element, ((Key)obj).element);
        }
    }
}
