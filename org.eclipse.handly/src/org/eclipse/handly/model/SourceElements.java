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
package org.eclipse.handly.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.internal.Activator;
import org.eclipse.handly.model.impl.SimpleSourceElementInfo;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.snapshot.StaleSnapshotException;

/**
 * Source element utilities.
 */
public class SourceElements
{
    /**
     * A 'null object' indicating that no info is available for source element.
     * <p>
     * The instance methods return either <code>null</code> (if allowed by
     * method contract) or an appropriate 'null object' (such as an empty array).
     * </p>
     */
    public static final ISourceElementInfo NO_INFO =
        new SimpleSourceElementInfo();

    /**
     * Returns source info for the given element, or {@link #NO_INFO}
     * if source info is not available.
     *
     * @param element a source element (not <code>null</code>)
     * @return source element info (never <code>null</code>)
     */
    public static ISourceElementInfo getSourceElementInfo(
        ISourceElement element)
    {
        try
        {
            return element.getSourceElementInfo();
        }
        catch (CoreException e)
        {
            if (!element.exists())
                ; // this is considered normal
            else
                Activator.log(e.getStatus());
        }
        return NO_INFO;
    }

    /**
     * Returns the smallest element within the given element that includes
     * the given position, or <code>null</code> if the given position is not
     * within the source range of the given element, or if the given element
     * does not exist or an exception occurs while accessing its corresponding
     * resource, or if snapshot inconsistency is detected. If no finer grained
     * element is found at the position, the given element itself is returned.
     *
     * @param element a source element (not <code>null</code>)
     * @param position a source position (0-based)
     * @param base a snapshot on which the given position is based,
     *  or <code>null</code> if the snapshot is unknown or doesn't matter
     * @return the innermost element enclosing the given source position,
     *  or <code>null</code> if none (including the given element itself)
     */
    public static ISourceElement getElementAt(ISourceElement element,
        int position, ISnapshot base)
    {
        try
        {
            return element.getElementAt(position, base);
        }
        catch (CoreException e)
        {
            if (!element.exists())
                ; // this is considered normal
            else
                Activator.log(e.getStatus());
        }
        catch (StaleSnapshotException e)
        {
            // ignore
        }
        return null;
    }

    /**
     * Returns the source file that contains the given element,
     * or <code>null</code> if the given element is not contained in a
     * source file. Returns the element itself if it is a source file.
     *
     * @param element a source element (not <code>null</code>)
     * @return the source file containing the given element,
     *  or <code>null</code> if none
     */
    public static ISourceFile getSourceFile(ISourceElement element)
    {
        if (element instanceof ISourceFile)
            return (ISourceFile)element;
        else
            return element.getAncestor(ISourceFile.class);
    }

    /**
     * Ensures that, if the given element is contained in a source file,
     * the source file is reconciled. Note that the call may result in
     * change of existence status for the given element: if the element
     * did not exist before, it may be brought into existence; conversely,
     * if the element existed, it may cease to exist.
     *
     * @param element a source element (not <code>null</code>)
     * @return <code>true</code> if the call completed successfully,
     *  <code>false</code> in case of failure
     */
    public static boolean ensureReconciled(ISourceElement element)
    {
        ISourceFile sourceFile = getSourceFile(element);
        if (sourceFile != null)
        {
            try
            {
                sourceFile.reconcile(false, null);
            }
            catch (CoreException e)
            {
                Activator.log(e.getStatus());
                return false;
            }
        }
        return true;
    }

    private SourceElements()
    {
    }
}
