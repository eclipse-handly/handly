/*******************************************************************************
 * Copyright (c) 2015 1C-Soft LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.ui.outline;

import org.eclipse.handly.model.IElementChangeEvent;
import org.eclipse.handly.model.IElementChangeListener;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.adapter.IContentAdapter;
import org.eclipse.handly.model.adapter.IContentAdapterProvider;
import org.eclipse.handly.model.adapter.NullContentAdapter;
import org.eclipse.handly.ui.IInputElementProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.OpenAndLinkWithEditorHelper;
import org.eclipse.ui.PlatformUI;

/**
 * A partial implementation of Handly-based outline page pre-wired with
 * contributions that are common to a typical outline. In particular, it
 * provides support for linking with editor and lexical sorting functionality.
 */
public abstract class HandlyOutlinePage
    extends CommonOutlinePage
    implements IContentAdapterProvider
{
    private IInputElementProvider inputElementProvider;

    /**
     * Sets the input element provider.
     *
     * @param provider the input element provider (not <code>null</code>)
     * @see IInputElementProvider
     */
    public void setInputElementProvider(IInputElementProvider provider)
    {
        if (provider == null)
            throw new IllegalArgumentException();
        inputElementProvider = provider;
    }

    @Override
    public void init(IEditorPart editor)
    {
        super.init(editor);
        addOutlineContributions();
    }

    /**
     * Returns the content adapter that defines a mapping between elements
     * of a Handly based model and the outline's content.
     * <p>
     * Default implementation returns a {@link NullContentAdapter}.
     * Subclasses may override.
     * </p>
     *
     * @return {@link IContentAdapter} (never <code>null</code>)
     */
    @Override
    public IContentAdapter getContentAdapter()
    {
        return NullContentAdapter.INSTANCE;
    }

    @Override
    protected Object computeInput()
    {
        IHandle inputElement = getInputElementProvider().getElement(
            getEditor().getEditorInput());
        return getContentAdapter().getCorrespondingElement(inputElement);
    }

    /**
     * Hook to add contributions to this outline page.
     * <p>
     * Default implementation adds contributions that are common to
     * a typical outline. Subclasses may extend this method.
     * </p>
     */
    protected void addOutlineContributions()
    {
        addOutlineContribution(new ElementChangeListenerContribution()
        {
            @Override
            protected void addElementChangeListener(
                IElementChangeListener listener)
            {
                HandlyOutlinePage.this.addElementChangeListener(listener);
            }

            @Override
            protected void removeElementChangeListener(
                IElementChangeListener listener)
            {
                HandlyOutlinePage.this.removeElementChangeListener(listener);
            }

            @Override
            protected void elementChanged(IElementChangeEvent event)
            {
                HandlyOutlinePage.this.elementChanged(event);
            }
        });
        addCollapseAllSupport();
        addLinkWithEditorSupport();
        addSortingSupport();
    }

    /**
     * Adds collapse-all support. Subclasses may override this method.
     */
    protected void addCollapseAllSupport()
    {
        addOutlineContribution(new CollapseAllActionContribution());
    }

    /**
     * Adds link-with-editor support. Subclasses may override this method.
     */
    protected void addLinkWithEditorSupport()
    {
        addOutlineContribution(new LinkWithEditorActionContribution());
        addOutlineContribution(new LinkWithEditorContribution()
        {
            @Override
            protected OpenAndLinkWithEditorHelper getLinkingHelper()
            {
                return new SourceElementLinkingHelper(getOutlinePage(),
                    getInputElementProvider());
            }
        });
    }

    /**
     * Adds sorting support. Subclasses may override this method.
     */
    protected void addSortingSupport()
    {
        addOutlineContribution(new LexicalSortActionContribution());
        addOutlineContribution(new LexicalSortContribution());
    }

    /**
     * Registers the given element change listener with the underlying model.
     *
     * @param listener never <code>null</code>
     */
    protected abstract void addElementChangeListener(
        IElementChangeListener listener);

    /**
     * Removes the given element change listener from the underlying model.
     *
     * @param listener never <code>null</code>
     */
    protected abstract void removeElementChangeListener(
        IElementChangeListener listener);

    /**
     * Notifies that the outline page is affected in some way
     * by the given element change event.
     * <p>
     * <b>Note</b> This method may be called in any thread.
     * The event object (and the delta within it) is valid only
     * for the duration of the invocation of this method.
     * </p>
     * <p>
     * Default implementation schedules {@link #refresh() refresh}
     * of this page in the UI thread.
     * </p>
     *
     * @param event never <code>null</code>
     */
    protected void elementChanged(IElementChangeEvent event)
    {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable()
        {
            public void run()
            {
                if (!getTreeViewer().getControl().isDisposed())
                {
                    refresh();
                }
            }
        });
    }

    protected IInputElementProvider getInputElementProvider()
    {
        return inputElementProvider;
    }
}
