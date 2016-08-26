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

import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.model.impl.DefaultWorkingCopyInfo;
import org.eclipse.jdt.core.IProblemRequestor;

/**
 * Model-specific extension of {@link DefaultWorkingCopyInfo}.
 */
public class JavaWorkingCopyInfo
    extends DefaultWorkingCopyInfo
{
    final IProblemRequestor problemRequestor;

    public JavaWorkingCopyInfo(IBuffer buffer,
        IProblemRequestor problemRequestor)
    {
        super(buffer);
        this.problemRequestor = problemRequestor;
    }
}
