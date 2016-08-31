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

import static org.eclipse.handly.context.Contexts.of;
import static org.eclipse.handly.context.Contexts.with;
import static org.eclipse.handly.util.ToStringOptions.FORMAT_STYLE;
import static org.eclipse.handly.util.ToStringOptions.FormatStyle.FULL;
import static org.eclipse.handly.util.ToStringOptions.FormatStyle.LONG;
import static org.eclipse.handly.util.ToStringOptions.FormatStyle.SHORT;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.examples.javamodel.IImportContainer;
import org.eclipse.handly.examples.javamodel.IImportDeclaration;
import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.impl.SourceConstruct;
import org.eclipse.handly.util.ToStringOptions.FormatStyle;

/**
 * Implementation of {@link IImportContainer}.
 */
public class ImportContainer
    extends SourceConstruct
    implements IImportContainer, IJavaElementInternal
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
    public IImportDeclaration getImport(String name)
    {
        return new ImportDeclaration(this, name);
    }

    @Override
    public IImportDeclaration[] getImports() throws CoreException
    {
        IElement[] children = getChildren();
        int length = children.length;
        IImportDeclaration[] result = new IImportDeclaration[length];
        System.arraycopy(children, 0, result, 0, length);
        return result;
    }

    @Override
    public String hToString(IContext context)
    {
        FormatStyle style = context.getOrDefault(FORMAT_STYLE);
        if (style == FULL || style == LONG)
        {
            StringBuilder builder = new StringBuilder();
            hToStringChildren(builder, hPeekAtBody(), with(of(FORMAT_STYLE,
                SHORT), context));
            return builder.toString();
        }
        return super.hToString(context);
    }

    @Override
    protected void hToStringName(StringBuilder builder, IContext context)
    {
        builder.append("<import container>"); //$NON-NLS-1$
    }
}
