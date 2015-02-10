/*******************************************************************************
 * Copyright (c) 2015 1C LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel;

import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.handly.examples.javamodel.ICompilationUnit;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.handly.model.impl.HandleManager;
import org.eclipse.handly.model.impl.SourceElementBody;
import org.eclipse.handly.model.impl.SourceFile;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;

/**
 * Implementation of {@link ICompilationUnit}.
 */
public class CompilationUnit
    extends SourceFile
    implements ICompilationUnit
{
    /**
     * Constructs a handle for a Java compilation unit with the given
     * parent element and the given underlying workspace file.
     * 
     * @param parent the parent of the element (not <code>null</code>)
     * @param file the workspace file underlying the element (not <code>null</code>)
     */
    public CompilationUnit(PackageFragment parent, IFile file)
    {
        super(parent, file);
        if (!file.getParent().equals(parent.getResource()))
            throw new IllegalArgumentException();
        if (!Arrays.asList(JavaCore.getJavaLikeExtensions()).contains(
            file.getFileExtension()))
            throw new IllegalArgumentException();
    }

    @Override
    public PackageFragment getParent()
    {
        return (PackageFragment)parent;
    }

    @Override
    public IJavaModel getRoot()
    {
        return (IJavaModel)super.getRoot();
    }

    @Override
    protected HandleManager getHandleManager()
    {
        return JavaModelManager.INSTANCE.getHandleManager();
    }

    @Override
    protected void validateExistence() throws CoreException
    {
        super.validateExistence();

        IStatus status = validateCompilationUnitName();
        if (status.getSeverity() == IStatus.ERROR)
            throw new CoreException(status);
    }

    private IStatus validateCompilationUnitName()
    {
        JavaProject javaProject = getAncestor(JavaProject.class);
        String sourceLevel =
            javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
        String complianceLevel =
            javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);
        return JavaConventions.validateCompilationUnitName(name, sourceLevel,
            complianceLevel);
    }

    @Override
    protected Object createStructuralAst(String source) throws CoreException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void buildStructure(SourceElementBody body,
        Map<IHandle, Body> newElements, Object ast, String source)
    {
        // TODO Auto-generated method stub
    }
}
