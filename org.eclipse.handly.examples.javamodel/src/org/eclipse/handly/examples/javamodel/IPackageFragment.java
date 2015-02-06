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
}
