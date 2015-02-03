/*******************************************************************************
 * Copyright (c) 2015 1C LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator
    extends Plugin
{
    public static final String PLUGIN_ID =
        "org.eclipse.handly.examples.javamodel"; //$NON-NLS-1$

    private static Activator plugin;

    public static Activator getDefault()
    {
        return plugin;
    }

    public static void log(IStatus status)
    {
        plugin.getLog().log(status);
    }

    public static IStatus createErrorStatus(String msg, Throwable e)
    {
        return new Status(IStatus.ERROR, PLUGIN_ID, 0, msg, e);
    }

    public Activator()
    {
        plugin = this;
    }

    @Override
    public void start(BundleContext context) throws Exception
    {
        super.start(context);
        JavaModelManager.INSTANCE.startup();
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        try
        {
            JavaModelManager.INSTANCE.shutdown();
        }
        finally
        {
            plugin = null;
            super.stop(context);
        }
    }
}
