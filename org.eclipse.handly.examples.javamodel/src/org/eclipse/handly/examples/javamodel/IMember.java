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
 * Common protocol for Java elements that can be members of types.
 * <p>
 * The children are listed in the order in which they appear in the
 * containing file.
 * </p>
 */
public interface IMember
    extends IJavaElement, ISourceConstruct
{
    /**
     * Returns the type in which this member is declared, or <code>null</code>
     * if this member is not declared in a type (for example, a top-level type).
     * This is a handle-only method.
     *
     * @return the type in which this member is declared, or <code>null</code>
     *  if this member is not declared in a type (for example, a top-level type)
     */
    IType getDeclaringType();
}
