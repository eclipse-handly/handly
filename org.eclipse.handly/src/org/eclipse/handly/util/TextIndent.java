/*******************************************************************************
 * Copyright (c) 2015 1C-Soft LLC.
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
 * A text indent has a level and is based on an indentation string.
 */
public final class TextIndent
{
    /**
     * An empty indent of level 0.
     */
    public static final TextIndent NONE = new TextIndent(0, ""); //$NON-NLS-1$

    private final int level;
    private final String space;

    /**
     * Returns a text indent of level 0 with the given indentation string.
     *
     * @param space an indentation string (not <code>null</code>)
     * @return a text indent of level 0 with the given indentation string
     */
    public static TextIndent with(String space)
    {
        if (space == null)
            throw new IllegalArgumentException();
        return new TextIndent(0, space);
    }

    /**
     * Returns the level of this text indent.
     *
     * @return the indentation level (0-based)
     */
    public int getLevel()
    {
        return level;
    }

    /**
     * Returns whether this text indent is empty, i.e. is based on an empty
     * indentation string.
     *
     * @return <code>true</code> if this text indent is empty,
     *  <code>false</code> otherwise
     */
    public boolean isEmpty()
    {
        return space.isEmpty();
    }

    /**
     * Returns a text indent increased by one level. Does not modify this object.
     *
     * @return a text indent increased by one level (never <code>null</code>)
     */
    public TextIndent getIncreasedIndent()
    {
        return new TextIndent(level + 1, space);
    }

    /**
     * Returns a text indent decreased by one level. Does not modify this object.
     *
     * @return a text indent decreased by one level (never <code>null</code>)
     * @throws IllegalStateException if this indent's level is zero
     */
    public TextIndent getDecreasedIndent()
    {
        if (level == 0)
            throw new IllegalStateException();
        return new TextIndent(level - 1, space);
    }

    /**
     * Appends a line separator to the given string builder.
     *
     * @param builder a string builder (not <code>null</code>)
     */
    public void appendLineSeparatorTo(StringBuilder builder)
    {
        builder.append('\n');
    }

    /**
     * Appends this text indent to the given string builder.
     *
     * @param builder a string builder (not <code>null</code>)
     */
    public void appendTo(StringBuilder builder)
    {
        for (int i = 0; i < level; i++)
            builder.append(space);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        appendTo(builder);
        return builder.toString();
    }

    private TextIndent(int level, String space)
    {
        this.level = level;
        this.space = space;
    }
}
