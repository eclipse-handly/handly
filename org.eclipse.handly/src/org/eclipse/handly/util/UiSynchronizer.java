/*******************************************************************************
 * Copyright (c) 2014, 2016 1C-Soft LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.util;

import java.util.concurrent.ExecutionException;

import org.eclipse.core.runtime.Platform;
import org.eclipse.handly.internal.DisplaySynchronizer;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

/**
 * Allows to execute runnables in the user-interface thread.
 *
 * @noextend This class is not intended to be extended by clients.
 */
public abstract class UiSynchronizer
{
    /**
     * A default instance of the synchronizer, or <code>null</code>
     * if not available at this time (e.g. when running headless).
     */
    public static UiSynchronizer getDefault()
    {
        Bundle b = Platform.getBundle("org.eclipse.ui"); //$NON-NLS-1$
        if (b != null && b.getState() == Bundle.ACTIVE
            && PlatformUI.isWorkbenchRunning())
        {
            return new DisplaySynchronizer(
                PlatformUI.getWorkbench().getDisplay());
        }
        return null;
    }

    /**
     * Returns the user-interface thread the synchronizer uses to execute
     * runnables.
     *
     * @return the user-interface thread (not <code>null</code>)
     * @throws IllegalStateException if the synchronizer can no longer be
     *  accessed (e.g. the underlying display has been disposed)
     */
    public abstract Thread getThread();

    /**
     * Causes the <code>run()</code> method of the runnable to
     * be invoked by the user-interface thread at the next
     * reasonable opportunity. The caller of this method continues
     * to run in parallel, and is not notified when the
     * runnable has completed.
     *
     * @param runnable code to run on the user-interface thread
     *  (not <code>null</code>)
     * @throws IllegalStateException if the synchronizer can no longer be
     *  accessed (e.g. the underlying display has been disposed)
     */
    public abstract void asyncExec(Runnable runnable);

    /**
     * Causes the <code>run()</code> method of the runnable to
     * be invoked by the user-interface thread at the next
     * reasonable opportunity. The thread which calls this method
     * is suspended until the runnable completes.
     *
     * @param runnable code to run on the user-interface thread
     *  (not <code>null</code>)
     * @throws IllegalStateException if the synchronizer can no longer be
     *  accessed (e.g. the underlying display has been disposed)
     * @throws ExecutionException if an exception occurred when executing
     *  the runnable
     */
    public abstract void syncExec(Runnable runnable) throws ExecutionException;
}
