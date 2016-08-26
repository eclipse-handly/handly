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

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.handly.examples.basic.foo.Def;
import org.eclipse.handly.examples.basic.foo.Module;
import org.eclipse.handly.examples.basic.foo.Var;
import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.handly.model.impl.SourceElementBody;
import org.eclipse.handly.model.impl.StructureHelper;
import org.eclipse.handly.util.TextRange;
import org.eclipse.xtext.resource.ILocationInFileProvider;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.util.ITextRegion;

/**
 * Builds the inner structure for a {@link FooFile}.
 */
class FooFileStructureBuilder
{
    private final StructureHelper helper;
    private final ILocationInFileProvider locationProvider;

    /**
     * Constructs a new Foo file structure builder.
     *
     * @param newElements the map to populate with structure elements
     *  (not <code>null</code>)
     * @param resourceServiceProvider Xtext's {@link IResourceServiceProvider}
     *  for the language (not <code>null</code>)
     */
    FooFileStructureBuilder(Map<IElement, Object> newElements,
        IResourceServiceProvider resourceServiceProvider)
    {
        this.helper = new StructureHelper(newElements);
        if (resourceServiceProvider == null)
            throw new IllegalArgumentException();
        this.locationProvider = resourceServiceProvider.get(
            ILocationInFileProvider.class);
    }

    /**
     * Builds the structure for the given {@link FooFile} based on
     * its {@link Module AST}.
     *
     * @param handle the handle to a Foo file (not <code>null</code>)
     * @param body the body of the Foo file (not <code>null</code>)
     * @param module the AST of the Foo file (not <code>null</code>)
     * @param monitor a progress monitor (not <code>null</code>)
     * @throws OperationCanceledException if this method is canceled
     */
    void buildStructure(FooFile handle, SourceElementBody body, Module module,
        IProgressMonitor monitor)
    {
        monitor.beginTask("", //$NON-NLS-1$
            module.getVars().size() + module.getDefs().size());
        try
        {
            for (Var var : module.getVars())
            {
                if (monitor.isCanceled())
                    throw new OperationCanceledException();
                buildStructure(handle, body, var);
                monitor.worked(1);
            }
            for (Def def : module.getDefs())
            {
                if (monitor.isCanceled())
                    throw new OperationCanceledException();
                buildStructure(handle, body, def);
                monitor.worked(1);
            }
            helper.complete(body);
        }
        finally
        {
            monitor.done();
        }
    }

    private void buildStructure(FooFile parent, Body parentBody, Var var)
    {
        if (var.getName() == null || var.getName().isEmpty())
            return;

        FooVar handle = new FooVar(parent, var.getName());
        SourceElementBody body = new SourceElementBody();
        body.setFullRange(getFullRange(var));
        body.setIdentifyingRange(getIdentifyingRange(var));
        helper.addChild(parentBody, handle, body);
    }

    private void buildStructure(FooFile parent, Body parentBody, Def def)
    {
        if (def.getName() == null || def.getName().isEmpty())
            return;

        int arity = def.getParams().size();
        FooDef handle = new FooDef(parent, def.getName(), arity);
        SourceElementBody body = new SourceElementBody();
        body.setFullRange(getFullRange(def));
        body.setIdentifyingRange(getIdentifyingRange(def));
        body.set(FooDef.PARAMETER_NAMES, def.getParams().toArray(
            new String[arity]));
        helper.addChild(parentBody, handle, body);
    }

    private TextRange getFullRange(EObject eObject)
    {
        return toTextRange(locationProvider.getFullTextRegion(eObject));
    }

    private TextRange getIdentifyingRange(EObject eObject)
    {
        return toTextRange(locationProvider.getSignificantTextRegion(eObject));
    }

    private static TextRange toTextRange(ITextRegion region)
    {
        if (region == null || region.equals(ITextRegion.EMPTY_REGION))
            return null;
        else
            return new TextRange(region.getOffset(), region.getLength());
    }
}
