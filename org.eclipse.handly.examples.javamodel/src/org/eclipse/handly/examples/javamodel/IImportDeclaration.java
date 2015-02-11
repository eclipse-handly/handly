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

import org.eclipse.handly.model.ISourceConstruct;

/**
 * Represents an import declaration in Java compilation unit.
 */
public interface IImportDeclaration
    extends IJavaElement, ISourceConstruct
{
    IImportContainer getParent();

    /**
     * Returns the name that has been imported.
     * For an on-demand import, this includes the trailing <code>".*"</code>.
     * For example, for the statement <code>"import java.util.*"</code>,
     * this returns <code>"java.util.*"</code>.
     * For the statement <code>"import java.util.Map"</code>,
     * this returns <code>"java.util.Map"</code>.
     *
     * @return the name that has been imported (never <code>null</code>)
     */
    public String getName();
}
