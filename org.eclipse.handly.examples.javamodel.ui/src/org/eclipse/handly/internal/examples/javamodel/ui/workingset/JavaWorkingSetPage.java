/*******************************************************************************
 * Copyright (c) 2015 1C LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *     Ondrej Ilcik (Codasip) - Java model adaptation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel.ui.workingset;

import org.eclipse.handly.examples.javamodel.ICompilationUnit;
import org.eclipse.handly.examples.javamodel.JavaModelCore;
import org.eclipse.handly.examples.javamodel.ui.JavaModelContentProvider;
import org.eclipse.handly.examples.javamodel.ui.JavaModelLabelProvider;
import org.eclipse.handly.ui.workingset.AbstractWorkingSetPage;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;

/**
 * The Java working set page allows the user to create and edit a Java working set.
 */
public class JavaWorkingSetPage
    extends AbstractWorkingSetPage
{
    /**
     * Default constructor.
     */
    public JavaWorkingSetPage()
    {
        super("javaWorkingSetPage", "Java Working Set", null); //$NON-NLS-1$
    }

    @Override
    protected String getPageId()
    {
        return "org.eclipse.handly.examples.javamodel.ui.JavaWorkingSetPage"; //$NON-NLS-1$
    }

    @Override
    protected void configureTree(TreeViewer tree)
    {
        tree.setContentProvider(new JavaModelContentProvider()
        {
            @Override
            public Object[] getChildren(Object parentElement)
            {
                if (parentElement instanceof ICompilationUnit)
                    return NO_CHILDREN;
                return super.getChildren(parentElement);
            }
        });
        tree.setLabelProvider(new JavaModelLabelProvider());
        tree.setInput(JavaModelCore.getJavaModel());
    }

    @Override
    protected void configureTable(TableViewer table)
    {
        table.setLabelProvider(new JavaModelLabelProvider());
    }
}
