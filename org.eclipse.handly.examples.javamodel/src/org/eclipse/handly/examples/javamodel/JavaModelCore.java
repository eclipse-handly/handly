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

    private JavaModelCore()
    {
    }
}
