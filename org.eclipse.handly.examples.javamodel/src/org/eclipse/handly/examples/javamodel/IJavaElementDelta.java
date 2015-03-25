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
package org.eclipse.handly.examples.javamodel;

import org.eclipse.handly.model.IHandleDelta;

/**
 * A Java element delta describes changes in Java element between two discrete
 * points in time.  Given a delta, clients can access the element that has
 * changed, and any children that have changed.
 * 
 * @see IHandleDelta
 */
public interface IJavaElementDelta
    extends IHandleDelta
{
    /**
     * Change flag indicating that the raw classpath (or the output folder)
     * of a project has changed. This flag is only valid if the element is
     * an {@link IJavaProject}.
     */
    int F_CLASSPATH_CHANGED = 0x10000; // lower end of model-specific range

    @Override
    IJavaElement getElement();

    @Override
    IJavaElementDelta[] getAffectedChildren();

    @Override
    IJavaElementDelta[] getAddedChildren();

    @Override
    IJavaElementDelta[] getRemovedChildren();

    @Override
    IJavaElementDelta[] getChangedChildren();

    @Override
    IJavaElement getMovedFromElement();

    @Override
    IJavaElement getMovedToElement();
}
