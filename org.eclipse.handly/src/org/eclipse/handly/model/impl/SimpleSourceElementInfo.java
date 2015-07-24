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
package org.eclipse.handly.model.impl;

import org.eclipse.handly.model.ISourceConstruct;
import org.eclipse.handly.model.ISourceElement.Property;
import org.eclipse.handly.model.ISourceElementInfo;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.util.TextRange;

/**
 * The simplest possible implementation of {@link ISourceElementInfo}.
 * <p>
 * Clients can use this class as it stands or subclass it
 * as circumstances warrant.
 * </p>
 */
public class SimpleSourceElementInfo
    implements ISourceElementInfo
{
    private static final ISourceConstruct[] NO_CHILDREN =
        new ISourceConstruct[0];

    @Override
    public ISnapshot getSnapshot()
    {
        return null;
    }

    @Override
    public <T> T get(Property<T> property)
    {
        return null;
    }

    @Override
    public ISourceConstruct[] getChildren()
    {
        return NO_CHILDREN;
    }

    @Override
    public TextRange getFullRange()
    {
        return null;
    }

    @Override
    public TextRange getIdentifyingRange()
    {
        return null;
    }
}
