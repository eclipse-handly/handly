/*******************************************************************************
 * Copyright (c) 2015 1C-Soft LLC.
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
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.handly.buffer.BufferChange;
import org.eclipse.handly.buffer.SaveMode;
import org.eclipse.handly.examples.javamodel.IField;
import org.eclipse.handly.examples.javamodel.IMethod;
import org.eclipse.handly.examples.javamodel.IType;
import org.eclipse.handly.examples.javamodel.JavaModelCore;
import org.eclipse.handly.junit.WorkspaceTestCase;
import org.eclipse.handly.model.impl.DelegatingWorkingCopyBuffer;
import org.eclipse.handly.model.impl.IWorkingCopyBuffer;
import org.eclipse.handly.model.impl.WorkingCopyReconciler;
import org.eclipse.handly.util.TextRange;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;

/**
 * Working copy tests.
 */
public class WorkingCopyTest
    extends WorkspaceTestCase
{
    private CompilationUnit workingCopy;
    private IWorkingCopyBuffer buffer;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        IProject project = setUpProject("Test010");
        workingCopy =
            (CompilationUnit)JavaModelCore.createCompilationUnitFrom(project.getFile(new Path(
                "src/X.java")));
        buffer =
            new DelegatingWorkingCopyBuffer(workingCopy.openBuffer(null),
                new WorkingCopyReconciler(workingCopy));
    }

    @Override
    protected void tearDown() throws Exception
    {
        buffer.dispose();
        super.tearDown();
    }

    public void test001() throws Exception
    {
        doWithWorkingCopy(new IWorkspaceRunnable()
        {
            public void run(IProgressMonitor monitor) throws CoreException
            {
                IType[] types = workingCopy.getTypes();
                assertEquals(1, types.length);
                IType typeX = workingCopy.getType("X");
                assertEquals(typeX, types[0]);

                TextRange r =
                    typeX.getSourceElementInfo().getIdentifyingRange();
                BufferChange change =
                    new BufferChange(new ReplaceEdit(r.getOffset(),
                        r.getLength(), "Y"));
                change.setSaveMode(SaveMode.LEAVE_UNSAVED);
                buffer.applyChange(change, null);

                types = workingCopy.getTypes();
                assertEquals(1, types.length);
                assertEquals(typeX, types[0]);

                workingCopy.reconcile(false, monitor);

                assertFalse(typeX.exists());

                types = workingCopy.getTypes();
                assertEquals(1, types.length);
                assertEquals(workingCopy.getType("Y"), types[0]);
            }
        });
    }

    public void test002() throws Exception
    {
        doWithWorkingCopy(new IWorkspaceRunnable()
        {
            public void run(IProgressMonitor monitor) throws CoreException
            {
                IType typeX = workingCopy.getType("X");

                IField[] fields = typeX.getFields();
                assertEquals(1, fields.length);
                IField fieldX = typeX.getField("x");
                assertEquals(fieldX, fields[0]);

                TextRange r = fieldX.getSourceElementInfo().getFullRange();
                BufferChange change =
                    new BufferChange(new DeleteEdit(r.getOffset(),
                        r.getLength()));
                change.setSaveMode(SaveMode.LEAVE_UNSAVED);
                buffer.applyChange(change, null);

                fields = typeX.getFields();
                assertEquals(1, fields.length);
                assertEquals(fieldX, fields[0]);

                workingCopy.reconcile(false, null);

                fields = typeX.getFields();
                assertEquals(0, fields.length);
                assertFalse(fieldX.exists());

                change =
                    new BufferChange(new InsertEdit(r.getOffset(), "int y;"));
                change.setSaveMode(SaveMode.LEAVE_UNSAVED);
                buffer.applyChange(change, null);

                fields = typeX.getFields();
                assertEquals(0, fields.length);

                workingCopy.reconcile(false, null);

                fields = typeX.getFields();
                assertEquals(1, fields.length);
                assertEquals(typeX.getField("y"), fields[0]);
            }
        });
    }

    public void test003() throws Exception
    {
        doWithWorkingCopy(new IWorkspaceRunnable()
        {
            public void run(IProgressMonitor monitor) throws CoreException
            {
                IType typeX = workingCopy.getType("X");

                IMethod[] methods = typeX.getMethods();
                assertEquals(1, methods.length);
                IMethod methodFI = typeX.getMethod("f", new String[] { "I" });
                assertEquals(methodFI, methods[0]);

                TextRange r = methodFI.getSourceElementInfo().getFullRange();
                BufferChange change =
                    new BufferChange(new ReplaceEdit(r.getOffset(),
                        r.getLength(), "void f() {}"));
                change.setSaveMode(SaveMode.LEAVE_UNSAVED);
                buffer.applyChange(change, null);

                methods = typeX.getMethods();
                assertEquals(1, methods.length);
                assertEquals(methodFI, methods[0]);

                workingCopy.reconcile(false, monitor);

                assertFalse(methodFI.exists());

                methods = typeX.getMethods();
                assertEquals(1, methods.length);
                assertEquals(typeX.getMethod("f", new String[0]), methods[0]);
            }
        });
    }

    private void doWithWorkingCopy(IWorkspaceRunnable runnable)
        throws CoreException
    {
        workingCopy.becomeWorkingCopy(buffer, null);
        try
        {
            runnable.run(null);
        }
        finally
        {
            workingCopy.discardWorkingCopy();
        }
    }
}
