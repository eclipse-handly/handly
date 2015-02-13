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

import org.eclipse.core.resources.IProject;
import org.eclipse.handly.examples.javamodel.ICompilationUnit;
import org.eclipse.handly.examples.javamodel.IField;
import org.eclipse.handly.examples.javamodel.IJavaProject;
import org.eclipse.handly.examples.javamodel.IMethod;
import org.eclipse.handly.examples.javamodel.IPackageFragment;
import org.eclipse.handly.examples.javamodel.IPackageFragmentRoot;
import org.eclipse.handly.examples.javamodel.IType;
import org.eclipse.handly.examples.javamodel.JavaModelCore;
import org.eclipse.handly.junit.WorkspaceTestCase;
import org.eclipse.handly.model.IHandle;
import org.eclipse.jdt.core.Flags;

/**
 * <code>Type</code>, <code>Field</code> and <code>Method</code> tests.
 */
public class MemberTest
    extends WorkspaceTestCase
{
    private ICompilationUnit cu;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        IProject project = setUpProject("Test009");
        IJavaProject javaProject = JavaModelCore.create(project);
        IPackageFragmentRoot srcRoot =
            javaProject.getPackageFragmentRoot(project.getFolder("src"));
        IPackageFragment pkg = srcRoot.getPackageFragment("");
        cu = pkg.getCompilationUnit("A.java");
    }

    public void test001() throws Exception
    {
        IType typeA = cu.getType("A");
        assertNull(typeA.getDeclaringType());
        assertEquals(Flags.AccPublic | Flags.AccFinal, typeA.getFlags());
        assertTrue(typeA.isClass());
        assertFalse(typeA.isInterface());
        assertFalse(typeA.isEnum());
        assertFalse(typeA.isAnnotation());
        assertFalse(typeA.isMember());
        assertEquals("QX;", typeA.getSuperclassType());
        String[] superInterfaceTypes = typeA.getSuperInterfaceTypes();
        assertEquals(2, superInterfaceTypes.length);
        assertEquals("QY;", superInterfaceTypes[0]);
        assertEquals("Qjava.io.Serializable;", superInterfaceTypes[1]);
        IHandle[] children = typeA.getChildren();
        assertEquals(0, children.length);
    }

    public void test002() throws Exception
    {
        IType typeX = cu.getType("X");
        assertNull(typeX.getDeclaringType());
        assertEquals(Flags.AccAbstract, typeX.getFlags());
        assertTrue(typeX.isClass());
        assertFalse(typeX.isInterface());
        assertFalse(typeX.isEnum());
        assertFalse(typeX.isAnnotation());
        assertFalse(typeX.isMember());
        assertNull(typeX.getSuperclassType());
        assertEquals(0, typeX.getSuperInterfaceTypes().length);

        IField fieldCapX = typeX.getField("X");
        assertEquals(Flags.AccPublic | Flags.AccStatic | Flags.AccFinal,
            fieldCapX.getFlags());
        assertEquals("QString;", fieldCapX.getType());

        IField fieldX = typeX.getField("x");
        assertEquals(Flags.AccPrivate, fieldX.getFlags());
        assertEquals("I", fieldX.getType());

        IField fieldY = typeX.getField("y");
        assertEquals(Flags.AccPrivate, fieldY.getFlags());
        assertEquals("[I", fieldY.getType());

        IMethod cons1 = typeX.getMethod("X", Method.NO_STRINGS);
        assertEquals(Flags.AccPublic, cons1.getFlags());
        assertTrue(cons1.isConstructor());

        IMethod cons2 = typeX.getMethod("X", new String[] { "I" });
        assertEquals(Flags.AccProtected, cons2.getFlags());
        String[] parameterNames = cons2.getParameterNames();
        assertEquals(1, parameterNames.length);
        assertEquals("x", parameterNames[0]);
        assertTrue(cons2.isConstructor());

        IMethod methodF1 = typeX.getMethod("f", Method.NO_STRINGS);
        assertEquals(Flags.AccPublic | Flags.AccFinal, methodF1.getFlags());
        assertEquals("I", methodF1.getReturnType());
        assertEquals(0, methodF1.getExceptionTypes().length);

        IMethod methodF2 =
            typeX.getMethod("f", new String[] { "[QY;", "[Qjava.lang.String;" });
        assertEquals(Flags.AccProtected, methodF2.getFlags());
        assertEquals("V", methodF2.getReturnType());
        parameterNames = methodF2.getParameterNames();
        assertEquals(2, parameterNames.length);
        assertEquals("y", parameterNames[0]);
        assertEquals("s", parameterNames[1]);
        assertEquals("([QY;[Qjava.lang.String;)V", methodF2.getSignature());

        IMethod methodF3 = typeX.getMethod("f", new String[] { "Z" });
        assertEquals(0, methodF3.getFlags());
        assertEquals("[QString;", methodF3.getReturnType());
        parameterNames = methodF3.getParameterNames();
        assertEquals(1, parameterNames.length);
        assertEquals("b", parameterNames[0]);

        IMethod methodG = typeX.getMethod("g", new String[] { "QList<+QT;>;" });
        assertEquals(Flags.AccPrivate | Flags.AccStatic, methodG.getFlags());
        assertEquals("Qjava.util.Map<QString;QT;>;", methodG.getReturnType());
        String[] exceptionTypes = methodG.getExceptionTypes();
        parameterNames = methodG.getParameterNames();
        assertEquals(1, parameterNames.length);
        assertEquals("arg", parameterNames[0]);
        assertEquals(1, exceptionTypes.length);
        assertEquals("QException;", exceptionTypes[0]);

        IType typeA = typeX.getType("A");

        IHandle[] children = typeX.getChildren();
        assertEquals(10, children.length);
        assertEquals(fieldCapX, children[0]);
        assertEquals(fieldX, children[1]);
        assertEquals(fieldY, children[2]);
        assertEquals(cons1, children[3]);
        assertEquals(cons2, children[4]);
        assertEquals(methodF1, children[5]);
        assertEquals(methodF2, children[6]);
        assertEquals(methodF3, children[7]);
        assertEquals(methodG, children[8]);
        assertEquals(typeA, children[9]);

        IField[] fields = typeX.getFields();
        assertEquals(3, fields.length);
        assertEquals(fieldCapX, fields[0]);
        assertEquals(fieldX, fields[1]);
        assertEquals(fieldY, fields[2]);

        IMethod[] methods = typeX.getMethods();
        assertEquals(6, methods.length);
        assertEquals(cons1, methods[0]);
        assertEquals(cons2, methods[1]);
        assertEquals(methodF1, methods[2]);
        assertEquals(methodF2, methods[3]);
        assertEquals(methodF3, methods[4]);
        assertEquals(methodG, methods[5]);

        IType[] types = typeX.getTypes();
        assertEquals(1, types.length);
        assertEquals(typeA, types[0]);
    }

    public void test003() throws Exception
    {
        IType typeX = cu.getType("X");
        IType typeA = typeX.getType("A");
        assertEquals(typeX, typeA.getDeclaringType());
        assertEquals(Flags.AccPrivate | Flags.AccInterface
            | Flags.AccAnnotation, typeA.getFlags());
        assertTrue(typeA.isAnnotation());
        assertTrue(typeA.isInterface());
        assertFalse(typeA.isClass());
        assertFalse(typeA.isEnum());
        assertTrue(typeA.isMember());
        assertNull(typeA.getSuperclassType());
        assertEquals(0, typeA.getSuperInterfaceTypes().length);

        IField fieldA = typeA.getField("A");
        assertEquals(0, fieldA.getFlags());
        assertEquals("QString;", fieldA.getType());

        IMethod methodV = typeA.getMethod("value", Method.NO_STRINGS);
        assertEquals(0, methodV.getFlags());
        assertEquals("[I", methodV.getReturnType());

        IType typeE = typeA.getType("E");

        IHandle[] children = typeA.getChildren();
        assertEquals(3, children.length);
        assertEquals(fieldA, children[0]);
        assertEquals(methodV, children[1]);
        assertEquals(typeE, children[2]);

        IField[] fields = typeA.getFields();
        assertEquals(1, fields.length);
        assertEquals(fieldA, fields[0]);

        IMethod[] methods = typeA.getMethods();
        assertEquals(1, methods.length);
        assertEquals(methodV, methods[0]);

        IType[] types = typeA.getTypes();
        assertEquals(1, types.length);
        assertEquals(typeE, types[0]);
    }

    public void test004() throws Exception
    {
        IType typeA = cu.getType("X").getType("A");
        IType typeE = typeA.getType("E");
        assertEquals(typeA, typeE.getDeclaringType());
        assertEquals(Flags.AccPublic | Flags.AccStatic | Flags.AccEnum,
            typeE.getFlags());
        assertTrue(typeE.isEnum());
        assertFalse(typeE.isClass());
        assertFalse(typeE.isInterface());
        assertFalse(typeE.isAnnotation());
        assertTrue(typeE.isMember());
        assertNull(typeE.getSuperclassType());
        String[] superInterfaceTypes = typeE.getSuperInterfaceTypes();
        assertEquals(1, superInterfaceTypes.length);
        assertEquals("QY;", superInterfaceTypes[0]);

        IField fieldE1 = typeE.getField("E1");
        assertEquals(Flags.AccEnum, fieldE1.getFlags());
        assertEquals("QE;", fieldE1.getType());
        assertTrue(fieldE1.isEnumConstant());

        IField fieldE2 = typeE.getField("E2");
        assertEquals(Flags.AccEnum, fieldE2.getFlags());
        assertEquals("QE;", fieldE2.getType());
        assertTrue(fieldE2.isEnumConstant());

        IField fieldE = typeE.getField("E");
        assertEquals(Flags.AccStatic | Flags.AccFinal, fieldE.getFlags());
        assertEquals("QString;", fieldE.getType());
        assertFalse(fieldE.isEnumConstant());

        IMethod cons = typeE.getMethod("E", Method.NO_STRINGS);
        assertEquals(0, cons.getFlags());
        assertTrue(cons.isConstructor());

        IMethod methodF = typeE.getMethod("f", Method.NO_STRINGS);
        assertEquals(Flags.AccPublic, methodF.getFlags());
        assertEquals("I", methodF.getReturnType());

        IType typeI = typeE.getType("I");
        assertEquals(Flags.AccPrivate | Flags.AccInterface, typeI.getFlags());
        assertEquals(0, typeI.getChildren().length);

        IHandle[] children = typeE.getChildren();
        assertEquals(6, children.length);
        assertEquals(fieldE1, children[0]);
        assertEquals(fieldE2, children[1]);
        assertEquals(fieldE, children[2]);
        assertEquals(cons, children[3]);
        assertEquals(methodF, children[4]);
        assertEquals(typeI, children[5]);

        IField[] fields = typeE.getFields();
        assertEquals(3, fields.length);
        assertEquals(fieldE1, fields[0]);
        assertEquals(fieldE2, fields[1]);
        assertEquals(fieldE, fields[2]);

        IMethod[] methods = typeE.getMethods();
        assertEquals(2, methods.length);
        assertEquals(cons, methods[0]);
        assertEquals(methodF, methods[1]);

        IType[] types = typeE.getTypes();
        assertEquals(1, types.length);
        assertEquals(typeI, types[0]);
    }

    public void test005() throws Exception
    {
        IType typeY = cu.getType("Y");
        assertNull(typeY.getDeclaringType());
        assertEquals(Flags.AccInterface, typeY.getFlags());
        assertTrue(typeY.isInterface());
        assertFalse(typeY.isClass());
        assertFalse(typeY.isEnum());
        assertFalse(typeY.isAnnotation());
        assertFalse(typeY.isMember());
        assertNull(typeY.getSuperclassType());
        String[] superInterfaceTypes = typeY.getSuperInterfaceTypes();
        assertEquals(1, superInterfaceTypes.length);
        assertEquals("Qjava.io.Serializable;", superInterfaceTypes[0]);

        IField fieldY = typeY.getField("Y");
        assertEquals(0, fieldY.getFlags());
        assertEquals("QString;", fieldY.getType());

        IMethod methodF = typeY.getMethod("f", Method.NO_STRINGS);
        assertEquals(Flags.AccAbstract, methodF.getFlags());
        assertEquals("I", methodF.getReturnType());

        IType typeZ = typeY.getType("Z");
        assertEquals(Flags.AccFinal, typeZ.getFlags());
        assertEquals(0, typeZ.getChildren().length);
    }
}
