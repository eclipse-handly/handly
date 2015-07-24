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
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.ISourceElement;
import org.eclipse.handly.model.SourceElements;
import org.eclipse.handly.model.adapter.IContentAdapter;
import org.eclipse.handly.model.adapter.IContentAdapterProvider;
import org.eclipse.handly.model.adapter.NullContentAdapter;
import org.eclipse.handly.ui.IElementForEditorInputFactory;
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
    protected final IElementForEditorInputFactory inputElementFactory;
    private LinkToOutlineJob linkToOutlineJob = new LinkToOutlineJob();

    /**
     * Creates a new linking helper for the given outline page
     * that is based on <code>ISourceElement</code>.
     *
     * @param outlinePage not <code>null</code>
     * @param factory {@link IElementForEditorInputFactory}
     */
    public SourceElementLinkingHelper(ICommonOutlinePage outlinePage,
        IElementForEditorInputFactory factory)
    {
        super(outlinePage);
        inputElementFactory = factory;
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
     * if the first element is not an {@link ISourceElement} contained
     * in the given editor or if the identifying range is not set.
     * If the {@link #getContentAdapter() content adapter} is installed,
     * the element is first adapted to {@link IHandle}.
     * </p>
     *
     * @param editor the text editor (never <code>null</code>)
     * @param selection the outline selection (never <code>null</code>,
     *  never empty)
     */
    protected void linkToEditor(ITextEditor editor,
        IStructuredSelection selection)
    {
        IHandle element = getContentAdapter().getHandle(
            selection.getFirstElement());
        if (!(element instanceof ISourceElement))
            return;
        ISourceElement sourceElement = (ISourceElement)element;
        if (!isInEditor(sourceElement, editor))
            return;
        TextRange identifyingRange = SourceElements.getSourceElementInfo(
            sourceElement).getIdentifyingRange();
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
     * or <code>null</code> if none. If the {@link #getContentAdapter()
     * content adapter} is installed, it will be used to adapt the element
     * before it is returned in the selection.
     * </p>
     *
     * @param selection the text selection in the editor
     *  (never <code>null</code>, never empty)
     * @return the outline selection corresponding to the given selection
     *  in the editor, or <code>null</code>
     */
    protected IStructuredSelection getLinkedSelection(ITextSelection selection)
    {
        IHandle input = getContentAdapter().getHandle(
            getOutlinePage().getTreeViewer().getInput());
        if (!(input instanceof ISourceElement))
            return null;
        ISourceElement sourceElement = (ISourceElement)input;
        if (!SourceElements.ensureReconciled(sourceElement))
            return null;
        Object element = getContentAdapter().getCorrespondingElement(
            SourceElements.getElementAt(sourceElement, selection.getOffset(),
                null));
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

    /**
     * Returns whether the given element is contained in the given editor.
     * <p>
     * Default implementation checks whether the element corresponding to
     * the input for the given editor contains the given element.
     * </p>
     *
     * @param element may be <code>null</code>
     * @param editor not <code>null</code>
     * @return <code>true</code> if the element is contained in the editor;
     *  <code>false</code> otherwise
     */
    protected boolean isInEditor(IHandle element, IEditorPart editor)
    {
        IHandle inputElement = inputElementFactory.getElement(
            editor.getEditorInput());
        while (element != null)
        {
            if (element.equals(inputElement))
                return true;
            element = element.getParent();
        }
        return false;
    }

    /**
     * Returns the installed content adapter, or a {@link NullContentAdapter}
     * if none.
     *
     * @return {@link IContentAdapter} (never <code>null</code>)
     */
    protected IContentAdapter getContentAdapter()
    {
        ICommonOutlinePage outlinePage = getOutlinePage();
        if (outlinePage instanceof IContentAdapterProvider)
            return ((IContentAdapterProvider)outlinePage).getContentAdapter();
        return NullContentAdapter.INSTANCE;
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
            IHandle input = getContentAdapter().getHandle(
                getOutlinePage().getTreeViewer().getInput());
            if (!(input instanceof ISourceElement))
                return Status.OK_STATUS;

            final IStructuredSelection linkedSelection = getLinkedSelection(
                baseSelection);

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
                    if (control == null || control.isDisposed()
                        || !baseSelection.equals(selection)
                        || !baseSelection.equals(
                            editor.getSite().getSelectionProvider().getSelection()))
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
