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
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.Path;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.examples.javamodel.IJavaProject;
import org.eclipse.handly.examples.javamodel.JavaModelCore;
import org.eclipse.handly.junit.WorkspaceTestCase;

/**
 * <code>JavaModel</code> notification tests.
 */
public class JavaModelNotificationTest
    extends WorkspaceTestCase
{
    private IJavaModel javaModel = JavaModelCore.getJavaModel();
    private IProject javaProject;
    private IProject simpleProject;
    private JavaModelListener listener = new JavaModelListener();

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        javaProject = setUpProject("Test001");
        simpleProject = setUpProject("SimpleProject");
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
        // delete java project
        javaProject.delete(true, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN}\n" +
            "    Test001[-]: {}"
        );
        //@formatter:on

        // (re-)create java project
        setUpProject(javaProject.getName());

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN}\n" +
            "    Test001[+]: {}"
        );
        //@formatter:on
    }

    public void test002() throws Exception
    {
        // delete simple project
        simpleProject.delete(true, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CONTENT}\n" +
            "    ResourceDelta(/SimpleProject)[-]"
        );
        //@formatter:on

        // (re-)create simple project
        setUpProject(simpleProject.getName());

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CONTENT}\n" +
            "    ResourceDelta(/SimpleProject)[+]"
        );
        //@formatter:on
    }

    public void test003() throws Exception
    {
        // close java project
        javaProject.close(null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN | CONTENT}\n" +
            "    Test001[*]: {OPEN}\n" +
            "    ResourceDelta(/Test001)[*]"
        );
        //@formatter:on

        // (re-)open java project
        javaProject.open(null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN | CONTENT}\n" +
            "    Test001[*]: {OPEN}\n" +
            "    ResourceDelta(/Test001)[*]"
        );
        //@formatter:on
    }

    public void test004() throws Exception
    {
        // close simple project
        simpleProject.close(null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CONTENT}\n" +
            "    ResourceDelta(/SimpleProject)[*]"
        );
        //@formatter:on

        // (re-)open simple project
        simpleProject.open(null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CONTENT}\n" +
            "    ResourceDelta(/SimpleProject)[*]"
        );
        //@formatter:on
    }

    public void test005() throws Exception
    {
        // remove java nature
        IProjectDescription description = javaProject.getDescription();
        description.setNatureIds(new String[0]);
        javaProject.setDescription(description, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN | CONTENT}\n" +
            "    Test001[-]: {}\n" +
            "    ResourceDelta(/Test001)[*]"
        );
        //@formatter:on
    }

    public void test006() throws Exception
    {
        // add java nature
        IProjectDescription description = simpleProject.getDescription();
        description.setNatureIds(new String[] { IJavaProject.NATURE_ID });
        simpleProject.setDescription(description, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN | CONTENT}\n" +
            "    SimpleProject[+]: {}\n" +
            "    ResourceDelta(/SimpleProject)[*]"
        );
        //@formatter:on
    }

    public void test007() throws Exception
    {
        // move java project
        javaProject.move(new Path("Test002"), true, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CHILDREN}\n" +
            "    Test001[-]: {MOVED_TO(Test002)}\n" +
            "    Test002[+]: {MOVED_FROM(Test001)}"
        );
        //@formatter:on
    }

    public void test008() throws Exception
    {
        // move simple project
        simpleProject.move(new Path("SimpleProject2"), true, null);

        //@formatter:off
        listener.assertDelta(
            "Java Model[*]: {CONTENT}\n" +
            "    ResourceDelta(/SimpleProject)[-]\n" +
            "    ResourceDelta(/SimpleProject2)[+]"
        );
        //@formatter:on
    }
}
