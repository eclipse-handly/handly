/*******************************************************************************
 * Copyright (c) 2014, 2015 1C-Soft LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.model;

import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.util.TextRange;

/**
 * Holds cached structure and properties for a source element. Those
 * structure and properties correlate to a source snapshot.
 * 
 * @see ISourceElement
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ISourceElementInfo
{
    /**
     * Returns the source snapshot on which this object is based,
     * or <code>null</code> if the element has no associated source code.
     *
     * @return the source snapshot on which this object is based,
     *  or <code>null</code> if the element has no associated source code
     */
    ISnapshot getSnapshot();

    /**
     * Returns the cached value of the given property, or <code>null</code>
     * if the property is not set.
     * <p>
     * Note that the result correlates to a source {@link #getSnapshot()
     * snapshot} (if there is one) and may be inconsistent with the current
     * source contents.
     * </p>
     *
     * @param property a source element's property (not <code>null</code>)
     * @return the cached value of the given property, or <code>null</code>
     *  if the property is not set
     */
    <T> T get(ISourceElement.Property<T> property);

    /**
     * Returns the cached children of the source element. The children appear
     * in declaration order.
     * <p>
     * Note that the result correlates to a source {@link #getSnapshot()
     * snapshot} (if there is one) and may be inconsistent with the current
     * source contents.
     * </p>
     *
     * @return the cached children of the source element
     *  (never <code>null</code>)
     */
    ISourceConstruct[] getChildren();

    /**
     * Returns the text range of the element in the source {@link #getSnapshot()
     * snapshot}, or <code>null</code> if none.
     *
     * @return the text range associated with the element,
     *  or <code>null</code> if none
     */
    TextRange getFullRange();

    /**
     * Returns the text range of the element's identifier in the source
     * {@link #getSnapshot() snapshot}, or <code>null</code> if none.
     *
     * @return the text range associated with the element's identifier,
     *  or <code>null</code> if none
     */
    TextRange getIdentifyingRange();
}