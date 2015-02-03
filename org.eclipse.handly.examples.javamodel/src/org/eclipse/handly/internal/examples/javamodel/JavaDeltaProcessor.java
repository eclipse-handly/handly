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

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.examples.javamodel.JavaModelCore;
import org.eclipse.handly.model.impl.HandleDelta;

/**
 * This class is used by the <code>JavaModelManager</code> to convert 
 * resource deltas into Java element deltas. It also does some processing 
 * on the Java elemens involved (e.g. closing them).
 */
class JavaDeltaProcessor
    implements IResourceDeltaVisitor
{
    private HandleDelta currentDelta = new HandleDelta(
        JavaModelCore.getJavaModel());

    /**
     * Returns the Java element delta built from the resource delta. 
     * Returns an empty delta if no Java elements were affected 
     * by the resource change.
     * 
     * @return Java element delta (never <code>null</code>)
     */
    public HandleDelta getDelta()
    {
        return currentDelta;
    }

    @Override
    public boolean visit(IResourceDelta delta) throws CoreException
    {
        // TODO Auto-generated method stub
        return false;
    }
}
