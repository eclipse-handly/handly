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
package org.eclipse.handly.internal.examples.basic.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.handly.examples.basic.ui.model.FooModelCore;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.ui.IInputElementProvider;
import org.eclipse.ui.IEditorInput;

/**
 * Implementation of {@link IInputElementProvider} to be bound
 * in the Xtext UI module for the language.
 */
public class FooInputElementProvider
    implements IInputElementProvider
{
    @Override
    public IHandle getElement(IEditorInput input)
    {
        if (input == null)
            return null;
        IFile file = (IFile)input.getAdapter(IFile.class);
        return FooModelCore.create(file);
    }
}
