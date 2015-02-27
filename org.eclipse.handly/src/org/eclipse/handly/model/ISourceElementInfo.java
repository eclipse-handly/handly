/*******************************************************************************
 * Copyright (c) 2014, 2015 1C LLC.
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
 * structure and properties correlate to a known snapshot of the source.
 * 
 * @see ISourceElement
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ISourceElementInfo
{
    /**
     * Returns the source snapshot on which this object is based
     *
     * @return the source snapshot on which this object is based
     *  (never <code>null</code>)
     */
    ISnapshot getSnapshot();

    /**
     * Returns the cached value of the given property, or <code>null</code> 
     * if the property is not set.
     * <p>
     * Note that the result correlates to a source {@link #getSnapshot()
     * snapshot} and may be inconsistent with the current contents 
     * of the source.
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
     * snapshot} and may be inconsistent with the current contents 
     * of the source.
     * </p> 
     *
     * @return the cached children of the source element 
     *  (never <code>null</code>)
     */
    ISourceConstruct[] getChildren();

    /**
     * Returns the cached text range of the source element.
     * <p>
     * Note that the result correlates to a source {@link #getSnapshot()
     * snapshot} and may be inconsistent with the current contents 
     * of the source.
     * </p>
     *
     * @return the cached text range of the source element 
     *  (never <code>null</code>)
     */
    TextRange getFullRange();

    /**
     * Returns the cached text range of the source element's identifier. 
     * Can be used for highlighting the element in a text editor, etc.
     * <p>
     * Note that the result correlates to a source {@link #getSnapshot()
     * snapshot} and may be inconsistent with the current contents 
     * of the source.
     * </p>
     *
     * @return the cached text range of the source element's identifier 
     *  (never <code>null</code>)
     */
    TextRange getIdentifyingRange();
}
