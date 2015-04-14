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

import org.eclipse.handly.ui.quickoutline.OutlinePopup;
import org.eclipse.handly.ui.quickoutline.OutlinePopupHandler;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * A simple handler that opens Handly-based outline popup for Xtext editor.
 * <p>
 * Note that this class relies on a language-specific implementation of
 * <code>OutlinePopup</code> being available through injection.
 * </p>
 */
public class HandlyXtextOutlinePopupHandler
    extends OutlinePopupHandler
{
    @Inject
    private Provider<OutlinePopup> outlinePopupProvider;

    @Override
    protected OutlinePopup createOutlinePopup()
    {
        return outlinePopupProvider.get();
    }
}
