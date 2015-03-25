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

import java.util.HashMap;

import org.eclipse.handly.examples.javamodel.ICompilationUnit;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.examples.javamodel.IJavaProject;
import org.eclipse.handly.examples.javamodel.IPackageFragment;
import org.eclipse.handly.examples.javamodel.IPackageFragmentRoot;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.handly.model.impl.ElementCache;
import org.eclipse.handly.model.impl.IBodyCache;

/**
 * The Java model cache.
 */
class JavaModelCache
    implements IBodyCache
{
    private static final int DEFAULT_PROJECT_SIZE = 5;
    private static final int DEFAULT_ROOT_SIZE = 50;
    private static final int DEFAULT_PKG_SIZE = 500;
    private static final int DEFAULT_FILE_SIZE = 250;
    private static final int DEFAULT_CHILDREN_SIZE = DEFAULT_FILE_SIZE * 20; // average 20 children per file

    // The memory ratio that should be applied to the above constants.
    private final double memoryRatio = getMemoryRatio();

    private Body modelBody; // Java model element's body
    private HashMap<IHandle, Body> projectCache; // cache of open Java projects
    private ElementCache rootCache; // cache of open package fragment roots
    private ElementCache pkgCache; // cache of open package fragments
    private ElementCache fileCache; // cache of open Java files
    private HashMap<IHandle, Body> childrenCache; // cache of children of open Foo files

    public JavaModelCache()
    {
        // set the size of the caches as a function of the maximum amount of memory available
        projectCache = new HashMap<IHandle, Body>(DEFAULT_PROJECT_SIZE);
        rootCache = new ElementCache((int)(DEFAULT_ROOT_SIZE * memoryRatio));
        pkgCache = new ElementCache((int)(DEFAULT_PKG_SIZE * memoryRatio));
        fileCache = new ElementCache((int)(DEFAULT_FILE_SIZE * memoryRatio));
        childrenCache =
            new HashMap<IHandle, Body>(
                (int)(DEFAULT_CHILDREN_SIZE * memoryRatio));
    }

    @Override
    public Body get(IHandle handle)
    {
        if (handle instanceof IJavaModel)
            return modelBody;
        else if (handle instanceof IJavaProject)
            return projectCache.get(handle);
        else if (handle instanceof IPackageFragmentRoot)
            return rootCache.get(handle);
        else if (handle instanceof IPackageFragment)
            return pkgCache.get(handle);
        else if (handle instanceof ICompilationUnit)
            return fileCache.get(handle);
        else
            return childrenCache.get(handle);
    }

    @Override
    public Body peek(IHandle handle)
    {
        if (handle instanceof IJavaModel)
            return modelBody;
        else if (handle instanceof IJavaProject)
            return projectCache.get(handle);
        else if (handle instanceof IPackageFragmentRoot)
            return rootCache.peek(handle);
        else if (handle instanceof IPackageFragment)
            return pkgCache.peek(handle);
        else if (handle instanceof ICompilationUnit)
            return fileCache.peek(handle);
        else
            return childrenCache.get(handle);
    }

    @Override
    public void put(IHandle handle, Body body)
    {
        if (handle instanceof IJavaModel)
            modelBody = body;
        else if (handle instanceof IJavaProject)
        {
            projectCache.put(handle, body);
            rootCache.ensureSpaceLimit(body, handle);
        }
        else if (handle instanceof IPackageFragmentRoot)
        {
            rootCache.put(handle, body);
            pkgCache.ensureSpaceLimit(body, handle);
        }
        else if (handle instanceof IPackageFragment)
        {
            pkgCache.put(handle, body);
            fileCache.ensureSpaceLimit(body, handle);
        }
        else if (handle instanceof ICompilationUnit)
            fileCache.put(handle, body);
        else
            childrenCache.put(handle, body);
    }

    @Override
    public void remove(IHandle handle)
    {
        if (handle instanceof IJavaModel)
            modelBody = null;
        else if (handle instanceof IJavaProject)
        {
            projectCache.remove(handle);
            rootCache.resetSpaceLimit((int)(DEFAULT_ROOT_SIZE * memoryRatio),
                handle);
        }
        else if (handle instanceof IPackageFragmentRoot)
        {
            rootCache.remove(handle);
            pkgCache.resetSpaceLimit((int)(DEFAULT_PKG_SIZE * memoryRatio),
                handle);
        }
        else if (handle instanceof IPackageFragment)
        {
            pkgCache.remove(handle);
            fileCache.resetSpaceLimit((int)(DEFAULT_FILE_SIZE * memoryRatio),
                handle);
        }
        else if (handle instanceof ICompilationUnit)
            fileCache.remove(handle);
        else
            childrenCache.remove(handle);
    }

    private double getMemoryRatio()
    {
        long maxMemory = Runtime.getRuntime().maxMemory();
        // if max memory is infinite, set the ratio to 4d 
        // which corresponds to the 256MB that Eclipse defaults to
        // (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=111299)
        return maxMemory == Long.MAX_VALUE ? 4d : ((double)maxMemory)
            / (64 * 0x100000); // 64MB is the base memory for most JVM
    }
}
