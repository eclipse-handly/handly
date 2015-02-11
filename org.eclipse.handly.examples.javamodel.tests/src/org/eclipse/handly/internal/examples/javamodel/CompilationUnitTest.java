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

import org.eclipse.core.resources.IProject;
import org.eclipse.handly.examples.javamodel.ICompilationUnit;
import org.eclipse.handly.examples.javamodel.IImportContainer;
import org.eclipse.handly.examples.javamodel.IImportDeclaration;
import org.eclipse.handly.examples.javamodel.IJavaProject;
import org.eclipse.handly.examples.javamodel.IPackageDeclaration;
import org.eclipse.handly.examples.javamodel.IPackageFragment;
import org.eclipse.handly.examples.javamodel.IPackageFragmentRoot;
import org.eclipse.handly.examples.javamodel.IType;
import org.eclipse.handly.examples.javamodel.JavaModelCore;
import org.eclipse.handly.junit.WorkspaceTestCase;
import org.eclipse.handly.model.IHandle;

/**
 * <code>CompilationUnit</code> tests.
 */
public class CompilationUnitTest
    extends WorkspaceTestCase
{
    private IPackageFragment pkg;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        IProject project = setUpProject("Test008");
        IJavaProject javaProject = JavaModelCore.create(project);
        IPackageFragmentRoot srcRoot =
            javaProject.getPackageFragmentRoot(project.getFolder("src"));
        pkg = srcRoot.getPackageFragment("");
    }

    public void test001() throws Exception
    {
        ICompilationUnit cu = pkg.getCompilationUnit("CU01.java");
        assertEquals(0, cu.getChildren().length);
    }

    public void test002() throws Exception
    {
        ICompilationUnit cu = pkg.getCompilationUnit("CU02.java");
        IPackageDeclaration pkgDecl = cu.getPackageDeclaration("foo");
        assertTrue(pkgDecl.exists());
        IImportContainer importContainer = cu.getImportContainer();
        assertTrue(importContainer.exists());
        IType typeX = cu.getType("X");
        assertTrue(typeX.exists());
        IType typeY = cu.getType("Y");
        assertTrue(typeY.exists());

        IHandle[] children = cu.getChildren();
        assertEquals(4, children.length);
        assertEquals(pkgDecl, children[0]);
        assertEquals(importContainer, children[1]);
        assertEquals(typeX, children[2]);
        assertEquals(typeY, children[3]);

        IPackageDeclaration[] pkgDecls = cu.getPackageDeclarations();
        assertEquals(1, pkgDecls.length);
        assertEquals(pkgDecl, pkgDecls[0]);

        IImportDeclaration import1 = cu.getImport("a.A");
        assertTrue(import1.exists());
        IImportDeclaration import2 = cu.getImport("java.util.*");
        assertTrue(import2.exists());
        IImportDeclaration import3 = cu.getImport("java.util.Arrays.*");
        assertTrue(import3.exists());

        IImportDeclaration[] imports = cu.getImports();
        assertEquals(3, imports.length);
        assertEquals(import1, imports[0]);
        assertEquals(import2, imports[1]);
        assertEquals(import3, imports[2]);
        assertTrue(Arrays.equals(importContainer.getImports(), imports));

        IType[] types = cu.getTypes();
        assertEquals(2, types.length);
        assertEquals(typeX, types[0]);
        assertEquals(typeY, types[1]);
    }

    public void test003() throws Exception
    {
        ICompilationUnit cu = pkg.getCompilationUnit("CU03.java");
        IPackageDeclaration pkgDecl = cu.getPackageDeclaration("foo");
        assertFalse(pkgDecl.exists());
        assertFalse(cu.getImportContainer().exists());
        IType typeX = cu.getType("X");
        assertTrue(typeX.exists());
        IType typeY = cu.getType("Y");
        assertFalse(typeY.exists());

        IHandle[] children = cu.getChildren();
        assertEquals(1, children.length);
        assertEquals(typeX, children[0]);

        IType[] types = cu.getTypes();
        assertEquals(1, types.length);
        assertEquals(typeX, types[0]);
    }

    public void test004() throws Exception
    {
        ICompilationUnit cu = pkg.getCompilationUnit("CU04.java");
        IPackageDeclaration pkgDecl = cu.getPackageDeclaration("foo");
        assertTrue(pkgDecl.exists());
        assertFalse(cu.getImportContainer().exists());
        IType typeX = cu.getType("X");
        assertFalse(typeX.exists());
        IType typeY = cu.getType("Y");
        assertTrue(typeY.exists());

        IHandle[] children = cu.getChildren();
        assertEquals(2, children.length);
        assertEquals(pkgDecl, children[0]);
        assertEquals(typeY, children[1]);

        IPackageDeclaration[] pkgDecls = cu.getPackageDeclarations();
        assertEquals(1, pkgDecls.length);
        assertEquals(pkgDecl, pkgDecls[0]);

        IType[] types = cu.getTypes();
        assertEquals(1, types.length);
        assertEquals(typeY, types[0]);
    }

    public void test005()
    {
        ICompilationUnit cu = pkg.getCompilationUnit("123.java");
        assertFalse(cu.exists()); // invalid CU name
    }
}
