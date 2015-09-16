/*******************************************************************************
 * Copyright (c) 2015 1C LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *     Ondrej Ilcik (Codasip) - adaptation (adapted from
 *         org.eclipse.handly.internal.examples.basic.ui.Activator)
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.handly.internal.examples.javamodel.ui.preferences.MembersOrderPreferenceCache;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator
    extends AbstractUIPlugin
{
    public static final String PLUGIN_ID =
        "org.eclipse.handly.examples.javamodel.ui"; //$NON-NLS-1$

    private static Activator plugin;
    private ImageDescriptorRegistry imageDescriptorRegistry;
    private MembersOrderPreferenceCache membersOrderPreferenceCache;

    public static void log(IStatus status)
    {
        plugin.getLog().log(status);
    }

    public static IStatus createErrorStatus(String msg, Throwable e)
    {
        return new Status(IStatus.ERROR, PLUGIN_ID, 0, msg, e);
    }

    @Override
    public void start(BundleContext context) throws Exception
    {
        super.start(context);
        plugin = this;

        imageDescriptorRegistry = new ImageDescriptorRegistry();
        membersOrderPreferenceCache = new MembersOrderPreferenceCache();
        IPreferenceStore javaPrefs = getJavaPreferenceStore();
        membersOrderPreferenceCache.install(javaPrefs);
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        if (membersOrderPreferenceCache != null)
        {
            membersOrderPreferenceCache.dispose();
            membersOrderPreferenceCache = null;
        }

        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault()
    {
        return plugin;
    }

    /**
     * Gets image descriptor registry for this bundle.
     * 
     * @return registry
     */
    public static ImageDescriptorRegistry getImageDescriptorRegistry()
    {
        return getDefault().imageDescriptorRegistry;
    }

    /**
     * Gets cache preferences for Java element member ordering.
     * 
     * @return preferences
     */
    public static MembersOrderPreferenceCache getMemberOrderPreferenceCache()
    {
        return getDefault().membersOrderPreferenceCache;
    }

    /**
     * Gets preference store for Java plugin.
     * 
     * @return store
     */
    private IPreferenceStore getJavaPreferenceStore()
    {
        return new ScopedPreferenceStore(InstanceScope.INSTANCE,
            JavaUI.ID_PLUGIN);
    }
}
