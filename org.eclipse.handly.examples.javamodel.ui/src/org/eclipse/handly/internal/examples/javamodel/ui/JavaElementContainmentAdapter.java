/*******************************************************************************
 * Copyright (c) 2015 1C LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *     Ondrej Ilcik (Codasip) - Java model adaption
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.handly.examples.javamodel.IJavaElement;
import org.eclipse.handly.examples.javamodel.JavaModelCore;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.ui.workingset.AbstractContainmentAdapter;

/**
 * Containment adapter for Java elements.
 */
public class JavaElementContainmentAdapter
    extends AbstractContainmentAdapter
{
    @Override
    protected IHandle getElementFor(IResource resource)
    {
        IJavaElement element = JavaModelCore.create(resource);
        if (element != null && element.exists())
            return element;
        return null;
    }
}
