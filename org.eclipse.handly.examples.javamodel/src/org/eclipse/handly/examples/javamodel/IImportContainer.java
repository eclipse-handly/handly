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
import org.eclipse.handly.model.ISourceConstruct;

/**
 * Represents an import container. It is a child of a Java compilation unit
 * that contains all (and only) import declarations. If a compilation unit
 * has no import declarations, no import container will be present.
 */
public interface IImportContainer
    extends IJavaElement, ISourceConstruct
{
    ICompilationUnit getParent();

    /**
     * Returns the import declaration in this import container
     * with the given name.
     * <p>
     * This is a handle-only method. The import declaration may or may not exist.
     * </p>
     *
     * @param name the given name (not <code>null</code>)
     * @return the import declaration in this import container
     *  with the given name (never <code>null</code>)
     */
    IImportDeclaration getImport(String name);

    /**
     * Returns the import declarations in this import container in the order
     * in which they appear in the source.
     *
     * @return the import declarations in this import container
     *  (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    IImportDeclaration[] getImports() throws CoreException;
}
