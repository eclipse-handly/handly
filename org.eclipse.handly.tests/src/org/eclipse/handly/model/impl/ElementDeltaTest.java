/*******************************************************************************
 * Copyright (c) 2014, 2016 1C-Soft LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.model.impl;

import junit.framework.TestCase;

/**
 * <code>ElementDelta</code> tests.
 */
public class ElementDeltaTest
    extends TestCase
{
    /**
     * Regression test for bug 456060 - AIOOB in #addAffectedChild.
     */
    public void testBug456060()
    {
        SimpleElement root = new SimpleElement(null, "root", new SimpleModel());
        ElementDelta.Builder builder = new ElementDelta.Builder(
            new ElementDelta(root));
        builder.added(root.getChild("A"));
        builder.added(root.getChild("B"));
        builder.added(root.getChild("C"));
        builder.added(root.getChild("D"));
        builder.movedFrom(root.getChild("C"), root.getChild("X"));
        builder.movedFrom(root.getChild("D"), root.getChild("Y"));
    }

    public void testBadlyFormedDeltaTree()
    {
        SimpleElement parent = new SimpleElement(null, "parent",
            new SimpleModel());
        SimpleElement child = parent.getChild("child");
        ElementDelta.Builder builder = new ElementDelta.Builder(
            new ElementDelta(child));
        try
        {
            builder.added(parent);
            fail();
        }
        catch (IllegalArgumentException e)
        {
        }
    }
}
