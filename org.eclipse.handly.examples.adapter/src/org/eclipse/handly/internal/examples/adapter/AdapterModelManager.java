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
package org.eclipse.handly.internal.examples.adapter;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.handly.model.IElementChangeEvent;
import org.eclipse.handly.model.IElementChangeListener;
import org.eclipse.handly.model.IHandleDelta;
import org.eclipse.handly.model.impl.ElementChangeEvent;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.JavaCore;

/**
 * The manager for the adapter model. Maintains a listener list and converts
 * JDT element change events to corresponding notifications in terms of the
 * adapter model.
 *
 * @threadsafe This class is intended to be thread-safe
 */
public class AdapterModelManager
    implements IElementChangedListener
{
    /**
     * The sole instance of the manager.
     */
    public static final AdapterModelManager INSTANCE =
        new AdapterModelManager();

    private ListenerList listenerList;

    public void startup() throws Exception
    {
        try
        {
            listenerList = new ListenerList();
            JavaCore.addElementChangedListener(this);
        }
        catch (Exception e)
        {
            shutdown();
            throw e;
        }
    }

    public void shutdown() throws Exception
    {
        JavaCore.removeElementChangedListener(this);
        listenerList = null;
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

    @Override
    public void elementChanged(ElementChangedEvent event)
    {
        fireElementChangeEvent(convert(event));
    }

    private static IElementChangeEvent convert(ElementChangedEvent event)
    {
        int type = event.getType();
        int convertedType = 0;
        if ((type & ElementChangedEvent.POST_CHANGE) != 0)
            convertedType |= IElementChangeEvent.POST_CHANGE;
        if ((type & ElementChangedEvent.POST_RECONCILE) != 0)
            convertedType |= IElementChangeEvent.POST_RECONCILE;
        IHandleDelta convertedDelta = new JavaHandleDelta(event.getDelta());
        return new ElementChangeEvent(convertedType, convertedDelta);
    }

    private AdapterModelManager()
    {
    }
}
