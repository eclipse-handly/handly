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
 * Represents a field declared in a type.
 */
public interface IField
    extends IMember
{
    /**
     * Type property.
     * @see #getType()
     */
    Property<String> TYPE = new Property<String>("type"); //$NON-NLS-1$
    
    IType getParent();

    /**
     * Returns the type signature of this field. For enum constants,
     * this returns the signature of the declaring enum class.
     *
     * @return the type signature of this field (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     * @see org.eclipse.jdt.core.Signature
     */
    String getType() throws CoreException;

    /**
     * Returns whether this field represents an enum constant.
     *
     * @return <code>true</code> if this field represents an enum constant,
     *  <code>false</code> otherwise
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    boolean isEnumConstant() throws CoreException;
}
