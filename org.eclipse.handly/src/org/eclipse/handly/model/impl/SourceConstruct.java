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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.context.IContext;

/**
 * Common superclass for source constructs.
 */
public abstract class SourceConstruct
    extends SourceElement
    implements ISourceConstructImpl
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
    public SourceConstruct(Element parent, String name)
    {
        super(parent, name);
        if (parent == null)
            throw new IllegalArgumentException();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof SourceConstruct))
            return false;
        return super.equals(obj)
            && occurrenceCount == ((SourceConstruct)obj).occurrenceCount;
    }

    /**
     * Returns the count used to uniquely identify this element in the case
     * that a duplicate named element exists. The occurrence count starts at 1
     * (thus the first occurrence is occurrence 1, not occurrence 0).
     *
     * @return the occurrence count for this element
     */
    public final int hOccurrenceCount()
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
     * @see #hOccurrenceCount()
     * @see StructureHelper
     */
    public final void hIncrementOccurrenceCount()
    {
        occurrenceCount++;
    }

    @Override
    public final IResource hResource()
    {
        return hParent().hResource();
    }

    @Override
    public final boolean hExists()
    {
        try
        {
            hBody();
            return true;
        }
        catch (CoreException e)
        {
            return false;
        }
    }

    @Override
    protected final boolean hIsOpenable()
    {
        // Source constructs are never openable
        return false;
    }

    @Override
    protected final void hValidateExistence(IContext context)
        throws CoreException
    {
        // The openable parent builds the whole structure and determines child existence
        throw new AssertionError("This method should not be called"); //$NON-NLS-1$
    }

    @Override
    protected final void hBuildStructure(IContext context,
        IProgressMonitor monitor) throws CoreException
    {
        // The openable parent builds the whole structure
        throw new AssertionError("This method should not be called"); //$NON-NLS-1$
    }

    @Override
    protected void hToStringName(StringBuilder builder, IContext context)
    {
        super.hToStringName(builder, context);
        if (occurrenceCount > 1)
        {
            builder.append('#');
            builder.append(occurrenceCount);
        }
    }
}
