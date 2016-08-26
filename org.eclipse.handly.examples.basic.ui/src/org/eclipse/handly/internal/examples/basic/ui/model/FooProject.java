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

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.examples.basic.ui.model.IFooFile;
import org.eclipse.handly.examples.basic.ui.model.IFooProject;
import org.eclipse.handly.internal.examples.basic.ui.Activator;
import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.handly.model.impl.Element;
import org.eclipse.handly.model.impl.ElementManager;

/**
 * Represents a Foo project.
 */
public class FooProject
    extends Element
    implements IFooProject
{
    private final IProject project;

    /**
     * Constructs a handle for a Foo project with the given parent element
     * and the given underlying workspace project.
     *
     * @param parent the parent of the element (not <code>null</code>)
     * @param project the workspace project underlying the element
     *  (not <code>null</code>)
     */
    public FooProject(FooModel parent, IProject project)
    {
        super(parent, project.getName());
        if (parent == null)
            throw new IllegalArgumentException();
        this.project = project;
    }

    @Override
    public void create(IProgressMonitor monitor) throws CoreException
    {
        create(null, monitor);
    }

    @Override
    public void create(final URI location, IProgressMonitor monitor)
        throws CoreException
    {
        final IWorkspace workspace = getParent().getWorkspace();
        workspace.run(new IWorkspaceRunnable()
        {
            @Override
            public void run(IProgressMonitor monitor) throws CoreException
            {
                if (monitor == null)
                    monitor = new NullProgressMonitor();
                try
                {
                    monitor.beginTask("", 4); //$NON-NLS-1$

                    IProjectDescription description =
                        workspace.newProjectDescription(getName());
                    description.setLocationURI(location);
                    project.create(description, new SubProgressMonitor(monitor,
                        1));
                    project.open(new SubProgressMonitor(monitor, 1));

                    description.setNatureIds(new String[] {
                        "org.eclipse.xtext.ui.shared.xtextNature", //$NON-NLS-1$
                        IFooProject.NATURE_ID });
                    project.setDescription(description, new SubProgressMonitor(
                        monitor, 1));

                    project.setDefaultCharset("UTF-8", //$NON-NLS-1$
                        new SubProgressMonitor(monitor, 1));
                }
                finally
                {
                    monitor.done();
                }
            }
        }, monitor);
    }

    @Override
    public IFooFile getFooFile(String name)
    {
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0)
            return null;
        String fileExtension = name.substring(lastDot + 1);
        if (!IFooFile.EXT.equals(fileExtension))
            return null;
        return new FooFile(this, project.getFile(name));
    }

    @Override
    public IFooFile[] getFooFiles() throws CoreException
    {
        IElement[] children = getChildren();
        int length = children.length;
        IFooFile[] result = new IFooFile[length];
        System.arraycopy(children, 0, result, 0, length);
        return result;
    }

    @Override
    public IResource[] getNonFooResources() throws CoreException
    {
        return ((FooProjectBody)hBody()).getNonFooResources(this);
    }

    @Override
    public IProject getProject()
    {
        return project;
    }

    @Override
    public IResource hResource()
    {
        return project;
    }

    @Override
    protected ElementManager hElementManager()
    {
        return FooModelManager.INSTANCE.getElementManager();
    }

    @Override
    protected void hValidateExistence(IContext context) throws CoreException
    {
        if (!project.exists())
            throw new CoreException(Activator.createErrorStatus(
                MessageFormat.format(
                    "Project ''{0}'' does not exist in workspace", getName()),
                null));

        if (!project.isOpen())
            throw new CoreException(Activator.createErrorStatus(
                MessageFormat.format("Project ''{0}'' is not open", getName()),
                null));

        if (!project.hasNature(NATURE_ID))
            throw new CoreException(Activator.createErrorStatus(
                MessageFormat.format(
                    "Project ''{0}'' does not have the Foo nature", getName()),
                null));
    }

    @Override
    protected void hBuildStructure(IContext context, IProgressMonitor monitor)
        throws CoreException
    {
        IResource[] members = project.members();
        List<IFooFile> fooFiles = new ArrayList<>(members.length);
        for (IResource member : members)
        {
            if (member instanceof IFile)
            {
                IFile file = (IFile)member;
                if (IFooFile.EXT.equals(file.getFileExtension()))
                {
                    FooFile fooFile = new FooFile(this, file);
                    if (fooFile != null)
                        fooFiles.add(fooFile);
                }
            }
        }
        FooProjectBody body = new FooProjectBody();
        body.setChildren(fooFiles.toArray(Body.NO_CHILDREN));
        context.get(NEW_ELEMENTS).put(this, body);
    }
}
