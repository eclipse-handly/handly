/*******************************************************************************
 * Copyright (c) 2015 1C LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel;

import org.eclipse.handly.examples.javamodel.IType;
import org.eclipse.handly.model.impl.Handle;

/**
 * Implementation of {@link IType}.
 */
public class Type
    extends Member
    implements IType
{
    /**
     * Creates a handle for a type with the given parent element 
     * and the given name.
     * 
     * @param parent the parent of the element (not <code>null</code>)
     * @param name the name of the element (not <code>null</code>)
     */
    public Type(Handle parent, String name)
    {
        super(parent, name);
        if (name == null)
            throw new IllegalArgumentException();
    }
}
