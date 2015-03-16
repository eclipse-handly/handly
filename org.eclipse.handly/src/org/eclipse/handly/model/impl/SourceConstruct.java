/*******************************************************************************
 * Copyright (c) 2014, 2015 1C-Soft LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.model.impl;

import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.ISourceConstruct;

/**
 * Common superclass of {@link ISourceConstruct} implementations.
 */
public abstract class SourceConstruct
    extends SourceElement
    implements ISourceConstruct
{
    private int occurrenceCount = 1;

    /**
     * Creates a handle for a source construct with the given parent element 
     * and the given name.
     * 
     * @param parent the parent of the element (not <code>null</code>)
     * @param name the name of the element, or <code>null</code> 
     *  if the element has no name
     */
    public SourceConstruct(Handle parent, String name)
    {
        super(parent, name);
    }

    /**
     * Returns the count used to uniquely identify this element in the case 
     * that a duplicate named element exists. The occurrence count starts at 1 
     * (thus the first occurrence is occurrence 1, not occurrence 0).
     * 
     * @return the occurrence count for this element 
     */
    public final int getOccurenceCount()
    {
        return occurrenceCount;
    }

    /**
     * Increments the occurrence count of this element.
     * <p> 
     * This method is intended to be used only when building structure of 
     * a source file to distinguish source constructs with duplicate names.
     * </p>
     * 
     * @see #getOccurenceCount()
     * @see StructureHelper
     */
    public final void incrementOccurenceCount()
    {
        occurrenceCount++;
    }

    @Override
    public final IResource getResource()
    {
        return parent.getResource();
    }

    @Override
    public final boolean exists()
    {
        try
        {
            getBody();
            return true;
        }
        catch (CoreException e)
        {
            return false;
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof SourceConstruct))
            return false;
        return super.equals(obj)
            && occurrenceCount == ((SourceConstruct)obj).occurrenceCount;
    }

    @Override
    public final boolean close()
    {
        // The openable parent builds the whole structure and controls child life-cycle
        throw new AssertionError("This method should not be called"); //$NON-NLS-1$
    }

    @Override
    protected final void validateExistence() throws CoreException
    {
        // The openable parent builds the whole structure and determines child existence
        throw new AssertionError("This method should not be called"); //$NON-NLS-1$
    }

    @Override
    protected final Body newBody()
    {
        // The openable parent builds the whole structure and knows how to create child bodies
        return null;
    }

    @Override
    protected final Handle getOpenableParent()
    {
        Handle result = parent;
        // Source constructs are never openable
        while (result instanceof SourceConstruct)
            result = result.parent;
        return result;
    }

    @Override
    protected final void buildStructure(Body body,
        Map<IHandle, Body> newElements) throws CoreException
    {
        // The openable parent builds the whole structure
        throw new AssertionError("This method should not be called"); //$NON-NLS-1$
    }

    @Override
    protected void toStringName(StringBuilder builder)
    {
        super.toStringName(builder);
        if (occurrenceCount > 1)
        {
            builder.append('#');
            builder.append(occurrenceCount);
        }
    }
}
