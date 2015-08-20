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
package org.eclipse.handly.xtext.ui.quickoutline;

import org.eclipse.handly.ui.IInputElementProvider;
import org.eclipse.handly.ui.quickoutline.HandlyOutlinePopup;

import com.google.inject.Inject;

/**
 * A partial implementation of Handly-based outline popup for Xtext editor.
 * <p>
 * Note that this class relies on a language-specific implementation of
 * {@link IInputElementProvider} being available through injection.
 * </p>
 */
public abstract class HandlyXtextOutlinePopup
    extends HandlyOutlinePopup
{
    @Inject
    public void setInputElementProvider(IInputElementProvider provider)
    {
        super.setInputElementProvider(provider);
    }
}
