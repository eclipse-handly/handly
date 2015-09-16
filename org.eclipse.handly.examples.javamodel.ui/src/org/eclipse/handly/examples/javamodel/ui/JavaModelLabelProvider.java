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
package org.eclipse.handly.examples.javamodel.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.handly.examples.javamodel.IJavaElement;
import org.eclipse.handly.internal.examples.javamodel.ui.Activator;
import org.eclipse.handly.internal.examples.javamodel.ui.JavaElementImageProvider;
import org.eclipse.handly.internal.examples.javamodel.ui.JavaElementLabelComposer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Common label provider for Java model.
 */
public class JavaModelLabelProvider
    extends LabelProvider
    implements IStyledLabelProvider
{
    private JavaElementImageProvider imageProvider =
        new JavaElementImageProvider();

    @Override
    public StyledString getStyledText(Object element)
    {
        if (element instanceof IJavaElement)
        {
            StyledString ss = new StyledString();
            try
            {
                JavaElementLabelComposer.create(ss).appendElementLabel(
                    (IJavaElement)element);
                return ss;
            }
            catch (CoreException e)
            {
                Activator.log(e.getStatus());
            }
        }

        return new StyledString(getText(element));
    }

    @Override
    public String getText(Object element)
    {
        if (element instanceof IJavaElement)
        {
            StringBuilder sb = new StringBuilder();
            try
            {
                JavaElementLabelComposer.create(sb).appendElementLabel(
                    (IJavaElement)element);
                return sb.toString();
            }
            catch (CoreException e)
            {
                Activator.log(e.getStatus());
            }
        }
        else if (element instanceof IAdaptable)
        {
            IWorkbenchAdapter wbadapter =
                (IWorkbenchAdapter)((IAdaptable)element).getAdapter(
                    IWorkbenchAdapter.class);
            if (wbadapter != null)
            {
                return wbadapter.getLabel(element);
            }
        }

        return super.getText(element);
    };

    @Override
    public Image getImage(Object element)
    {
        try
        {
            return imageProvider.getImage(element);
        }
        catch (CoreException e)
        {
            Activator.log(e.getStatus());
            return null;
        }
    }
}
