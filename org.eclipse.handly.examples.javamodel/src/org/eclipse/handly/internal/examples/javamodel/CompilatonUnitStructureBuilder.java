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
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

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
        List<? extends AbstractTypeDeclaration> types = cu.types();
        for (AbstractTypeDeclaration type : types)
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
        AbstractTypeDeclaration type)
    {
        Type handle = new Type(parent, type.getName().getIdentifier());
        SourceElementBody body = new SourceElementBody();
        body.setFullRange(getTextRange(type));
        body.setIdentifyingRange(getTextRange(type.getName()));
        int flags = type.getModifiers();
        if (type instanceof TypeDeclaration)
        {
            TypeDeclaration typeDeclaration = (TypeDeclaration)type;

            if (typeDeclaration.isInterface())
                flags |= Flags.AccInterface;

            org.eclipse.jdt.core.dom.Type superclassType =
                typeDeclaration.getSuperclassType();
            if (superclassType != null)
                body.set(Type.SUPERCLASS_TYPE,
                    AstUtil.getSignature(superclassType));
            @SuppressWarnings("unchecked")
            List<? extends org.eclipse.jdt.core.dom.Type> superInterfaceTypes =
                typeDeclaration.superInterfaceTypes();
            body.set(Type.SUPER_INTERFACE_TYPES,
                AstUtil.getSignatures(superInterfaceTypes));
        }
        else if (type instanceof EnumDeclaration)
        {
            EnumDeclaration enumDeclaration = (EnumDeclaration)type;

            flags |= Flags.AccEnum;

            @SuppressWarnings("unchecked")
            List<? extends org.eclipse.jdt.core.dom.Type> superInterfaceTypes =
                enumDeclaration.superInterfaceTypes();
            body.set(Type.SUPER_INTERFACE_TYPES,
                AstUtil.getSignatures(superInterfaceTypes));

            @SuppressWarnings("unchecked")
            List<EnumConstantDeclaration> enumConstants =
                enumDeclaration.enumConstants();
            for (EnumConstantDeclaration enumConstant : enumConstants)
                buildStructure(handle, body, enumDeclaration, enumConstant);
        }
        else if (type instanceof AnnotationTypeDeclaration)
        {
            flags |= Flags.AccInterface | Flags.AccAnnotation;
        }
        body.set(Type.FLAGS, flags);
        @SuppressWarnings("unchecked")
        List<? extends BodyDeclaration> bodyDeclarations =
            type.bodyDeclarations();
        for (BodyDeclaration bd : bodyDeclarations)
        {
            if (bd instanceof FieldDeclaration)
                buildStructure(handle, body, (FieldDeclaration)bd);
            else if (bd instanceof MethodDeclaration)
                buildStructure(handle, body, (MethodDeclaration)bd);
            else if (bd instanceof AbstractTypeDeclaration)
                buildStructure(handle, body, (AbstractTypeDeclaration)bd);
            else if (bd instanceof AnnotationTypeMemberDeclaration)
                buildStructure(handle, body,
                    (AnnotationTypeMemberDeclaration)bd);
        }
        addChild(parentBody, handle, body);
        complete(body);
    }

    private void buildStructure(Type parent, Body parentBody,
        FieldDeclaration field)
    {
        @SuppressWarnings("unchecked")
        List<VariableDeclarationFragment> fragments = field.fragments();
        for (VariableDeclarationFragment fragment : fragments)
            buildStructure(parent, parentBody, field, fragment);
    }

    private void buildStructure(Type parent, Body parentBody,
        FieldDeclaration field, VariableDeclarationFragment fragment)
    {
        Field handle = new Field(parent, fragment.getName().getIdentifier());
        SourceElementBody body = new SourceElementBody();
        body.setFullRange(getTextRange(field));
        body.setIdentifyingRange(getTextRange(fragment.getName()));
        body.set(Field.FLAGS, field.getModifiers());
        body.set(
            Field.TYPE,
            Signature.createArraySignature(
                AstUtil.getSignature(field.getType()),
                fragment.getExtraDimensions()));
        addChild(parentBody, handle, body);
        complete(body);
    }

    private void buildStructure(Type parent, Body parentBody,
        EnumDeclaration enumDeclaration, EnumConstantDeclaration enumConstant)
    {
        Field handle =
            new Field(parent, enumConstant.getName().getIdentifier());
        SourceElementBody body = new SourceElementBody();
        body.setFullRange(getTextRange(enumConstant));
        body.setIdentifyingRange(getTextRange(enumConstant.getName()));
        body.set(Field.FLAGS, enumConstant.getModifiers() | Flags.AccEnum);
        body.set(Field.TYPE, Signature.createTypeSignature(
            enumDeclaration.getName().getIdentifier(), false));
        addChild(parentBody, handle, body);
        complete(body);
    }

    private void buildStructure(Type parent, Body parentBody,
        MethodDeclaration method)
    {
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> parameters = method.parameters();
        int numberOfParameters = parameters.size();
        String[] parameterTypes = new String[numberOfParameters];
        String[] parameterNames = new String[numberOfParameters];
        int i = 0;
        for (SingleVariableDeclaration parameter : parameters)
        {
            parameterTypes[i] =
                Signature.createArraySignature(
                    AstUtil.getSignature(parameter.getType()),
                    parameter.getExtraDimensions());
            parameterNames[i] = parameter.getName().getIdentifier();
            i++;
        }

        Method handle =
            new Method(parent, method.getName().getIdentifier(), parameterTypes);
        SourceElementBody body = new SourceElementBody();
        body.setFullRange(getTextRange(method));
        body.setIdentifyingRange(getTextRange(method.getName()));
        body.set(Method.FLAGS, method.getModifiers());
        body.set(Method.PARAMETER_NAMES, parameterNames);
        org.eclipse.jdt.core.dom.Type returnType = method.getReturnType2();
        if (returnType != null)
        {
            body.set(Method.RETURN_TYPE, Signature.createArraySignature(
                AstUtil.getSignature(returnType), method.getExtraDimensions()));
        }
        @SuppressWarnings("unchecked")
        List<? extends Name> thrownExceptions = method.thrownExceptions();
        body.set(Method.EXCEPTION_TYPES,
            AstUtil.toTypeSignatures(thrownExceptions));
        if (method.isConstructor())
            body.set(Method.IS_CONSTRUCTOR, Boolean.TRUE);
        addChild(parentBody, handle, body);
        complete(body);
    }

    private void buildStructure(Type parent, Body parentBody,
        AnnotationTypeMemberDeclaration annotationTypeMember)
    {
        Method handle =
            new Method(parent, annotationTypeMember.getName().getIdentifier(),
                Method.NO_STRINGS);
        SourceElementBody body = new SourceElementBody();
        body.setFullRange(getTextRange(annotationTypeMember));
        body.setIdentifyingRange(getTextRange(annotationTypeMember.getName()));
        body.set(Method.FLAGS, annotationTypeMember.getModifiers());
        body.set(Method.RETURN_TYPE,
            AstUtil.getSignature(annotationTypeMember.getType()));
        addChild(parentBody, handle, body);
        complete(body);
    }

    private static TextRange getTextRange(ASTNode node)
    {
        return new TextRange(node.getStartPosition(), node.getLength());
    }
}
