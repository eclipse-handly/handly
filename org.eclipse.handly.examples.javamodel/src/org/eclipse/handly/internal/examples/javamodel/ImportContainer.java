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
package org.eclipse.handly.internal.examples.javamodel;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.examples.javamodel.IImportContainer;
import org.eclipse.handly.examples.javamodel.IImportDeclaration;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.handly.model.impl.Handle;
import org.eclipse.handly.model.impl.HandleManager;
import org.eclipse.handly.model.impl.SourceConstruct;
import org.eclipse.handly.util.TextIndent;

/**
 * Implementation of {@link IImportContainer}.
 */
public class ImportContainer
    extends SourceConstruct
    implements IImportContainer
{
    /**
     * Creates a handle for an import container  with the given parent element.
     *
     * @param parent the parent of the element (not <code>null</code>)
     */
    public ImportContainer(CompilationUnit parent)
    {
        super(parent, null);
    }

    @Override
    public CompilationUnit getParent()
    {
        return (CompilationUnit)parent;
    }

    @Override
    public IJavaModel getRoot()
    {
        return (IJavaModel)super.getRoot();
    }

    @Override
    public IImportDeclaration getImport(String name)
    {
        return new ImportDeclaration(this, name);
    }

    @Override
    public IImportDeclaration[] getImports() throws CoreException
    {
        IHandle[] children = getChildren();
        int length = children.length;
        IImportDeclaration[] result = new IImportDeclaration[length];
        System.arraycopy(children, 0, result, 0, length);
        return result;
    }

    @Override
    protected HandleManager getHandleManager()
    {
        return JavaModelManager.INSTANCE.getHandleManager();
    }

    @Override
    protected void toStringName(StringBuilder builder)
    {
        builder.append("<import container>"); //$NON-NLS-1$
    }

    @Override
    protected void toString(TextIndent indent, StringBuilder builder)
    {
        Body body = peekAtBody();
        if (body == null)
            return;
        IHandle[] children = body.getChildren();
        for (int i = 0; i < children.length; i++)
        {
            if (i > 0)
                indent.appendLineSeparatorTo(builder);
            ((Handle)children[i]).toStringBody(indent, builder);
        }
    }
}
