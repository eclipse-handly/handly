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
package org.eclipse.handly.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;

/**
 * Allows to execute runnables in the user-interface thread.
 *
 * @noextend This class is not intended to be extended by clients.
 */
public abstract class UiSynchronizer
{
    private static UiSynchronizer instance;

    public static UiSynchronizer getInstance()
    {
        if (instance == null)
        {
            instance =
                getSingletonExtension("org.eclipse.handly.uisynchronizer",
                    UiSynchronizer.class);
        }
        return instance;
    }

    /**
     * Returns the user-interface thread the synchronizer uses to execute
     * runnables.
     *
     * @return the user-interface thread (not <code>null</code>)
     */
    public abstract Thread getThread();

    /**
     * Causes the <code>run()</code> method of the runnable to be invoked by the
     * user-interface thread at the next reasonable opportunity. The caller of
     * this method continues to run in parallel, and is not notified when the
     * runnable has completed.
     *
     * @param runnable
     *            code to run on the user-interface thread (not
     *            <code>null</code>)
     */
    public abstract void asyncExec(Runnable runnable);

    /**
     * Causes the <code>run()</code> method of the runnable to be invoked by the
     * user-interface thread at the next reasonable opportunity. The thread
     * which calls this method is suspended until the runnable completes.
     *
     * @param runnable
     *            code to run on the user-interface thread (not
     *            <code>null</code>)
     */
    public abstract void syncExec(Runnable runnable);

    private static <T> T getSingletonExtension(final String id,
        final Class<? extends T> clazz)
    {
        final IExtensionRegistry reg = RegistryFactory.getRegistry();
        final IConfigurationElement[] elements =
            reg.getConfigurationElementsFor(id);

        for (final IConfigurationElement element : elements)
        {
            try
            {
                final Object object =
                    element.createExecutableExtension("class");
                if (clazz.isInstance(object))
                {
                    return clazz.cast(object);
                }
            }
            catch (final CoreException e)
            {
                e.printStackTrace();
            }
        }
        return null;
    }

}
