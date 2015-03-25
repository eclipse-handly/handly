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
import org.eclipse.handly.examples.javamodel.IJavaElementDelta;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.IHandleDelta;
import org.eclipse.handly.model.impl.HandleDelta;

/**
 * Implementation of {@link IJavaElementDelta}.
 * 
 * @see HandleDelta
 */
public class JavaElementDelta
    extends HandleDelta
    implements IJavaElementDelta
{
    /**
     * Constructs an initially empty delta for the given element.
     * 
     * @param element the element that this delta describes a change to
     *  (not <code>null</code>)
     */
    public JavaElementDelta(IJavaElement element)
    {
        super(element);
    }

    @Override
    public IJavaElement getElement()
    {
        return (IJavaElement)super.getElement();
    }

    @Override
    public IJavaElementDelta[] getAffectedChildren()
    {
        return convert(super.getAffectedChildren());
    }

    @Override
    public IJavaElementDelta[] getAddedChildren()
    {
        return convert(super.getAddedChildren());
    }

    @Override
    public IJavaElementDelta[] getRemovedChildren()
    {
        return convert(super.getRemovedChildren());
    }

    @Override
    public IJavaElementDelta[] getChangedChildren()
    {
        return convert(super.getChangedChildren());
    }

    @Override
    public IJavaElement getMovedFromElement()
    {
        return (IJavaElement)super.getMovedFromElement();
    }

    @Override
    public IJavaElement getMovedToElement()
    {
        return (IJavaElement)super.getMovedToElement();
    }

    @Override
    protected HandleDelta newDelta(IHandle element)
    {
        return new JavaElementDelta((IJavaElement)element);
    }

    @Override
    protected boolean toDebugString(StringBuilder builder, int flags)
    {
        boolean prev = super.toDebugString(builder, flags);
        if ((flags & IJavaElementDelta.F_CLASSPATH_CHANGED) != 0)
        {
            if (prev)
                builder.append(" | "); //$NON-NLS-1$
            builder.append("CLASSPATH_CHANGED"); //$NON-NLS-1$
            prev = true;
        }
        return prev;
    }

    private static IJavaElementDelta[] convert(IHandleDelta[] array)
    {
        IJavaElementDelta[] result = new IJavaElementDelta[array.length];
        System.arraycopy(array, 0, result, 0, array.length);
        return result;
    }
}
