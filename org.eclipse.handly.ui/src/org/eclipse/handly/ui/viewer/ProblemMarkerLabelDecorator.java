/*******************************************************************************
 * Copyright (c) 2014 1C LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.ui.viewer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.handly.internal.ui.Activator;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.ISourceConstruct;
import org.eclipse.handly.util.TextRange;

/**
 * Decorates an element's image with error and warning overlays that represent 
 * the severity of markers attached to the element's underlying resource. 
 * To see a problem decoration for a marker, the marker needs to be a sub-type
 * of <code>IMarker.PROBLEM</code>.
 * <p>
 * Note that this decorator does not listen to problem marker changes. 
 * Hence, a viewer using this decorator requires a separate listener 
 * for updating elements on problem changes.
 * </p>
 */
public class ProblemMarkerLabelDecorator
    extends ProblemLabelDecorator
{
    @Override
    protected Severity computeProblemSeverity(Object element)
    {
        try
        {
            return computeProblemSeverityFromMarkers(element);
        }
        catch (CoreException e)
        {
            if (e.getStatus().getCode() == IResourceStatus.MARKER_NOT_FOUND)
                ; // this is ok
            else
                Activator.log(e.getStatus());
            return null;
        }
    }

    /**
     * Computes problem severity for the given element from markers attached 
     * to the element's underlying resource.
     *
     * @param element never <code>null</code>
     * @return problem severity, or <code>null</code> if there is no problem
     * @throws CoreException
     */
    protected Severity computeProblemSeverityFromMarkers(Object element)
        throws CoreException
    {
        IResource resource = null;
        if (element instanceof IResource)
            resource = (IResource)element;
        else if (element instanceof IHandle)
            resource = ((IHandle)element).getResource();
        else if (element instanceof IAdaptable)
        {
            IAdaptable adaptable = (IAdaptable)element;
            resource = (IResource)adaptable.getAdapter(IResource.class);
        }
        if (resource == null || !resource.isAccessible())
            return null;
        TextRange textRange = null;
        if (element instanceof ISourceConstruct)
        {
            ISourceConstruct sourceConstruct = (ISourceConstruct)element;
            if (!sourceConstruct.exists())
                return null;
            textRange = sourceConstruct.getSourceElementInfo().getFullRange();
            if (textRange == null)
                return null;
        }
        return findMaxProblemSeverity(resource, IResource.DEPTH_INFINITE,
            textRange);
    }

    /**
     * Returns the maximum severity across problem markers attached to 
     * the given resource, and, optionally, to its descendants. If 
     * <code>textRange</code> is not <code>null</code>, only markers 
     * strictly covered by the given text range are considered. 
     * Returns <code>null</code> if there are no matching markers.  
     *
     * @param resource not <code>null</code>
     * @param depth how far to recurse (see <code>IResource.DEPTH_* </code>)
     * @param textRange the text range to further constrain the marker set, 
     *  or <code>null</code>. Makes sense only if the given resource is 
     *  a text file
     * @return the maximum problem severity, or <code>null</code>
     * @throws CoreException
     */
    protected static Severity findMaxProblemSeverity(IResource resource,
        int depth, TextRange textRange) throws CoreException
    {
        int severity = 0;
        if (textRange == null)
        {
            severity =
                resource.findMaxProblemSeverity(IMarker.PROBLEM, true, depth);
        }
        else
        {
            IMarker[] markers =
                resource.findMarkers(IMarker.PROBLEM, true, depth);
            if (markers != null)
            {
                for (int i = 0; i < markers.length
                    && severity != IMarker.SEVERITY_ERROR; i++)
                {
                    IMarker marker = markers[i];
                    if (isMarkerInRange(marker, textRange))
                    {
                        int val = marker.getAttribute(IMarker.SEVERITY, -1);
                        if (val == IMarker.SEVERITY_WARNING
                            || val == IMarker.SEVERITY_ERROR)
                        {
                            severity = val;
                        }
                    }
                }
            }
        }
        if (severity == IMarker.SEVERITY_ERROR)
            return Severity.ERROR;
        else if (severity == IMarker.SEVERITY_WARNING)
            return Severity.WARNING;
        return null;
    }

    private static boolean isMarkerInRange(IMarker marker, TextRange textRange)
    {
        int position = marker.getAttribute(IMarker.CHAR_START, -1);
        return textRange.strictlyCovers(position);
    }
}
