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

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.handly.examples.javamodel.ICompilationUnit;
import org.eclipse.handly.examples.javamodel.IImportContainer;
import org.eclipse.handly.examples.javamodel.IImportDeclaration;
import org.eclipse.handly.examples.javamodel.IJavaModel;
import org.eclipse.handly.examples.javamodel.IPackageDeclaration;
import org.eclipse.handly.examples.javamodel.IType;
import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.handly.model.impl.ElementChangeEvent;
import org.eclipse.handly.model.impl.HandleManager;
import org.eclipse.handly.model.impl.SourceElementBody;
import org.eclipse.handly.model.impl.SourceFile;
import org.eclipse.handly.snapshot.NonExpiringSnapshot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

/**
 * Implementation of {@link ICompilationUnit}.
 */
public class CompilationUnit
    extends SourceFile
    implements ICompilationUnit
{
    private static final IImportDeclaration[] NO_IMPORTS =
        new IImportDeclaration[0];

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
        if (!"java".equals(file.getFileExtension())) //$NON-NLS-1$
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
    public IImportDeclaration getImport(String name)
    {
        return getImportContainer().getImport(name);
    }

    @Override
    public IImportContainer getImportContainer()
    {
        return new ImportContainer(this);
    }

    @Override
    public IImportDeclaration[] getImports() throws CoreException
    {
        IImportContainer container = getImportContainer();
        if (container.exists())
            return container.getImports();
        return NO_IMPORTS;
    }

    @Override
    public IPackageDeclaration getPackageDeclaration(String name)
    {
        return new PackageDeclaration(this, name);
    }

    @Override
    public IPackageDeclaration[] getPackageDeclarations() throws CoreException
    {
        return getChildren(IPackageDeclaration.class);
    }

    @Override
    public IType getType(String name)
    {
        return new Type(this, name);
    }

    @Override
    public IType[] getTypes() throws CoreException
    {
        return getChildren(IType.class);
    }

    @Override
    public ReconcileOperation getReconcileOperation()
    {
        return new NotifyingReconcileOperation();
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

    IStatus validateCompilationUnitName()
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
        ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setSource(source.toCharArray());
        parser.setFocalPosition(0); // reduced AST
        JavaProject javaProject = getAncestor(JavaProject.class);
        Map<String, String> options = javaProject.getOptions(true);
        parser.setCompilerOptions(options);
        return parser.createAST(null);
    }

    @Override
    protected void buildStructure(SourceElementBody body,
        Map<IHandle, Body> newElements, Object ast, String source)
    {
        CompilatonUnitStructureBuilder builder =
            new CompilatonUnitStructureBuilder(newElements);
        builder.buildStructure(this, body,
            (org.eclipse.jdt.core.dom.CompilationUnit)ast);
    }

    @Override
    protected void workingCopyModeChanged()
    {
        super.workingCopyModeChanged();

        JavaElementDelta delta = new JavaElementDelta(getRoot());
        if (file.exists())
            delta.insertChanged(this, JavaElementDelta.F_WORKING_COPY);
        else
            delta.insertAdded(this, JavaElementDelta.F_WORKING_COPY);
        JavaModelManager.INSTANCE.fireElementChangeEvent(new ElementChangeEvent(
            ElementChangeEvent.POST_CHANGE, delta));
    }

    private class NotifyingReconcileOperation
        extends ReconcileOperation
    {
        @Override
        public void reconcile(Object ast, NonExpiringSnapshot snapshot,
            boolean forced) throws CoreException
        {
            JavaElementDeltaBuilder deltaBuilder =
                new JavaElementDeltaBuilder(CompilationUnit.this);

            super.reconcile(ast, snapshot, forced);

            deltaBuilder.buildDelta();

            JavaElementDelta delta = deltaBuilder.getDelta();
            if (!delta.isEmpty())
            {
                JavaModelManager.INSTANCE.fireElementChangeEvent(new ElementChangeEvent(
                    ElementChangeEvent.POST_RECONCILE, delta));
            }
        }
    }
}
