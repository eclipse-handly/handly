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
 * Common protocol for Java elements that can be members of types.
 * <p>
 * The children are listed in the order in which they appear in the source.
 * </p>
 */
public interface IMember
    extends IJavaElement, ISourceConstruct
{
    /**
     * Flags property.
     * @see #getFlags()
     */
    Property<Integer> FLAGS = new Property<Integer>("flags"); //$NON-NLS-1$
    
    /**
     * Returns the type in which this member is declared, or <code>null</code>
     * if this member is not declared in a type (for example, a top-level type).
     * This is a handle-only method.
     *
     * @return the type in which this member is declared, or <code>null</code>
     *  if this member is not declared in a type (for example, a top-level type)
     */
    IType getDeclaringType();

    /**
     * Returns the modifier flags for this member. The flags can be examined
     * using class <code>Flags</code>.
     * <p>
     * Note that only flags as indicated in the source are returned.
     * Thus if an interface defines a method <code>void myMethod();</code>
     * the flags don't include the 'public' flag.
     * </p>
     *
     * @return the modifier flags for this member
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     * @see org.eclipse.jdt.core.Flags
     */
    int getFlags() throws CoreException;
}
