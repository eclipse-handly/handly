/*******************************************************************************
 * Copyright (c) 2008, 2022 itemis AG (http://www.itemis.eu) and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - adaptation of XtextReconciler code
 *******************************************************************************/
package org.eclipse.handly.xtext.ui.editor;

import static org.eclipse.xtext.ui.editor.XtextSourceViewerConfiguration.XTEXT_TEMPLATE_POS_CATEGORY;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.handly.internal.xtext.ui.Activator;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.source.ContentAssistantFacade;
import org.eclipse.jface.text.source.ISourceViewerExtension4;
import org.eclipse.xtext.ui.editor.IXtextEditorCallback;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.model.IXtextDocumentContentObserver;
import org.eclipse.xtext.ui.editor.reconciler.TemplatePositionUpdater;
import org.eclipse.xtext.ui.editor.reconciler.XtextReconciler;

import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;

/**
 * Adaptation of {@link XtextReconciler} for Handly reconciling story.
 * <p>
 * Bind this class in place of the default <code>XtextReconciler</code>
 * if you have {@link HandlyXtextDocument} bound:
 * </p>
 * <pre>
 * public Class&lt;? extends IReconciler&gt; bindIReconciler() {
 *     return HandlyXtextReconciler.class;
 * }</pre>
 *
 * @noextend This class is not intended to be extended by clients.
 */
// NOTE: This class extends XtextReconciler to retain assignment compatibility.
// The actual implementation is delegated to the inner class InternalReconciler
public class HandlyXtextReconciler
    extends XtextReconciler
{
    private final InternalReconciler delegate;

    @Inject
    public HandlyXtextReconciler(Injector injector)
    {
        super(null);
        delegate = new InternalReconciler(injector);
    }

    @Override
    public void install(ITextViewer textViewer)
    {
        delegate.install(textViewer);
    }

    @Override
    public void uninstall()
    {
        delegate.uninstall();
    }

    @Override
    public IReconcilingStrategy getReconcilingStrategy(String contentType)
    {
        return delegate.getReconcilingStrategy(contentType);
    }

    @Override
    public void setReconcilingStrategy(IReconcilingStrategy strategy)
    {
    }

    @Override
    public void setEditor(XtextEditor editor)
    {
    }

    @Override
    public void setDelay(int delay)
    {
        // delegate is null when this method is called from the super constructor
        if (delegate == null)
            return;
        delegate.setDelay(delay);
    }

    @Override
    public void forceReconcile()
    {
        delegate.forceReconcile();
    }

    @Override
    public boolean shouldSchedule()
    {
        // schedule() should never be called for this job
        throw new AssertionError(); // fail on a schedule request
    }

    private static class InternalReconciler
        extends Job
        implements IReconciler
    {
        private Injector injector;
        private boolean isInstalled;
        private boolean shouldInstallCompletionListener;
        private volatile boolean paused;
        private final AtomicBoolean initialForce = new AtomicBoolean(true);
        private final AtomicBoolean forced = new AtomicBoolean();
        private ITextViewer viewer;
        private final TextInputListener textInputListener =
            new TextInputListener();
        private final DocumentListener documentListener =
            new DocumentListener();
        private int delay = 500;

        public InternalReconciler(Injector injector)
        {
            super("Xtext Editor Reconciler"); //$NON-NLS-1$
            setPriority(Job.SHORT);
            setSystem(true);
            this.injector = injector;
        }

        public void setDelay(int delay)
        {
            this.delay = delay;
        }

        public void forceReconcile()
        {
            if (viewer == null || viewer.getDocument() == null)
                return;
            if (initialForce.compareAndSet(true, false)
                && isHandlyXtextEditorCallbackInstalled())
                return; // ignore call from XtextEditor#createPartControl; see bug 507162 for details
            cancel();
            forced.set(true);
            schedule(delay);
        }

        @Override
        public void install(ITextViewer textViewer)
        {
            if (!isInstalled)
            {
                viewer = textViewer;
                IDocument document0 = viewer.getDocument();
                viewer.addTextInputListener(textInputListener);
                IDocument document = viewer.getDocument();
                if (document instanceof HandlyXtextDocument
                    && document == document0) // a bit of paranoia: document != document0 means the document has changed under us and the text input listener has already handled it
                {
                    ((HandlyXtextDocument)document).addXtextDocumentContentObserver(
                        documentListener);
                }
                if (viewer instanceof ISourceViewerExtension4)
                {
                    ContentAssistantFacade facade =
                        ((ISourceViewerExtension4)viewer).getContentAssistantFacade();
                    if (facade == null)
                        shouldInstallCompletionListener = true;
                    else
                        facade.addCompletionListener(documentListener);
                }
                isInstalled = true;
            }
        }

        @Override
        public void uninstall()
        {
            if (isInstalled)
            {
                viewer.removeTextInputListener(textInputListener);
                IDocument document = viewer.getDocument();
                if (document instanceof HandlyXtextDocument)
                {
                    ((HandlyXtextDocument)document).removeXtextDocumentContentObserver(
                        documentListener);
                }
                if (viewer instanceof ISourceViewerExtension4)
                {
                    ContentAssistantFacade facade =
                        ((ISourceViewerExtension4)viewer).getContentAssistantFacade();
                    facade.removeCompletionListener(documentListener);
                }
                cancel();
                isInstalled = false;
            }
        }

        @Override
        public IReconcilingStrategy getReconcilingStrategy(String contentType)
        {
            return null; // it's safe to return no strategy
        }

        @Override
        public boolean belongsTo(Object family)
        {
            return XtextReconciler.class.getName().equals(family);
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor)
        {
            if (monitor.isCanceled() || paused)
                return Status.CANCEL_STATUS;

            IDocument document = viewer.getDocument();
            if (document instanceof HandlyXtextDocument)
            {
                HandlyXtextDocument doc = (HandlyXtextDocument)document;
                final boolean forced = this.forced.compareAndSet(true, false);
                if (forced || doc.needsReconciling())
                {
                    try
                    {
                        doc.reconcile(forced, monitor);
                    }
                    catch (OperationCanceledException e)
                    {
                        if (forced)
                            this.forced.set(true);
                        throw e;
                    }
                    catch (NoXtextResourceException e)
                    {
                        // document has no resource -- nothing to do
                    }
                }
            }
            return Status.OK_STATUS;
        }

        private void handleDocumentChanged(DocumentEvent event)
        {
            cancel();
            schedule(delay);
        }

        private void pause()
        {
            paused = true;
        }

        private void resume()
        {
            paused = false;
            schedule(delay);
        }

        private boolean isHandlyXtextEditorCallbackInstalled()
        {
            if (injector == null)
                return false;
            List<Binding<IXtextEditorCallback>> bindings =
                injector.findBindingsByType(TypeLiteral.get(
                    IXtextEditorCallback.class));
            for (Binding<IXtextEditorCallback> binding : bindings)
            {
                IXtextEditorCallback callback;
                try
                {
                    callback = binding.getProvider().get();
                }
                catch (Exception e)
                {
                    continue;
                }
                if (callback instanceof HandlyXtextEditorCallback)
                    return true;
            }
            return false;
        }

        private class DocumentListener
            implements IXtextDocumentContentObserver, ICompletionListener
        {
            private final IPositionUpdater templatePositionUpdater =
                new TemplatePositionUpdater(XTEXT_TEMPLATE_POS_CATEGORY);

            private volatile boolean sessionStarted = false;

            @Override
            public void documentAboutToBeChanged(DocumentEvent event)
            {
            }

            @Override
            public void documentChanged(DocumentEvent event)
            {
                handleDocumentChanged(event);
            }

            @Override
            public boolean performNecessaryUpdates(Processor processor)
            {
                // Note: this method is always called with the doc's readLock held

                boolean hadUpdates = false;
                IDocument document = viewer.getDocument();
                if (document instanceof HandlyXtextDocument && !paused)
                {
                    HandlyXtextDocument doc = (HandlyXtextDocument)document;
                    try
                    {
                        if (doc.needsReconciling()) // this check is required to avoid constant rescheduling of ValidationJob
                            hadUpdates = doc.reconcile(processor);
                    }
                    catch (Throwable e)
                    {
                        Activator.logError("Error while forcing reconciliation", //$NON-NLS-1$
                            e);
                    }
                }
                if (sessionStarted && !paused)
                {
                    pause();
                }
                return hadUpdates;
            }

            @Override
            public boolean hasPendingUpdates()
            {
                IDocument document = viewer.getDocument();
                if (document instanceof HandlyXtextDocument)
                    return ((HandlyXtextDocument)document).needsReconciling();
                return false;
            }

            @Override
            public void assistSessionStarted(ContentAssistEvent event)
            {
                IDocument document = viewer.getDocument();
                document.addPositionCategory(XTEXT_TEMPLATE_POS_CATEGORY);
                document.addPositionUpdater(templatePositionUpdater);
                sessionStarted = true;
            }

            @Override
            public void assistSessionEnded(ContentAssistEvent event)
            {
                sessionStarted = false;
                IDocument document = viewer.getDocument();
                document.removePositionUpdater(templatePositionUpdater);
                try
                {
                    document.removePositionCategory(
                        XTEXT_TEMPLATE_POS_CATEGORY);
                }
                catch (BadPositionCategoryException e)
                {
                }
                resume();
            }

            @Override
            public void selectionChanged(ICompletionProposal proposal,
                boolean smartToggle)
            {
            }
        }

        private class TextInputListener
            implements ITextInputListener
        {
            @Override
            public void inputDocumentAboutToBeChanged(IDocument oldInput,
                IDocument newInput)
            {
                if (oldInput instanceof HandlyXtextDocument)
                {
                    ((HandlyXtextDocument)oldInput).removeXtextDocumentContentObserver(
                        documentListener);
                    cancel();
                }
            }

            @Override
            public void inputDocumentChanged(IDocument oldInput,
                IDocument newInput)
            {
                if (newInput instanceof HandlyXtextDocument)
                {
                    ((HandlyXtextDocument)newInput).addXtextDocumentContentObserver(
                        documentListener);
                    schedule(delay);
                }

                if (shouldInstallCompletionListener)
                {
                    ContentAssistantFacade facade =
                        ((ISourceViewerExtension4)viewer).getContentAssistantFacade();
                    if (facade != null)
                        facade.addCompletionListener(documentListener);
                    shouldInstallCompletionListener = false;
                }
            }
        }
    }
}
