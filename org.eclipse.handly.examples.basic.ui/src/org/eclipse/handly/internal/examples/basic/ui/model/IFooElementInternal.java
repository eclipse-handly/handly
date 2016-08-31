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
package org.eclipse.handly.internal.examples.basic.ui.model;

import org.eclipse.handly.model.impl.IModelManager;

/**
 * A "mix-in" interface that all Foo elements implement.
 */
interface IFooElementInternal
    extends IModelManager.Provider
{
    @Override
    default IModelManager hModelManager()
    {
        return FooModelManager.INSTANCE;
    }
}
