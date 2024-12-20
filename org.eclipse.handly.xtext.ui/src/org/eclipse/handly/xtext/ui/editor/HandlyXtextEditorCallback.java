/*******************************************************************************
 * Copyright (c) 2014, 2022 1C-Soft LLC and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.xtext.ui.editor;

import static org.eclipse.handly.context.Contexts.of;
import static org.eclipse.handly.context.Contexts.with;
import static org.eclipse.handly.model.Elements.exists;
import static org.eclipse.handly.model.Elements.getSourceElementAt;
import static org.eclipse.handly.model.Elements.getSourceElementInfo;
import static org.eclipse.handly.model.Elements.reconcile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.internal.xtext.ui.Activator;
import org.eclipse.handly.model.Elements;
import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.ISourceElement;
import org.eclipse.handly.model.ISourceFile;
import org.eclipse.handly.model.impl.ISourceFileImplExtension;
import org.eclipse.handly.ui.IInputElementProvider;
import org.eclipse.handly.ui.PartListenerAdapter;
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
 * <p>
 * Multiple Xtext editor instances may simultaneously be open for a given
 * source file, each with its own underlying document, but only one of them
 * (the most recently used one) is connected to the source file's working copy.
 * </p>
 * <p>
 * Note that this class relies on the language-specific implementation of
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

    private Map<IEditorInput, WorkingCopyEditorInfo> workingCopyEditors =
        new HashMap<>();
    private Map<MultiPageEditorPart, Set<XtextEditor>> nestedEditors =
        new HashMap<>();
    private Map<XtextEditor, EditorInfo> editorInfoMap = new HashMap<>();

    @Inject
    public void setInputElementProvider(IInputElementProvider provider)
    {
        inputElementProvider = provider;
    }

    @Override
    public void afterCreatePartControl(XtextEditor editor)
    {
        connect(editor);
        registerContainer(editor);
    }

    @Override
    public void beforeDispose(XtextEditor editor)
    {
        disconnectWorkingCopy(editor);
        deregisterContainer(editor);
        disconnect(editor);
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

    /**
     * Notifies that the selection has changed in the editor.
     * <p>
     * This implementation invokes <code>setHighlightRange(editor, selection)</code>
     * if the selection is not <code>null</code>.
     * </p>
     *
     * @param editor never <code>null</code>
     * @param selection may be <code>null</code> or empty
     */
    protected void afterSelectionChange(XtextEditor editor,
        ISelection selection)
    {
        if (selection != null)
            setHighlightRange(editor, selection);
    }

    /**
     * Sets the highlighted range of the editor according to the selection.
     * <p>
     * This implementation schedules a background job to set the highlight range
     * asynchronously.
     * </p>
     *
     * @param editor never <code>null</code>
     * @param selection never <code>null</code>
     */
    protected void setHighlightRange(XtextEditor editor, ISelection selection)
    {
        scheduleHighlightRangeJob(editor, selection);
    }

    /**
     * Returns the corresponding source file for the editor.
     * <p>
     * This implementation uses the injected {@link IInputElementProvider}
     * to obtain an {@link IElement} corresponding to the editor input
     * and returns the <code>IElement</code> if it is an {@link ISourceFile}.
     * </p>
     *
     * @param editor never <code>null</code>
     * @return the corresponding source file, or <code>null</code> if none
     */
    protected ISourceFile getSourceFile(XtextEditor editor)
    {
        IElement inputElement = inputElementProvider.getElement(
            editor.getEditorInput());
        if (!(inputElement instanceof ISourceFile))
            return null;
        return (ISourceFile)inputElement;
    }

    /**
     * Returns the working copy that the editor is connected to, or <code>null</code>
     * if the editor is not currently connected to a working copy.
     * <p>
     * Note that multiple Xtext editor instances may simultaneously be open for
     * a given source file, each with its own underlying document, but only one
     * of them (the most recently used one) is connected to the source file's
     * working copy.
     * </p>
     *
     * @param editor never <code>null</code>
     * @return the working copy that the editor is connected to, or <code>null</code>
     *  if the editor is not currently connected to a working copy
     */
    protected final ISourceFile getWorkingCopy(XtextEditor editor)
    {
        WorkingCopyEditorInfo info = workingCopyEditors.get(
            editor.getEditorInput());
        if (info == null || info.editor != editor)
            return null;
        return info.workingCopy;
    }

    /**
     * Attempts to acquire a working copy for the corresponding source file of
     * the editor. A working copy acquired by this method <b>must</b> be released
     * eventually via a call to {@link #releaseWorkingCopy(XtextEditor, ISourceFile)
     * releaseWorkingCopy}.
     * <p>
     * This implementation obtains the corresponding source file for the
     * editor via {@link #getSourceFile(XtextEditor)} and, if the source file
     * implements {@link ISourceFileImplExtension}, invokes {@link
     * ISourceFileImplExtension#becomeWorkingCopy_ becomeWorkingCopy_} on it
     * providing a working copy buffer backed by the editor and an Xtext-specific
     * working copy callback, and returns the acquired working copy. Otherwise,
     * <code>null</code> is returned.
     * </p>
     *
     * @param editor never <code>null</code>
     * @return the acquired working copy, or <code>null</code>
     *  if no working copy can be acquired
     * @throws CoreException if the working copy could not be acquired successfully
     */
    protected ISourceFile acquireWorkingCopy(XtextEditor editor)
        throws CoreException
    {
        ISourceFile sourceFile = getSourceFile(editor);
        if (sourceFile instanceof ISourceFileImplExtension)
        {
            try (
                TextEditorBuffer buffer = new TextEditorBuffer(
                    editor.getDocumentProvider(), editor.getEditorInput()))
            {
                ((ISourceFileImplExtension)sourceFile).becomeWorkingCopy_(with(
                    of(ISourceFileImplExtension.WORKING_COPY_BUFFER, buffer),
                    of(ISourceFileImplExtension.WORKING_COPY_CALLBACK,
                        new XtextWorkingCopyCallback())), null);
                return sourceFile;
            }
        }
        return null;
    }

    /**
     * Releases the given working copy that was acquired via a call to
     * {@link #acquireWorkingCopy(XtextEditor) acquireWorkingCopy}.
     * <p>
     * This implementation invokes <code>((ISourceFileImplExtension)workingCopy).{@link
     * ISourceFileImplExtension#releaseWorkingCopy_() releaseWorkingCopy_()}</code>.
     * </p>
     *
     * @param editor never <code>null</code>
     * @param workingCopy never <code>null</code>
     */
    protected void releaseWorkingCopy(XtextEditor editor,
        ISourceFile workingCopy)
    {
        ((ISourceFileImplExtension)workingCopy).releaseWorkingCopy_();
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

    private void connect(XtextEditor editor)
    {
        IPartListener partListener = new PartListenerAdapter()
        {
            @Override
            public void partActivated(IWorkbenchPart part)
            {
                if (part == editor || part == getContainer(editor))
                    connectWorkingCopy(editor);
            }

            @Override
            public void partBroughtToTop(IWorkbenchPart part)
            {
                // Treat this the same as part activation.
                partActivated(part);
            }

            @Override
            public void partOpened(IWorkbenchPart part)
            {
                // Treat this the same as part activation.
                partActivated(part);
            }
        };
        editor.getSite().getWorkbenchWindow().getPartService().addPartListener(
            partListener);

        ISelectionChangedListener selectionChangedListener =
            new ISelectionChangedListener()
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
                selectionChangedListener);
        else
            selectionProvider.addSelectionChangedListener(
                selectionChangedListener);

        EditorInfo info = new EditorInfo();
        info.partListener = partListener;
        info.selectionChangedListener = selectionChangedListener;
        info.highlightRangeJob = new HighlightRangeJob(editor);
        editorInfoMap.put(editor, info);
    }

    private void disconnect(XtextEditor editor)
    {
        EditorInfo info = editorInfoMap.remove(editor);
        if (info == null)
            return;

        editor.getSite().getWorkbenchWindow().getPartService().removePartListener(
            info.partListener);

        ISelectionProvider selectionProvider = editor.getSelectionProvider();
        if (selectionProvider instanceof IPostSelectionProvider)
            ((IPostSelectionProvider)selectionProvider).removePostSelectionChangedListener(
                info.selectionChangedListener);
        else
            selectionProvider.removeSelectionChangedListener(
                info.selectionChangedListener);

        HighlightRangeJob highlightRangeJob = info.highlightRangeJob;
        highlightRangeJob.cancel();
        highlightRangeJob.setArgs(null);
    }

    private void connectWorkingCopy(XtextEditor editor)
    {
        XtextEditor workingCopyEditor = getWorkingCopyEditor(
            editor.getEditorInput());
        if (editor != workingCopyEditor)
        {
            if (workingCopyEditor != null)
                disconnectWorkingCopy0(workingCopyEditor);

            connectWorkingCopy0(editor);
        }
    }

    private void disconnectWorkingCopy(XtextEditor editor)
    {
        if (!disconnectWorkingCopy0(editor))
            return;

        XtextEditor mruClone = findMruClone(editor);
        if (mruClone != null)
        {
            connectWorkingCopy0(mruClone);
        }
    }

    private void connectWorkingCopy0(XtextEditor editor)
    {
        ISourceFile workingCopy = null;
        try
        {
            workingCopy = acquireWorkingCopy(editor);
        }
        catch (CoreException e)
        {
            if (!editor.getEditorInput().exists())
                ; // this is considered normal
            else
                Activator.logError(e);
        }
        if (workingCopy != null)
        {
            if (!Elements.isWorkingCopy(workingCopy))
                throw new AssertionError();
            try (IBuffer buffer = Elements.getBuffer(workingCopy))
            {
                if (buffer.getDocument() != editor.getDocumentProvider().getDocument(
                    editor.getEditorInput()))
                {
                    releaseWorkingCopy(editor, workingCopy);
                    throw new AssertionError();
                }
            }
            catch (CoreException e)
            {
                Activator.logError(e);
            }
        }
        workingCopyEditors.put(editor.getEditorInput(),
            new WorkingCopyEditorInfo(editor, workingCopy));
        if (workingCopy != null)
            setHighlightRange(editor,
                editor.getSelectionProvider().getSelection());
        else
            editor.resetHighlightRange();
    }

    private boolean disconnectWorkingCopy0(XtextEditor editor)
    {
        WorkingCopyEditorInfo info = workingCopyEditors.get(
            editor.getEditorInput());
        if (info == null || info.editor != editor)
            return false;
        workingCopyEditors.remove(editor.getEditorInput());
        if (info.workingCopy != null)
        {
            releaseWorkingCopy(editor, info.workingCopy);
            editor.resetHighlightRange();
        }
        return true;
    }

    private XtextEditor getWorkingCopyEditor(IEditorInput editorInput)
    {
        WorkingCopyEditorInfo info = workingCopyEditors.get(editorInput);
        if (info == null)
            return null;
        return info.editor;
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
        ISelection selection)
    {
        ISourceFile workingCopy = getWorkingCopy(editor);
        if (workingCopy == null)
            return;
        EditorInfo info = editorInfoMap.get(editor);
        if (info == null)
            return;
        HighlightRangeJob highlightRangeJob = info.highlightRangeJob;
        highlightRangeJob.cancel();
        highlightRangeJob.setArgs(new HighlightArgs(workingCopy, selection));
        highlightRangeJob.schedule();
    }

    private class HighlightRangeJob
        extends Job
    {
        private final XtextEditor editor;
        private volatile HighlightArgs args;

        HighlightRangeJob(XtextEditor editor)
        {
            super(""); //$NON-NLS-1$
            setSystem(true);
            this.editor = editor;
        }

        void setArgs(HighlightArgs args)
        {
            this.args = args;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            HighlightArgs args = this.args;
            if (args == null)
                return Status.OK_STATUS;
            ISourceFile sourceFile = args.sourceFile;
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
                        Activator.logError(e);
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
                    Activator.logError(e);
                    resetEditorHighlightRange(args);
                    return e.getStatus();
                }
                if (r != null)
                    setEditorHighlightRange(args, r.getOffset(), r.getLength());
            }
            return Status.OK_STATUS;
        }

        private void setEditorHighlightRange(HighlightArgs args, int offset,
            int length)
        {
            PlatformUI.getWorkbench().getDisplay().asyncExec(() ->
            {
                if (!hasWorldChanged(args))
                    editor.setHighlightRange(offset, length, false);
            });
        }

        private void resetEditorHighlightRange(HighlightArgs args)
        {
            PlatformUI.getWorkbench().getDisplay().asyncExec(() ->
            {
                if (!hasWorldChanged(args))
                    editor.resetHighlightRange();
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
        final ISourceFile sourceFile;
        final ISelection selection;

        /*
         * @param sourceFile not null
         * @param selection not null
         */
        HighlightArgs(ISourceFile sourceFile, ISelection selection)
        {
            this.sourceFile = sourceFile;
            this.selection = selection;
        }
    }

    private static class EditorInfo
    {
        IPartListener partListener;
        ISelectionChangedListener selectionChangedListener;
        HighlightRangeJob highlightRangeJob;
    }

    private static class WorkingCopyEditorInfo
    {
        final XtextEditor editor;
        final ISourceFile workingCopy;

        /*
         * @param editor not null
         * @param workingCopy may be null
         */
        WorkingCopyEditorInfo(XtextEditor editor, ISourceFile workingCopy)
        {
            this.editor = editor;
            this.workingCopy = workingCopy;
        }
    }
}
