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
package org.eclipse.handly.ui.quickoutline;

import org.eclipse.handly.internal.ui.SourceElementUtil;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.ISourceElement;
import org.eclipse.handly.model.adapter.IContentAdapter;
import org.eclipse.handly.model.adapter.IContentAdapterProvider;
import org.eclipse.handly.model.adapter.NullContentAdapter;
import org.eclipse.handly.ui.IElementForEditorInputFactory;
import org.eclipse.handly.util.TextRange;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;

/**
 * A partial implementation of Handly-based outline popup. This class
 * is intended to be used with a text-based host such as text editor.
 */
public abstract class HandlyOutlinePopup
    extends FilteringOutlinePopup
    implements IContentAdapterProvider
{
    private IElementForEditorInputFactory inputElementFactory;

    /**
     * Sets the input element factory.
     *
     * @param factory the input element factory (not <code>null</code>)
     * @see IElementForEditorInputFactory
     */
    public void setInputElementFactory(IElementForEditorInputFactory factory)
    {
        if (factory == null)
            throw new IllegalArgumentException();
        inputElementFactory = factory;
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
        IHandle inputElement = getInputElementFactory().getElement(
            getHost().getEditorInput());
        return getContentAdapter().getCorrespondingElement(inputElement);
    }

    @Override
    protected Object getCorrespondingElement(ISelection hostSelection)
    {
        if (!(hostSelection instanceof ITextSelection))
            return null;
        IHandle input = getContentAdapter().getHandle(
            getTreeViewer().getInput());
        if (!(input instanceof ISourceElement))
            return null;
        return getContentAdapter().getCorrespondingElement(
            SourceElementUtil.getElementAt((ISourceElement)input,
                ((ITextSelection)hostSelection).getOffset()));
    }

    @Override
    protected boolean revealInHost(Object outlineElement)
    {
        IHandle element = getContentAdapter().getHandle(outlineElement);
        if (!(element instanceof ISourceElement))
            return false;
        ISourceElement sourceElement = (ISourceElement)element;
        if (!isInHost(sourceElement))
            return false;
        TextRange identifyingRange = SourceElementUtil.getIdentifyingRange(
            sourceElement);
        if (identifyingRange == null)
            return false;
        TextSelection textSelection = new TextSelection(
            identifyingRange.getOffset(), identifyingRange.getLength());
        getHost().getSelectionProvider().setSelection(textSelection);
        return true;
    }

    /**
     * Returns whether the given element is contained in the host
     * of this outline popup.
     *
     * @param element may be <code>null</code>
     * @return <code>true</code> if the element is contained in the host;
     *  <code>false</code> otherwise
     */
    protected boolean isInHost(IHandle element)
    {
        IHandle inputElement = getInputElementFactory().getElement(
            getHost().getEditorInput());
        while (element != null)
        {
            if (element.equals(inputElement))
                return true;
            element = element.getParent();
        }
        return false;
    }

    protected IElementForEditorInputFactory getInputElementFactory()
    {
        return inputElementFactory;
    }
}