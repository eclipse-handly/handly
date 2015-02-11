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

import java.util.List;
import java.util.Map;

import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.impl.Body;
import org.eclipse.handly.model.impl.Handle;
import org.eclipse.handly.model.impl.SourceElementBody;
import org.eclipse.handly.model.impl.StructureHelper;
import org.eclipse.handly.util.TextRange;
import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Builds the inner structure for a compilation unit.
 */
class CompilatonUnitStructureBuilder
    extends StructureHelper
{
    /**
     * Constructs a new compilation unit structure builder.
     * 
     * @param newElements the map to populate with structure elements 
     *  (not <code>null</code>)
     */
    CompilatonUnitStructureBuilder(Map<IHandle, Body> newElements)
    {
        super(newElements);
    }

    /**
     * Builds the structure for the given compilation unit based on its AST.
     *
     * @param handle the handle to a compilation unit (not <code>null</code>)
     * @param body the body of the compilation unit (not <code>null</code>)
     * @param cu the AST of the compilation unit (not <code>null</code>)
     */
    void buildStructure(CompilationUnit handle, SourceElementBody body,
        org.eclipse.jdt.core.dom.CompilationUnit cu)
    {
        org.eclipse.jdt.core.dom.PackageDeclaration pkg = cu.getPackage();
        if (pkg != null)
            buildStructure(handle, body, pkg);

        @SuppressWarnings("unchecked")
        List<org.eclipse.jdt.core.dom.ImportDeclaration> imports = cu.imports();
        if (!imports.isEmpty())
            buildStructure(handle, body, imports);

        @SuppressWarnings("unchecked")
        List<org.eclipse.jdt.core.dom.AbstractTypeDeclaration> types =
            cu.types();
        for (org.eclipse.jdt.core.dom.AbstractTypeDeclaration type : types)
            buildStructure(handle, body, type);

        complete(body);
    }

    private void buildStructure(CompilationUnit parent, Body parentBody,
        org.eclipse.jdt.core.dom.PackageDeclaration pkg)
    {
        PackageDeclaration handle =
            new PackageDeclaration(parent,
                pkg.getName().getFullyQualifiedName());
        SourceElementBody body = new SourceElementBody();
        body.setFullRange(getTextRange(pkg));
        body.setIdentifyingRange(getTextRange(pkg.getName()));
        addChild(parentBody, handle, body);
        complete(body);
    }

    private void buildStructure(CompilationUnit parent, Body parentBody,
        List<org.eclipse.jdt.core.dom.ImportDeclaration> imports)
    {
        ImportContainer handle = new ImportContainer(parent);
        SourceElementBody body = new SourceElementBody();
        org.eclipse.jdt.core.dom.ImportDeclaration firstImport = imports.get(0);
        org.eclipse.jdt.core.dom.ImportDeclaration lastImport =
            imports.get(imports.size() - 1);
        body.setFullRange(new TextRange(firstImport.getStartPosition(),
            lastImport.getStartPosition() + lastImport.getLength()
                - firstImport.getStartPosition()));
        addChild(parentBody, handle, body);
        for (org.eclipse.jdt.core.dom.ImportDeclaration importDecl : imports)
            buildStructure(handle, body, importDecl);
        complete(body);
    }

    private void buildStructure(ImportContainer parent, Body parentBody,
        org.eclipse.jdt.core.dom.ImportDeclaration importDecl)
    {
        String name = importDecl.getName().getFullyQualifiedName();
        if (importDecl.isOnDemand())
            name += ".*"; //$NON-NLS-1$
        ImportDeclaration handle = new ImportDeclaration(parent, name);
        SourceElementBody body = new SourceElementBody();
        body.setFullRange(getTextRange(importDecl));
        body.setIdentifyingRange(getTextRange(importDecl.getName()));
        addChild(parentBody, handle, body);
        complete(body);
    }

    private void buildStructure(Handle parent, Body parentBody,
        org.eclipse.jdt.core.dom.AbstractTypeDeclaration type)
    {
        Type handle = new Type(parent, type.getName().getIdentifier());
        SourceElementBody body = new SourceElementBody();
        body.setFullRange(getTextRange(type));
        body.setIdentifyingRange(getTextRange(type.getName()));
        addChild(parentBody, handle, body);
        complete(body);
    }

    private static TextRange getTextRange(ASTNode node)
    {
        return new TextRange(node.getStartPosition(), node.getLength());
    }
}
