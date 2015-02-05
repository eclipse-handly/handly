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
import org.eclipse.handly.internal.examples.javamodel.JavaModelManager;

/**
 * Facade to the Java model.
 */
public class JavaModelCore
{
    /**
     * Returns the Java model element.
     *
     * @return the Java model element (never <code>null</code>)
     */
    public static IJavaModel getJavaModel()
    {
        return JavaModelManager.INSTANCE.getJavaModel();
    }

    /**
     * Returns the Java project corresponding to the given project.
     * <p>
     * Note that no check is done at this time on the existence 
     * or the nature of this project.
     * </p>
     *
     * @param project the given project (maybe <code>null</code>)
     * @return the Java project corresponding to the given project, 
     *  or <code>null</code> if the given project is <code>null</code>
     */
    public static IJavaProject create(IProject project)
    {
        if (project == null)
            return null;
        return getJavaModel().getJavaProject(project.getName());
    }

    private JavaModelCore()
    {
    }
}
