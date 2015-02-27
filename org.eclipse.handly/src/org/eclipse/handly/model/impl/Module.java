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
package org.eclipse.handly.model.impl;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.model.IModule;
import org.eclipse.handly.model.ISourceElement;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.snapshot.StaleSnapshotException;

/**
 * Represents the topmost source element.
 */
public abstract class Module
    extends SourceElement
    implements IModule
{
    /**
     * Constructs a handle for a module with the given parent element
     * and the given name.
     * 
     * @param parent the parent of the element (not <code>null</code>)
     * @param name the name of the element, or <code>null</code>
     *  if the element has no name
     */
    public Module(Handle parent, String name)
    {
        super(parent, name);
    }

    @Override
    public final Module getModule()
    {
        return this;
    }

    @Override
    public SourceFile getSourceFile()
    {
        if (this instanceof SourceFile)
            return (SourceFile)this;
        return null;
    }

    @Override
    public final ISourceElement getElementAt(int position, ISnapshot base)
    {
        try
        {
            return getElementAt(this, position, base);
        }
        catch (CoreException e)
        {
            // ignore
        }
        catch (StaleSnapshotException e)
        {
            // ignore
        }
        return null;
    }
}
