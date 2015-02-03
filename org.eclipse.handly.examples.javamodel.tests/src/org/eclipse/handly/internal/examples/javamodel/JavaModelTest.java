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
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.examples.javamodel.IJavaProject;
import org.eclipse.handly.examples.javamodel.JavaModelCore;
import org.eclipse.handly.junit.WorkspaceTestCase;

/**
 * <code>JavaModel</code> tests.
 */
public class JavaModelTest
    extends WorkspaceTestCase
{
    private IJavaModel javaModel = JavaModelCore.getJavaModel();
    private IJavaProject javaProject = javaModel.getJavaProject("Test001");
    private IProject simpleProject;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        simpleProject = setUpProject("SimpleProject");
        setUpProject(javaProject.getName());
    }

    public void test001() throws Exception
    {
        IJavaProject[] javaProjects = javaModel.getJavaProjects();
        assertEquals(1, javaProjects.length);
        assertEquals(javaProject, javaProjects[0]);
        assertTrue(javaProject.exists());

        IProject[] nonJavaProjects = javaModel.getNonJavaProjects();
        assertEquals(1, nonJavaProjects.length);
        assertEquals(simpleProject, nonJavaProjects[0]);

        // delete java project
        javaProject.getProject().delete(true, null);

        javaProjects = javaModel.getJavaProjects();
        assertEquals(0, javaProjects.length);
        assertFalse(javaProject.exists());

        nonJavaProjects = javaModel.getNonJavaProjects();
        assertEquals(1, nonJavaProjects.length);
        assertEquals(simpleProject, nonJavaProjects[0]);

        // (re-)create java project
        setUpProject(javaProject.getName());

        javaProjects = javaModel.getJavaProjects();
        assertEquals(1, javaProjects.length);
        assertEquals(javaProject, javaProjects[0]);
        assertTrue(javaProject.exists());

        nonJavaProjects = javaModel.getNonJavaProjects();
        assertEquals(1, nonJavaProjects.length);
        assertEquals(simpleProject, nonJavaProjects[0]);

        // delete simple project
        simpleProject.delete(true, null);

        javaProjects = javaModel.getJavaProjects();
        assertEquals(1, javaProjects.length);
        assertEquals(javaProject, javaProjects[0]);
        assertTrue(javaProject.exists());

        nonJavaProjects = javaModel.getNonJavaProjects();
        assertEquals(0, nonJavaProjects.length);

        // (re-)create simple project
        setUpProject(simpleProject.getName());

        javaProjects = javaModel.getJavaProjects();
        assertEquals(1, javaProjects.length);
        assertEquals(javaProject, javaProjects[0]);
        assertTrue(javaProject.exists());

        nonJavaProjects = javaModel.getNonJavaProjects();
        assertEquals(1, nonJavaProjects.length);
        assertEquals(simpleProject, nonJavaProjects[0]);

        // close java project
        javaProject.getProject().close(null);

        javaProjects = javaModel.getJavaProjects();
        assertEquals(0, javaProjects.length);
        assertFalse(javaProject.exists());

        nonJavaProjects = javaModel.getNonJavaProjects();
        assertEquals(2, nonJavaProjects.length);
        assertTrue(Arrays.asList(nonJavaProjects).contains(simpleProject));
        assertTrue(Arrays.asList(nonJavaProjects).contains(
            javaProject.getProject()));

        // (re-)open java project
        javaProject.getProject().open(null);

        javaProjects = javaModel.getJavaProjects();
        assertEquals(1, javaProjects.length);
        assertEquals(javaProject, javaProjects[0]);
        assertTrue(javaProject.exists());

        nonJavaProjects = javaModel.getNonJavaProjects();
        assertEquals(1, nonJavaProjects.length);
        assertEquals(simpleProject, nonJavaProjects[0]);

        // close simple project
        simpleProject.close(null);

        javaProjects = javaModel.getJavaProjects();
        assertEquals(1, javaProjects.length);
        assertEquals(javaProject, javaProjects[0]);
        assertTrue(javaProject.exists());

        nonJavaProjects = javaModel.getNonJavaProjects();
        assertEquals(1, nonJavaProjects.length);
        assertEquals(simpleProject, nonJavaProjects[0]);

        // (re-)open simple project
        simpleProject.open(null);

        javaProjects = javaModel.getJavaProjects();
        assertEquals(1, javaProjects.length);
        assertEquals(javaProject, javaProjects[0]);
        assertTrue(javaProject.exists());

        nonJavaProjects = javaModel.getNonJavaProjects();
        assertEquals(1, nonJavaProjects.length);
        assertEquals(simpleProject, nonJavaProjects[0]);
    }

    public void test002() throws Exception
    {
        IJavaProject[] javaProjects = javaModel.getJavaProjects();
        assertEquals(1, javaProjects.length);
        assertEquals(javaProject, javaProjects[0]);
        assertTrue(javaProject.exists());

        IProject[] nonJavaProjects = javaModel.getNonJavaProjects();
        assertEquals(1, nonJavaProjects.length);
        assertEquals(simpleProject, nonJavaProjects[0]);

        // remove java nature
        IProjectDescription description =
            javaProject.getProject().getDescription();
        description.setNatureIds(new String[0]);
        javaProject.getProject().setDescription(description, null);

        javaProjects = javaModel.getJavaProjects();
        assertEquals(0, javaProjects.length);
        assertFalse(javaProject.exists());

        nonJavaProjects = javaModel.getNonJavaProjects();
        assertEquals(2, nonJavaProjects.length);
        assertTrue(Arrays.asList(nonJavaProjects).contains(simpleProject));
        assertTrue(Arrays.asList(nonJavaProjects).contains(
            javaProject.getProject()));
    }

    public void test003() throws Exception
    {
        IJavaProject[] javaProjects = javaModel.getJavaProjects();
        assertEquals(1, javaProjects.length);
        assertEquals(javaProject, javaProjects[0]);
        assertTrue(javaProject.exists());

        IProject[] nonJavaProjects = javaModel.getNonJavaProjects();
        assertEquals(1, nonJavaProjects.length);
        assertEquals(simpleProject, nonJavaProjects[0]);

        // add java nature
        IProjectDescription description = simpleProject.getDescription();
        description.setNatureIds(new String[] { IJavaProject.NATURE_ID });
        simpleProject.setDescription(description, null);

        javaProjects = javaModel.getJavaProjects();
        assertEquals(2, javaProjects.length);
        assertTrue(Arrays.asList(javaProjects).contains(javaProject));
        assertTrue(Arrays.asList(javaProjects).contains(
            javaModel.getJavaProject(simpleProject.getName())));

        nonJavaProjects = javaModel.getNonJavaProjects();
        assertEquals(0, nonJavaProjects.length);
    }

    public void test004() throws Exception
    {
        IJavaProject[] javaProjects = javaModel.getJavaProjects();
        assertEquals(1, javaProjects.length);
        assertEquals(javaProject, javaProjects[0]);
        assertTrue(javaProject.exists());

        IProject[] nonJavaProjects = javaModel.getNonJavaProjects();
        assertEquals(1, nonJavaProjects.length);
        assertEquals(simpleProject, nonJavaProjects[0]);

        // move java project
        IJavaProject otherProject = javaModel.getJavaProject("Test002");
        assertFalse(otherProject.exists());
        javaProject.getProject().move(otherProject.getPath(), true, null);
        assertFalse(javaProject.exists());

        javaProjects = javaModel.getJavaProjects();
        assertEquals(1, javaProjects.length);
        assertEquals(otherProject, javaProjects[0]);
        assertTrue(otherProject.exists());

        nonJavaProjects = javaModel.getNonJavaProjects();
        assertEquals(1, nonJavaProjects.length);
        assertEquals(simpleProject, nonJavaProjects[0]);
    }

    public void test005() throws Exception
    {
        IJavaProject[] javaProjects = javaModel.getJavaProjects();
        assertEquals(1, javaProjects.length);
        assertEquals(javaProject, javaProjects[0]);
        assertTrue(javaProject.exists());

        IProject[] nonJavaProjects = javaModel.getNonJavaProjects();
        assertEquals(1, nonJavaProjects.length);
        assertEquals(simpleProject, nonJavaProjects[0]);

        // move simple project
        IProject otherProject = getProject("SimpleProject2");
        simpleProject.move(otherProject.getFullPath(), true, null);

        javaProjects = javaModel.getJavaProjects();
        assertEquals(1, javaProjects.length);
        assertEquals(javaProject, javaProjects[0]);
        assertTrue(javaProject.exists());

        nonJavaProjects = javaModel.getNonJavaProjects();
        assertEquals(1, nonJavaProjects.length);
        assertEquals(otherProject, nonJavaProjects[0]);
    }
}
