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
package org.eclipse.handly.ui;

import org.eclipse.handly.model.IHandle;
import org.eclipse.ui.IEditorInput;

/**
 * Given an editor input, the input element provider returns an appropriate
 * <code>IHandle</code>. This interface may be implemented by clients.
 */
public interface IInputElementProvider
{
    /**
     * Returns the input element for the given editor input.
     *
     * @param input the editor input (may be <code>null</code>)
     * @return the input element, or <code>null</code> if none
     */
    IHandle getElement(IEditorInput input);
}
