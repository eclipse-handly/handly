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
package org.eclipse.handly.examples.javamodel;

import org.eclipse.core.runtime.CoreException;

/**
 * A package fragment root contains a set of package fragments.
 * The children are of type <code>IPackageFragment</code>,
 * and are in no particular order.
 */
public interface IPackageFragmentRoot
    extends IJavaElement
{
    IJavaProject getParent();
    
    /**
     * Returns the package fragment with the given package name.
     * An empty string indicates the default package.
     * <p>
     * This is a handle-only method. The package fragment may or may not exist.
     * </p>
     *
     * @param packageName the given package name (not <code>null</code>)
     * @return the package fragment with the given package name
     *  (never <code>null</code>)
     */
    IPackageFragment getPackageFragment(String packageName);

    /**
     * Returns the package fragments contained in this package fragment root.
     * This is equivalent to <code>getChildren()</code>.
     *
     * @return the package fragments contained in this package fragment root
     *  (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    IPackageFragment[] getPackageFragments() throws CoreException;

    /**
     * Returns the non-Java resources directly contained in this
     * package fragment root.
     *
     * @return the non-Java resources directly contained
     *  in this package fragment root (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    Object[] getNonJavaResources() throws CoreException;
}
