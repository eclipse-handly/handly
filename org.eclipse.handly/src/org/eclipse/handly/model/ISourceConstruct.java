/*******************************************************************************
 * Copyright (c) 2014, 2015 1C-Soft LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.model;

/**
 * Represents an element in a source file or, more generally,
 * an element inside a "resource" that may have associated source
 * (an example of such "resource" might be a class file in a jar).
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ISourceConstruct
    extends ISourceElement
{
}
