/*******************************************************************************
 * Copyright (c) 2015, 2024 1C-Soft LLC.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.jmodel;

import static org.eclipse.handly.context.Contexts.of;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.Path;
import org.eclipse.handly.buffer.BufferChange;
import org.eclipse.handly.buffer.ChildBuffer;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.buffer.SaveMode;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.examples.jmodel.ICompilationUnit;
import org.eclipse.handly.examples.jmodel.IField;
import org.eclipse.handly.examples.jmodel.IMethod;
import org.eclipse.handly.examples.jmodel.IType;
import org.eclipse.handly.examples.jmodel.JavaModelCore;
import org.eclipse.handly.junit.NoJobsWorkspaceTestCase;
import org.eclipse.handly.model.impl.ISourceFileImplExtension;
import org.eclipse.handly.util.TextRange;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;

/**
 * Working copy tests.
 */
public class WorkingCopyTest
    extends NoJobsWorkspaceTestCase
{
    private static final int AST_LEVEL = AST.getJLSLatest();

    private CompilationUnit workingCopy;
    private List<IProblem> problems;
    private IProblemRequestor problemRequestor = new ProblemRequestor();

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        JavaRuntime.getDefaultVMInstall(); // force VM install initialization
        IProject project = setUpProject("Test010");
        workingCopy = (CompilationUnit)JavaModelCore.createCompilationUnitFrom(
            project.getFile(new Path("src/X.java")));
        problems = new ArrayList<IProblem>();
    }

    public void test001() throws Exception
    {
        doWithWorkingCopy(monitor ->
        {
            IType[] types = workingCopy.getTypes();
            assertEquals(1, types.length);
            IType typeX = workingCopy.getType("X");
            assertEquals(typeX, types[0]);

            TextRange r = typeX.getSourceElementInfo().getIdentifyingRange();
            BufferChange change = new BufferChange(new ReplaceEdit(
                r.getOffset(), r.getLength(), "Y"));
            change.setSaveMode(SaveMode.LEAVE_UNSAVED);
            try (IBuffer buffer = workingCopy.getBuffer())
            {
                buffer.applyChange(change, null);
            }

            types = workingCopy.getTypes();
            assertEquals(1, types.length);
            assertEquals(typeX, types[0]);

            workingCopy.reconcile(ICompilationUnit.NO_AST, 0, monitor);

            assertFalse(typeX.exists());

            types = workingCopy.getTypes();
            assertEquals(1, types.length);
            assertEquals(workingCopy.getType("Y"), types[0]);
        });
    }

    public void test002() throws Exception
    {
        doWithWorkingCopy(monitor ->
        {
            IType typeX = workingCopy.getType("X");

            IField[] fields = typeX.getFields();
            assertEquals(1, fields.length);
            IField fieldX = typeX.getField("x");
            assertEquals(fieldX, fields[0]);

            TextRange r = fieldX.getSourceElementInfo().getFullRange();
            BufferChange change = new BufferChange(new DeleteEdit(r.getOffset(),
                r.getLength()));
            change.setSaveMode(SaveMode.LEAVE_UNSAVED);
            try (IBuffer buffer = workingCopy.getBuffer())
            {
                buffer.applyChange(change, null);
            }

            fields = typeX.getFields();
            assertEquals(1, fields.length);
            assertEquals(fieldX, fields[0]);

            workingCopy.reconcile(ICompilationUnit.NO_AST, 0, monitor);

            fields = typeX.getFields();
            assertEquals(0, fields.length);
            assertFalse(fieldX.exists());

            change = new BufferChange(new InsertEdit(r.getOffset(), "int y;"));
            change.setSaveMode(SaveMode.LEAVE_UNSAVED);
            try (IBuffer buffer = workingCopy.getBuffer())
            {
                buffer.applyChange(change, null);
            }

            fields = typeX.getFields();
            assertEquals(0, fields.length);

            workingCopy.reconcile(ICompilationUnit.NO_AST, 0, monitor);

            fields = typeX.getFields();
            assertEquals(1, fields.length);
            assertEquals(typeX.getField("y"), fields[0]);
        });
    }

    public void test003() throws Exception
    {
        doWithWorkingCopy(monitor ->
        {
            IType typeX = workingCopy.getType("X");

            IMethod[] methods = typeX.getMethods();
            assertEquals(1, methods.length);
            IMethod methodFI = typeX.getMethod("f", new String[] { "I" });
            assertEquals(methodFI, methods[0]);

            TextRange r = methodFI.getSourceElementInfo().getFullRange();
            BufferChange change = new BufferChange(new ReplaceEdit(
                r.getOffset(), r.getLength(), "void f() {}"));
            change.setSaveMode(SaveMode.LEAVE_UNSAVED);
            try (IBuffer buffer = workingCopy.getBuffer())
            {
                buffer.applyChange(change, null);
            }

            methods = typeX.getMethods();
            assertEquals(1, methods.length);
            assertEquals(methodFI, methods[0]);

            workingCopy.reconcile(ICompilationUnit.NO_AST, 0, monitor);

            assertFalse(methodFI.exists());

            methods = typeX.getMethods();
            assertEquals(1, methods.length);
            assertEquals(typeX.getMethod("f", new String[0]), methods[0]);
        });
    }

    public void test004() throws Exception
    {
        doWithWorkingCopy(monitor ->
        {
            org.eclipse.jdt.core.dom.CompilationUnit cu = workingCopy.reconcile(
                AST_LEVEL, 0, monitor);
            assertNull(cu);
        });
    }

    public void test005() throws Exception
    {
        doWithWorkingCopy(monitor ->
        {
            org.eclipse.jdt.core.dom.CompilationUnit cu = workingCopy.reconcile(
                AST_LEVEL, ICompilationUnit.FORCE_PROBLEM_DETECTION, monitor);
            assertNotNull(cu);
        });
    }

    public void test006() throws Exception
    {
        doWithWorkingCopy(monitor ->
        {
            org.eclipse.jdt.core.dom.CompilationUnit cu = workingCopy.reconcile(
                ICompilationUnit.NO_AST,
                ICompilationUnit.FORCE_PROBLEM_DETECTION, monitor);
            assertNull(cu);
        });
    }

    public void test007() throws Exception
    {
        doWithWorkingCopy(monitor ->
        {
            org.eclipse.jdt.core.dom.CompilationUnit cu = workingCopy.reconcile(
                AST_LEVEL, ICompilationUnit.FORCE_PROBLEM_DETECTION, monitor);
            assertNotNull(cu);
            assertTrue(cu.getAST().hasResolvedBindings());
            assertFalse(cu.getAST().hasStatementsRecovery());
            assertFalse(cu.getAST().hasBindingsRecovery());
            List<?> types = cu.types();
            assertEquals(1, types.size());
            TypeDeclaration typeX = (TypeDeclaration)types.get(0);
            assertEquals("X", typeX.getName().getIdentifier());
            List<?> bodyDeclarations = typeX.bodyDeclarations();
            assertEquals(2, bodyDeclarations.size());
            MethodDeclaration methodF = (MethodDeclaration)bodyDeclarations.get(
                1);
            assertEquals("f", methodF.getName().getIdentifier());
            Block body = methodF.getBody();
            assertNotNull(body);
            assertEquals(1, body.statements().size());
        });
    }

    public void test008() throws Exception
    {
        doWithWorkingCopy(monitor ->
        {
            org.eclipse.jdt.core.dom.CompilationUnit cu = workingCopy.reconcile(
                AST_LEVEL, ICompilationUnit.FORCE_PROBLEM_DETECTION
                    | ICompilationUnit.ENABLE_STATEMENTS_RECOVERY
                    | ICompilationUnit.ENABLE_BINDINGS_RECOVERY
                    | ICompilationUnit.IGNORE_METHOD_BODIES, monitor);
            assertNotNull(cu);
            assertTrue(cu.getAST().hasResolvedBindings());
            assertTrue(cu.getAST().hasStatementsRecovery());
            assertTrue(cu.getAST().hasBindingsRecovery());
            List<?> types = cu.types();
            assertEquals(1, types.size());
            TypeDeclaration typeX = (TypeDeclaration)types.get(0);
            assertEquals("X", typeX.getName().getIdentifier());
            List<?> bodyDeclarations = typeX.bodyDeclarations();
            assertEquals(2, bodyDeclarations.size());
            MethodDeclaration methodF = (MethodDeclaration)bodyDeclarations.get(
                1);
            assertEquals("f", methodF.getName().getIdentifier());
            Block body = methodF.getBody();
            assertNotNull(body);
            assertEquals(0, body.statements().size());
        });
    }

    public void test009() throws Exception
    {
        doWithWorkingCopy(monitor ->
        {
            workingCopy.reconcile(ICompilationUnit.NO_AST,
                ICompilationUnit.FORCE_PROBLEM_DETECTION, monitor);
            assertFalse(problems.isEmpty());
        });
    }

    public void test010() throws Exception
    {
        doWithWorkingCopy(monitor ->
        {
            final CompilationUnit privateCopy = new CompilationUnit(
                workingCopy.getParent(), workingCopy.getFile(),
                new WorkingCopyOwner()
                {
                });
            assertFalse(privateCopy.equals(workingCopy));
            try (
                IBuffer buffer = workingCopy.getBuffer();
                IBuffer privateBuffer = new ChildBuffer(buffer))
            {
                doWithWorkingCopy(privateCopy, of(
                    ISourceFileImplExtension.WORKING_COPY_BUFFER,
                    privateBuffer), pm ->
                    {
                        IType[] types = privateCopy.getTypes();
                        assertEquals(1, types.length);
                        IType typeX = privateCopy.getType("X");
                        assertEquals(typeX, types[0]);

                        TextRange r =
                            typeX.getSourceElementInfo().getIdentifyingRange();
                        BufferChange change = new BufferChange(new ReplaceEdit(
                            r.getOffset(), r.getLength(), "Y"));
                        change.setSaveMode(SaveMode.LEAVE_UNSAVED);
                        privateBuffer.applyChange(change, null);

                        privateCopy.reconcile(ICompilationUnit.NO_AST, 0, pm);

                        assertFalse(typeX.exists());

                        types = privateCopy.getTypes();
                        assertEquals(1, types.length);
                        assertEquals(privateCopy.getType("Y"), types[0]);

                        workingCopy.reconcile(ICompilationUnit.NO_AST, 0, pm);

                        types = workingCopy.getTypes();
                        assertEquals(1, types.length);
                        assertEquals(workingCopy.getType("X"), types[0]);
                        assertFalse(typeX.equals(types[0]));
                    });
            }
        });
    }

    private void doWithWorkingCopy(ICoreRunnable runnable) throws CoreException
    {
        doWithWorkingCopy(workingCopy, of(IProblemRequestor.class,
            problemRequestor), runnable);
    }

    private static void doWithWorkingCopy(CompilationUnit cu, IContext context,
        ICoreRunnable runnable) throws CoreException
    {
        cu.becomeWorkingCopy_(context, null);
        try
        {
            runnable.run(null);
        }
        finally
        {
            cu.releaseWorkingCopy_();
        }
    }

    private class ProblemRequestor
        implements IProblemRequestor
    {
        @Override
        public void acceptProblem(IProblem problem)
        {
            problems.add(problem);
        }

        @Override
        public void beginReporting()
        {
            problems.clear();
        }

        @Override
        public void endReporting()
        {
        }

        @Override
        public boolean isActive()
        {
            return true;
        }
    }
}
