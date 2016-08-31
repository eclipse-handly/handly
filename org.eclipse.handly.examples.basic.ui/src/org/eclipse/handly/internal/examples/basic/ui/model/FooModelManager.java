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

import static org.eclipse.handly.context.Contexts.EMPTY_CONTEXT;

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
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.internal.examples.basic.ui.Activator;
import org.eclipse.handly.model.IElementChangeEvent;
import org.eclipse.handly.model.IElementChangeListener;
import org.eclipse.handly.model.impl.ElementChangeEvent;
import org.eclipse.handly.model.impl.ElementManager;
import org.eclipse.handly.model.impl.IModelManager;

/**
 * The manager for the Foo Model.
 *
 * @threadsafe This class is intended to be thread-safe
 */
public class FooModelManager
    implements IModelManager, IResourceChangeListener
{
    /**
     * The sole instance of the manager.
     */
    public static final FooModelManager INSTANCE = new FooModelManager();

    private FooModel fooModel;
    private ElementManager elementManager;
    private ListenerList listenerList;

    public void startup() throws Exception
    {
        try
        {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();

            fooModel = new FooModel(workspace);
            elementManager = new ElementManager(new FooModelCache());
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
        elementManager = null;
        fooModel = null;
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event)
    {
        if (event.getType() != IResourceChangeEvent.POST_CHANGE)
            return;
        FooDeltaProcessor deltaProcessor = new FooDeltaProcessor();
        try
        {
            event.getDelta().accept(deltaProcessor);
        }
        catch (CoreException e)
        {
            Activator.log(e.getStatus());
        }
        if (!deltaProcessor.isEmptyDelta())
        {
            fireElementChangeEvent(new ElementChangeEvent(
                ElementChangeEvent.POST_CHANGE, deltaProcessor.getDelta()));
        }
    }

    @Override
    public FooModel getModel()
    {
        if (fooModel == null)
            throw new IllegalStateException();
        return fooModel;
    }

    @Override
    public ElementManager getElementManager()
    {
        if (elementManager == null)
            throw new IllegalStateException();
        return elementManager;
    }

    public IContext getModelContext()
    {
        return EMPTY_CONTEXT;
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

    private FooModelManager()
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
                    FooModelManager.INSTANCE);
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
