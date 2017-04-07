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
package org.eclipse.handly.model;

/**
 * Represents an element of a Handly-based model.
 * <p>
 * Elements of a Handly-based model are exposed to clients as handles
 * to the actual underlying element. The model may hand out any number
 * of handles for each element. Handles that refer to the same element
 * are guaranteed to be equal, but not necessarily identical.
 * </p>
 * <p>
 * The class {@link Elements} provides methods for generic access to elements
 * of a Handly-based model.
 * </p>
 * <p>
 * Elements of a Handly-based model are safe for use by multiple threads.
 * </p>
 */
public interface IElement
{
    /*
     * Implementors of this interface must also implement IElementImpl.
     */

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();
}