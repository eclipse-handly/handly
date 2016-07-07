/*******************************************************************************
 * Copyright (c) 2016 1C-Soft LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.buffer;

import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.handly.junit.WorkspaceTestCase;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.WorkbenchAdvisor;

/**
 * <code>TextFileBuffer</code> tests.
 */
public class TextFileBufferTest
    extends WorkspaceTestCase
{
    private TextFileBuffer buffer;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        IProject p = getProject("p");
        p.create(null);
        p.open(null);
        buffer = new TextFileBuffer(p.getFile("f"),
            ITextFileBufferManager.DEFAULT);
    }

    @Override
    protected void tearDown() throws Exception
    {
        if (buffer != null)
            buffer.release();
        super.tearDown();
    }

    public void testBug496840() throws Throwable
    {
        buffer.applyChange(new BufferChange(new InsertEdit(0, "a")), null);

        Throwable[] exception = new Throwable[1];
        PlatformUI.createAndRunWorkbench(PlatformUI.createDisplay(),
            new WorkbenchAdvisor()
            {
                @Override
                public void eventLoopIdle(Display display)
                {
                    try
                    {
                        buffer.applyChange(new BufferChange(new InsertEdit(0,
                            "b")), null);

                        buffer.getDelegate().requestSynchronizationContext();
                        buffer.applyChange(new BufferChange(new InsertEdit(0,
                            "c")), null);
                    }
                    catch (Throwable e)
                    {
                        exception[0] = e;
                    }
                    PlatformUI.getWorkbench().close();
                    display.dispose();
                }

                @Override
                public String getInitialWindowPerspectiveId()
                {
                    return null;
                }
            });
        if (exception[0] != null)
            throw exception[0];

        try
        {
            buffer.applyChange(new BufferChange(new InsertEdit(0, "d")), null);
            fail("Synchronization context is requested but UI is not running");
        }
        catch (AssertionError e)
        {
        }

        buffer.getDelegate().releaseSynchronizationContext();
        buffer.applyChange(new BufferChange(new InsertEdit(0, "d")), null);
        assertEquals("dcba", buffer.getContents());
    }
}
