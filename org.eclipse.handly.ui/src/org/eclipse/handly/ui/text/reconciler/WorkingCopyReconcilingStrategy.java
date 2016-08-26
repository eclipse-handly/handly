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
package org.eclipse.handly.ui.text.reconciler;

import static org.eclipse.handly.context.Contexts.of;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.handly.model.Elements;
import org.eclipse.handly.model.ISourceFile;
import org.eclipse.handly.ui.IWorkingCopyProvider;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;

/**
 * Working copy reconciling strategy.
 */
public class WorkingCopyReconcilingStrategy
    implements IReconcilingStrategy, IReconcilingStrategyExtension
{
    private final IWorkingCopyProvider provider;
    private IProgressMonitor monitor;

    /**
     * Creates a new working copy reconciling strategy
     * with the given working copy provider.
     *
     * @param provider the working copy provider (not <code>null</code>)
     */
    public WorkingCopyReconcilingStrategy(IWorkingCopyProvider provider)
    {
        if (provider == null)
            throw new IllegalArgumentException();
        this.provider = provider;
    }

    @Override
    public void setProgressMonitor(IProgressMonitor monitor)
    {
        this.monitor = monitor;
    }

    @Override
    public final void initialReconcile()
    {
        reconcile(true);
    }

    @Override
    public void setDocument(IDocument document)
    {
    }

    @Override
    public final void reconcile(DirtyRegion dirtyRegion, IRegion subRegion)
    {
        reconcile(false);
    }

    @Override
    public final void reconcile(IRegion partition)
    {
        reconcile(false);
    }

    /**
     * Reconciles the given working copy.
     *
     * @param workingCopy the given working copy (never <code>null</code>)
     * @param initialReconcile <code>true</code> if this is the initial reconcile
     * @throws CoreException if the given working copy cannot be reconciled
     * @see #initialReconcile()
     */
    protected void reconcile(ISourceFile workingCopy, boolean initialReconcile)
        throws CoreException
    {
        Elements.reconcile(workingCopy, of(Elements.FORCE_RECONCILING, true),
            getProgressMonitor());
    }

    /**
     * @return the progress monitor set for this strategy,
     *  or <code>null</code> if none
     */
    protected final IProgressMonitor getProgressMonitor()
    {
        return monitor;
    }

    private void reconcile(final boolean initialReconcile)
    {
        final ISourceFile workingCopy = provider.getWorkingCopy();
        if (workingCopy != null)
        {
            SafeRunner.run(new ISafeRunnable()
            {
                public void run() throws Exception
                {
                    reconcile(workingCopy, initialReconcile);
                }

                public void handleException(Throwable exception)
                {
                    // already logged by Platform
                }
            });
        }
    }
}
