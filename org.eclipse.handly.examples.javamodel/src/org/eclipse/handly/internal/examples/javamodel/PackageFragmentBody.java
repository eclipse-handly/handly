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

import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;

/**
 * <code>Body</code> extension for the package fragment.
 */
public class PackageFragmentBody
    extends Body
{
    static final Object[] NO_NON_JAVA_RESOURCES = new Object[0];

    private volatile Object[] nonJavaResources;

    public Object[] getNonJavaResources(PackageFragment packageFragment)
        throws CoreException
    {
        Object[] nonJavaResources = this.nonJavaResources;
        if (nonJavaResources == null)
        {
            nonJavaResources = computeNonJavaResources(packageFragment);
            this.nonJavaResources = nonJavaResources;
        }
        return nonJavaResources;
    }

    void setNonJavaResources(Object[] resources)
    {
        this.nonJavaResources = resources;
    }

    static Object[] computeNonJavaResources(PackageFragment packageFragment)
        throws CoreException
    {
        IResource[] members =
            ((IContainer)packageFragment.getResource()).members();
        if (members.length == 0)
            return NO_NON_JAVA_RESOURCES;
        ArrayList<Object> result = new ArrayList<Object>();
        JavaProject javaProject =
            packageFragment.getAncestor(JavaProject.class);
        String sourceLevel =
            javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
        String complianceLevel =
            javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);
        for (IResource member : members)
        {
            if (member.getType() == IResource.FILE)
            {
                if (JavaConventions.validateCompilationUnitName(
                    member.getName(), sourceLevel, complianceLevel).getSeverity() != IStatus.ERROR)
                {
                    continue; // ignore .java files
                }
            }
            else if (member.getType() == IResource.FOLDER)
            {
                if (JavaConventions.validateIdentifier(member.getName(),
                    sourceLevel, complianceLevel).getSeverity() != IStatus.ERROR)
                {
                    continue; // ignore valid packages
                }
            }
            result.add(member);
        }
        return result.toArray(new Object[result.size()]);
    }
}
