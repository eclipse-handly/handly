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
package org.eclipse.handly.examples.javamodel;

import org.eclipse.handly.model.ISourceConstruct;

/**
 * Represents a package declaration in Java compilation unit.
 */
public interface IPackageDeclaration
    extends IJavaElement, ISourceConstruct
{
    ICompilationUnit getParent();
}