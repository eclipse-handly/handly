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

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.examples.javamodel.ICompilationUnit;
import org.eclipse.handly.examples.javamodel.IImportContainer;
import org.eclipse.handly.examples.javamodel.IImportDeclaration;
import org.eclipse.handly.examples.javamodel.IJavaSourceElement;
import org.eclipse.handly.examples.javamodel.IPackageDeclaration;
import org.eclipse.handly.examples.javamodel.IType;
import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.impl.ElementChangeEvent;
import org.eclipse.handly.model.impl.ElementDifferencer;
import org.eclipse.handly.model.impl.SourceElementBody;
import org.eclipse.handly.model.impl.SourceFile;
import org.eclipse.handly.model.impl.WorkingCopyInfo;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.snapshot.NonExpiringSnapshot;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

/**
 * Implementation of {@link ICompilationUnit}.
 */
public class CompilationUnit
    extends SourceFile
    implements ICompilationUnit, IJavaElementInternal
{
    @SuppressWarnings("restriction")
    private static final WorkingCopyOwner PRIMARY_OWNER =
        org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner.PRIMARY;
    private static final IImportDeclaration[] NO_IMPORTS =
        new IImportDeclaration[0];

    private final WorkingCopyOwner owner;

    /**
     * Constructs a handle for a Java compilation unit with the given
     * parent element and the given underlying workspace file.
     *
     * @param parent the parent of the element (not <code>null</code>)
     * @param file the workspace file underlying the element (not <code>null</code>)
     * @param owner the working copy owner, or <code>null</code> if the primary
     *  owner should be used
     */
    public CompilationUnit(PackageFragment parent, IFile file,
        WorkingCopyOwner owner)
    {
        super(parent, file);
        if (!file.getParent().equals(parent.getResource()))
            throw new IllegalArgumentException();
        if (!"java".equals(file.getFileExtension())) //$NON-NLS-1$
            throw new IllegalArgumentException();
        if (owner == null)
            owner = PRIMARY_OWNER;
        this.owner = owner;
    }

    @Override
    public PackageFragment getParent()
    {
        return (PackageFragment)hParent();
    }

    @Override
    public IFile getFile()
    {
        return hFile();
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
    public IJavaSourceElement getElementAt(int position, ISnapshot base)
        throws CoreException
    {
        return (IJavaSourceElement)hSourceElementAt(position, base);
    }

    @Override
    public boolean isWorkingCopy()
    {
        return hIsWorkingCopy();
    }

    @Override
    public org.eclipse.jdt.core.dom.CompilationUnit reconcile(int astLevel,
        int reconcileFlags, IProgressMonitor monitor) throws CoreException
    {
        boolean force = (reconcileFlags & FORCE_PROBLEM_DETECTION) != 0;
        ReconcileInfo info = new ReconcileInfo(astLevel, reconcileFlags);
        hReconcile(force, info, monitor);
        return info.getAst();
    }

    @Override
    public IBuffer getBuffer() throws CoreException
    {
        return hBuffer();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof CompilationUnit))
            return false;
        CompilationUnit other = (CompilationUnit)obj;
        return owner.equals(other.owner) && super.equals(obj);
    }

    @Override
    public ReconcileOperation hReconcileOperation()
    {
        return new NotifyingReconcileOperation();
    }

    @Override
    protected void hValidateExistence() throws CoreException
    {
        super.hValidateExistence();

        IStatus status = validateCompilationUnitName();
        if (status.getSeverity() == IStatus.ERROR)
            throw new CoreException(status);
    }

    IStatus validateCompilationUnitName()
    {
        JavaProject javaProject = getAncestor(JavaProject.class);
        String sourceLevel = javaProject.getOption(JavaCore.COMPILER_SOURCE,
            true);
        String complianceLevel = javaProject.getOption(
            JavaCore.COMPILER_COMPLIANCE, true);
        return JavaConventions.validateCompilationUnitName(getElementName(),
            sourceLevel, complianceLevel);
    }

    @Override
    protected Object hCreateStructuralAst(String source,
        IProgressMonitor monitor) throws CoreException
    {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(source.toCharArray());
        parser.setUnitName(getPath().toString());
        parser.setProject(JavaCore.create(getResource().getProject()));
        parser.setFocalPosition(0); // reduced AST
        return parser.createAST(monitor);
    }

    org.eclipse.jdt.core.dom.CompilationUnit createAst(String source,
        int astLevel, boolean resolveBindings, boolean enableStatementsRecovery,
        boolean enableBindingsRecovery, boolean ignoreMethodBodies,
        IProgressMonitor monitor) throws CoreException
    {
        ASTParser parser = ASTParser.newParser(astLevel);
        parser.setSource(source.toCharArray());
        parser.setUnitName(getPath().toString());
        parser.setProject(JavaCore.create(getResource().getProject()));
        parser.setResolveBindings(resolveBindings);
        parser.setStatementsRecovery(enableStatementsRecovery);
        parser.setBindingsRecovery(enableBindingsRecovery);
        parser.setIgnoreMethodBodies(ignoreMethodBodies);
        return (org.eclipse.jdt.core.dom.CompilationUnit)parser.createAST(
            monitor);
    }

    @Override
    protected void hBuildStructure(SourceElementBody body,
        Map<IElement, Object> newElements, Object ast, String source,
        IProgressMonitor monitor)
    {
        CompilatonUnitStructureBuilder builder =
            new CompilatonUnitStructureBuilder(newElements);
        builder.buildStructure(this, body,
            (org.eclipse.jdt.core.dom.CompilationUnit)ast);
    }

    @Override
    protected void hWorkingCopyModeChanged()
    {
        super.hWorkingCopyModeChanged();

        JavaElementDelta.Builder builder = new JavaElementDelta.Builder(
            new JavaElementDelta(getJavaModel()));
        if (getFile().exists())
            builder.changed(this, JavaElementDelta.F_WORKING_COPY);
        else if (isWorkingCopy())
            builder.added(this, JavaElementDelta.F_WORKING_COPY);
        else
            builder.removed(this, JavaElementDelta.F_WORKING_COPY);
        JavaModelManager.INSTANCE.fireElementChangeEvent(new ElementChangeEvent(
            ElementChangeEvent.POST_CHANGE, builder.getDelta()));
    }

    private class NotifyingReconcileOperation
        extends ReconcileOperation
    {
        @Override
        public void reconcile(Object ast, NonExpiringSnapshot snapshot,
            boolean forced, IProgressMonitor monitor) throws CoreException
        {
            ElementDifferencer differ = new ElementDifferencer(
                new JavaElementDelta.Builder(new JavaElementDelta(
                    CompilationUnit.this)));

            super.reconcile(ast, snapshot, forced, monitor);

            reportProblems(
                ((org.eclipse.jdt.core.dom.CompilationUnit)ast).getProblems());

            differ.buildDelta();

            if (!differ.isEmptyDelta())
            {
                JavaModelManager.INSTANCE.fireElementChangeEvent(
                    new ElementChangeEvent(ElementChangeEvent.POST_RECONCILE,
                        differ.getDelta()));
            }
        }

        private void reportProblems(IProblem[] problems)
        {
            if (problems == null || problems.length == 0)
                return;
            WorkingCopyInfo info = hPeekAtWorkingCopyInfo();
            if (info instanceof JavaWorkingCopyInfo)
            {
                reportProblems(((JavaWorkingCopyInfo)info).problemRequestor,
                    problems);
            }
        }

        private void reportProblems(IProblemRequestor requestor,
            IProblem[] problems)
        {
            if (requestor == null || !requestor.isActive())
                return;
            try
            {
                requestor.beginReporting();
                for (IProblem problem : problems)
                {
                    requestor.acceptProblem(problem);
                }
            }
            finally
            {
                requestor.endReporting();
            }
        }
    }
}
