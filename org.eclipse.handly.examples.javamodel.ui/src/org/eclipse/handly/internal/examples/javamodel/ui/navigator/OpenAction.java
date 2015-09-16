/*******************************************************************************
 * Copyright (c) 2014, 2015 1C LLC and others.
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

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.ide.IDE;

/**
 * Opens an editor on an applicable element.
 * <p>
 * Adapted from <code>org.eclipse.handly.internal.examples.basic.ui.navigator.OpenAction</code>.
 * </p> 
 */
class OpenAction
    extends BaseSelectionListenerAction
{
    private IWorkbenchPage page;

    /**
     * Constructs an open action for the given workbench page. 
     * 
     * @param page the workbench page to open the editor in 
     *  (not <code>null</code>)
     */
    public OpenAction(IWorkbenchPage page)
    {
        super(Messages.OpenAction_Label);
        if (page == null)
            throw new IllegalArgumentException();
        this.page = page;
    }

    @Override
    public void run()
    {
        IStructuredSelection selection = getStructuredSelection();
        if (selection == null)
            return;
        Iterator<?> it = selection.iterator();
        while (it.hasNext())
        {
            Object element = it.next();
            IFile file = JavaLinkHelper.toFile(element);
            if (file != null)
            {
                IEditorPart editor = null;
                try
                {
                    editor = IDE.openEditor(page, file,
                        OpenStrategy.activateOnOpen());
                }
                catch (PartInitException e)
                {
                }
                if (editor != null)
                {
                    JavaLinkHelper.revealInEditor(editor, element);
                }
            }
        }
    }

    @Override
    protected boolean updateSelection(IStructuredSelection selection)
    {
        if (selection == null || selection.isEmpty())
            return false;
        Iterator<?> it = selection.iterator();
        while (it.hasNext())
        {
            Object element = it.next();
            if (JavaLinkHelper.toFile(element) == null)
                return false;
        }
        return true;
    }
}
