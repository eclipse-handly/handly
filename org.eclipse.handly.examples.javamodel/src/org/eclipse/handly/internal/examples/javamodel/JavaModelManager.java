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
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.model.IElementChangeEvent;
import org.eclipse.handly.model.IElementChangeListener;
import org.eclipse.handly.model.impl.ElementChangeEvent;
import org.eclipse.handly.model.impl.HandleManager;

/**
 * The manager for the Java model. 
 *
 * @threadsafe This class is intended to be thread-safe
 */
public class JavaModelManager
    implements IResourceChangeListener
{
    /**
     * The sole instance of the manager. 
     */
    public static final JavaModelManager INSTANCE = new JavaModelManager();

    private IJavaModel javaModel;
    private HandleManager handleManager;
    private ListenerList listenerList;

    public void startup() throws Exception
    {
        try
        {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();

            javaModel = new JavaModel(workspace);
            handleManager = new HandleManager(new JavaModelCache());
            listenerList = new ListenerList();

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
        listenerList = null;
        handleManager = null;
        javaModel = null;
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event)
    {
        if (event.getType() != IResourceChangeEvent.POST_CHANGE)
            return;
        JavaDeltaProcessor deltaProcessor = new JavaDeltaProcessor();
        try
        {
            event.getDelta().accept(deltaProcessor);
        }
        catch (CoreException e)
        {
            Activator.log(e.getStatus());
        }
        if (!deltaProcessor.getDelta().isEmpty())
        {
            fireElementChangeEvent(new ElementChangeEvent(
                ElementChangeEvent.POST_CHANGE, deltaProcessor.getDelta()));
        }
    }

    public IJavaModel getJavaModel()
    {
        if (javaModel == null)
            throw new IllegalStateException();
        return javaModel;
    }

    public HandleManager getHandleManager()
    {
        if (handleManager == null)
            throw new IllegalStateException();
        return handleManager;
    }

    public void addElementChangeListener(IElementChangeListener listener)
    {
        if (listenerList == null)
            throw new IllegalStateException();
        listenerList.add(listener);
    }

    public void removeElementChangeListener(IElementChangeListener listener)
    {
        if (listenerList == null)
            throw new IllegalStateException();
        listenerList.remove(listener);
    }

    public void fireElementChangeEvent(final IElementChangeEvent event)
    {
        if (listenerList == null)
            throw new IllegalStateException();
        Object[] listeners = listenerList.getListeners();
        for (final Object listener : listeners)
        {
            SafeRunner.run(new ISafeRunnable()
            {
                public void handleException(Throwable exception)
                {
                    // already logged by Platform
                }

                public void run() throws Exception
                {
                    ((IElementChangeListener)listener).elementChanged(event);
                }
            });
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
                savedState.processResourceChangeEvents(JavaModelManager.INSTANCE);
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
            public void prepareToSave(ISaveContext context)
                throws CoreException
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
