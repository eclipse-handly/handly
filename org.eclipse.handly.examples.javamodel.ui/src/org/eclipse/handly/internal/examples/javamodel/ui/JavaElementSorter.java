/*******************************************************************************
 * Copyright (c) 2015 Codasip Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ondrej Ilcik (Codasip) - initial implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel.ui;

import java.text.Collator;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

/**
 * Java element sorter. Uses {@link JavaElementComparator} internally.
 */
public class JavaElementSorter
    extends ViewerSorter
{
    private final JavaElementComparator comparator = new JavaElementComparator();

    public JavaElementSorter()
    {
    }

    public JavaElementSorter(Collator collator)
    {
        super(collator);
    }

    @Override
    public int category(Object element)
    {
        return comparator.category(element);
    }

    @Override
    public int compare(Viewer viewer, Object e1, Object e2)
    {
        return comparator.compare(viewer, e1, e2);
    }
}
