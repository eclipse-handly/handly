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
package org.eclipse.handly.model;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResourceDelta;

/**
 * A handle delta describes changes in the corresponding element between
 * two discrete points in time. Given a delta, clients can access the element
 * that has changed, and any children that have changed.
 * <p>
 * Deltas have a different status depending on the kind of change they represent.
 * The list below summarizes each status (as returned by {@link #getKind})
 * and its meaning (see individual constants for a more detailed description):
 * </p>
 * <ul>
 * <li>{@link #ADDED} - The element described by the delta has been added.
 * <li>{@link #REMOVED} - The element described by the delta has been removed.
 * <li>{@link #CHANGED} - The element described by the delta has been changed
 * in some way.
 * </ul>
 * <p>
 * Specification of the type of change is provided by {@link #getFlags}.
 * </p>
 * <p>
 * Move operations are indicated by special change flags. If element A is moved
 * to become B, the delta for the change in A will have status {@link #REMOVED},
 * with change flag {@link #F_MOVED_TO}. In this case, {@link #getMovedToElement}
 * on delta A will return the handle for B. The delta for B will have
 * status {@link #ADDED}, with change flag {@link #F_MOVED_FROM}, and {@link
 * #getMovedFromElement} on delta B will return the handle for A. (Note,
 * the handle to A in this case represents an element that no longer exists).
 * Note that the move change flags only describe the changes to a single element,
 * they do not imply anything about the parent or children of the element.
 * </p>
 * <p>
 * No assumptions should be made on which element level the delta tree is rooted.
 * Delta objects are not valid outside the dynamic scope of the notification.
 * </p>
 * <p>
 * Adapted from <code>org.eclipse.jdt.core.IJavaElementDelta</code>.
 * </p>
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IHandleDelta
{
    /**
     * Status constant indicating that the element has been added. Note that
     * an added element delta has no children, as they are all implicitly added.
     */
    int ADDED = 1;

    /**
     * Status constant indicating that the element has been removed. Note that
     * a removed element delta has no children, as they are all implicitly removed.
     */
    int REMOVED = 2;

    /**
     * Status constant indicating that the element has been changed,
     * as described by the change flags.
     * @see #getFlags()
     */
    int CHANGED = 3;

    /**
     * Change flag indicating that the content of the element has changed.
     * This flag is only valid for elements which correspond to source elements.
     */
    long F_CONTENT = 1L << 0;

    /**
     * Change flag indicating that there are changes to the children of the element.
     */
    long F_CHILDREN = 1L << 1;

    /**
     * Change flag indicating that the element was moved from another location.
     * The location of the old element can be retrieved using {@link #getMovedFromElement}.
     */
    long F_MOVED_FROM = 1L << 2;

    /**
     * Change flag indicating that the element was moved to another location.
     * The location of the new element can be retrieved using {@link #getMovedToElement}.
     */
    long F_MOVED_TO = 1L << 3;

    /**
     * Change flag indicating that the element has changed position relatively
     * to its siblings.
     */
    long F_REORDER = 1L << 4;

    /**
     * Change flag indicating that this is a fine-grained delta, that is,
     * an analysis down to the source constructs level was done to determine
     * if there were structural changes to source constructs.
     * <p>
     * Clients can use this flag to find out if a source file that has a
     * {@link #F_CONTENT} change should assume that there are no finer grained
     * changes ({@link #F_FINE_GRAINED} is set) or if finer grained changes
     * were not considered ({@link #F_FINE_GRAINED} is not set).
     * </p>
     */
    long F_FINE_GRAINED = 1L << 5;

    /**
     * Change flag indicating that the underlying <code>IProject</code>
     * has been opened or closed. This flag is only valid if the element
     * represents a project.
     */
    long F_OPEN = 1L << 6;

    /**
     * Change flag indicating that the underlying <code>IProject</code>'s
     * description has changed. This flag is only valid if the element
     * represents a project.
     */
    long F_DESCRIPTION = 1L << 7;

    /**
     * Change flag indicating that a source file has become a working copy,
     * or that a working copy has reverted to a source file. This flag is
     * only valid if the element represents a source file.
     */
    long F_WORKING_COPY = 1L << 8;

    /**
     * Change flag indicating that the underlying <code>IFile</code> of
     * a working copy has changed. This flag is only valid if the element
     * represents a source file.
     */
    long F_UNDERLYING_RESOURCE = 1L << 9;

    /**
     * Change flag indicating that markers on the element's corresponding
     * resource have changed. This flag is only valid if the element has
     * a corresponding resource.
     *
     * @see #getMarkerDeltas()
     */
    long F_MARKERS = 1L << 10;

    /**
     * Change flag indicating that sync status of the element's corresponding
     * resource has changed. This flag is only valid if the element has
     * a corresponding resource.
     */
    long F_SYNC = 1L << 11;

    /**
     * @return the element that this delta describes a change to
     *  (never <code>null</code>)
     */
    IHandle getElement();

    /**
     * Returns the kind of this delta - one of {@link #ADDED}, {@link #REMOVED},
     * or {@link #CHANGED}.
     * @return the kind of this delta
     */
    int getKind();

    /**
     * Returns flags that describe how the element has changed.
     * Such flags should be tested using the <code>&amp;</code> operator.
     * For example:
     * <pre>
     * if ((delta.getFlags() &amp; IHandleDelta.F_CONTENT) != 0)
     * {
     *     // the delta indicates a content change
     * }</pre>
     * <p>
     * Some change flags are meaningful for most models and predefined
     * in this interface, while others are model-specific and defined by
     * the model implementor. The range for model-specific flags starts from
     * {@code 1L << 32} and includes the upper 32 bits of <code>long</code>
     * value. The lower 32 bits are reserved for predefined generic change flags.
     * </p>
     * @return flags that describe how the element has changed
     */
    long getFlags();

    /**
     * @return deltas for the affected (added, removed, or changed) children
     *  (never <code>null</code>). Clients <b>must not</b> modify
     *  the returned array.
     */
    IHandleDelta[] getAffectedChildren();

    /**
     * @return deltas for the children that have been added
     *  (never <code>null</code>). Clients <b>must not</b> modify
     *  the returned array.
     */
    IHandleDelta[] getAddedChildren();

    /**
     * @return deltas for the children that have been removed
     *  (never <code>null</code>). Clients <b>must not</b> modify
     *  the returned array.
     */
    IHandleDelta[] getRemovedChildren();

    /**
     * @return deltas for the children that have been changed
     *  (never <code>null</code>). Clients <b>must not</b> modify
     *  the returned array.
     */
    IHandleDelta[] getChangedChildren();

    /**
     * @return an element describing this element before it was moved
     * to its current location, or <code>null</code> if the {@link #F_MOVED_FROM}
     * change flag is not set
     */
    IHandle getMovedFromElement();

    /**
     * @return an element describing this element in its new location,
     * or <code>null</code> if the {@link #F_MOVED_TO} change flag is not set
     */
    IHandle getMovedToElement();

    /**
     * Returns the changes to markers on the element's corresponding resource.
     * Returns an empty array if none.
     * <p>
     * Note that marker deltas, like handle deltas, are generally only valid
     * for the dynamic scope of a notification. Clients <b>must not</b>
     * hang on to these objects.
     * </p>
     *
     * @return the marker deltas (never <code>null</code>).
     *  Clients <b>must not</b> modify the returned array.
     */
    IMarkerDelta[] getMarkerDeltas();

    /**
     * Returns the changes to children of the element's corresponding resource
     * that cannot be described in terms of handle deltas. Returns an empty array
     * if none.
     * <p>
     * Note that resource deltas, like handle deltas, are generally only valid
     * for the dynamic scope of a notification. Clients <b>must not</b>
     * hang on to these objects.
     * </p>
     *
     * @return the resource deltas (never <code>null</code>).
     *  Clients <b>must not</b> modify the returned array.
     */
    IResourceDelta[] getResourceDeltas();
}
