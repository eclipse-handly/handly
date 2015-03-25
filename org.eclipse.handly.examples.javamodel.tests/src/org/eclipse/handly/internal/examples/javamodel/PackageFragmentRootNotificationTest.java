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

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.examples.javamodel.JavaModelCore;
import org.eclipse.handly.junit.WorkspaceTestCase;

/**
 * <code>PackageFragmentRoot</code> notification tests.
 */
public class PackageFragmentRootNotificationTest
    extends WorkspaceTestCase
{
    private IJavaModel javaModel = JavaModelCore.getJavaModel();
    private JavaModelListener listener = new JavaModelListener();
    private IFolder srcFolder;
    private IFolder fooFolder;
    private IFolder barFolder;
    private IFolder metainfFolder;
    private IFile abcFile;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        IProject project = setUpProject("Test005");
        srcFolder = project.getFolder("src");
        fooFolder = srcFolder.getFolder("foo");
        barFolder = fooFolder.getFolder("bar");
        metainfFolder = srcFolder.getFolder("META-INF");
        abcFile = srcFolder.getFile("abc");
        javaModel.addElementChangeListener(listener);
    }

    @Override
    protected void tearDown() throws Exception
    {
        javaModel.removeElementChangeListener(listener);
        super.tearDown();
    }

    public void test001() throws Exception
    {
        // delete foo.bar
        barFolder.delete(true, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN}\n" +
            "    Test005[*]: {CHILDREN}\n" +
            "        src[*]: {CHILDREN}\n" +
            "            foo.bar[-]: {}"
        );
        //@formatter:on

        // (re-)create foo.bar
        barFolder.create(true, true, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN}\n" +
            "    Test005[*]: {CHILDREN}\n" +
            "        src[*]: {CHILDREN}\n" +
            "            foo.bar[+]: {}"
        );
        //@formatter:on
    }

    public void test002() throws Exception
    {
        // delete META-INF and abc
        javaModel.getWorkspace().run(new IWorkspaceRunnable()
        {
            public void run(IProgressMonitor monitor) throws CoreException
            {
                metainfFolder.delete(true, null);
                abcFile.delete(true, null);
            }
        }, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN}\n" +
            "    Test005[*]: {CHILDREN}\n" +
            "        src[*]: {CONTENT}\n" +
            "            ResourceDelta(/Test005/src/META-INF)[-]\n" +
            "            ResourceDelta(/Test005/src/abc)[-]"
        );
        //@formatter:on

        // (re-)create META-INF and abc
        javaModel.getWorkspace().run(new IWorkspaceRunnable()
        {
            public void run(IProgressMonitor monitor) throws CoreException
            {
                metainfFolder.create(true, true, null);
                abcFile.create(new ByteArrayInputStream(new byte[0]), true,
                    null);
            }
        }, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN}\n" +
            "    Test005[*]: {CHILDREN}\n" +
            "        src[*]: {CONTENT}\n" +
            "            ResourceDelta(/Test005/src/META-INF)[+]\n" +
            "            ResourceDelta(/Test005/src/abc)[+]"
        );
        //@formatter:on
    }

    public void test003() throws Exception
    {
        // rename foo to foo2
        fooFolder.move(new Path("foo2"), true, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN}\n" +
            "    Test005[*]: {CHILDREN}\n" +
            "        src[*]: {CHILDREN}\n" +
            "            foo[-]: {MOVED_TO(foo2 [in src [in Test005]])}\n" +
            "            foo.bar[-]: {MOVED_TO(foo2.bar [in src [in Test005]])}\n" +
            "            foo2[+]: {MOVED_FROM(foo [in src [in Test005]])}\n" +
            "            foo2.bar[+]: {MOVED_FROM(foo.bar [in src [in Test005]])}"
        );
        //@formatter:on
    }

    public void test004() throws Exception
    {
        // move foo to a non-Java resource
        IFolder osgiinfFolder = srcFolder.getFolder("OSGI-INF");
        fooFolder.move(osgiinfFolder.getFullPath(), true, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN}\n" +
            "    Test005[*]: {CHILDREN}\n" +
            "        src[*]: {CHILDREN | CONTENT}\n" +
            "            foo[-]: {}\n" +
            "            foo.bar[-]: {}\n" +
            "            ResourceDelta(/Test005/src/OSGI-INF)[+]"
        );
        //@formatter:on

        // move it back
        osgiinfFolder.move(fooFolder.getFullPath(), true, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN}\n" +
            "    Test005[*]: {CHILDREN}\n" +
            "        src[*]: {CHILDREN | CONTENT}\n" +
            "            foo[+]: {}\n" +
            "            foo.bar[+]: {}\n" +
            "            ResourceDelta(/Test005/src/OSGI-INF)[-]"
        );
        //@formatter:on
    }

    public void test005() throws Exception
    {
        // move META-INF, abc and A.java
        javaModel.getWorkspace().run(new IWorkspaceRunnable()
        {
            public void run(IProgressMonitor monitor) throws CoreException
            {
                metainfFolder.move(new Path("OSGI-INF"), true, null);
                abcFile.move(new Path("abc.java"), true, null);
                IFile aJavaFile = srcFolder.getFile("A.java");
                aJavaFile.move(new Path("A"), true, null);
            }
        }, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN}\n" +
            "    Test005[*]: {CHILDREN}\n" +
            "        src[*]: {CHILDREN | CONTENT}\n" +
            "            <default>[*]: {CHILDREN}\n" +
            "                A.java[-]: {}\n" +
            "                abc.java[+]: {}\n" +
            "            ResourceDelta(/Test005/src/A)[+]\n" +
            "            ResourceDelta(/Test005/src/META-INF)[-]\n" +
            "            ResourceDelta(/Test005/src/OSGI-INF)[+]\n" +
            "            ResourceDelta(/Test005/src/abc)[-]"
        );
        //@formatter:on
    }
}
