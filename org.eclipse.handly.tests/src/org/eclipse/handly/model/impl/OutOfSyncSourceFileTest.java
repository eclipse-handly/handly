/*******************************************************************************
 * Copyright (c) 2015, 2017 1C-Soft LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.model.impl;

import static org.eclipse.handly.context.Contexts.of;
import static org.eclipse.handly.model.impl.IElementImplSupport.NEW_ELEMENTS;

import java.io.File;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.handly.junit.WorkspaceTestCase;

/**
 * Regression test for bug 470336 -
 * AssertionError when building structure for out-of-sync source file.
 */
public class OutOfSyncSourceFileTest
    extends WorkspaceTestCase
{
    private IEclipsePreferences preferences;
    private boolean autoRefresh;
    private boolean lightweightAutoRefresh;
    private IFile file;
    private File localFile;
    private SimpleSourceFile sourceFile;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        preferences = InstanceScope.INSTANCE.getNode(
            ResourcesPlugin.PI_RESOURCES);
        autoRefresh = preferences.getBoolean(ResourcesPlugin.PREF_AUTO_REFRESH,
            false);
        lightweightAutoRefresh = preferences.getBoolean(
            ResourcesPlugin.PREF_LIGHTWEIGHT_AUTO_REFRESH, true);
        preferences.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH, false);
        preferences.putBoolean(ResourcesPlugin.PREF_LIGHTWEIGHT_AUTO_REFRESH,
            false);
        file = setUpProject("Test001").getFile("file.txt");
        localFile = file.getLocation().toFile();
        sourceFile = new SimpleSourceFile(null, file.getName(), file,
            new SimpleModelManager());
    }

    @Override
    protected void tearDown() throws Exception
    {
        if (preferences != null)
        {
            preferences.putBoolean(ResourcesPlugin.PREF_AUTO_REFRESH,
                autoRefresh);
            preferences.putBoolean(
                ResourcesPlugin.PREF_LIGHTWEIGHT_AUTO_REFRESH,
                lightweightAutoRefresh);
        }
        super.tearDown();
    }

    public void testInSync() throws Exception
    {
        sourceFile.hBuildStructure(of(NEW_ELEMENTS, new HashMap<>()),
            new NullProgressMonitor());
    }

    public void testOutOfSync() throws Exception
    {
        assertTrue(localFile.setLastModified(localFile.lastModified() + 1000));
        sourceFile.hBuildStructure(of(NEW_ELEMENTS, new HashMap<>()),
            new NullProgressMonitor());
    }
}
