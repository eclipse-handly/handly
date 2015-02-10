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
package org.eclipse.handly.internal.examples.javamodel;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.model.impl.Body;

/**
 * <code>Body</code> extension for the package fragment root.
 */
public class PackageFragmentRootBody
    extends Body
{
    private volatile Object[] nonJavaResources;

    public Object[] getNonJavaResources(PackageFragmentRoot root)
        throws CoreException
    {
        Object[] nonJavaResources = this.nonJavaResources;
        if (nonJavaResources == null)
        {
            nonJavaResources = computeNonJavaResources(root);
            this.nonJavaResources = nonJavaResources;
        }
        return nonJavaResources;
    }

    void setNonJavaResources(Object[] resources)
    {
        this.nonJavaResources = resources;
    }

    private Object[] computeNonJavaResources(PackageFragmentRoot root)
        throws CoreException
    {
        return PackageFragmentBody.computeNonJavaResources(root.getPackageFragment("")); //$NON-NLS-1$
    }
}
