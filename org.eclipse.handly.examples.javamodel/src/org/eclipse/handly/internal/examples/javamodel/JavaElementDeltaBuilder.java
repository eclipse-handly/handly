/*******************************************************************************
 * Copyright (c) 2015 1C-Soft LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel;

import org.eclipse.handly.examples.javamodel.IJavaElement;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.impl.HandleDelta;
import org.eclipse.handly.model.impl.HandleDeltaBuilder;

/**
 * Specialization of {@link HandleDeltaBuilder}
 * that builds a {@link JavaElementDelta}.
 */
public class JavaElementDeltaBuilder
    extends HandleDeltaBuilder
{
    /**
     * Constructs a delta builder on the given element
     * looking as deep as necessary.
     * 
     * @param element the tracked element (not <code>null</code>)
     */
    public JavaElementDeltaBuilder(IJavaElement element)
    {
        super(element);
    }

    @Override
    protected HandleDelta newDelta(IHandle element)
    {
        return new JavaElementDelta((IJavaElement)element);
    }
}
