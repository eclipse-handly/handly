/*******************************************************************************
 * Copyright (c) 2015 Codasip Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ondrej Ilcik (Codasip) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.jmodel.ui.workingset;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.handly.examples.jmodel.IJavaElement;
import org.eclipse.handly.examples.jmodel.JavaModelCore;
import org.eclipse.handly.ui.workingset.AbstractWorkingSetElementAdapter;

/**
 * Element adapter for Java working set.
 */
public class JavaWorkingSetElementAdapter
    extends AbstractWorkingSetElementAdapter
{
    @Override
    protected boolean isModelElement(IAdaptable element)
    {
        return element instanceof IJavaElement;
    }

    @Override
    protected IAdaptable adaptFromResource(IResource resource)
    {
        IJavaElement element = JavaModelCore.create(resource);
        if (element != null && element.exists())
            return element;
        return resource;
    }
}