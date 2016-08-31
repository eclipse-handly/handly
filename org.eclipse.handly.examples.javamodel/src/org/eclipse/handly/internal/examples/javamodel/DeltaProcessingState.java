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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.handly.examples.javamodel.ICompilationUnit;
import org.eclipse.handly.examples.javamodel.IJavaElement;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.examples.javamodel.IJavaProject;
import org.eclipse.handly.examples.javamodel.IPackageFragment;
import org.eclipse.handly.examples.javamodel.IPackageFragmentRoot;
import org.eclipse.jdt.core.IClasspathEntry;

/**
 * Keeps the global state used during Java element delta processing.
 * <p>
 * This class is only used inside the dynamic scope of the resource POST_CHANGE
 * notification, which is guarded by the workspace lock. Hence, it doesn't need
 * to be thread-safe.
 * </p>
 */
class DeltaProcessingState
{
    private Map<JavaProject, ClasspathInfo> classpaths;
    private Map<JavaProject, ClasspathInfo> oldClasspaths;
    private Set<String> oldJavaProjectNames;

    void initialize()
    {
        classpaths = new HashMap<JavaProject, ClasspathInfo>();
        IJavaProject[] javaProjects;
        try
        {
            javaProjects = getJavaModel().getJavaProjects();
        }
        catch (CoreException e)
        {
            // nothing can be done
            return;
        }
        for (IJavaProject each : javaProjects)
        {
            JavaProject javaProject = (JavaProject)each;
            IClasspathEntry[] classpath;
            IPath outputLocation;
            try
            {
                classpath = javaProject.getRawClasspath();
                outputLocation = javaProject.getOutputLocation();
            }
            catch (CoreException e)
            {
                // continue with next project
                continue;
            }
            classpaths.put(javaProject, new ClasspathInfo(classpath,
                outputLocation));
        }
    }

    void reset()
    {
        oldClasspaths = null;
        oldJavaProjectNames = null;
    }

    void initOldClasspathInfo()
    {
        oldClasspaths = new HashMap<JavaProject, ClasspathInfo>(classpaths);
    }

    void initOldJavaProjectNames()
    {
        IJavaProject[] javaProjects;
        try
        {
            javaProjects = getJavaModel().getJavaProjects();
        }
        catch (CoreException e)
        {
            // nothing can be done
            return;
        }
        Set<String> javaProjectNames = new HashSet<String>(javaProjects.length);
        for (IJavaProject javaProject : javaProjects)
        {
            javaProjectNames.add(javaProject.getElementName());
        }
        oldJavaProjectNames = javaProjectNames;
    }

    Set<String> getOldJavaProjectNames()
    {
        return oldJavaProjectNames;
    }

    boolean classpathChanged(JavaProject javaProject, boolean remove)
    {
        if (remove)
        {
            classpaths.remove(javaProject);
            return true;
        }
        else
        {
            javaProject.resetRawClasspath();
            IClasspathEntry[] classpath;
            IPath outputLocation;
            try
            {
                classpath = javaProject.getRawClasspath();
                outputLocation = javaProject.getOutputLocation();
            }
            catch (CoreException e)
            {
                return true;
            }
            ClasspathInfo classpathInfo = new ClasspathInfo(classpath,
                outputLocation);
            ClasspathInfo oldClasspathInfo = classpaths.put(javaProject,
                classpathInfo);
            return !classpathInfo.isEqualTo(oldClasspathInfo);
        }
    }

    IJavaElement createElement(IResource resource, boolean oldState)
    {
        if (resource instanceof IProject)
            return createElement((IProject)resource, oldState);
        else if (resource instanceof IFolder)
            return createElement((IFolder)resource, oldState);
        else if (resource instanceof IFile)
            return createElement((IFile)resource, oldState);
        else if (resource instanceof IWorkspaceRoot)
            return getJavaModel();
        else
            throw new IllegalArgumentException();
    }

    IJavaProject createElement(IProject project, boolean oldState)
    {
        String name = project.getName();
        if (oldState)
        {
            if (!oldJavaProjectNames.contains(name))
                return null;
        }
        else
        {
            try
            {
                if (!project.hasNature(IJavaProject.NATURE_ID))
                    return null;
            }
            catch (CoreException e)
            {
                return null;
            }
        }
        return getJavaModel().getJavaProject(name);
    }

    IJavaElement createElement(IFolder folder, boolean oldState)
    {
        JavaProject javaProject = (JavaProject)createElement(
            folder.getProject(), oldState);
        if (javaProject == null)
            return null;
        ClasspathInfo classpathInfo;
        if (oldState)
            classpathInfo = oldClasspaths.get(javaProject);
        else
            classpathInfo = classpaths.get(javaProject);
        if (classpathInfo == null)
            return null;
        IPackageFragment pkg = javaProject.findPackageFragment(folder,
            classpathInfo.classpath);
        if (pkg != null && pkg.isDefaultPackage())
            return pkg.getParent();
        return pkg;
    }

    IJavaElement createElement(IFile file, boolean oldState)
    {
        // In this example model, we consider only Java source files
        if ("java".equals(file.getFileExtension())) //$NON-NLS-1$
            return createCompilationUnitFrom(file, oldState);
        return null;
    }

    ICompilationUnit createCompilationUnitFrom(IFile file, boolean oldState)
    {
        IContainer parent = file.getParent();
        if (!(parent instanceof IFolder))
            return null; // in this example model, CU's parent must be a folder
        IJavaElement element = createElement((IFolder)parent, oldState);
        if (element == null)
            return null;
        IPackageFragment pkg = element instanceof IPackageFragment
            ? (IPackageFragment)element
            : ((IPackageFragmentRoot)element).getPackageFragment(""); //$NON-NLS-1$
        CompilationUnit cu = (CompilationUnit)pkg.getCompilationUnit(
            file.getName());
        if (cu.validateCompilationUnitName().getSeverity() == IStatus.ERROR)
            return null;
        return cu;
    }

    IJavaModel getJavaModel()
    {
        return JavaModelManager.INSTANCE.getModel();
    }

    private static class ClasspathInfo
    {
        final IClasspathEntry[] classpath;
        final IPath outputLocation;

        ClasspathInfo(IClasspathEntry[] classpath, IPath outputLocation)
        {
            if (classpath == null)
                throw new IllegalArgumentException();
            if (outputLocation == null)
                throw new IllegalArgumentException();
            this.classpath = classpath;
            this.outputLocation = outputLocation;
        }

        boolean isEqualTo(ClasspathInfo other)
        {
            if (other == null)
                return false;
            return Arrays.equals(classpath, other.classpath)
                && outputLocation.equals(other.outputLocation);
        }
    }
}
