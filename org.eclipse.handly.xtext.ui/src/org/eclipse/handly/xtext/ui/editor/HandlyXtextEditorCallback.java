/*******************************************************************************
 * Copyright (c) 2014, 2016 1C-Soft LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.xtext.ui.editor;

import static org.eclipse.handly.model.Elements.exists;
import static org.eclipse.handly.model.Elements.getSourceElementAt;
import static org.eclipse.handly.model.Elements.getSourceElementInfo;
import static org.eclipse.handly.model.Elements.reconcile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.internal.xtext.ui.Activator;
import org.eclipse.handly.model.IElement;
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.eclipse.xtext.ui.editor.IXtextEditorCallback;
import org.eclipse.xtext.ui.editor.XtextEditor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

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
@Singleton
public class HandlyXtextEditorCallback
    extends IXtextEditorCallback.NullImpl
{
    private IInputElementProvider inputElementProvider;

    private Map<XtextEditor, WorkingCopyInfo> workingCopies =
        new HashMap<XtextEditor, WorkingCopyInfo>();
    private Map<MultiPageEditorPart, Set<XtextEditor>> nestedEditors =
        new HashMap<MultiPageEditorPart, Set<XtextEditor>>();
    private Map<XtextEditor, IPartListener> partListeners =
        new HashMap<XtextEditor, IPartListener>();
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
        registerContainer(editor);
        registerPartListener(editor);
        registerSelectionListener(editor);
    }

    @Override
    public void beforeDispose(XtextEditor editor)
    {
        deregisterPartListener(editor);
        deregisterSelectionListener(editor);
        disconnectWorkingCopy(editor);
        disposeHighlightRangeJob(editor);
        deregisterContainer(editor);
    }

    @Override
    public void beforeSetInput(XtextEditor editor)
    {
        disconnectWorkingCopy(editor);
    }

    @Override
    public void afterSetInput(XtextEditor editor)
    {
        if (isActive(editor))
            connectWorkingCopy(editor);
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
        IElement inputElement = inputElementProvider.getElement(
            editor.getEditorInput());
        if (!(inputElement instanceof SourceFile))
            return null;
        return (SourceFile)inputElement;
    }

    protected final SourceFile getWorkingCopy(XtextEditor editor)
    {
        WorkingCopyInfo workingCopyInfo = workingCopies.get(editor);
        if (workingCopyInfo == null || !workingCopyInfo.success)
            return null;
        return workingCopyInfo.sourceFile;
    }

    private boolean isActive(XtextEditor editor)
    {
        IEditorSite site = editor.getEditorSite();
        if (site == null)
            return false;
        IEditorPart activeEditor = site.getPage().getActiveEditor();
        return editor == activeEditor || (activeEditor != null && getContainer(
            editor) == activeEditor);
    }

    private MultiPageEditorPart getContainer(XtextEditor editor)
    {
        MultiPageEditorPart container = null;
        IEditorSite site = editor.getEditorSite();
        while (site instanceof MultiPageEditorSite)
        {
            container = ((MultiPageEditorSite)site).getMultiPageEditor();
            site = container.getEditorSite();
        }
        return container;
    }

    private void registerContainer(XtextEditor editor)
    {
        MultiPageEditorPart container = getContainer(editor);
        if (container == null)
            return;
        Set<XtextEditor> nestedSet = nestedEditors.get(container);
        if (nestedSet == null)
        {
            nestedSet = new HashSet<XtextEditor>();
            nestedEditors.put(container, nestedSet);
        }
        nestedSet.add(editor);
    }

    private void deregisterContainer(XtextEditor editor)
    {
        MultiPageEditorPart container = getContainer(editor);
        if (container == null)
            return;
        Set<XtextEditor> nestedSet = nestedEditors.get(container);
        if (nestedSet != null)
        {
            nestedSet.remove(editor);
            if (nestedSet.isEmpty())
                nestedEditors.remove(container);
        }
    }

    private void registerPartListener(final XtextEditor editor)
    {
        IPartListener listener = new IPartListener()
        {
            public void partActivated(IWorkbenchPart part)
            {
                if (part == editor || part == getContainer(editor))
                    connectWorkingCopy(editor);
            }

            public void partBroughtToTop(IWorkbenchPart part)
            {
                // Treat this the same as part activation.
                partActivated(part);
            }

            // @formatter:off
            public void partDeactivated(IWorkbenchPart part) {}
            public void partOpened(IWorkbenchPart part) {}
            public void partClosed(IWorkbenchPart part) {}
            // @formatter:on
        };
        editor.getSite().getWorkbenchWindow().getPartService().addPartListener(
            listener);
        partListeners.put(editor, listener);
    }

    private void deregisterPartListener(XtextEditor editor)
    {
        IPartListener listener = partListeners.remove(editor);
        if (listener != null)
        {
            editor.getSite().getWorkbenchWindow().getPartService().removePartListener(
                listener);
        }
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

    private void connectWorkingCopy(XtextEditor editor)
    {
        SourceFile sourceFile = getSourceFile(editor);
        if (sourceFile == null)
            return;

        XtextEditor workingCopyEditor = getWorkingCopyEditor(sourceFile);
        if (editor != workingCopyEditor)
        {
            if (workingCopyEditor != null)
                discardWorkingCopy(workingCopyEditor);

            createWorkingCopy(sourceFile, editor);
        }
    }

    private void disconnectWorkingCopy(XtextEditor editor)
    {
        SourceFile sourceFile = discardWorkingCopy(editor);
        if (sourceFile == null)
            return;

        XtextEditor mruClone = findMruClone(editor);
        if (mruClone != null)
        {
            createWorkingCopy(sourceFile, mruClone);
        }
    }

    private void createWorkingCopy(SourceFile sourceFile, XtextEditor editor)
    {
        try (TextEditorBuffer buffer = new TextEditorBuffer(editor))
        {
            if (sourceFile.hBecomeWorkingCopy(buffer, // will addRef() the buffer
                (IBuffer b) -> new XtextWorkingCopyInfo(b), null) != null)
            {
                sourceFile.hDiscardWorkingCopy();

                throw new IllegalStateException("Already a working copy: " //$NON-NLS-1$
                    + sourceFile);
            }

            workingCopies.put(editor, new WorkingCopyInfo(sourceFile, true));

            setHighlightRange(editor,
                editor.getSelectionProvider().getSelection());
        }
        catch (CoreException e)
        {
            workingCopies.put(editor, new WorkingCopyInfo(sourceFile, false));

            editor.resetHighlightRange();

            if (!editor.getEditorInput().exists())
                ; // this is considered normal
            else
                Activator.log(e.getStatus());
        }
    }

    private SourceFile discardWorkingCopy(XtextEditor editor)
    {
        WorkingCopyInfo workingCopyInfo = workingCopies.remove(editor);
        if (workingCopyInfo == null)
            return null;
        if (workingCopyInfo.success)
        {
            workingCopyInfo.sourceFile.hDiscardWorkingCopy();
            editor.resetHighlightRange();
        }
        return workingCopyInfo.sourceFile;
    }

    private XtextEditor getWorkingCopyEditor(SourceFile sourceFile)
    {
        Set<Entry<XtextEditor, WorkingCopyInfo>> entrySet =
            workingCopies.entrySet();
        for (Entry<XtextEditor, WorkingCopyInfo> entry : entrySet)
        {
            if (entry.getValue().sourceFile.equals(sourceFile))
                return entry.getKey();
        }
        return null;
    }

    private XtextEditor findMruClone(XtextEditor editor)
    {
        IEditorInput editorInput = editor.getEditorInput();
        IEditorReference[] references = editor.getSite().getPage().findEditors(
            editorInput, null, IWorkbenchPage.MATCH_INPUT);
        for (IEditorReference reference : references)
        {
            IEditorPart candidate = reference.getEditor(false);
            if (candidate instanceof XtextEditor)
            {
                if (candidate != editor)
                    return (XtextEditor)candidate;
            }
            else if (candidate instanceof MultiPageEditorPart)
            {
                // assume at most one XtextEditor with a given input is nested
                Set<XtextEditor> nestedSet = nestedEditors.get(candidate);
                if (nestedSet != null)
                {
                    for (XtextEditor nested : nestedSet)
                    {
                        if (nested != editor && editorInput.equals(
                            nested.getEditorInput()))
                        {
                            return nested;
                        }
                    }
                }
            }
        }
        return null;
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
                        reconcile(sourceFile, monitor);
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
                        selectedElement = getSourceElementAt(sourceFile,
                            position, null);
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
            if (selectedElement == null || !exists(selectedElement))
            {
                resetEditorHighlightRange(args);
            }
            else
            {
                TextRange r;
                try
                {
                    r = getSourceElementInfo(selectedElement).getFullRange();
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

    /*
     * Multiple XtextEditor instances may simultaneously be opened on a given
     * source file, each with its own underlying document, but only one of them
     * can be designated the working copy editor and connected to the source
     * file's working copy. This class is used for tracking the source file's
     * working copy editor. The success flag indicates whether the working copy
     * of the source file was created successfully by the working copy editor.
     * (A common reason for failure is that the editor input doesn't exist.)
     *
     * @see #getWorkingCopyEditor(SourceFile)
     */
    private static class WorkingCopyInfo
    {
        public final SourceFile sourceFile;
        public final boolean success;

        /*
         * @param sourceFile not null
         * @param success whether sourceFile.becomeWorkingCopy was successful,
         *  so sourceFile.discardWorkingCopy() is to be called
         */
        public WorkingCopyInfo(SourceFile sourceFile, boolean success)
        {
            this.sourceFile = sourceFile;
            this.success = success;
        }
    }
}
