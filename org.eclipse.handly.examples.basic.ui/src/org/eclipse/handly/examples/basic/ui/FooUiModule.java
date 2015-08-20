/*******************************************************************************
 * Copyright (c) 2014, 2015 1C-Soft LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.examples.basic.ui;

import org.eclipse.handly.examples.basic.ui.internal.FooActivator;
import org.eclipse.handly.internal.examples.basic.ui.FooInputElementProvider;
import org.eclipse.handly.internal.examples.basic.ui.outline2.FooOutlinePage;
import org.eclipse.handly.internal.examples.basic.ui.outline2.FooOutlinePopup;
import org.eclipse.handly.ui.IInputElementProvider;
import org.eclipse.handly.ui.quickoutline.OutlinePopup;
import org.eclipse.handly.xtext.ui.editor.HandlyDirtyStateEditorSupport;
import org.eclipse.handly.xtext.ui.editor.HandlyXtextDocument;
import org.eclipse.handly.xtext.ui.editor.HandlyXtextEditorCallback;
import org.eclipse.handly.xtext.ui.editor.HandlyXtextReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.xtext.ui.editor.DirtyStateEditorSupport;
import org.eclipse.xtext.ui.editor.IXtextEditorCallback;
import org.eclipse.xtext.ui.editor.model.XtextDocument;

import com.google.inject.Binder;
import com.google.inject.name.Names;

/**
 * Use this class to register components to be used within the IDE.
 * <p>
 * Note: Xtext-generated {@link FooActivator} implementation assumes
 * that this class lives in this package. Don't rename/move this class.
 */
public class FooUiModule
    extends AbstractFooUiModule
{
    public FooUiModule(AbstractUIPlugin plugin)
    {
        super(plugin);
    }

    public Class<? extends IInputElementProvider> bindIInputElementProvider()
    {
        return FooInputElementProvider.class;
    }

    @Override
    public Class<? extends IContentOutlinePage> bindIContentOutlinePage()
    {
        return FooOutlinePage.class;
    }

    public Class<? extends OutlinePopup> bindOutlinePopup()
    {
        return FooOutlinePopup.class;
    }

// the following bindings are required for Handly/Xtext integration:

    @Override
    public Class<? extends IReconciler> bindIReconciler()
    {
        return HandlyXtextReconciler.class;
    }

    public Class<? extends XtextDocument> bindXtextDocument()
    {
        return HandlyXtextDocument.class;
    }

    public Class<? extends DirtyStateEditorSupport> bindDirtyStateEditorSupport()
    {
        return HandlyDirtyStateEditorSupport.class;
    }

    public void configureXtextEditorCallback(Binder binder)
    {
        binder.bind(IXtextEditorCallback.class).annotatedWith(Names.named(
            HandlyXtextEditorCallback.class.getName())).to(
                HandlyXtextEditorCallback.class);
    }
}
