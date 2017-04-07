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
package org.eclipse.handly.model.impl;

import static org.eclipse.handly.context.Contexts.of;
import static org.eclipse.handly.context.Contexts.with;
import static org.eclipse.handly.model.Elements.FORCE_RECONCILING;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.snapshot.NonExpiringSnapshot;

/**
 * Default implementation of working copy info.
 * <p>
 * Clients can use this class as it stands or subclass it
 * as circumstances warrant.
 * </p>
 *
 * @see WorkingCopyInfo
 */
public class DefaultWorkingCopyInfo
    extends WorkingCopyInfo
{
    private final Object reconcilingLock = new Object();
    private volatile ISnapshot reconciledSnapshot;

    /**
     * Constructs a new working copy info and associates it with the given
     * source file and buffer. Does not <code>addRef</code> the given buffer.
     * <p>
     * Clients should explicitly {@link #dispose} the working copy info
     * after it is no longer needed.
     * </p>
     *
     * @param sourceFile the working copy's source file (not <code>null</code>)
     * @param buffer the working copy's buffer (not <code>null</code>)
     */
    public DefaultWorkingCopyInfo(SourceFile sourceFile, IBuffer buffer)
    {
        super(sourceFile, buffer);
    }

    @Override
    protected final boolean needsReconciling()
    {
        return !getBuffer().getSnapshot().isEqualTo(reconciledSnapshot);
    }

    @Override
    protected final void reconcile(IContext context, IProgressMonitor monitor)
        throws CoreException
    {
        if (context.containsKey(SOURCE_AST))
            throw new IllegalArgumentException(); // just to be safe that we don't pass SOURCE_AST to #reconcile0 accidentally

        synchronized (reconcilingLock)
        {
            boolean needsReconciling = needsReconciling();
            if (needsReconciling || context.getOrDefault(FORCE_RECONCILING))
            {
                NonExpiringSnapshot snapshot = new NonExpiringSnapshot(
                    getBuffer());
                reconcile0(with(of(SOURCE_CONTENTS, snapshot.getContents()), of(
                    SOURCE_SNAPSHOT, snapshot.getWrappedSnapshot()), of(
                        RECONCILING_FORCED, !needsReconciling), context),
                    monitor);
                reconciledSnapshot = snapshot.getWrappedSnapshot();
            }
        }
    }
}