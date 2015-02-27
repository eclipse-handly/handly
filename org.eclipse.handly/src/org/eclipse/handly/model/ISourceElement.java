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

import org.eclipse.core.runtime.CoreException;

/**
 * Common protocol for elements that have associated source code.
 * The children are source constructs and appear in declaration order.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ISourceElement
    extends IHandle
{
    /**
     * Returns the module containing this element.
     * Returns this element if it is a module.
     * This is a handle-only method.
     * 
     * @return the element's module (never <code>null</code>)
     */
    IModule getModule();

    /**
     * Returns the source file containing this element, or <code>null</code>
     * if this element is not contained in a source file.
     * Returns this element if it is a source file. 
     * This is a handle-only method.
     * 
     * @return the element's source file, or <code>null</code> if this element
     *  is not contained in a source file
     * @see #getModule()
     */
    ISourceFile getSourceFile();

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
