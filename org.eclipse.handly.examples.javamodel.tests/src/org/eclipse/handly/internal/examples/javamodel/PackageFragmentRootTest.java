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

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.handly.examples.javamodel.IJavaProject;
import org.eclipse.handly.examples.javamodel.IPackageFragment;
import org.eclipse.handly.examples.javamodel.IPackageFragmentRoot;
import org.eclipse.handly.examples.javamodel.JavaModelCore;
import org.eclipse.handly.junit.WorkspaceTestCase;

/**
 * <code>PackageFragmentRoot</code> tests.
 */
public class PackageFragmentRootTest
    extends WorkspaceTestCase
{
    private IPackageFragmentRoot root;
    private IPackageFragment defaultPkg;
    private IPackageFragment fooPkg;
    private IPackageFragment foobarPkg;
    private IFolder metainfFolder;
    private IFile abcFile;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        IProject project = setUpProject("Test005");
        IJavaProject javaProject = JavaModelCore.create(project);
        root = javaProject.getPackageFragmentRoot(project.getFolder("src"));
        defaultPkg = root.getPackageFragment("");
        fooPkg = root.getPackageFragment("foo");
        foobarPkg = root.getPackageFragment("foo.bar");
        IFolder rootFolder = (IFolder)root.getResource();
        metainfFolder = rootFolder.getFolder("META-INF");
        abcFile = rootFolder.getFile("abc");
    }

    public void test001() throws Exception
    {
        IPackageFragment[] packageFragments = root.getPackageFragments();
        assertEquals(3, packageFragments.length);
        assertTrue(Arrays.asList(packageFragments).contains(defaultPkg));
        assertTrue(defaultPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(fooPkg));
        assertTrue(fooPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(foobarPkg));
        assertTrue(foobarPkg.exists());

        Object[] nonJavaResources = root.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));

        // delete foo.bar
        foobarPkg.getResource().delete(true, null);

        packageFragments = root.getPackageFragments();
        assertEquals(2, packageFragments.length);
        assertTrue(Arrays.asList(packageFragments).contains(defaultPkg));
        assertTrue(defaultPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(fooPkg));
        assertTrue(fooPkg.exists());
        assertFalse(foobarPkg.exists());

        nonJavaResources = root.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));

        // (re-)create foo.bar
        ((IFolder)foobarPkg.getResource()).create(true, true, null);

        packageFragments = root.getPackageFragments();
        assertEquals(3, packageFragments.length);
        assertTrue(Arrays.asList(packageFragments).contains(defaultPkg));
        assertTrue(defaultPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(fooPkg));
        assertTrue(fooPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(foobarPkg));
        assertTrue(foobarPkg.exists());

        nonJavaResources = root.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));

        // delete META-INF and abc
        metainfFolder.delete(true, null);
        abcFile.delete(true, null);

        packageFragments = root.getPackageFragments();
        assertEquals(3, packageFragments.length);
        assertTrue(Arrays.asList(packageFragments).contains(defaultPkg));
        assertTrue(defaultPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(fooPkg));
        assertTrue(fooPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(foobarPkg));
        assertTrue(foobarPkg.exists());

        nonJavaResources = root.getNonJavaResources();
        assertEquals(0, nonJavaResources.length);

        // (re-)create META-INF and abc
        metainfFolder.create(true, true, null);
        abcFile.create(new ByteArrayInputStream(new byte[0]), true, null);

        packageFragments = root.getPackageFragments();
        assertEquals(3, packageFragments.length);
        assertTrue(Arrays.asList(packageFragments).contains(defaultPkg));
        assertTrue(defaultPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(fooPkg));
        assertTrue(fooPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(foobarPkg));
        assertTrue(foobarPkg.exists());

        nonJavaResources = root.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));
    }

    public void test002() throws Exception
    {
        IPackageFragment[] packageFragments = root.getPackageFragments();
        assertEquals(3, packageFragments.length);
        assertTrue(Arrays.asList(packageFragments).contains(defaultPkg));
        assertTrue(defaultPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(fooPkg));
        assertTrue(fooPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(foobarPkg));
        assertTrue(foobarPkg.exists());

        Object[] nonJavaResources = root.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));

        // move foo
        IPackageFragment foo2Pkg = root.getPackageFragment("foo2");
        assertFalse(foo2Pkg.exists());
        fooPkg.getResource().move(foo2Pkg.getResource().getFullPath(), true,
            null);
        assertFalse(fooPkg.exists());
        assertFalse(foobarPkg.exists());

        packageFragments = root.getPackageFragments();
        assertEquals(3, packageFragments.length);
        assertTrue(Arrays.asList(packageFragments).contains(defaultPkg));
        assertTrue(defaultPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(foo2Pkg));
        assertTrue(foo2Pkg.exists());
        IPackageFragment foo2barPkg = root.getPackageFragment("foo2.bar");
        assertTrue(Arrays.asList(packageFragments).contains(foo2barPkg));
        assertTrue(foo2barPkg.exists());

        nonJavaResources = root.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));
    }

    public void test003() throws Exception
    {
        IPackageFragment[] packageFragments = root.getPackageFragments();
        assertEquals(3, packageFragments.length);
        assertTrue(Arrays.asList(packageFragments).contains(defaultPkg));
        assertTrue(defaultPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(fooPkg));
        assertTrue(fooPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(foobarPkg));
        assertTrue(foobarPkg.exists());

        Object[] nonJavaResources = root.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));

        // move foo to a non-Java resource
        IFolder rootFolder = (IFolder)root.getResource();
        IFolder osgiinfFolder = rootFolder.getFolder("OSGI-INF");
        fooPkg.getResource().move(osgiinfFolder.getFullPath(), true, null);
        assertFalse(fooPkg.exists());
        assertFalse(foobarPkg.exists());

        packageFragments = root.getPackageFragments();
        assertEquals(1, packageFragments.length);
        assertTrue(Arrays.asList(packageFragments).contains(defaultPkg));
        assertTrue(defaultPkg.exists());

        nonJavaResources = root.getNonJavaResources();
        assertEquals(3, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(osgiinfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));

        // move it back
        osgiinfFolder.move(fooPkg.getResource().getFullPath(), true, null);

        packageFragments = root.getPackageFragments();
        assertEquals(3, packageFragments.length);
        assertTrue(Arrays.asList(packageFragments).contains(defaultPkg));
        assertTrue(defaultPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(fooPkg));
        assertTrue(fooPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(foobarPkg));
        assertTrue(foobarPkg.exists());

        nonJavaResources = root.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));
    }

    public void test004() throws Exception
    {
        IPackageFragment[] packageFragments = root.getPackageFragments();
        assertEquals(3, packageFragments.length);
        assertTrue(Arrays.asList(packageFragments).contains(defaultPkg));
        assertTrue(defaultPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(fooPkg));
        assertTrue(fooPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(foobarPkg));
        assertTrue(foobarPkg.exists());

        Object[] nonJavaResources = root.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));

        // move META-INF, abc and A.java
        IFolder rootFolder = (IFolder)root.getResource();
        IFolder osgiinfFolder = rootFolder.getFolder("OSGI-INF");
        metainfFolder.move(osgiinfFolder.getFullPath(), true, null);
        IFile abcJavaFile = rootFolder.getFile("abc.java");
        abcFile.move(abcJavaFile.getFullPath(), true, null);
        IFile aJavaFile = rootFolder.getFile("A.java");
        IFile aFile = rootFolder.getFile("A");
        aJavaFile.move(aFile.getFullPath(), true, null);

        packageFragments = root.getPackageFragments();
        assertEquals(3, packageFragments.length);
        assertTrue(Arrays.asList(packageFragments).contains(defaultPkg));
        assertTrue(defaultPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(fooPkg));
        assertTrue(fooPkg.exists());
        assertTrue(Arrays.asList(packageFragments).contains(foobarPkg));
        assertTrue(foobarPkg.exists());

        nonJavaResources = root.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(osgiinfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(aFile));
    }
}
