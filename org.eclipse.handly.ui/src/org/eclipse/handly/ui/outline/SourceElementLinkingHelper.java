/*******************************************************************************
 * Copyright (c) 2014, 2015 1C-Soft LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.ui.outline;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.handly.internal.ui.SourceElementUtil;
import org.eclipse.handly.model.ISourceElement;
import org.eclipse.handly.util.TextRange;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Implements linking logic for outlines of <code>ISourceElement</code>.
 */
public class SourceElementLinkingHelper
    extends OutlineLinkingHelper
{
    private LinkToOutlineJob linkToOutlineJob = new LinkToOutlineJob();

    /**
     * Creates a new linking helper for the given outline page
     * that is based on <code>ISourceElement</code>.
     * 
     * @param outlinePage not <code>null</code>
     */
    public SourceElementLinkingHelper(ICommonOutlinePage outlinePage)
    {
        super(outlinePage);
    }

    @Override
    public void dispose()
    {
        super.dispose();
        cancelLinkToOutlineJob();
    }

    @Override
    protected final void linkToOutline(ISelection selection)
    {
        if (selection == null || selection.isEmpty())
            return;
        scheduleLinkToOutlineJob(selection);
    }

    @Override
    protected void linkToEditor(ISelection selection)
    {
        if (selection == null || selection.isEmpty())
            return;
        IEditorPart editor = getTargetEditor();
        if (editor instanceof ITextEditor)
            linkToEditor((ITextEditor)editor, (IStructuredSelection)selection);
        else
            editor.getSite().getSelectionProvider().setSelection(selection);
    }

    /**
     * Tells to link the given outline selection to the given text editor.
     * <p>
     * Default implementation selects and reveals the identifying range of 
     * the selection's first element in the text editor. Does nothing 
     * if the first element is not an {@link ISourceElement} or 
     * if the identifying range is not set.
     * </p>
     *
     * @param editor the text editor (never <code>null</code>)
     * @param selection the outline selection (never <code>null</code>,
     *  never empty)
     */
    protected void linkToEditor(ITextEditor editor,
        IStructuredSelection selection)
    {
        Object element = selection.getFirstElement();
        if (!(element instanceof ISourceElement))
            return;
        TextRange identifyingRange =
            SourceElementUtil.getIdentifyingRange((ISourceElement)element);
        if (identifyingRange == null)
            return;
        editor.selectAndReveal(identifyingRange.getOffset(),
            identifyingRange.getLength());
    }

    /**
     * Returns the outline selection corresponding to the given selection
     * in the editor.
     *
     * @param selection the selection in the editor
     *  (never <code>null</code>, never empty)
     * @return the outline selection corresponding to the given selection 
     *  in the editor, or <code>null</code>
     */
    protected IStructuredSelection getLinkedSelection(ISelection selection)
    {
        if (selection instanceof ITextSelection)
            return getLinkedSelection((ITextSelection)selection);
        if (selection instanceof IStructuredSelection)
            return (IStructuredSelection)selection;
        return null;
    }

    /**
     * Returns the outline selection corresponding to the given text selection 
     * in the editor.
     * <p>
     * Default implementation returns the selection consisting of the smallest 
     * {@link ISourceElement} that includes the offset of the given selection, 
     * or <code>null</code> if none.
     * </p>
     *
     * @param selection the text selection in the editor
     *  (never <code>null</code>, never empty)
     * @return the outline selection corresponding to the given selection 
     *  in the editor, or <code>null</code>
     */
    protected IStructuredSelection getLinkedSelection(ITextSelection selection)
    {
        Object input = getOutlinePage().getTreeViewer().getInput();
        if (!(input instanceof ISourceElement))
            return null;
        ISourceElement element =
            SourceElementUtil.getElementAt((ISourceElement)input,
                selection.getOffset());
        if (element == null)
            return null;
        return new StructuredSelection(element);
    }

    /**
     * Returns the editor the outline should be linked to.
     * <p>
     * Default implementation returns the editor that created the outline page 
     * or, if that editor is a multi-page editor, the currently selected 
     * editor page.
     * </p>
     *
     * @return the editor the outline should be linked to
     */
    protected IEditorPart getTargetEditor()
    {
        IEditorPart editor = getOutlinePage().getEditor();
        while (editor instanceof IPageChangeProvider)
        {
            Object page = ((IPageChangeProvider)editor).getSelectedPage();
            if (!(page instanceof IEditorPart))
                break;
            editor = (IEditorPart)page;
        }
        return editor;
    }

    private void cancelLinkToOutlineJob()
    {
        linkToOutlineJob.cancel();
        linkToOutlineJob.setSelection(null);
    }

    private void scheduleLinkToOutlineJob(ISelection selection)
    {
        linkToOutlineJob.cancel();
        linkToOutlineJob.setSelection(selection);
        linkToOutlineJob.schedule();
    }

    private class LinkToOutlineJob
        extends Job
    {
        private volatile ISelection selection;

        public LinkToOutlineJob()
        {
            super(""); //$NON-NLS-1$
            setSystem(true);
        }

        public void setSelection(ISelection selection)
        {
            this.selection = selection;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            final ISelection baseSelection = selection;
            if (baseSelection == null || baseSelection.isEmpty())
                return Status.OK_STATUS;
            final Object baseInput =
                getOutlinePage().getTreeViewer().getInput();
            if (!(baseInput instanceof ISourceElement))
                return Status.OK_STATUS;

            final IStructuredSelection linkedSelection =
                getLinkedSelection(baseSelection);

            if (linkedSelection == null)
                return Status.OK_STATUS;
            if (monitor.isCanceled())
                return Status.CANCEL_STATUS;
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable()
            {
                @SuppressWarnings("unchecked")
                public void run()
                {
                    Control control = getOutlinePage().getControl();
                    TreeViewer treeViewer = getOutlinePage().getTreeViewer();
                    IEditorPart editor = getOutlinePage().getEditor();
                    if (control == null
                        || control.isDisposed()
                        || !baseSelection.equals(selection)
                        || !baseSelection.equals(editor.getSite().getSelectionProvider().getSelection()))
                        return; // the world has changed -> no work needs to be done
                    final IStructuredSelection currentSelection =
                        (IStructuredSelection)treeViewer.getSelection();
                    if (currentSelection == null
                        || !currentSelection.toList().containsAll(
                            linkedSelection.toList()))
                    {
                        treeViewer.setSelection(linkedSelection, true);
                    }
                }
            });
            return Status.OK_STATUS;
        }
    }
}
