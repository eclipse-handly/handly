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

import static junit.framework.Assert.*;

import org.eclipse.handly.examples.javamodel.IJavaElementDelta;
import org.eclipse.handly.model.IElementChangeEvent;
import org.eclipse.handly.model.IElementChangeListener;

class JavaModelListener
    implements IElementChangeListener
{
    IJavaElementDelta delta;

    @Override
    public void elementChanged(IElementChangeEvent event)
    {
        delta = (IJavaElementDelta)event.getDelta();
    }

    void assertDelta(String expected)
    {
        String actual =
            (delta == null ? "<null>" : delta.toString().replace("\t", "    "));
        assertEquals(expected, actual);
    }
}
