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

import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.handly.examples.javamodel.IJavaProject;
import org.eclipse.handly.examples.javamodel.IPackageFragmentRoot;
import org.eclipse.handly.examples.javamodel.JavaModelCore;
import org.eclipse.handly.junit.WorkspaceTestCase;

/**
 * <code>JavaProject</code> tests.
 * <p>
 * <b>Note</b> In this example model, only source folders that are direct
 * children of the project resource are represented as package fragment roots.
 * Other classpath entries are ignored.
 * </p>
 */
public class JavaProjectTest
    extends WorkspaceTestCase
{
    public void test001() throws Exception
    {
        IProject project = setUpProject("Test001");
        IJavaProject javaProject = JavaModelCore.create(project);

        IPackageFragmentRoot[] roots = javaProject.getPackageFragmentRoots();
        assertEquals(0, roots.length);

        IResource[] nonJavaResources = javaProject.getNonJavaResources();
        assertEquals(1, nonJavaResources.length);
        assertEquals(project.getFile(".project"), nonJavaResources[0]);
    }

    public void test002() throws Exception
    {
        IProject project = setUpProject("Test002");
        IJavaProject javaProject = JavaModelCore.create(project);

        IPackageFragmentRoot[] roots = javaProject.getPackageFragmentRoots();
        assertEquals(0, roots.length);

        IResource[] nonJavaResources = javaProject.getNonJavaResources();
        assertEquals(2, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(
            project.getFile(".project")));
        assertTrue(Arrays.asList(nonJavaResources).contains(
            project.getFile(".classpath")));
    }

    public void test003() throws Exception
    {
        IProject project = setUpProject("Test003");
        IJavaProject javaProject = JavaModelCore.create(project);

        IPackageFragmentRoot[] roots = javaProject.getPackageFragmentRoots();
        assertEquals(1, roots.length);
        IPackageFragmentRoot srcRoot =
            javaProject.getPackageFragmentRoot(project.getFolder("src"));
        assertEquals(srcRoot, roots[0]);
        assertTrue(srcRoot.exists());

        IResource[] nonJavaResources = javaProject.getNonJavaResources();
        assertEquals(4, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(
            project.getFile(".project")));
        assertTrue(Arrays.asList(nonJavaResources).contains(
            project.getFile(".classpath")));
        assertTrue(Arrays.asList(nonJavaResources).contains(
            project.getFolder("abc")));
        assertTrue(Arrays.asList(nonJavaResources).contains(
            project.getFolder("bin")));
    }

    public void test004() throws Exception
    {
        IProject project = setUpProject("Test004");
        IJavaProject javaProject = JavaModelCore.create(project);

        IPackageFragmentRoot[] roots = javaProject.getPackageFragmentRoots();
        assertEquals(2, roots.length);
        IPackageFragmentRoot srcRoot =
            javaProject.getPackageFragmentRoot(project.getFolder("src"));
        assertEquals(srcRoot, roots[0]);
        assertTrue(srcRoot.exists());
        IPackageFragmentRoot srcGenRoot =
            javaProject.getPackageFragmentRoot(project.getFolder("src-gen"));
        assertEquals(srcGenRoot, roots[1]);
        assertTrue(srcGenRoot.exists());

        IResource[] nonJavaResources = javaProject.getNonJavaResources();
        assertEquals(3, nonJavaResources.length);
        IFile projectFile = project.getFile(".project");
        assertTrue(Arrays.asList(nonJavaResources).contains(projectFile));
        IFile classpathFile = project.getFile(".classpath");
        assertTrue(Arrays.asList(nonJavaResources).contains(classpathFile));
        IFolder binFolder = project.getFolder("bin");
        assertTrue(Arrays.asList(nonJavaResources).contains(binFolder));

        // delete src-gen
        srcGenRoot.getResource().delete(true, null);

        roots = javaProject.getPackageFragmentRoots();
        assertEquals(1, roots.length);
        assertEquals(srcRoot, roots[0]);
        assertTrue(srcRoot.exists());
        assertFalse(srcGenRoot.exists());

        nonJavaResources = javaProject.getNonJavaResources();
        assertEquals(3, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(projectFile));
        assertTrue(Arrays.asList(nonJavaResources).contains(classpathFile));
        assertTrue(Arrays.asList(nonJavaResources).contains(binFolder));

        // (re-)create src-gen
        ((IFolder)srcGenRoot.getResource()).create(true, true, null);

        roots = javaProject.getPackageFragmentRoots();
        assertEquals(2, roots.length);
        assertEquals(srcRoot, roots[0]);
        assertEquals(srcGenRoot, roots[1]);
        assertTrue(srcRoot.exists());
        assertTrue(srcGenRoot.exists());

        nonJavaResources = javaProject.getNonJavaResources();
        assertEquals(3, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(projectFile));
        assertTrue(Arrays.asList(nonJavaResources).contains(classpathFile));
        assertTrue(Arrays.asList(nonJavaResources).contains(binFolder));

        // remove src-gen from classpath
        classpathFile.setContents(
            binFolder.getFile("classpath01").getContents(), true, false, null);

        roots = javaProject.getPackageFragmentRoots();
        assertEquals(1, roots.length);
        assertEquals(srcRoot, roots[0]);
        assertTrue(srcRoot.exists());
        assertFalse(srcGenRoot.exists());

        nonJavaResources = javaProject.getNonJavaResources();
        assertEquals(4, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(projectFile));
        assertTrue(Arrays.asList(nonJavaResources).contains(classpathFile));
        assertTrue(Arrays.asList(nonJavaResources).contains(binFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(
            srcGenRoot.getResource()));

        // put src-gen back to classpath, this time as the first entry
        classpathFile.setContents(
            binFolder.getFile("classpath02").getContents(), true, false, null);

        roots = javaProject.getPackageFragmentRoots();
        assertEquals(2, roots.length);
        assertEquals(srcGenRoot, roots[0]);
        assertEquals(srcRoot, roots[1]);
        assertTrue(srcGenRoot.exists());
        assertTrue(srcRoot.exists());

        nonJavaResources = javaProject.getNonJavaResources();
        assertEquals(3, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(projectFile));
        assertTrue(Arrays.asList(nonJavaResources).contains(classpathFile));
        assertTrue(Arrays.asList(nonJavaResources).contains(binFolder));

        // move src-gen
        IFolder otherFolder = project.getFolder("src-gen2");
        srcGenRoot.getResource().move(otherFolder.getFullPath(), true, null);

        roots = javaProject.getPackageFragmentRoots();
        assertEquals(1, roots.length);
        assertEquals(srcRoot, roots[0]);
        assertTrue(srcRoot.exists());
        assertFalse(srcGenRoot.exists());

        nonJavaResources = javaProject.getNonJavaResources();
        assertEquals(4, nonJavaResources.length);
        assertTrue(Arrays.asList(nonJavaResources).contains(projectFile));
        assertTrue(Arrays.asList(nonJavaResources).contains(classpathFile));
        assertTrue(Arrays.asList(nonJavaResources).contains(binFolder));
        assertTrue(Arrays.asList(nonJavaResources).contains(otherFolder));
    }
}
