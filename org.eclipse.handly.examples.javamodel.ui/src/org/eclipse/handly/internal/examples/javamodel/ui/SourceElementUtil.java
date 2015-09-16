/*******************************************************************************
 * Copyright (c) 2014, 2015 1C LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *     Ondrej Ilcik (Codasip) - adapted for Java model
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.model.ISourceElement;
import org.eclipse.handly.model.ISourceElementInfo;
import org.eclipse.handly.model.ISourceFile;
import org.eclipse.handly.util.TextRange;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Utilities for <code>ISourceElement</code>s.
 * <p>
 * Note how this code is free from specifics of the Java Model, 
 * thanks to the uniform API provided by Handly.
 * </p>
 */
public class SourceElementUtil
{
    /**
     * Returns the smallest element within the given source file 
     * that includes the given source position, or <code>null</code> 
     * if the given position is not within the text range of this source file, 
     * or if the source file does not exist or an exception occurs while 
     * accessing its corresponding resource. If no finer grained element 
     * is found at the position, the source file itself is returned.
     * <p>
     * As a side effect, reconciles the given source file.
     * </p>
     *
     * @param sourceFile the source file (not <code>null</code>)
     * @param position a source position inside the source file (0-based)
     * @return the innermost element enclosing the given source position, 
     *  or <code>null</code> if none (including the source file itself)
     */
    public static ISourceElement getSourceElement(ISourceFile sourceFile,
        int position)
    {
        try
        {
            sourceFile.reconcile(false, null);
        }
        catch (CoreException e)
        {
            Activator.log(e.getStatus());
            return null;
        }
        return sourceFile.getElementAt(position, null);
    }

    /**
     * Selects and reveals the identifying range of the given source element 
     * in the given text editor. Returns <code>false</code> if the identifying 
     * range is not set or cannot be obtained (e.g., the element does not exist). 
     *
     * @param textEditor not <code>null</code>
     * @param element not <code>null</code>
     * @return <code>true</code> if the element was successfully revealed 
     *  in the editor; <code>false</code> otherwise
     */
    public static boolean revealInTextEditor(ITextEditor textEditor,
        ISourceElement element)
    {
        ISourceElementInfo info;
        try
        {
            info = element.getSourceElementInfo();
        }
        catch (CoreException e)
        {
            if (!element.exists())
                ; // this is considered normal
            else
                Activator.log(e.getStatus());
            return false;
        }
        TextRange identifyingRange = info.getIdentifyingRange();
        if (identifyingRange.isNull())
            return false;
        textEditor.selectAndReveal(identifyingRange.getOffset(),
            identifyingRange.getLength());
        return true;
    }

    private SourceElementUtil()
    {
    }
}
