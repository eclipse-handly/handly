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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.handly.examples.javamodel.ICompilationUnit;
import org.eclipse.handly.examples.javamodel.IJavaElement;
import org.eclipse.handly.examples.javamodel.JavaModelCore;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

/**
 * This class is used to save and recreate an IJavaElement object.
 * As such, it implements the IPersistableElement interface for storage
 * and the IElementFactory interface for recreation.
 *
 * @see IMemento
 * @see IPersistableElement
 * @see IElementFactory
 */
public class PersistableJavaElementFactory
    implements IElementFactory, IPersistableElement
{
    private static final String KEY = "elementID"; //$NON-NLS-1$
    private static final String FACTORY_ID =
        "org.eclipse.handly.examples.javamodel.ui.PersistableJavaElementFactory"; //$NON-NLS-1$

    private IJavaElement element;

    public PersistableJavaElementFactory()
    {
    }

    public PersistableJavaElementFactory(IJavaElement element)
    {
        this.element = element;
    }

    @Override
    public IAdaptable createElement(IMemento memento)
    {
        String value = memento.getString(KEY);
        if (value != null)
        {
            IPath path = Path.fromPortableString(value);
            IResource resource =
                ResourcesPlugin.getWorkspace().getRoot().findMember(path);
            if (resource != null)
                return JavaModelCore.create(resource);
        }
        return null;
    }

    @Override
    public void saveState(IMemento memento)
    {
        if (element.getAncestor(ICompilationUnit.class) != null)
            return; // element inside Java file
        memento.putString(KEY, element.getPath().toPortableString());
    }

    @Override
    public String getFactoryId()
    {
        return FACTORY_ID;
    }
}
