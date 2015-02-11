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
import org.eclipse.handly.model.ISourceFile;

/**
 * Represents an entire Java compilation unit (Java source file).
 * <p>
 * The children are of type {@link IPackageDeclaration},
 * {@link IImportContainer}, and {@link IType}, and appear
 * in the order in which they are declared in the source.
 * </p>
 */
public interface ICompilationUnit
    extends IJavaElement, ISourceFile
{
    IPackageFragment getParent();

    /**
     * Returns the import declaration in this compilation unit
     * with the given name. This is a convenience method - imports
     * can also be accessed from a compilation unit's import container.
     * <p>
     * This is a handle-only method. The import declaration may or may not exist.
     * </p>
     *
     * @param name the given name (not <code>null</code>)
     * @return the import declaration in this compilation unit
     *  with the given name (never <code>null</code>)
     */
    IImportDeclaration getImport(String name);

    /**
     * Returns the import container for this compilation unit.
     * <p>
     * This is a handle-only method. The import container may or may not exist.
     * </p>

     * @return the import container for this compilation unit
     *  (never <code>null</code>)
     */
    IImportContainer getImportContainer();

    /**
     * Returns the import declarations in this compilation unit in the order
     * in which they appear in the source. This is a convenience method - 
     * imports can also be accessed from a compilation unit's import container.
     *
     * @return the import declarations in this compilation unit
     *  (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    IImportDeclaration[] getImports() throws CoreException;

    /**
     * Returns the package declaration in this compilation unit
     * with the given package name (there normally is at most one
     * package declaration).
     * <p>
     * This is a handle-only method. The package declaration may or may not exist.
     * </p>
     *
     * @param name the given package name (not <code>null</code>)
     * @return the package declaration in this compilation unit
     *  with the given package name (never <code>null</code>)
     */
    IPackageDeclaration getPackageDeclaration(String name);

    /**
     * Returns the package declarations in this compilation unit
     * in the order in which they appear in the source.
     * There normally is at most one package declaration.
     *
     * @return the package declarations in this compilation unit - normally one
     *  (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    IPackageDeclaration[] getPackageDeclarations() throws CoreException;

    /**
     * Returns the top-level type declared in this compilation unit
     * with the given simple name.
     * <p>
     * This is a handle-only method. The type may or may not exist.
     * </p>
     *
     * @param name the simple type name (not <code>null</code>)
     * @return the top-level type declared in this compilation unit
     *  with the given simple name (never <code>null</code>)
     */
    IType getType(String name);

    /**
     * Returns the top-level types declared in this compilation unit
     * in the order in which they appear in the source.
     *
     * @return the top-level types declared in this compilation unit
     *  (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    IType[] getTypes() throws CoreException;
}
