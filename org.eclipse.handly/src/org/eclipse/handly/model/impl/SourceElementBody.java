/*******************************************************************************
 * Copyright (c) 2014, 2015 1C LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.model.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.handly.model.IHandle;
import org.eclipse.handly.model.ISourceConstruct;
import org.eclipse.handly.model.ISourceElement;
import org.eclipse.handly.model.ISourceElementInfo;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.util.TextRange;

/**
 * Holds cached structure and properties for a source element. 
 * Can be subclassed for specific kinds of elements.
 * 
 * @see ISourceElementInfo
 */
public class SourceElementBody
    extends Body
    implements ISourceElementInfo
{
    private static final Property[] NO_PROPERTIES = new Property[0];

    private ISnapshot snapshot;
    private Property[] properties = NO_PROPERTIES;
    private TextRange fullRange = TextRange.NULL_RANGE;
    private TextRange identifyingRange = TextRange.NULL_RANGE;

    @Override
    public ISourceConstruct[] getChildren()
    {
        IHandle[] children = super.getChildren();
        int len = children.length;
        ISourceConstruct[] result = new ISourceConstruct[len];
        for (int i = 0; i < len; i++)
        {
            IHandle child = children[i];
            if (!(child instanceof ISourceConstruct))
                throw new AssertionError();
            result[i] = (ISourceConstruct)child;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(ISourceElement.Property<T> p)
    {
        return (T)getPropertyValue(p.getName());
    }

    @Override
    public ISnapshot getSnapshot()
    {
        return snapshot;
    }

    @Override
    public TextRange getFullRange()
    {
        return fullRange;
    }

    @Override
    public TextRange getIdentifyingRange()
    {
        return identifyingRange;
    }

    /**
     * Sets the value of the given property according to the {@link 
     * #getSnapshot() snapshot}.
     *
     * @param p a source element's property (not <code>null</code>)
     * @param value the value of the given property (may be <code>null</code>)
     */
    public <T> void set(ISourceElement.Property<T> p, T value)
    {
        String name = p.getName();
        int len = properties.length;
        if (len == 0)
            properties = new Property[] { new Property(name, value) };
        else
        {
            for (int i = 0; i < len; i++)
            {
                if (properties[i].name.equals(name))
                {
                    properties[i].value = value;
                    return;
                }
            }
            Property[] newProperties = new Property[len + 1];
            System.arraycopy(properties, 0, newProperties, 0, len);
            newProperties[len] = new Property(name, value);
            properties = newProperties;
        }
    }

    /**
     * Sets the source snapshot on which this object is based.
     *
     * @param snapshot not <code>null</code>
     */
    public void setSnapshot(ISnapshot snapshot)
    {
        if (snapshot == null)
            throw new IllegalArgumentException();
        this.snapshot = snapshot;
    }

    /**
     * Sets the text range of the source element in the {@link #getSnapshot() 
     * snapshot}.
     *
     * @param fullRange not <code>null</code>
     */
    public void setFullRange(TextRange fullRange)
    {
        if (fullRange == null)
            throw new IllegalArgumentException();
        this.fullRange = fullRange;
    }

    /**
     * Sets the text range of the source element's identifier in the {@link 
     * #getSnapshot() snapshot}. 
     *
     * @param identifyingRange not <code>null</code>
     */
    public void setIdentifyingRange(TextRange identifyingRange)
    {
        if (identifyingRange == null)
            throw new IllegalArgumentException();
        this.identifyingRange = identifyingRange;
    }

    @Override
    public void findContentChange(Body oldBody, IHandle element,
        HandleDelta delta)
    {
        Set<String> newPropertyNames = getPropertyNames();
        Set<String> oldPropertyNames =
            ((SourceElementBody)oldBody).getPropertyNames();
        Set<String> combinedPropertyNames =
            new HashSet<String>(newPropertyNames.size()
                + oldPropertyNames.size());
        combinedPropertyNames.addAll(newPropertyNames);
        combinedPropertyNames.addAll(oldPropertyNames);
        for (String propertyName : combinedPropertyNames)
        {
            Object newValue = getPropertyValue(propertyName);
            Object oldValue =
                ((SourceElementBody)oldBody).getPropertyValue(propertyName);
            if (isPropertyChanged(propertyName, newValue, oldValue))
            {
                delta.insertChanged(element, HandleDelta.F_CONTENT);
                return;
            }
        }
    }

    /**
     * Returns whether the given property has changed value.
     * <p>
     * Default implementation compares the new value and the old value 
     * for equality (arrays are compared with <code>Arrays.equals</code>).
     * </p>
     *
     * @param propertyName the name of the property (never <code>null</code>)
     * @param newValue the new value of the property (may be <code>null</code>)
     * @param oldValue the old value of the property (may be <code>null</code>)
     * @return <code>true</code> if the property has changed value, and 
     *  <code>false</code> otherwise
     */
    protected boolean isPropertyChanged(String propertyName, Object newValue,
        Object oldValue)
    {
        if (newValue == null)
        {
            if (oldValue != null)
                return true;
        }
        else
        {
            // @formatter:off
            boolean eq;
            if (newValue instanceof Object[] && oldValue instanceof Object[])
                eq = Arrays.deepEquals((Object[])newValue, (Object[])oldValue);
            else if (newValue instanceof byte[] && oldValue instanceof byte[])
                eq = Arrays.equals((byte[])newValue, (byte[])oldValue);
            else if (newValue instanceof short[] && oldValue instanceof short[])
                eq = Arrays.equals((short[])newValue, (short[])oldValue);
            else if (newValue instanceof int[] && oldValue instanceof int[])
                eq = Arrays.equals((int[])newValue, (int[])oldValue);
            else if (newValue instanceof long[] && oldValue instanceof long[])
                eq = Arrays.equals((long[])newValue, (long[])oldValue);
            else if (newValue instanceof char[] && oldValue instanceof char[])
                eq = Arrays.equals((char[])newValue, (char[])oldValue);
            else if (newValue instanceof float[] && oldValue instanceof float[])
                eq = Arrays.equals((float[])newValue, (float[])oldValue);
            else if (newValue instanceof double[] && oldValue instanceof double[])
                eq = Arrays.equals((double[])newValue, (double[])oldValue);
            else if (newValue instanceof boolean[] && oldValue instanceof boolean[])
                eq = Arrays.equals((boolean[])newValue, (boolean[])oldValue);
            else
                eq = newValue.equals(oldValue);
            if (!eq)
                return true;
            // @formatter:on
        }
        return false;
    }

    protected final Object getPropertyValue(String propertyName)
    {
        int length = properties.length;
        for (int i = 0; i < length; i++)
        {
            Property property = properties[i];
            if (property.name.equals(propertyName))
                return property.value;
        }
        return null;
    }

    protected final Set<String> getPropertyNames()
    {
        int length = properties.length;
        Set<String> names = new HashSet<String>(length);
        for (int i = 0; i < length; i++)
            names.add(properties[i].name);
        return names;
    }

    private static class Property
    {
        public final String name;
        public Object value;

        public Property(String name, Object value)
        {
            this.name = name;
            this.value = value;
        }
    }
}
