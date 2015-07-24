/*******************************************************************************
 * Copyright (c) 2014 1C LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.basic.ui.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.handly.examples.basic.ui.model.FooModelCore;
import org.eclipse.handly.examples.basic.ui.model.IFooElement;
import org.eclipse.handly.examples.basic.ui.model.IFooFile;
import org.eclipse.handly.model.ISourceElement;
import org.eclipse.handly.model.SourceElements;
import org.eclipse.handly.util.TextRange;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.ILinkHelper;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Link helper for the Foo Navigator.
 */
public class FooLinkHelper
    implements ILinkHelper
{
    @Override
    public IStructuredSelection findSelection(IEditorInput editorInput)
    {
        if (editorInput instanceof IFileEditorInput)
        {
            IFile file = ((IFileEditorInput)editorInput).getFile();
            IFooFile fooFile = FooModelCore.create(file);
            if (fooFile != null)
            {
                IViewPart navigatorView =
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(
                        FooNavigator.ID);
                if (navigatorView != null)
                {
                    IStructuredSelection currentSelection =
                        (IStructuredSelection)navigatorView.getSite().getSelectionProvider().getSelection();
                    if (currentSelection != null
                        && currentSelection.size() == 1)
                    {
                        Object element = currentSelection.getFirstElement();
                        if (element instanceof IFooElement)
                        {
                            if (fooFile.equals(
                                ((IFooElement)element).getAncestor(
                                    IFooFile.class)))
                                return currentSelection;
                        }
                    }
                }
                return new StructuredSelection(fooFile);
            }
            else
            {
                return new StructuredSelection(file);
            }
        }
        return null;
    }

    @Override
    public void activateEditor(IWorkbenchPage page,
        IStructuredSelection selection)
    {
        if (selection == null || selection.size() != 1)
            return;
        Object element = selection.getFirstElement();
        IFile file = toFile(element);
        if (file != null)
        {
            IEditorPart editor = page.findEditor(new FileEditorInput(file));
            if (editor != null)
            {
                page.bringToTop(editor);
                revealInEditor(editor, element);
            }
        }
    }

    static IFile toFile(Object element)
    {
        IResource resource = null;
        if (element instanceof IFooElement)
            resource = ((IFooElement)element).getResource();
        else if (element instanceof IResource)
            resource = (IResource)element;
        if (resource instanceof IFile)
            return (IFile)resource;
        return null;
    }

    static void revealInEditor(IEditorPart editor, Object element)
    {
        if (editor instanceof ITextEditor && element instanceof ISourceElement)
        {
            TextRange identifyingRange = SourceElements.getSourceElementInfo(
                (ISourceElement)element).getIdentifyingRange();
            if (identifyingRange == null)
                return;
            ((ITextEditor)editor).selectAndReveal(identifyingRange.getOffset(),
                identifyingRange.getLength());
        }
    }
}
