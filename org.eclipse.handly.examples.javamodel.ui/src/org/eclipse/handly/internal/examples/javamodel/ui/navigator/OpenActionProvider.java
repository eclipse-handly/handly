/*******************************************************************************
 * Copyright (c) 2014, 2015 1C LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *     Ondrej Ilcik (Codasip) - adapted for Java model
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel.ui.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.handly.examples.javamodel.ICompilationUnit;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;

/**
 * Provides 'Open' action and 'Open with' submenu for the Java Navigator.
 * <p>
 * Adapted from <code>org.eclipse.handly.internal.examples.basic.ui.navigator.OpenActionProvider</code>.
 * </p>
 */
public class OpenActionProvider
    extends CommonActionProvider
{
    private OpenAction openAction;

    @Override
    public void init(ICommonActionExtensionSite actionSite)
    {
        super.init(actionSite);
        openAction = new OpenAction(getPage());
    }

    @Override
    public void fillContextMenu(IMenuManager menu)
    {
        if (openAction == null)
            return;

        IStructuredSelection selection =
            (IStructuredSelection)getContext().getSelection();
        openAction.selectionChanged(selection);
        if (openAction.isEnabled())
        {
            menu.insertAfter(ICommonMenuConstants.GROUP_OPEN, openAction);
        }

        addOpenWithMenu(menu);
    }

    @Override
    public void fillActionBars(IActionBars actionBars)
    {
        if (openAction == null)
            return;

        IStructuredSelection selection =
            (IStructuredSelection)getContext().getSelection();
        openAction.selectionChanged(selection);
        if (openAction.isEnabled())
        {
            actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN,
                openAction);
        }
    }

    private void addOpenWithMenu(IMenuManager menu)
    {
        IStructuredSelection selection =
            (IStructuredSelection)getContext().getSelection();
        if (selection == null || selection.size() != 1)
            return;

        Object element = selection.getFirstElement();

        IFile file = null;
        if (element instanceof ICompilationUnit)
            file = ((ICompilationUnit)element).getFile();
        else if (element instanceof IFile)
            file = (IFile)element;
        if (file != null)
        {
            IMenuManager submenu =
                new MenuManager(Messages.OpenActionProvider_Label);
            submenu.add(new OpenWithMenu(getPage(), file));

            menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN_WITH, submenu);
        }
    }

    private IWorkbenchPage getPage()
    {
        return ((ICommonViewerWorkbenchSite)getActionSite().getViewSite()).getPage();
    }
}
