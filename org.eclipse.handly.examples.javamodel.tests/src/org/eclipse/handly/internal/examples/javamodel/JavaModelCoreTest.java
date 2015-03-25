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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.examples.javamodel.IJavaProject;
import org.eclipse.handly.examples.javamodel.IPackageFragmentRoot;
import org.eclipse.handly.examples.javamodel.JavaModelCore;
import org.eclipse.handly.junit.WorkspaceTestCase;

/**
 * <code>JavaModelCore</code> tests.
 */
public class JavaModelCoreTest
    extends WorkspaceTestCase
{
    private IProject project;
    private IFolder srcFolder;
    private IProject simpleProject;
    private IProject nonExistingProject;
    private IJavaModel javaModel;
    private IJavaProject javaProject;
    private IPackageFragmentRoot srcRoot;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        project = setUpProject("Test007");
        srcFolder = project.getFolder("src");
        simpleProject = setUpProject("SimpleProject");
        nonExistingProject = getProject("nonExisting");
        javaModel = JavaModelCore.getJavaModel();
        javaProject = javaModel.getJavaProject(project.getName());
        srcRoot = javaProject.getPackageFragmentRoot(srcFolder);
    }

    public void test001()
    {
        assertEquals(javaModel,
            JavaModelCore.create(javaModel.getWorkspace().getRoot()));
    }

    public void test002() throws Exception
    {
        assertEquals(javaProject, JavaModelCore.create(project));

        project.close(null);
        assertEquals(javaProject, JavaModelCore.create(project));

        assertEquals(javaModel.getJavaProject(simpleProject.getName()),
            JavaModelCore.create(simpleProject));

        assertEquals(javaModel.getJavaProject(nonExistingProject.getName()),
            JavaModelCore.create(nonExistingProject));
    }

    public void test003() throws Exception
    {
        assertEquals(srcRoot, JavaModelCore.create(srcFolder));

        assertNull(JavaModelCore.create(project.getFolder("src-gen")));

        project.close(null);
        assertNull(JavaModelCore.create(srcFolder));

        assertNull(JavaModelCore.create(simpleProject.getFolder("src")));

        assertNull(JavaModelCore.create(nonExistingProject.getFolder("src")));
    }

    public void test004() throws Exception
    {
        IFolder fooFolder = srcFolder.getFolder("foo");
        assertEquals(srcRoot.getPackageFragment("foo"),
            JavaModelCore.create(fooFolder));
        assertEquals(srcRoot.getPackageFragment("foo.bar"),
            JavaModelCore.create(fooFolder.getFolder("bar")));

        assertNull(JavaModelCore.create(srcFolder.getFolder("META-INF")));

        project.close(null);
        assertNull(JavaModelCore.create(fooFolder));
    }

    public void test005() throws Exception
    {
        IFile aFile = srcFolder.getFile("A.java");
        assertEquals(
            srcRoot.getPackageFragment("").getCompilationUnit(aFile.getName()),
            JavaModelCore.create(aFile));

        aFile = srcFolder.getFolder("foo").getFile("A.java");
        assertEquals(
            srcRoot.getPackageFragment("foo").getCompilationUnit(
                aFile.getName()), JavaModelCore.create(aFile));

        assertNull(JavaModelCore.create(srcFolder.getFile("A")));

        try
        {
            JavaModelCore.createCompilationUnitFrom(srcFolder.getFile("A"));
            fail();
        }
        catch (IllegalArgumentException e)
        {
        }

        assertNull(JavaModelCore.create(srcFolder.getFolder("META-INF").getFile(
            "A.java")));

        assertNull(JavaModelCore.create(project.getFolder("src-gen").getFile(
            "A.java")));

        project.close(null);
        assertNull(JavaModelCore.create(aFile));

        assertNull(JavaModelCore.create(simpleProject.getFile("A.java")));

        assertNull(JavaModelCore.create(nonExistingProject.getFile("A.java")));
    }
}
