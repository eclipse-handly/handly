/*******************************************************************************
 * Copyright (c) 2014, 2016 1C-Soft LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.basic.ui.model;

import static org.eclipse.handly.model.IElementDeltaConstants.F_CONTENT;
import static org.eclipse.handly.model.IElementDeltaConstants.F_UNDERLYING_RESOURCE;
import static org.eclipse.handly.model.IElementDeltaConstants.F_WORKING_COPY;

import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.buffer.BufferChange;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.buffer.SaveMode;
import org.eclipse.handly.buffer.TextFileBuffer;
import org.eclipse.handly.examples.basic.ui.model.FooModelCore;
import org.eclipse.handly.examples.basic.ui.model.IFooDef;
import org.eclipse.handly.examples.basic.ui.model.IFooElement;
import org.eclipse.handly.examples.basic.ui.model.IFooModel;
import org.eclipse.handly.examples.basic.ui.model.IFooProject;
import org.eclipse.handly.examples.basic.ui.model.IFooVar;
import org.eclipse.handly.junit.WorkspaceTestCase;
import org.eclipse.handly.model.IElementChangeEvent;
import org.eclipse.handly.model.IElementChangeListener;
import org.eclipse.handly.model.ISourceElementInfo;
import org.eclipse.handly.model.impl.ElementDelta;
import org.eclipse.handly.util.TextRange;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;

/**
 * <code>FooFile</code> working copy change notification tests.
 */
public class FooWorkingCopyNotificationTest
    extends WorkspaceTestCase
{
    private FooFile workingCopy;
    private IBuffer buffer;
    private IFooModel fooModel = FooModelCore.getFooModel();
    private FooModelListener listener = new FooModelListener();

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        IFooProject fooProject = FooModelCore.create(setUpProject("Test002"));
        workingCopy = (FooFile)fooProject.getFooFile("test.foo");
        fooModel.addElementChangeListener(listener);
        buffer = new TextFileBuffer(workingCopy.getFile(),
            ITextFileBufferManager.DEFAULT);
    }

    @Override
    protected void tearDown() throws Exception
    {
        if (buffer != null)
            buffer.release();
        fooModel.removeElementChangeListener(listener);
        super.tearDown();
    }

    public void test1() throws Exception
    {
        doWithWorkingCopy(new IWorkspaceRunnable()
        {
            @Override
            public void run(IProgressMonitor monitor) throws CoreException
            {
                assertDelta(newDeltaBuilder(fooModel).changed(workingCopy,
                    F_WORKING_COPY).getDelta(), listener.delta);

                workingCopy.getFile().touch(null);

                assertDelta(newDeltaBuilder(fooModel).changed(workingCopy,
                    F_CONTENT | F_UNDERLYING_RESOURCE).getDelta(),
                    listener.delta);

                listener.delta = null;
            }
        });
        assertDelta(newDeltaBuilder(fooModel).changed(workingCopy,
            F_WORKING_COPY).getDelta(), listener.delta);
    }

    public void test2() throws Exception
    {
        doWithWorkingCopy(new IWorkspaceRunnable()
        {
            @Override
            public void run(IProgressMonitor monitor) throws CoreException
            {
                listener.delta = null;

                IFooDef[] defs = workingCopy.getDefs();
                assertEquals(3, defs.length);
                IFooDef def = workingCopy.getDef("f", 0);
                assertEquals(def, defs[0]);

                TextRange r =
                    defs[0].getSourceElementInfo().getIdentifyingRange();
                BufferChange change = new BufferChange(new ReplaceEdit(
                    r.getOffset(), r.getLength(), "g")); // rename f() to g()
                change.setSaveMode(SaveMode.LEAVE_UNSAVED);
                buffer.applyChange(change, null);

                assertDelta(null, listener.delta);

                workingCopy.reconcile(null);

                assertFalse(def.exists());

                assertDelta(newDeltaBuilder(workingCopy).added(
                    workingCopy.getDef("g", 0)).removed(def).getDelta(),
                    listener.delta);
            }
        });
    }

    public void test3() throws Exception
    {
        doWithWorkingCopy(new IWorkspaceRunnable()
        {
            @Override
            public void run(IProgressMonitor monitor) throws CoreException
            {
                listener.delta = null;

                IFooVar[] vars = workingCopy.getVars();
                assertEquals(2, vars.length);
                IFooVar varX = workingCopy.getVar("x");
                assertEquals(varX, vars[0]);
                IFooVar varY = workingCopy.getVar("y");
                assertEquals(varY, vars[1]);

                ISourceElementInfo info = varY.getSourceElementInfo();
                TextRange r = info.getFullRange();
                String varYText = info.getSnapshot().getContents().substring(
                    r.getOffset(), r.getEndOffset());

                BufferChange change = new BufferChange(new DeleteEdit(
                    r.getOffset(), r.getLength())); // delete 'var y;'
                change.setSaveMode(SaveMode.LEAVE_UNSAVED);
                buffer.applyChange(change, null);

                assertDelta(null, listener.delta);

                workingCopy.reconcile(null);

                assertDelta(newDeltaBuilder(workingCopy).removed(
                    varY).getDelta(), listener.delta);

                listener.delta = null;

                info = varX.getSourceElementInfo();
                r = info.getFullRange();

                change = // insert 'var y;' before 'var x;'
                    new BufferChange(new InsertEdit(r.getOffset(), varYText));
                change.setSaveMode(SaveMode.LEAVE_UNSAVED);
                buffer.applyChange(change, null);

                assertDelta(null, listener.delta);

                workingCopy.reconcile(null);

                assertDelta(newDeltaBuilder(workingCopy).added(varY).getDelta(),
                    listener.delta);
            }
        });
    }

    public void test4() throws Exception
    {
        doWithWorkingCopy(new IWorkspaceRunnable()
        {
            @Override
            public void run(IProgressMonitor monitor) throws CoreException
            {
                listener.delta = null;

                IFooDef[] defs = workingCopy.getDefs();
                assertEquals(3, defs.length);
                IFooDef def = workingCopy.getDef("f", 1);
                assertEquals(def, defs[1]);

                ISourceElementInfo info = def.getSourceElementInfo();
                TextRange r = info.getFullRange();

                BufferChange change = new BufferChange(new ReplaceEdit(
                    r.getOffset(), r.getLength(), "def f(y) {}")); // instead of 'def f(x) {}'
                change.setSaveMode(SaveMode.LEAVE_UNSAVED);
                buffer.applyChange(change, null);

                assertDelta(null, listener.delta);

                workingCopy.reconcile(null);

                assertDelta(newDeltaBuilder(workingCopy).changed(def,
                    F_CONTENT).getDelta(), listener.delta); // 'parameterNames' property changed
            }
        });
    }

    private void doWithWorkingCopy(IWorkspaceRunnable runnable)
        throws CoreException
    {
        workingCopy.hBecomeWorkingCopy(buffer, null);
        try
        {
            runnable.run(null);
        }
        finally
        {
            workingCopy.hDiscardWorkingCopy();
        }
    }

    private ElementDelta.Builder newDeltaBuilder(IFooElement element)
    {
        return new ElementDelta.Builder(new ElementDelta(element));
    }

    private static void assertDelta(ElementDelta expected, ElementDelta actual)
    {
        if (expected == null)
        {
            assertNull(actual);
            return;
        }
        assertNotNull(actual);
        assertEquals(expected.hElement(), actual.hElement());
        assertEquals(expected.hKind(), actual.hKind());
        assertEquals(expected.hFlags(), actual.hFlags());
        assertEquals(expected.hMovedToElement(), actual.hMovedToElement());
        assertEquals(expected.hMovedFromElement(), actual.hMovedFromElement());
        ElementDelta[] expectedChildren = expected.hAffectedChildren();
        ElementDelta[] actualChildren = actual.hAffectedChildren();
        assertEquals(expectedChildren.length, actualChildren.length);
        for (int i = 0; i < expectedChildren.length; i++)
            assertDelta(expectedChildren[i], actualChildren[i]);
    }

    private static class FooModelListener
        implements IElementChangeListener
    {
        public ElementDelta delta;

        @Override
        public void elementChanged(IElementChangeEvent event)
        {
            delta = (ElementDelta)event.getDelta();
        }
    }
}
