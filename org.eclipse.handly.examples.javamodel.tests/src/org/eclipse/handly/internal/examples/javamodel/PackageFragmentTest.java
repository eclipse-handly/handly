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
import org.eclipse.handly.examples.javamodel.ICompilationUnit;
import org.eclipse.handly.examples.javamodel.IJavaProject;
import org.eclipse.handly.examples.javamodel.IPackageFragment;
import org.eclipse.handly.examples.javamodel.IPackageFragmentRoot;
import org.eclipse.handly.examples.javamodel.JavaModelCore;
import org.eclipse.handly.junit.WorkspaceTestCase;

/**
 * <code>PackageFragment</code> tests.
 */
public class PackageFragmentTest
    extends WorkspaceTestCase
{
    private IPackageFragment fooPkg;
    private ICompilationUnit aCU;
    private IFolder metainfFolder;
    private IFile abcFile;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        IProject project = setUpProject("Test006");
        IJavaProject javaProject = JavaModelCore.create(project);
        IPackageFragmentRoot root =
            javaProject.getPackageFragmentRoot(project.getFolder("src"));
        fooPkg = root.getPackageFragment("foo");
        aCU = fooPkg.getCompilationUnit("A.java");
        IFolder fooFolder = (IFolder)fooPkg.getResource();
        metainfFolder = fooFolder.getFolder("META-INF");
        abcFile = fooFolder.getFile("abc");
    }

    public void test001() throws Exception
    {
        ICompilationUnit[] compilationUnits = fooPkg.getCompilationUnits();
        assertEquals(1, compilationUnits.length);
        assertEquals(aCU, compilationUnits[0]);
        assertTrue(aCU.exists());

        Object[] nonJavaResources = fooPkg.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));

        // delete A.java
        aCU.getResource().delete(true, null);

        compilationUnits = fooPkg.getCompilationUnits();
        assertEquals(0, compilationUnits.length);
        assertFalse(aCU.exists());

        nonJavaResources = fooPkg.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));

        // (re-)create A.java
        aCU.getFile().create(new ByteArrayInputStream(new byte[0]), true, null);

        compilationUnits = fooPkg.getCompilationUnits();
        assertEquals(1, compilationUnits.length);
        assertEquals(aCU, compilationUnits[0]);
        assertTrue(aCU.exists());

        nonJavaResources = fooPkg.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));

        // delete META-INF and abc
        metainfFolder.delete(true, null);
        abcFile.delete(true, null);

        compilationUnits = fooPkg.getCompilationUnits();
        assertEquals(1, compilationUnits.length);
        assertEquals(aCU, compilationUnits[0]);
        assertTrue(aCU.exists());

        nonJavaResources = fooPkg.getNonJavaResources();
        assertEquals(0, nonJavaResources.length);

        // (re-)create META-INF and abc
        metainfFolder.create(true, true, null);
        abcFile.create(new ByteArrayInputStream(new byte[0]), true, null);

        compilationUnits = fooPkg.getCompilationUnits();
        assertEquals(1, compilationUnits.length);
        assertEquals(aCU, compilationUnits[0]);
        assertTrue(aCU.exists());

        nonJavaResources = fooPkg.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));
    }

    public void test002() throws Exception
    {
        ICompilationUnit[] compilationUnits = fooPkg.getCompilationUnits();
        assertEquals(1, compilationUnits.length);
        assertEquals(aCU, compilationUnits[0]);
        assertTrue(aCU.exists());

        Object[] nonJavaResources = fooPkg.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));

        // rename A.java
        ICompilationUnit bCU = fooPkg.getCompilationUnit("B.java");
        assertFalse(bCU.exists());
        aCU.getResource().move(bCU.getResource().getFullPath(), true, null);

        compilationUnits = fooPkg.getCompilationUnits();
        assertEquals(1, compilationUnits.length);
        assertEquals(bCU, compilationUnits[0]);
        assertTrue(bCU.exists());
        assertFalse(aCU.exists());

        nonJavaResources = fooPkg.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));
    }

    public void test003() throws Exception
    {
        ICompilationUnit[] compilationUnits = fooPkg.getCompilationUnits();
        assertEquals(1, compilationUnits.length);
        assertEquals(aCU, compilationUnits[0]);
        assertTrue(aCU.exists());

        Object[] nonJavaResources = fooPkg.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));

        // move A.java to default package
        IPackageFragment defaultPkg = fooPkg.getParent().getPackageFragment("");
        ICompilationUnit otherCU = defaultPkg.getCompilationUnit("A.java");
        assertFalse(otherCU.exists());
        aCU.getResource().move(otherCU.getResource().getFullPath(), true, null);

        compilationUnits = fooPkg.getCompilationUnits();
        assertEquals(0, compilationUnits.length);
        assertFalse(aCU.exists());

        nonJavaResources = fooPkg.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));

        compilationUnits = defaultPkg.getCompilationUnits();
        assertEquals(1, compilationUnits.length);
        assertEquals(otherCU, compilationUnits[0]);
        assertTrue(otherCU.exists());

        nonJavaResources = defaultPkg.getNonJavaResources();
        assertEquals(0, nonJavaResources.length);
    }

    public void test004() throws Exception
    {
        ICompilationUnit[] compilationUnits = fooPkg.getCompilationUnits();
        assertEquals(1, compilationUnits.length);
        assertEquals(aCU, compilationUnits[0]);
        assertTrue(aCU.exists());

        Object[] nonJavaResources = fooPkg.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(metainfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(abcFile));

        // move META-INF, abc and A.java
        IFolder fooFolder = (IFolder)fooPkg.getResource();
        IFolder osgiinfFolder = fooFolder.getFolder("OSGI-INF");
        metainfFolder.move(osgiinfFolder.getFullPath(), true, null);
        ICompilationUnit abcCU = fooPkg.getCompilationUnit("abc.java");
        abcFile.move(abcCU.getResource().getFullPath(), true, null);
        IFile aFile = fooFolder.getFile("A");
        aCU.getResource().move(aFile.getFullPath(), true, null);

        compilationUnits = fooPkg.getCompilationUnits();
        assertEquals(1, compilationUnits.length);
        assertEquals(abcCU, compilationUnits[0]);
        assertTrue(abcCU.exists());
        assertFalse(aCU.exists());

        nonJavaResources = fooPkg.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(osgiinfFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(aFile));
    }
}
