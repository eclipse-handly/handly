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
package org.eclipse.handly.internal.examples.basic.ui.outline;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.handly.examples.basic.ui.model.FooModelCore;
import org.eclipse.handly.internal.examples.basic.ui.FooContentProvider;
import org.eclipse.handly.internal.examples.basic.ui.FooLabelProvider;
import org.eclipse.handly.model.IElementChangeEvent;
import org.eclipse.handly.model.IElementChangeListener;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.IHandleDelta;
import org.eclipse.handly.model.ISourceElement;
import org.eclipse.handly.model.SourceElements;
import org.eclipse.handly.util.TextRange;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.OpenAndLinkWithEditorHelper;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.xtext.ui.editor.IXtextEditorAware;
import org.eclipse.xtext.ui.editor.XtextEditor;

import com.google.inject.Inject;

/**
 * Foo Outline page.
 * <p>
 * Note that much of the code is free from specifics of the Foo Model,
 * thanks to the uniform API provided by Handly.
 * </p>
 *
 * @deprecated This class implements a basic Foo Outline page from scratch.
 *  Since 0.3, you can use Handly outline framework to easily build
 *  outline pages of rich functionality. {@link FooOutlinePage2}
 *  provides an example.
 */
public final class FooOutlinePage
    extends ContentOutlinePage
    implements IXtextEditorAware, IElementChangeListener
{
    private XtextEditor editor;
    private LinkingHelper linkingHelper;
    private IPropertyListener editorInputListener = new IPropertyListener()
    {
        public void propertyChanged(Object source, int propId)
        {
            if (propId == IEditorPart.PROP_INPUT)
            {
                getTreeViewer().setInput(computeInput());
            }
        }
    };

    @Inject
    private FooContentProvider contentProvider;
    @Inject
    private FooLabelProvider labelProvider;

    @Override
    public void setEditor(XtextEditor editor)
    {
        this.editor = editor;
    }

    @Override
    public void createControl(Composite parent)
    {
        super.createControl(parent);
        getTreeViewer().setContentProvider(contentProvider);
        getTreeViewer().setLabelProvider(labelProvider);
        getTreeViewer().setInput(computeInput());
        linkingHelper = new LinkingHelper();
        editor.addPropertyListener(editorInputListener);
        FooModelCore.getFooModel().addElementChangeListener(this);
    }

    @Override
    public void dispose()
    {
        FooModelCore.getFooModel().removeElementChangeListener(this);
        editor.removePropertyListener(editorInputListener);
        if (linkingHelper != null)
            linkingHelper.dispose();
        editor.outlinePageClosed();
        super.dispose();
    }

    @Override
    public void elementChanged(IElementChangeEvent event)
    {
        if (affects(event.getDelta(), (IHandle)getTreeViewer().getInput()))
        {
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable()
            {
                public void run()
                {
                    Control control = getTreeViewer().getControl();
                    if (control != null && !control.isDisposed())
                    {
                        refresh();
                    }
                }
            });
        }
    }

    private boolean affects(IHandleDelta delta, IHandle element)
    {
        if (delta.getElement().equals(element))
            return true;
        IHandleDelta[] children = delta.getAffectedChildren();
        for (IHandleDelta child : children)
        {
            if (affects(child, element))
                return true;
        }
        return false;
    }

    private Object computeInput()
    {
        IEditorInput editorInput = editor.getEditorInput();
        if (editorInput instanceof IFileEditorInput)
        {
            IFile file = ((IFileEditorInput)editorInput).getFile();
            return FooModelCore.create(file);
        }
        return null;
    }

    private void refresh()
    {
        Control control = getControl();
        control.setRedraw(false);
        BusyIndicator.showWhile(control.getDisplay(), new Runnable()
        {
            public void run()
            {
                TreePath[] treePaths = getTreeViewer().getExpandedTreePaths();
                getTreeViewer().refresh();
                getTreeViewer().setExpandedTreePaths(treePaths);
            }
        });
        control.setRedraw(true);
    }

    private class LinkingHelper
        extends OpenAndLinkWithEditorHelper
    {
        private LinkToOutlineJob linkToOutlineJob = new LinkToOutlineJob();
        private ISelectionChangedListener editorListener =
            new ISelectionChangedListener()
            {
                public void selectionChanged(SelectionChangedEvent event)
                {
                    if (!getTreeViewer().getControl().isFocusControl())
                    {
                        linkToOutline(event.getSelection());
                    }
                }
            };

        public LinkingHelper()
        {
            super(getTreeViewer());
            setLinkWithEditor(true);
            ISelectionProvider selectionProvider =
                editor.getSite().getSelectionProvider();
            if (selectionProvider instanceof IPostSelectionProvider)
                ((IPostSelectionProvider)selectionProvider).addPostSelectionChangedListener(
                    editorListener);
            else
                selectionProvider.addSelectionChangedListener(editorListener);
        }

        @Override
        public void dispose()
        {
            ISelectionProvider selectionProvider =
                editor.getSite().getSelectionProvider();
            if (selectionProvider instanceof IPostSelectionProvider)
                ((IPostSelectionProvider)selectionProvider).removePostSelectionChangedListener(
                    editorListener);
            else
                selectionProvider.removeSelectionChangedListener(
                    editorListener);
            cancelLinkToOutlineJob();
            super.dispose();
        }

        @Override
        public void setLinkWithEditor(boolean enabled)
        {
            super.setLinkWithEditor(enabled);
            if (enabled)
                linkToOutline(
                    editor.getSite().getSelectionProvider().getSelection());
        }

        @Override
        protected void activate(ISelection selection)
        {
            linkToEditor(selection);
        }

        @Override
        protected void open(ISelection selection, boolean activate)
        {
            linkToEditor(selection);
        }

        @Override
        protected void linkToEditor(ISelection selection)
        {
            cancelLinkToOutlineJob();
            if (selection == null || selection.isEmpty())
                return;
            Object element =
                ((IStructuredSelection)selection).getFirstElement();
            if (!(element instanceof ISourceElement))
                return;
            TextRange identifyingRange = SourceElements.getSourceElementInfo(
                (ISourceElement)element).getIdentifyingRange();
            if (identifyingRange == null)
                return;
            editor.selectAndReveal(identifyingRange.getOffset(),
                identifyingRange.getLength());
        }

        protected void linkToOutline(ISelection selection)
        {
            if (selection == null || selection.isEmpty())
                return;
            if (selection instanceof ITextSelection)
                scheduleLinkToOutlineJob((ITextSelection)selection);
        }

        private void cancelLinkToOutlineJob()
        {
            linkToOutlineJob.cancel();
            linkToOutlineJob.setSelection(null);
        }

        private void scheduleLinkToOutlineJob(ITextSelection selection)
        {
            linkToOutlineJob.cancel();
            linkToOutlineJob.setSelection(selection);
            linkToOutlineJob.schedule();
        }

        private class LinkToOutlineJob
            extends Job
        {
            private volatile ITextSelection selection;

            public LinkToOutlineJob()
            {
                super(""); //$NON-NLS-1$
                setSystem(true);
            }

            public void setSelection(ITextSelection selection)
            {
                this.selection = selection;
            }

            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                final ITextSelection baseSelection = selection;
                if (baseSelection == null || baseSelection.isEmpty())
                    return Status.OK_STATUS;
                final TreeViewer treeViewer = getTreeViewer();
                Object input = treeViewer.getInput();
                if (!(input instanceof ISourceElement))
                    return Status.OK_STATUS;
                if (!SourceElements.ensureReconciled((ISourceElement)input))
                    return Status.OK_STATUS;
                final ISourceElement element = SourceElements.getElementAt(
                    (ISourceElement)input, baseSelection.getOffset(), null);
                if (element == null)
                    return Status.OK_STATUS;
                if (monitor.isCanceled())
                    return Status.CANCEL_STATUS;
                // note that reconciling will have asyncExec'ed #refresh by this time
                PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable()
                {
                    public void run()
                    {
                        Control control = treeViewer.getControl();
                        if (control == null || control.isDisposed()
                            || !baseSelection.equals(selection)
                            || !baseSelection.equals(
                                editor.getSelectionProvider().getSelection()))
                            return; // the world has changed -> no work needs to be done
                        final IStructuredSelection currentSelection =
                            (IStructuredSelection)treeViewer.getSelection();
                        if (currentSelection == null
                            || !currentSelection.toList().contains(element))
                        {
                            treeViewer.setSelection(new StructuredSelection(
                                element), true);
                        }
                    }
                });
                return Status.OK_STATUS;
            }
        }
    }
}
