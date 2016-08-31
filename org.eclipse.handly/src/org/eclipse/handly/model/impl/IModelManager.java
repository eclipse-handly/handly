/*******************************************************************************
 * Copyright (c) 2016 1C-Soft LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.model.impl;

import org.eclipse.handly.model.IModel;

/**
 * The central point for an {@link Element} to access information and services
 * related to the model as a whole.
 * <p>
 * An instance of the model manager is safe for use by multiple threads.
 * </p>
 */
public interface IModelManager
{
    /**
     * Returns the managed model.
     *
     * @return the managed model (never <code>null</code>)
     * @throws IllegalStateException if the model is not accessible
     */
    IModel getModel();

    /**
     * Returns the element manager that is to be shared between all elements of
     * the model. Typical implementations would answer a model-specific singleton.
     *
     * @return the element manager (never <code>null</code>)
     * @throws IllegalStateException if the element manager is not accessible
     */
    ElementManager getElementManager();

    /**
     * Provides access to the model manager. This interface is implemented
     * by all {@link Element}s.
     * <p>
     * The same manager instance must be returned each time the provider
     * is invoked.
     * </p>
     * <p>
     * An instance of the provider is safe for use by multiple threads.
     * </p>
     */
    interface Provider
    {
        /**
         * Returns the model manager.
         *
         * @return the model manager (never <code>null</code>)
         */
        IModelManager hModelManager();
    }
}
