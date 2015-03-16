/*******************************************************************************
 * Copyright (c) 2014, 2015 1C-Soft LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.util;

/**
 * Describes a certain range in an indexed text store. Text stores are
 * for example documents or strings. A text range is defined by its offset
 * into the text store and its length.
 * <p>
 * A text range is a value object. Its offset and length do not change over time.
 * </p>
 */
public final class TextRange
{
    private final int offset;
    private final int length;

    /**
     * Instantiates a new text range using the given offset and the given length.
     *
     * @param offset the given offset (0-based)
     * @param length the given length (non-negative)
     */
    public TextRange(int offset, int length)
    {
        if (offset < 0)
            throw new IllegalArgumentException();
        if (length < 0)
            throw new IllegalArgumentException();
        if (offset < 0 && length > 0)
            throw new IllegalArgumentException();
        this.offset = offset;
        this.length = length;
    }

    /**
     * @return the 0-based index of the first character of this text range
     */
    public int getOffset()
    {
        return offset;
    }

    /**
     * @return the number of characters in this text range. Returns 0 for
     *  an {@link #isEmpty() empty} range
     */
    public int getLength()
    {
        return length;
    }

    /**
     * @return the 0-based index of the next character of this text range.
     *  The returned value is the result of the following calculation:
     *  <code>getOffset() + getLength()</code>
     */
    public int getEndOffset()
    {
        return offset + length;
    }

    /**
     * @return <code>true</code> if this text range is empty
     *  (i.e. its length is 0), and <code>false</code> otherwise
     */
    public boolean isEmpty()
    {
        return length == 0;
    }

    /**
     * Returns whether this text range covers the given position,
     * excluding the {@link #getEndOffset() end offset} of the range.
     *
     * @param position a text position (0-based)
     * @return <code>true</code> if this text range strictly covers the given
     *  position, and <code>false</code> otherwise
     * @see #covers(int)
     */
    public boolean strictlyCovers(int position)
    {
        return position >= offset && position < offset + length;
    }

    /**
     * Returns whether this text range covers the given position,
     * including the {@link #getEndOffset() end offset} of the range.
     *
     * @param position a text position (0-based)
     * @return <code>true</code> if this text range covers the given position,
     *  and <code>false</code> otherwise
     * @see #strictlyCovers(int)
     */
    public boolean covers(int position)
    {
        return position >= offset && position <= offset + length;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + length;
        result = prime * result + offset;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TextRange other = (TextRange)obj;
        if (length != other.length)
            return false;
        if (offset != other.offset)
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "[offset=" + offset + ", length=" + length + ']'; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
