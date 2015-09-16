/*******************************************************************************
 * Copyright (c) 2015 Codasip Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ondrej Ilcik (Codasip) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel.ui.workingset;

import org.eclipse.handly.examples.javamodel.JavaModelCore;
import org.eclipse.handly.model.IElementChangeListener;
import org.eclipse.handly.ui.workingset.AbstractWorkingSetUpdater;

/**
 * Java working set updater.
 */
public class JavaWorkingSetUpdater
    extends AbstractWorkingSetUpdater
{
    @Override
    protected void addElementChangeListener(IElementChangeListener listener)
    {
        JavaModelCore.getJavaModel().addElementChangeListener(listener);
    }

    @Override
    protected void removeElementChangeListener(IElementChangeListener listener)
    {
        JavaModelCore.getJavaModel().removeElementChangeListener(listener);
    }
}
