/*******************************************************************************
 * Copyright (c) 2014 1C LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.basic.ui.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.handly.examples.basic.ui.model.FooModelCore;
import org.eclipse.handly.examples.basic.ui.model.IFooElement;
import org.eclipse.handly.examples.basic.ui.model.IFooFile;
import org.eclipse.handly.examples.basic.ui.model.IFooProject;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.IHandleDelta;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.handly.model.impl.Handle;
import org.eclipse.handly.model.impl.HandleDelta;

/**
 * This class is used by the <code>FooModelManager</code> to convert 
 * resource deltas into Foo element deltas. It also does some processing 
 * on the Foo elements involved (e.g. closing them).
 */
class FooDeltaProcessor
    implements IResourceDeltaVisitor
{
    private HandleDelta currentDelta = new HandleDelta(
        FooModelCore.getFooModel());
    private Set<String> oldFooProjectNames = new HashSet<String>();

    /**
     * Returns the Foo element delta built from the resource delta. 
     * Returns an empty delta if no Foo elements were affected 
     * by the resource change.
     * 
     * @return Foo element delta (never <code>null</code>)
     */
    public HandleDelta getDelta()
    {
        return currentDelta;
    }

    @Override
    public boolean visit(IResourceDelta delta) throws CoreException
    {
        switch (delta.getResource().getType())
        {
        case IResource.ROOT:
            return processRoot(delta);

        case IResource.PROJECT:
            return processProject(delta);

        case IResource.FOLDER:
            return processFolder(delta);

        case IResource.FILE:
            return processFile(delta);

        default:
            return true;
        }
    }

    private boolean processRoot(IResourceDelta delta) throws CoreException
    {
        initOldFooProjectNames();

        if ((delta.getFlags() & IResourceDelta.MARKERS) != 0)
            markersChanged(FooModelCore.getFooModel(), delta.getMarkerDeltas());

        return true;
    }

    private boolean processProject(IResourceDelta delta) throws CoreException
    {
        switch (delta.getKind())
        {
        case IResourceDelta.ADDED:
            return processAddedProject(delta);

        case IResourceDelta.REMOVED:
            return processRemovedProject(delta);

        case IResourceDelta.CHANGED:
            return processChangedProject(delta);

        default:
            return true;
        }
    }

    private boolean processAddedProject(IResourceDelta delta)
        throws CoreException
    {
        IProject project = (IProject)delta.getResource();
        if (project.hasNature(IFooProject.NATURE_ID))
        {
            IFooProject fooProject = FooModelCore.create(project);
            addToModel(fooProject);
            translateAddedDelta(delta, fooProject);
        }
        else
        {
            addResourceDelta(delta);
        }
        return false;
    }

    private boolean processRemovedProject(IResourceDelta delta)
        throws CoreException
    {
        IProject project = (IProject)delta.getResource();
        if (wasFooProject(project))
        {
            IFooProject fooProject = FooModelCore.create(project);
            removeFromModel(fooProject);
            translateRemovedDelta(delta, fooProject);
        }
        else
        {
            addResourceDelta(delta);
        }
        return false;
    }

    private boolean processChangedProject(IResourceDelta delta)
        throws CoreException
    {
        IProject project = (IProject)delta.getResource();
        IFooProject fooProject = FooModelCore.create(project);

        if ((delta.getFlags() & IResourceDelta.OPEN) != 0)
        {
            if (project.isOpen())
            {
                if (project.hasNature(IFooProject.NATURE_ID))
                {
                    addToModel(fooProject);
                    currentDelta.insertAdded(fooProject, IHandleDelta.F_OPEN);
                }
                else
                {
                    addResourceDelta(delta);
                }
            }
            else
            {
                if (wasFooProject(project))
                {
                    removeFromModel(fooProject);
                    currentDelta.insertRemoved(fooProject, IHandleDelta.F_OPEN);
                }
                else
                {
                    addResourceDelta(delta);
                }
            }
            return false;
        }

        boolean isFooProject = project.hasNature(IFooProject.NATURE_ID);
        if ((delta.getFlags() & IResourceDelta.DESCRIPTION) != 0)
        {
            boolean wasFooProject = wasFooProject(project);
            if (wasFooProject != isFooProject)
            {
                // Foo nature has been added or removed
                if (isFooProject)
                {
                    addToModel(fooProject);
                    currentDelta.insertAdded(fooProject,
                        IHandleDelta.F_DESCRIPTION);
                }
                else
                {
                    removeFromModel(fooProject);
                    currentDelta.insertRemoved(fooProject,
                        IHandleDelta.F_DESCRIPTION);
                }
                return false; // when Foo nature is added/removed don't process children
            }
            else
            {
                if (isFooProject)
                {
                    currentDelta.insertChanged(fooProject,
                        IHandleDelta.F_DESCRIPTION);
                }
            }
        }

        if (isFooProject)
        {
            if ((delta.getFlags() & IResourceDelta.MARKERS) != 0)
                markersChanged(fooProject, delta.getMarkerDeltas());

            Body parentBody = findBody(fooProject.getParent());
            IHandle[] children = parentBody.getChildren();
            if (!Arrays.asList(children).contains(fooProject))
                addToModel(fooProject); // in case the project was removed then added then changed

            return true;
        }
        else
        {
            addResourceDelta(delta);
            return false;
        }
    }

    private boolean processFolder(IResourceDelta delta)
    {
        addResourceDelta(delta);
        return false;
    }

    private boolean processFile(IResourceDelta delta)
    {
        switch (delta.getKind())
        {
        case IResourceDelta.ADDED:
            return processAddedFile(delta);

        case IResourceDelta.REMOVED:
            return processRemovedFile(delta);

        case IResourceDelta.CHANGED:
            return processChangedFile(delta);

        default:
            return false;
        }
    }

    private boolean processAddedFile(IResourceDelta delta)
    {
        IFile file = (IFile)delta.getResource();
        IFooFile fooFile = FooModelCore.create(file);
        if (fooFile != null)
        {
            addToModel(fooFile);
            translateAddedDelta(delta, fooFile);
        }
        else
        {
            addResourceDelta(delta);
        }
        return false;
    }

    private boolean processRemovedFile(IResourceDelta delta)
    {
        IFile file = (IFile)delta.getResource();
        IFooFile fooFile = FooModelCore.create(file);
        if (fooFile != null)
        {
            removeFromModel(fooFile);
            translateRemovedDelta(delta, fooFile);
        }
        else
        {
            addResourceDelta(delta);
        }
        return false;
    }

    private boolean processChangedFile(IResourceDelta delta)
    {
        IFile file = (IFile)delta.getResource();
        IFooFile fooFile = FooModelCore.create(file);
        if (fooFile != null)
        {
            if ((delta.getFlags() & ~(IResourceDelta.MARKERS | IResourceDelta.SYNC)) != 0)
                contentChanged(fooFile);

            if ((delta.getFlags() & IResourceDelta.MARKERS) != 0)
                markersChanged(fooFile, delta.getMarkerDeltas());

            if ((delta.getFlags() & IResourceDelta.SYNC) != 0)
                currentDelta.insertChanged(fooFile, IHandleDelta.F_SYNC);
        }
        else
        {
            addResourceDelta(delta);
        }
        return false;
    }

    private void initOldFooProjectNames() throws CoreException
    {
        IFooProject[] fooProjects = FooModelCore.getFooModel().getFooProjects();
        for (IFooProject fooProject : fooProjects)
        {
            oldFooProjectNames.add(fooProject.getName());
        }
    }

    private boolean wasFooProject(IProject project)
    {
        return oldFooProjectNames.contains(project.getName());
    }

    private void addToModel(IFooElement element)
    {
        Body parentBody = findBody(element.getParent());
        if (parentBody != null)
            parentBody.addChild(element);
        close(element);
    }

    private void removeFromModel(IFooElement element)
    {
        Body parentBody = findBody(element.getParent());
        if (parentBody != null)
            parentBody.removeChild(element);
        close(element);
    }

    private void translateAddedDelta(IResourceDelta delta, IFooElement element)
    {
        if ((delta.getFlags() & IResourceDelta.MOVED_FROM) == 0) // regular addition
        {
            currentDelta.insertAdded(element);
        }
        else
        {
            IFooElement movedFromElement =
                FooModelCore.create(getResource(delta.getMovedFromPath(),
                    delta.getResource().getType()));
            if (movedFromElement == null)
                currentDelta.insertAdded(element);
            else
                currentDelta.insertMovedTo(element, movedFromElement);
        }
    }

    private void translateRemovedDelta(IResourceDelta delta, IFooElement element)
    {
        if ((delta.getFlags() & IResourceDelta.MOVED_TO) == 0) // regular removal
        {
            currentDelta.insertRemoved(element);
        }
        else
        {
            IFooElement movedToElement =
                FooModelCore.create(getResource(delta.getMovedToPath(),
                    delta.getResource().getType()));
            if (movedToElement == null)
                currentDelta.insertRemoved(element);
            else
                currentDelta.insertMovedFrom(element, movedToElement);
        }
    }

    private void contentChanged(IFooFile fooFile)
    {
        if (fooFile.isWorkingCopy())
        {
            currentDelta.insertChanged(fooFile, IHandleDelta.F_CONTENT
                | IHandleDelta.F_UNDERLYING_RESOURCE);
            return;
        }

        close(fooFile);
        currentDelta.insertChanged(fooFile, IHandleDelta.F_CONTENT);
    }

    private void markersChanged(IFooElement fooElement,
        IMarkerDelta[] markerDeltas)
    {
        HandleDelta delta = currentDelta.getDeltaFor(fooElement);
        if (delta == null)
        {
            delta = new HandleDelta(fooElement);
            currentDelta.insert(delta);
        }
        delta.setMarkerDeltas(markerDeltas);
    }

    private void addResourceDelta(IResourceDelta delta)
    {
        HandleDelta handleDelta;
        IResource parent = delta.getResource().getParent();
        if (parent instanceof IWorkspaceRoot)
            handleDelta = currentDelta;
        else if (parent instanceof IProject)
        {
            IFooProject fooProject = FooModelCore.create((IProject)parent);
            handleDelta = currentDelta.getDeltaFor(fooProject);
            if (handleDelta == null)
            {
                handleDelta = new HandleDelta(fooProject);
                currentDelta.insert(handleDelta);
            }
            if ((delta.getKind() & (IResourceDelta.ADDED | IResourceDelta.REMOVED)) != 0)
            {
                // reset non-Foo resources
                FooProjectBody body = (FooProjectBody)findBody(fooProject);
                if (body != null)
                    body.setNonFooResources(null);
            }
        }
        else
            throw new AssertionError();
        handleDelta.addResourceDelta(delta);
    }

    private static Body findBody(IFooElement element)
    {
        return ((Handle)element).findBody();
    }

    private static void close(IFooElement element)
    {
        ((Handle)element).close();
    }

    private static IResource getResource(IPath fullPath, int resourceType)
    {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        switch (resourceType)
        {
        case IResource.ROOT:
            return root;

        case IResource.PROJECT:
            return root.getProject(fullPath.lastSegment());

        case IResource.FOLDER:
            return root.getFolder(fullPath);

        case IResource.FILE:
            return root.getFile(fullPath);

        default:
            return null;
        }
    }
}
