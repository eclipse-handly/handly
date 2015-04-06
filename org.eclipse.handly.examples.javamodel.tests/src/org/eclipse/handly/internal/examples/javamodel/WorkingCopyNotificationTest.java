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
 * Working copy change notification tests.
 */
public class WorkingCopyNotificationTest
    extends WorkspaceTestCase
{
    private CompilationUnit workingCopy;
    private IWorkingCopyBuffer buffer;
    private JavaModelListener listener = new JavaModelListener();

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
        workingCopy.getRoot().addElementChangeListener(listener);
    }

    @Override
    protected void tearDown() throws Exception
    {
        workingCopy.getRoot().removeElementChangeListener(listener);
        buffer.dispose();
        super.tearDown();
    }

    public void test001() throws Exception
    {
        doWithWorkingCopy(new IWorkspaceRunnable()
        {
            public void run(IProgressMonitor monitor) throws CoreException
            {
                //@formatter:off
                listener.assertDelta(
                    "Java Model[*]: {CHILDREN}\n" +
                    "    Test010[*]: {CHILDREN}\n" +
                    "        src[*]: {CHILDREN}\n" +
                    "            <default>[*]: {CHILDREN}\n" +
                    "                [Working copy] X.java[*]: {WORKING COPY}"
                );
                //@formatter:on

                workingCopy.getFile().touch(monitor);

                //@formatter:off
                listener.assertDelta(
                    "Java Model[*]: {CHILDREN}\n" +
                    "    Test010[*]: {CHILDREN}\n" +
                    "        src[*]: {CHILDREN}\n" +
                    "            <default>[*]: {CHILDREN}\n" +
                    "                [Working copy] X.java[*]: {CONTENT | UNDERLYING_RESOURCE}"
                );
                //@formatter:on
            }
        });
        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN}\n" +
            "    Test010[*]: {CHILDREN}\n" +
            "        src[*]: {CHILDREN}\n" +
            "            <default>[*]: {CHILDREN}\n" +
            "                X.java[*]: {WORKING COPY}"
        );
        //@formatter:on
    }

    public void test002() throws Exception
    {
        doWithWorkingCopy(new IWorkspaceRunnable()
        {
            public void run(IProgressMonitor monitor) throws CoreException
            {
                listener.delta = null;

                IType typeX = workingCopy.getType("X");
                TextRange r =
                    typeX.getSourceElementInfo().getIdentifyingRange();
                BufferChange change =
                    new BufferChange(new ReplaceEdit(r.getOffset(),
                        r.getLength(), "Y"));
                change.setSaveMode(SaveMode.LEAVE_UNSAVED);
                buffer.applyChange(change, null);

                assertNull(listener.delta);

                workingCopy.reconcile(false, monitor);

                //@formatter:off
                listener.assertDelta(
                    "[Working copy] X.java[*]: {CHILDREN | FINE GRAINED}\n" +
                    "    Y[+]: {}\n" +
                    "    X[-]: {}"
                );
                //@formatter:on
            }
        });
    }

    public void test003() throws Exception
    {
        doWithWorkingCopy(new IWorkspaceRunnable()
        {
            public void run(IProgressMonitor monitor) throws CoreException
            {
                listener.delta = null;

                IField fieldX = workingCopy.getType("X").getField("x");
                TextRange r = fieldX.getSourceElementInfo().getFullRange();
                BufferChange change =
                    new BufferChange(new DeleteEdit(r.getOffset(),
                        r.getLength()));
                change.setSaveMode(SaveMode.LEAVE_UNSAVED);
                buffer.applyChange(change, null);

                assertNull(listener.delta);

                workingCopy.reconcile(false, null);

                //@formatter:off
                listener.assertDelta(
                    "[Working copy] X.java[*]: {CHILDREN | FINE GRAINED}\n" +
                    "    X[*]: {CHILDREN}\n" +
                    "        x[-]: {}"
                );
                //@formatter:on

                listener.delta = null;

                change =
                    new BufferChange(new InsertEdit(r.getOffset(), "int y;"));
                change.setSaveMode(SaveMode.LEAVE_UNSAVED);
                buffer.applyChange(change, null);

                assertNull(listener.delta);

                workingCopy.reconcile(false, null);

                //@formatter:off
                listener.assertDelta(
                    "[Working copy] X.java[*]: {CHILDREN | FINE GRAINED}\n" +
                    "    X[*]: {CHILDREN}\n" +
                    "        y[+]: {}"
                );
                //@formatter:on
            }
        });
    }

    public void test004() throws Exception
    {
        doWithWorkingCopy(new IWorkspaceRunnable()
        {
            public void run(IProgressMonitor monitor) throws CoreException
            {
                listener.delta = null;

                IMethod methodFI =
                    workingCopy.getType("X").getMethod("f",
                        new String[] { "I" });
                TextRange r = methodFI.getSourceElementInfo().getFullRange();
                BufferChange change =
                    new BufferChange(new ReplaceEdit(r.getOffset(),
                        r.getLength(), "void f() {}"));
                change.setSaveMode(SaveMode.LEAVE_UNSAVED);
                buffer.applyChange(change, null);

                assertNull(listener.delta);

                workingCopy.reconcile(false, monitor);

                //@formatter:off
                listener.assertDelta(
                    "[Working copy] X.java[*]: {CHILDREN | FINE GRAINED}\n" +
                    "    X[*]: {CHILDREN}\n" +
                    "        f()[+]: {}\n" +
                    "        f(int)[-]: {}"
                );
                //@formatter:on
            }
        });
    }

    public void test005() throws Exception
    {
        doWithWorkingCopy(new IWorkspaceRunnable()
        {
            public void run(IProgressMonitor monitor) throws CoreException
            {
                listener.delta = null;

                IMethod methodFI =
                    workingCopy.getType("X").getMethod("f",
                        new String[] { "I" });
                TextRange r = methodFI.getSourceElementInfo().getFullRange();
                BufferChange change =
                    new BufferChange(new ReplaceEdit(r.getOffset(),
                        r.getLength(), "void f(int y) {}")); // renamed arg
                change.setSaveMode(SaveMode.LEAVE_UNSAVED);
                buffer.applyChange(change, null);

                assertNull(listener.delta);

                workingCopy.reconcile(false, monitor);

                //@formatter:off
                listener.assertDelta(
                    "[Working copy] X.java[*]: {CHILDREN | FINE GRAINED}\n" +
                    "    X[*]: {CHILDREN}\n" +
                    "        f(int)[*]: {CONTENT}"
                );
                //@formatter:on
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
