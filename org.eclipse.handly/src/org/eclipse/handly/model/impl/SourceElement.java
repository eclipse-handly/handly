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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.model.ISourceElement;
import org.eclipse.handly.model.ISourceElementInfo;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.snapshot.StaleSnapshotException;
import org.eclipse.handly.util.TextRange;

/**
 * Common superclass of {@link ISourceElement} implementations.
 * 
 * @see SourceFile
 * @see SourceConstruct
 */
public abstract class SourceElement
    extends Handle
    implements ISourceElement
{
    /**
     * Constructs a handle for a source element with the given parent element
     * and the given name.
     * 
     * @param parent the parent of the element (not <code>null</code>)
     * @param name the name of the element, or <code>null</code> 
     *  if the element has no name
     */
    public SourceElement(Handle parent, String name)
    {
        super(parent, name);
        if (parent == null)
            throw new IllegalArgumentException();
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

    @Override
    public ISourceElementInfo getSourceElementInfo() throws CoreException
    {
        return (ISourceElementInfo)getBody();
    }

    /*
     * Returns the smallest element within the given element that includes
     * the given source position, or <code>null</code> if the given position
     * is not within the source range of the given element. If no finer grained
     * element is found at the position, the given element is returned.
     *
     * @param element a source element (not <code>null</code>)
     * @param position a source position (0-based)
     * @param base a snapshot on which the given position is based,
     *  or <code>null</code> if the snapshot is unknown or doesn't matter
     * @return the innermost element within the given element enclosing
     *  the given source position, or <code>null</code> if none (including
     *  the given element)
     * @throws CoreException if the given element does not exist or if an
     *  exception occurs while accessing its corresponding resource
     * @throws StaleSnapshotException if snapshot inconsistency is detected,
     *  i.e. the given element's current structure and properties are based on
     *  a different snapshot
     */
    private static ISourceElement getElementAt(ISourceElement element,
        int position, ISnapshot base) throws CoreException
    {
        ISourceElementInfo info = element.getSourceElementInfo();
        if (base != null && !base.isEqualTo(info.getSnapshot()))
        {
            throw new StaleSnapshotException();
        }
        TextRange textRange = info.getFullRange();
        if (textRange == null || !textRange.covers(position))
            return null; // not found
        ISourceElement[] children = info.getChildren();
        for (int i = children.length - 1; i >= 0; i--)
        {
            ISourceElement found =
                getElementAt(children[i], position, info.getSnapshot());
            if (found != null)
                return found;
        }
        return element;
    }
}
