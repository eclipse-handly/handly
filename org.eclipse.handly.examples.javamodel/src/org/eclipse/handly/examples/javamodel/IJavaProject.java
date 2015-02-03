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

import org.eclipse.core.resources.IProject;

/**
 * A Java project represents a view of a project resource in terms of Java
 * elements. Each Java project has a classpath, defining which folders
 * contain source code, etc.
 * <p>
 * The children of a Java project are the package fragment roots that are
 * defined by the classpath. They appear in the order they are defined
 * by the classpath.
 * </p>
 */
public interface IJavaProject
    extends IJavaElement
{
    /**
     * The identifier for the Java nature.
     */
    String NATURE_ID = "org.eclipse.jdt.core.javanature"; //$NON-NLS-1$

    /**
     * Returns the <code>IProject</code> on which this <code>IJavaProject</code>
     * was created. This is handle-only method.
     *
     * @return the <code>IProject</code> on which this <code>IJavaProject</code>
     *  was created (never <code>null</code>)
     */
    IProject getProject();
}
