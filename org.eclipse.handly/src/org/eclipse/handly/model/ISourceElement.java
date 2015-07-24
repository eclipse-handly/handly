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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.snapshot.StaleSnapshotException;

/**
 * Common protocol for elements that may have associated source code.
 * The children are of type {@link ISourceConstruct} and appear
 * in declaration order.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ISourceElement
    extends IHandle
{
    /**
     * Returns the smallest element within this element that includes
     * the given source position, or <code>null</code> if the given position
     * is not within the source range of this element. If no finer grained
     * element is found at the position, this element itself is returned.
     *
     * @param position a source position (0-based)
     * @param base a snapshot on which the given position is based,
     *  or <code>null</code> if the snapshot is unknown or doesn't matter
     * @return the innermost element enclosing the given source position,
     *  or <code>null</code> if none (including this element itself)
     * @throws CoreException if this element does not exist or if an
     *  exception occurs while accessing its corresponding resource
     * @throws StaleSnapshotException if snapshot inconsistency is detected,
     *  i.e. this element's current structure and properties are based on
     *  a different snapshot
     */
    ISourceElement getElementAt(int position, ISnapshot base)
        throws CoreException;

    /**
     * Returns an object holding cached structure and properties for this element.
     *
     * @return an object holding cached structure and properties for this element
     *  (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an
     *  exception occurs while accessing its corresponding resource
     * @see ISourceElementInfo
     */
    ISourceElementInfo getSourceElementInfo() throws CoreException;

    /**
     * Represents a named property of the source element.
     *
     * @param <T> the type of property values
     * @see ISourceElementInfo#get(Property)
     */
    final class Property<T>
    {
        private final String name;

        /**
         * Constructs a new property with the given name.
         *
         * @param name the name of the property (not <code>null</code>)
         */
        public Property(String name)
        {
            if (name == null)
                throw new IllegalArgumentException();
            this.name = name;
        }

        /**
         * Returns the name of the property.
         *
         * @return the property name (never <code>null</code>)
         */
        public String getName()
        {
            return name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }
}
