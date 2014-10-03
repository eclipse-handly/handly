/*******************************************************************************
 * Copyright (c) 2014 1C LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.ui;

import org.eclipse.handly.util.UiSynchronizer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * Implements {@link UiSynchronizer} on top of a {@link Display}.
 */
public class DisplaySynchronizer
    extends UiSynchronizer
{
    private final Display display;

    public DisplaySynchronizer()
    {
        this(PlatformUI.getWorkbench().getDisplay());
    }

    /**
     * Creates a new synchronizer.
     *
     * @param display
     *            the display the synchronizer will use (not <code>null</code>)
     */
    public DisplaySynchronizer(final Display display)
    {
        if ((this.display = display) == null)
        {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Thread getThread()
    {
        return display.getThread();
    }

    @Override
    public void asyncExec(final Runnable runnable)
    {
        display.asyncExec(runnable);
    }

    @Override
    public void syncExec(final Runnable runnable)
    {
        display.syncExec(runnable);
    }
}
