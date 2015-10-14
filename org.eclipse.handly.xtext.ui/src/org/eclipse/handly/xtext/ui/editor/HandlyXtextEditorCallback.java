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
package org.eclipse.handly.xtext.ui.editor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.handly.internal.xtext.ui.Activator;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.ISourceElement;
import org.eclipse.handly.model.impl.SourceFile;
import org.eclipse.handly.ui.IInputElementProvider;
import org.eclipse.handly.ui.texteditor.TextEditorBuffer;
import org.eclipse.handly.util.TextRange;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.ui.editor.IXtextEditorCallback;
import org.eclipse.xtext.ui.editor.XtextEditor;

import com.google.inject.Inject;

/**
 * Integrates Xtext editor with Handly working copy management facility.
 * Creates a working copy when a source file is opened in Xtext editor.
 * Discards the working copy when the editor is being disposed. Also,
 * sets the editor highlight range for the currently selected element.
 * <p>
 * Note that this class relies on a language-specific implementation of
 * {@link IInputElementProvider} being available through injection.
 * Also, {@link HandlyXtextDocument} and other classes pertaining to
 * Handly/Xtext integration should be bound if this callback is configured.
 * For example:
 * </p>
 * <pre>
 * public Class&lt;? extends IInputElementProvider&gt; bindIInputElementProvider() {
 *     return FooInputElementProvider.class;
 * }
 *
 * public void configureXtextEditorCallback(Binder binder) {
 *     binder.bind(IXtextEditorCallback.class).annotatedWith(Names.named(
 *         HandlyXtextEditorCallback.class.getName())).to(
 *             HandlyXtextEditorCallback.class);
 * }
 *
 * public Class&lt;? extends XtextDocument&gt; bindXtextDocument() {
 *     return HandlyXtextDocument.class;
 * }
 *
 * public Class&lt;? extends IReconciler&gt; bindIReconciler() {
 *     return HandlyXtextReconciler.class;
 * }
 *
 * public Class&lt;? extends DirtyStateEditorSupport&gt; bindDirtyStateEditorSupport() {
 *     return HandlyDirtyStateEditorSupport.class; // or its subclass
 * }
 * </pre>
 */
public class HandlyXtextEditorCallback
    extends IXtextEditorCallback.NullImpl
{
    private IInputElementProvider inputElementProvider;

    private Map<XtextEditor, SourceFile> workingCopies =
        new HashMap<XtextEditor, SourceFile>();
    private Map<XtextEditor, ISelectionChangedListener> selectionListeners =
        new HashMap<XtextEditor, ISelectionChangedListener>();
    private Map<XtextEditor, HighlightRangeJob> highlightRangeJobs =
        new HashMap<XtextEditor, HighlightRangeJob>();

    @Inject
    public void setInputElementProvider(IInputElementProvider provider)
    {
        inputElementProvider = provider;
    }

    @Override
    public void afterCreatePartControl(XtextEditor editor)
    {
        setHighlightRange(editor, editor.getSelectionProvider().getSelection());
        registerSelectionListener(editor);
    }

    @Override
    public void beforeDispose(XtextEditor editor)
    {
        deregisterSelectionListener(editor);
        discardWorkingCopy(editor);
        disposeHighlightRangeJob(editor);
    }

    @Override
    public void beforeSetInput(XtextEditor editor)
    {
        discardWorkingCopy(editor);
    }

    @Override
    public void afterSetInput(XtextEditor editor)
    {
        createWorkingCopy(editor);
        setHighlightRange(editor, editor.getSelectionProvider().getSelection());
    }

    protected void afterSelectionChange(XtextEditor editor,
        ISelection selection)
    {
        setHighlightRange(editor, selection);
    }

    protected void setHighlightRange(XtextEditor editor, ISelection selection)
    {
        if (selection == null)
            return;
        SourceFile sourceFile = getWorkingCopy(editor);
        if (sourceFile == null)
            return;
        scheduleHighlightRangeJob(editor, sourceFile, selection);
    }

    protected SourceFile getSourceFile(XtextEditor editor)
    {
        IHandle inputElement = inputElementProvider.getElement(
            editor.getEditorInput());
        if (!(inputElement instanceof SourceFile))
            return null;
        return (SourceFile)inputElement;
    }

    protected final SourceFile getWorkingCopy(XtextEditor editor)
    {
        return workingCopies.get(editor);
    }

    private void registerSelectionListener(final XtextEditor editor)
    {
        ISelectionChangedListener listener = new ISelectionChangedListener()
        {
            @Override
            public void selectionChanged(SelectionChangedEvent event)
            {
                afterSelectionChange(editor, event.getSelection());
            }
        };
        ISelectionProvider selectionProvider = editor.getSelectionProvider();
        if (selectionProvider instanceof IPostSelectionProvider)
            ((IPostSelectionProvider)selectionProvider).addPostSelectionChangedListener(
                listener);
        else
            selectionProvider.addSelectionChangedListener(listener);
        selectionListeners.put(editor, listener);
    }

    private void deregisterSelectionListener(XtextEditor editor)
    {
        ISelectionChangedListener listener = selectionListeners.remove(editor);
        if (listener != null)
        {
            ISelectionProvider selectionProvider =
                editor.getSelectionProvider();
            if (selectionProvider instanceof IPostSelectionProvider)
                ((IPostSelectionProvider)selectionProvider).removePostSelectionChangedListener(
                    listener);
            else
                selectionProvider.removeSelectionChangedListener(listener);
        }
    }

    private void createWorkingCopy(XtextEditor editor)
    {
        SourceFile sourceFile = getSourceFile(editor);
        if (sourceFile != null && !sourceFile.isWorkingCopy()
            && sourceFile.getParent().exists())
        {
            try
            {
                TextEditorBuffer delegate = new TextEditorBuffer(editor);
                try
                {
                    XtextWorkingCopyBuffer buffer = new XtextWorkingCopyBuffer(
                        sourceFile, delegate); // will addRef() the delegate
                    try
                    {
                        if (sourceFile.becomeWorkingCopy(buffer, // will addRef() the buffer
                            null) != null)
                        {
                            sourceFile.discardWorkingCopy();

                            throw new IllegalStateException(
                                "Already a working copy: " //$NON-NLS-1$
                                    + sourceFile);
                        }
                        workingCopies.put(editor, sourceFile);
                    }
                    finally
                    {
                        buffer.release();
                    }
                }
                finally
                {
                    delegate.release();
                }
            }
            catch (CoreException e)
            {
                Activator.log(e.getStatus());
            }
        }
    }

    private void discardWorkingCopy(XtextEditor editor)
    {
        SourceFile sourceFile = workingCopies.remove(editor);
        if (sourceFile != null)
        {
            sourceFile.discardWorkingCopy();
        }
    }

    private void scheduleHighlightRangeJob(XtextEditor editor,
        SourceFile sourceFile, ISelection selection)
    {
        HighlightRangeJob highlightRangeJob = highlightRangeJobs.get(editor);
        if (highlightRangeJob == null)
        {
            highlightRangeJob = new HighlightRangeJob(editor);
            highlightRangeJobs.put(editor, highlightRangeJob);
        }
        highlightRangeJob.cancel();
        highlightRangeJob.setArgs(new HighlightArgs(sourceFile, selection));
        highlightRangeJob.schedule();
    }

    private void disposeHighlightRangeJob(XtextEditor editor)
    {
        HighlightRangeJob highlightRangeJob = highlightRangeJobs.remove(editor);
        if (highlightRangeJob != null)
        {
            highlightRangeJob.cancel();
            highlightRangeJob.setArgs(null);
        }
    }

    private class HighlightRangeJob
        extends Job
    {
        private final XtextEditor editor;
        private volatile HighlightArgs args;

        public HighlightRangeJob(XtextEditor editor)
        {
            super(""); //$NON-NLS-1$
            setSystem(true);
            this.editor = editor;
        }

        public void setArgs(HighlightArgs args)
        {
            this.args = args;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            HighlightArgs args = this.args;
            if (args == null)
                return Status.OK_STATUS;
            SourceFile sourceFile = args.sourceFile;
            ISelection selection = args.selection;
            ISourceElement selectedElement = null;
            if (selection instanceof ITextSelection)
            {
                int position = ((ITextSelection)selection).getOffset();
                if (position >= 0)
                {
                    try
                    {
                        sourceFile.reconcile(false, monitor);
                    }
                    catch (CoreException e)
                    {
                        Activator.log(e.getStatus());
                        resetEditorHighlightRange(args);
                        return e.getStatus();
                    }
                    if (monitor.isCanceled())
                        return Status.CANCEL_STATUS;
                    try
                    {
                        selectedElement = sourceFile.getElementAt(position,
                            null);
                        if (sourceFile.equals(selectedElement))
                            selectedElement = null;
                    }
                    catch (CoreException e)
                    {
                        selectedElement = null;
                    }
                }
            }
            if (monitor.isCanceled())
                return Status.CANCEL_STATUS;
            if (selectedElement == null || !selectedElement.exists())
            {
                resetEditorHighlightRange(args);
            }
            else
            {
                TextRange r;
                try
                {
                    r = selectedElement.getSourceElementInfo().getFullRange();
                }
                catch (CoreException e)
                {
                    Activator.log(e.getStatus());
                    resetEditorHighlightRange(args);
                    return e.getStatus();
                }
                if (r != null)
                    setEditorHighlightRange(args, r.getOffset(), r.getLength());
            }
            return Status.OK_STATUS;
        }

        private void setEditorHighlightRange(final HighlightArgs args,
            final int offset, final int length)
        {
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable()
            {
                public void run()
                {
                    if (!hasWorldChanged(args))
                        editor.setHighlightRange(offset, length, false);
                }
            });
        }

        private void resetEditorHighlightRange(final HighlightArgs args)
        {
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable()
            {
                public void run()
                {
                    if (!hasWorldChanged(args))
                        editor.resetHighlightRange();
                }
            });
        }

        private boolean hasWorldChanged(HighlightArgs baseArgs)
        {
            return baseArgs != args || !baseArgs.sourceFile.equals(
                getWorkingCopy(editor)) || !baseArgs.selection.equals(
                    editor.getSelectionProvider().getSelection());
        }
    }

    private static class HighlightArgs
    {
        public final SourceFile sourceFile;
        public final ISelection selection;

        /*
         * @param sourceFile not null
         * @param selection not null
         */
        public HighlightArgs(SourceFile sourceFile, ISelection selection)
        {
            this.sourceFile = sourceFile;
            this.selection = selection;
        }
    }
}
