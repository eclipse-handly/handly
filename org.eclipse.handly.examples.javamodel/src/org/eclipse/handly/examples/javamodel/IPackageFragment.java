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
 * A package fragment is a portion of the workspace corresponding to
 * an entire package, or to a portion thereof. The distinction between
 * a package fragment and a package is that a package with some name
 * is the union of all package fragments in the class path
 * which have the same name.
 */
public interface IPackageFragment
    extends IJavaElement
{
    IPackageFragmentRoot getParent();
    
    /**
     * Returns the compilation unit with the specified name in this package
     * (for example, <code>"Object.java"</code>). The name must end with ".java".
     * <p>
     * This is a handle-only method. The compilation unit may or may not exist.
     * </p>
     *
     * @param name the given name (not <code>null</code>)
     * @return the compilation unit with the specified name in this package
     *  (never <code>null</code>)
     */
    ICompilationUnit getCompilationUnit(String name);

    /**
     * Returns all of the compilation units in this package fragment.
     *
     * @return all of the compilation units in this package fragment
     *  (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    ICompilationUnit[] getCompilationUnits() throws CoreException;

    /**
     * Returns the non-Java resources directly contained in this
     * package fragment.
     *
     * @return the non-Java resources directly contained
     *  in this package fragment (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    Object[] getNonJavaResources() throws CoreException;

    /**
     * Returns whether this package fragment is a default package.
     * This is a handle-only method.
     *
     * @return <code>true</code> if this package fragment is a default package
     */
    boolean isDefaultPackage();
}
