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
package org.eclipse.handly.examples.javamodel;

import org.eclipse.core.runtime.CoreException;

/**
 * Represents a type.
 * <p>
 * The children are of type <code>IMember</code> and are listed in the order
 * in which they appear in the source.
 * </p>
 */
public interface IType
    extends IMember
{
    /**
     * Superclass type property.
     * @see #getSuperclassType()
     */
    Property<String> SUPERCLASS_TYPE = new Property<String>("superclassType"); //$NON-NLS-1$

    /**
     * Super interface types property.
     * @see #getSuperInterfaceTypes()
     */
    Property<String[]> SUPER_INTERFACE_TYPES = new Property<String[]>(
        "superInterfaceTypes"); //$NON-NLS-1$

    /**
     * Returns the simple name of this type, unqualified by package or
     * enclosing type. Note that the name of an anonymous type is empty.
     * This is a handle-only method.
     *
     * @return the simple name of this type (never <code>null</code>)
     */
    String getName();

    /**
     * Returns the field with the specified name in this type.
     * <p>
     * This is a handle-only method. The field may or may not exist.
     * </p>
     *
     * @param name the given name (not <code>null</code>)
     * @return the field with the specified name in this type
     *  (never <code>null</code>)
     */
    IField getField(String name);

    /**
     * Returns the fields declared by this type. The results are listed
     * in the order in which they appear in the source.
     *
     * @return the fields declared by this type (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    IField[] getFields() throws CoreException;

    /**
     * Returns the method with the specified name and parameter types
     * in this type (for example, <code>"foo", {"I", "QString;"}</code>).
     * To get the handle for a constructor, the name specified must be the
     * simple name of the enclosing type.
     * <p>
     * This is a handle-only method. The method may or may not exist.
     * </p>
     *
     * @param name the given name (not <code>null</code>)
     * @param parameterTypes the type signatures for the method parameters
     *  (not <code>null</code>)
     * @return the method with the specified name and parameter types
     *  in this type (never <code>null</code>)
     * @see org.eclipse.jdt.core.Signature
     */
    IMethod getMethod(String name, String[] parameterTypes);

    /**
     * Returns the methods and constructors declared by this type.
     * The results are listed in the order in which they appear in the source. 
     *
     * @return the methods and constructors declared by this type
     *  (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    IMethod[] getMethods() throws CoreException;

    /**
     * Returns the member type declared in this type with the given simple name.
     * <p>
     * This is a handle-only method. The type may or may not exist.
     * </p>
     *
     * @param name the given simple name (not <code>null</code>)
     * @return the member type declared in this type with the given simple name
     *  (never <code>null</code>)
     */
    IType getType(String name);

    /**
     * Returns the immediate member types declared by this type.
     * The results are listed in the order in which they appear in the source.
     *
     * @return the immediate member types declared by this type
     *  (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    IType[] getTypes() throws CoreException;

    /**
     * Returns the type signature of this type's superclass,
     * or <code>null</code> if none.
     * 
     * @return the type signature of this type's superclass,
     *  or <code>null</code> if none
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     * @see org.eclipse.jdt.core.Signature
     */
    String getSuperclassType() throws CoreException;

    /**
     * Returns the type signatures of the interfaces that this type
     * implements or extends, in the order in which they are listed in the
     * source.
     *
     * @return the type signatures of the interfaces that this type
     *  implements or extends, in the order in which they are listed in the
     *  source (never <code>null</code>)
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     * @see org.eclipse.jdt.core.Signature
     */
    String[] getSuperInterfaceTypes() throws CoreException;

    /**
     * Returns whether this type represents a class.
     * <p>
     * Note that a class can neither be an interface, an enumeration class,
     * nor an annotation type.
     * </p>
     *
     * @return <code>true</code> if this type represents a class,
     *  <code>false</code> otherwise
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    boolean isClass() throws CoreException;

    /**
     * Returns whether this type represents an enumeration class.
     * <p>
     * Note that an enumeration class can neither be a class, an interface,
     * nor an annotation type.
     * </p>
     *
     * @return <code>true</code> if this type represents an enumeration class,
     *  <code>false</code> otherwise
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    boolean isEnum() throws CoreException;

    /**
     * Returns whether this type represents an interface.
     * <p>
     * Note that an interface can also be an annotation type,
     * but it can neither be a class nor an enumeration class.
     * </p>
     *
     * @return <code>true</code> if this type represents an interface,
     *  <code>false</code> otherwise
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    boolean isInterface() throws CoreException;

    /**
     * Returns whether this type represents an annotation type.
     * <p>
     * Note that an annotation type is also an interface,
     * but it can neither be a class nor an enumeration class.
     * </p>
     *
     * @return <code>true</code> if this type represents an annotation type,
     *  <code>false</code> otherwise
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    boolean isAnnotation() throws CoreException;

    /**
     * Returns whether this type represents a member type.
     *
     * @return <code>true</code> if this type represents a member type,
     *  <code>false</code> otherwise
     * @throws CoreException if this element does not exist or if an exception 
     *  occurs while accessing its corresponding resource
     */
    boolean isMember() throws CoreException;
}
