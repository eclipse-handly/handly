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
package org.eclipse.handly.model;

import org.eclipse.handly.snapshot.ISnapshot;

/**
 * Represents the topmost source element such as source file.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @see ISourceFile
 */
public interface IModule
    extends ISourceElement
{
    /**
     * Returns the smallest element within this module that includes
     * the given source position, or <code>null</code> if the given position
     * is not within the source range of this module, or if the module does not
     * exist or an exception occurs while accessing its corresponding resource,
     * or if snapshot inconsistency is detected. If no finer grained element
     * is found at the position, the module itself is returned.
     *
     * @param position a source position inside the module (0-based)
     * @param base a snapshot on which the given position is based,
     *  or <code>null</code> if the snapshot is unknown or doesn't matter
     * @return the innermost element enclosing the given source position,
     *  or <code>null</code> if none (including the module itself).
     */
    ISourceElement getElementAt(int position, ISnapshot base);
}
