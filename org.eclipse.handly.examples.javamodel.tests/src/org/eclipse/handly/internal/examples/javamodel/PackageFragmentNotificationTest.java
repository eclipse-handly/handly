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
 * <code>PackageFragment</code> notification tests.
 */
public class PackageFragmentNotificationTest
    extends WorkspaceTestCase
{
    private IJavaModel javaModel = JavaModelCore.getJavaModel();
    private JavaModelListener listener = new JavaModelListener();
    private IFolder fooFolder;
    private IFile aJavaFile;
    private IFolder metainfFolder;
    private IFile abcFile;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        IProject project = setUpProject("Test006");
        IFolder srcFolder = project.getFolder("src");
        fooFolder = srcFolder.getFolder("foo");
        aJavaFile = fooFolder.getFile("A.java");
        metainfFolder = fooFolder.getFolder("META-INF");
        abcFile = fooFolder.getFile("abc");
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
        // delete A.java
        aJavaFile.delete(true, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN}\n" +
            "    Test006[*]: {CHILDREN}\n" +
            "        src[*]: {CHILDREN}\n" +
            "            foo[*]: {CHILDREN}\n" +
            "                A.java[-]: {}"
        );
        //@formatter:on

        // (re-)create A.java
        aJavaFile.create(new ByteArrayInputStream(new byte[0]), true, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN}\n" +
            "    Test006[*]: {CHILDREN}\n" +
            "        src[*]: {CHILDREN}\n" +
            "            foo[*]: {CHILDREN}\n" +
            "                A.java[+]: {}"
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
            "    Test006[*]: {CHILDREN}\n" +
            "        src[*]: {CHILDREN}\n" +
            "            foo[*]: {CONTENT}\n" +
            "                ResourceDelta(/Test006/src/foo/META-INF)[-]\n" +
            "                ResourceDelta(/Test006/src/foo/abc)[-]"
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
            "    Test006[*]: {CHILDREN}\n" +
            "        src[*]: {CHILDREN}\n" +
            "            foo[*]: {CONTENT}\n" +
            "                ResourceDelta(/Test006/src/foo/META-INF)[+]\n" +
            "                ResourceDelta(/Test006/src/foo/abc)[+]"
        );
        //@formatter:on
    }

    public void test003() throws Exception
    {
        // rename A.java
        aJavaFile.move(new Path("B.java"), true, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN}\n" +
            "    Test006[*]: {CHILDREN}\n" +
            "        src[*]: {CHILDREN}\n" +
            "            foo[*]: {CHILDREN}\n" +
            "                A.java[-]: {MOVED_TO(B.java [in foo [in src [in Test006]]])}\n" +
            "                B.java[+]: {MOVED_FROM(A.java [in foo [in src [in Test006]]])}"
        );
        //@formatter:on
    }

    public void test004() throws Exception
    {
        // move A.java to default package
        aJavaFile.move(new Path("../A.java"), true, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN}\n" +
            "    Test006[*]: {CHILDREN}\n" +
            "        src[*]: {CHILDREN}\n" +
            "            <default>[*]: {CHILDREN}\n" +
            "                A.java[+]: {MOVED_FROM(A.java [in foo [in src [in Test006]]])}\n" +
            "            foo[*]: {CHILDREN}\n" +
            "                A.java[-]: {MOVED_TO(A.java [in <default> [in src [in Test006]]])}"
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
                aJavaFile.move(new Path("A"), true, null);
            }
        }, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN}\n" +
            "    Test006[*]: {CHILDREN}\n" +
            "        src[*]: {CHILDREN}\n" +
            "            foo[*]: {CHILDREN | CONTENT}\n" +
            "                A.java[-]: {}\n" +
            "                abc.java[+]: {}\n" +
            "                ResourceDelta(/Test006/src/foo/A)[+]\n" +
            "                ResourceDelta(/Test006/src/foo/META-INF)[-]\n" +
            "                ResourceDelta(/Test006/src/foo/OSGI-INF)[+]\n" +
            "                ResourceDelta(/Test006/src/foo/abc)[-]"
        );
        //@formatter:on
    }
}
