/*******************************************************************************
 * Copyright (c) 2014, 2018 1C-Soft LLC and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *     Mike Begletsov <begletsov.mihail@gmail.com> - Widget is disposed for outline tree - https://bugs.eclipse.org/473296
 *******************************************************************************/
package org.eclipse.handly.ui.outline;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * A base class for outline context menu contributions.
 * <p>
 * Contributes an empty menu which has no id and is not eligible for extension.
 * Subclasses may override corresponding methods to configure the menu
 * as necessary.
 * </p>
 */
public class OutlineContextMenuContribution
    extends OutlineContribution
{
    private Menu menu;
    private Menu oldMenu;

    /**
     * {@inheritDoc}
     * <p>
     * <code>OutlineContextMenuContribution</code> extends this method to create
     * a context menu for the outline page's tree viewer. The menu will have id
     * as computed by {@link #getContextMenuId()} and will invoke {@link
     * #contextMenuAboutToShow(IMenuManager)} when it is about to be shown.
     * If an {@link #getContextMenuExtensionId() extension id} is provided,
     * the menu will be registered with the workbench for extension.
     * </p>
     */
    @Override
    public void init(ICommonOutlinePage outlinePage)
    {
        super.init(outlinePage);

        MenuManager manager = new MenuManager(null, getContextMenuId());
        manager.setRemoveAllWhenShown(true);
        manager.addMenuListener(new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                contextMenuAboutToShow(manager);
            }
        });
        Tree tree = outlinePage.getTreeViewer().getTree();
        menu = manager.createContextMenu(tree);
        oldMenu = tree.getMenu();
        tree.setMenu(menu);

        String menuId = getContextMenuExtensionId();
        if (menuId != null)
        {
            outlinePage.getSite().registerContextMenu(menuId, manager,
                outlinePage.getTreeViewer());
        }
    }

    @Override
    public void dispose()
    {
        if (menu != null)
        {
            Tree tree = getOutlinePage().getTreeViewer().getTree();
            if (!tree.isDisposed())
                tree.setMenu(oldMenu);
            menu.dispose();
            menu = null;
            oldMenu = null;
        }
        super.dispose();
    }

    /**
     * Returns the id of the context menu manager, or <code>null</code>
     * if the menu has no id.
     * <p>
     * Default implementation returns <code>null</code>.
     * Subclasses may override this method.
     * </p>
     */
    protected String getContextMenuId()
    {
        return null;
    }

    /**
     * Returns the unique id to use for registration the context menu with
     * the workbench, or <code>null</code> if the menu is not eligible
     * for extension.
     * <p>
     * Default implementation returns <code>null</code>.
     * Subclasses may override this method.
     * </p>
     */
    protected String getContextMenuExtensionId()
    {
        return null;
    }

    /**
     * Notifies that the context menu of the outline page is about to be
     * shown by the given menu manager.
     * <p>
     * Default implementation contributes {@link IWorkbenchActionConstants#MB_ADDITIONS}
     * group if the context menu was registered with the workbench for extension.
     * Subclasses may extend this method and contribute other items.
     * </p>
     *
     * @param manager the menu manager (never <code>null</code>)
     */
    protected void contextMenuAboutToShow(IMenuManager manager)
    {
        if (getContextMenuExtensionId() != null)
            manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }
}
