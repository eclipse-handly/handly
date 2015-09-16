/*******************************************************************************
 * Copyright (c) 2014, 2015 1C LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *     Ondrej Ilcik (Codasip) - adaptation (adapted from 
 *        org.eclipse.handly.internal.examples.basic.ui.navigator.FooLinkHelper)
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel.ui.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.handly.examples.javamodel.ICompilationUnit;
import org.eclipse.handly.examples.javamodel.IJavaElement;
import org.eclipse.handly.examples.javamodel.JavaModelCore;
import org.eclipse.handly.internal.examples.javamodel.ui.SourceElementUtil;
import org.eclipse.handly.model.ISourceElement;
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
 * Link helper for the Java Package Explorer.
 */
public class JavaLinkHelper
    implements ILinkHelper
{
    @Override
    public IStructuredSelection findSelection(IEditorInput editorInput)
    {
        if (editorInput instanceof IFileEditorInput)
        {
            IFile file = ((IFileEditorInput)editorInput).getFile();
            ICompilationUnit javaFile =
                (ICompilationUnit)JavaModelCore.create(file);
            if (javaFile != null)
            {
                IViewPart navigatorView =
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(
                        JavaNavigator.ID);
                if (navigatorView != null)
                {
                    IStructuredSelection currentSelection =
                        (IStructuredSelection)navigatorView.getSite().getSelectionProvider().getSelection();
                    if (currentSelection != null
                        && currentSelection.size() == 1)
                    {
                        Object element = currentSelection.getFirstElement();
                        if (element instanceof IJavaElement)
                        {
                            if (javaFile.equals(
                                ((IJavaElement)element).getAncestor(
                                    ICompilationUnit.class)))
                                return currentSelection;
                        }
                    }
                }
                return new StructuredSelection(javaFile);
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
        if (element instanceof IJavaElement)
            resource = ((IJavaElement)element).getResource();
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
            SourceElementUtil.revealInTextEditor((ITextEditor)editor,
                (ISourceElement)element);
        }
    }
}
