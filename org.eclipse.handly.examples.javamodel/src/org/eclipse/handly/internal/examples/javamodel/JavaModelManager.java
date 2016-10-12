/*******************************************************************************
 * Copyright (c) 2015, 2016 1C-Soft LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel;

import static org.eclipse.handly.context.Contexts.EMPTY_CONTEXT;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.handly.context.Context;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.examples.javamodel.IJavaElement;
import org.eclipse.handly.model.impl.ElementChangeEvent;
import org.eclipse.handly.model.impl.ElementDelta;
import org.eclipse.handly.model.impl.ElementManager;
import org.eclipse.handly.model.impl.IModelManager;
import org.eclipse.handly.model.impl.INotificationManager;
import org.eclipse.handly.model.impl.NotificationManager;

/**
 * The manager for the Java model.
 *
 * @threadsafe This class is intended to be thread-safe
 */
public class JavaModelManager
    implements IModelManager, IResourceChangeListener
{
    /**
     * The sole instance of the manager.
     */
    public static final JavaModelManager INSTANCE = new JavaModelManager();

    private JavaModel javaModel;
    private ElementManager elementManager;
    private NotificationManager notificationManager;
    private DeltaProcessingState deltaState;
    private Context modelContext;
    private Map<IProject, PerProjectInfo> perProjectInfo =
        new HashMap<IProject, PerProjectInfo>(5); // NOTE: this object itself is used as a lock to synchronize creation/removal of per project info

    public void startup() throws Exception
    {
        try
        {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();

            javaModel = new JavaModel(workspace);
            elementManager = new ElementManager(new JavaModelCache());
            notificationManager = new NotificationManager();
            deltaState = new DeltaProcessingState();
            deltaState.initialize();

            modelContext = new Context();
            modelContext.bind(INotificationManager.class).to(
                notificationManager);
            modelContext.bind(ElementDelta.Factory.class).to(
                element -> new JavaElementDelta((IJavaElement)element));

            workspace.addResourceChangeListener(this,
                IResourceChangeEvent.POST_CHANGE);

            new SavedStateJob().schedule();
        }
        catch (Exception e)
        {
            shutdown();
            throw e;
        }
    }

    public void shutdown() throws Exception
    {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        modelContext = null;
        deltaState = null;
        notificationManager = null;
        elementManager = null;
        javaModel = null;
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event)
    {
        if (event.getType() != IResourceChangeEvent.POST_CHANGE)
            return;
        DeltaProcessor deltaProcessor = new DeltaProcessor(deltaState);
        try
        {
            event.getDelta().accept(deltaProcessor);
        }
        catch (CoreException e)
        {
            Activator.log(e.getStatus());
        }
        finally
        {
            deltaState.reset();
        }
        if (!deltaProcessor.isEmptyDelta())
        {
            getNotificationManager().fireElementChangeEvent(
                new ElementChangeEvent(ElementChangeEvent.POST_CHANGE,
                    deltaProcessor.getDelta()));
        }
    }

    @Override
    public JavaModel getModel()
    {
        if (javaModel == null)
            throw new IllegalStateException();
        return javaModel;
    }

    @Override
    public ElementManager getElementManager()
    {
        if (elementManager == null)
            throw new IllegalStateException();
        return elementManager;
    }

    public NotificationManager getNotificationManager()
    {
        if (notificationManager == null)
            throw new IllegalStateException();
        return notificationManager;
    }

    public IContext getModelContext()
    {
        if (modelContext == null)
            throw new IllegalStateException();
        return modelContext;
    }

    /**
     * Returns the per-project info for the given project.
     * If specified, create the info if the info doesn't exist.
     * <p>
     * Note that no check is done at this time on the existence
     * or the nature of this project.
     * </p>
     *
     * @param project the given project
     * @param create indicates whether to create the info if it doesn't exist
     * @return the per-project info for the given project, or <code>null</code>
     *  if the info doesn't exist and <code>create == false</code>
     */
    public PerProjectInfo getPerProjectInfo(IProject project, boolean create)
    {
        synchronized (perProjectInfo)
        {
            PerProjectInfo info = perProjectInfo.get(project);
            if (info == null && create)
            {
                info = new PerProjectInfo(project);
                perProjectInfo.put(project, info);
            }
            return info;
        }
    }

    /**
     * Returns the per-project info for the given project. If the info
     * doesn't exist, check for the project existence and create the info.
     *
     * @param project the given project
     * @return the per-project info for the given project (never <code>null</code>)
     * @throws CoreException if the Java project doesn't exist
     */
    public PerProjectInfo getPerProjectInfoCheckExistence(IProject project)
        throws CoreException
    {
        PerProjectInfo info = getPerProjectInfo(project, false);
        if (info == null)
        {
            new JavaProject(javaModel, project).hValidateExistence(
                EMPTY_CONTEXT);
            info = getPerProjectInfo(project, true);
        }
        return info;
    }

    public void removePerProjectInfo(IProject project)
    {
        synchronized (perProjectInfo)
        {
            perProjectInfo.remove(project);
        }
    }

    private JavaModelManager()
    {
    }

    private static class SavedStateJob
        extends WorkspaceJob
    {
        public SavedStateJob()
        {
            super("SavedState"); //$NON-NLS-1$
            setSystem(true);
            setPriority(SHORT);
            setRule(ResourcesPlugin.getWorkspace().getRoot());
        }

        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor)
            throws CoreException
        {
            ISavedState savedState =
                ResourcesPlugin.getWorkspace().addSaveParticipant(
                    Activator.PLUGIN_ID, new SaveParticipant());
            if (savedState != null)
                savedState.processResourceChangeEvents(
                    JavaModelManager.INSTANCE);
            return Status.OK_STATUS;
        }

        private static class SaveParticipant
            implements ISaveParticipant
        {
            @Override
            public void doneSaving(ISaveContext context)
            {
            }

            @Override
            public void prepareToSave(ISaveContext context) throws CoreException
            {
            }

            @Override
            public void rollback(ISaveContext context)
            {
            }

            @Override
            public void saving(ISaveContext context) throws CoreException
            {
                if (context.getKind() == ISaveContext.FULL_SAVE)
                {
                    context.needDelta();
                }
            }
        }
    }
}
