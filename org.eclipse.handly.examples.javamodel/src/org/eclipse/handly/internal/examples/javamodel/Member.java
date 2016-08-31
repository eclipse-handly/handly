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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.examples.javamodel.IMember;
import org.eclipse.handly.examples.javamodel.IType;
import org.eclipse.handly.model.impl.Element;
import org.eclipse.handly.model.impl.SourceConstruct;

/**
 * Implementation of {@link IMember}.
 */
public abstract class Member
    extends SourceConstruct
    implements IMember, IJavaElementInternal
{
    static final String[] NO_STRINGS = new String[0];

    /**
     * Creates a handle for a member with the given parent element
     * and the given name.
     *
     * @param parent the parent of the element (not <code>null</code>)
     * @param name the name of the element, or <code>null</code>
     *  if the element has no name
     */
    public Member(Element parent, String name)
    {
        super(parent, name);
    }

    @Override
    public IType getDeclaringType()
    {
        if (getParent() instanceof IType)
            return (IType)getParent();
        return null;
    }

    @Override
    public int getFlags() throws CoreException
    {
        return getSourceElementInfo().get(FLAGS);
    }
}
