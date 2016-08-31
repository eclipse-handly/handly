/*******************************************************************************
 * Copyright (c) 2016 1C-Soft LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.model;

import org.eclipse.handly.context.IContext;

/**
 * A model serves as the common owner of any number of elements.
 * <p>
 * Each element belongs to a model, called the owning model. The children of an
 * element always have the same {@link Elements#getModel(IElement) owner} as
 * their parent element.
 * </p>
 * <p>
 * There can be any number of elements owned by a model that are {@link
 * Elements#getParent(IElement) unparented}. Each of these elements is the root
 * of a separate tree of elements. The method {@link Elements#getRoot(IElement)}
 * navigates from any element to the root of the tree that it is contained in.
 * One can navigate from any element to its model, but not conversely.
 * </p>
 * <p>
 * An instance of the model is safe for use by multiple threads.
 * </p>
 *
 * @see IElement
 */
public interface IModel
{
    /**
     * Returns a context which provides information and services pertaining
     * to the model as a whole. The context, as a set of bindings, is immutable.
     * <p>
     * Note that the relationship between a model and its context does not
     * change over the lifetime of a model.
     * </p>
     *
     * @param model not <code>null</code>
     * @return the model's context (never <code>null</code>)
     * @throws IllegalStateException if the model is no longer accessible
     */
    IContext getModelContext();

    /**
     * Returns the API level supported by this model.
     * <p>
     * Implementations are encouraged to return {@link ApiLevel#CURRENT}, which
     * corresponds to the version of Handly the model was built against.
     * </p>
     *
     * @return the API level; one of the level constants declared on
     *  {@link ApiLevel}; assume this set is open-ended
     */
    int getApiLevel();

    /**
     * Declares API level constants.
     */
    class ApiLevel
    {
        /**
         * Corresponds to Handly 1.0 Release.
         */
        public static final int _1_0 = 0;
        /**
         * Corresponds to the version of Handly the code using this constant
         * was built against.
         */
        public static final int CURRENT = _1_0;

        private ApiLevel()
        {
        }
    }
}
