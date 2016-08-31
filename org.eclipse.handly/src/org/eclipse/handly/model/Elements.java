/*******************************************************************************
 * Copyright (c) 2014, 2016 1C-Soft LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *     (inspired by Eclipse JDT work)
 *******************************************************************************/
package org.eclipse.handly.model;

import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.internal.Activator;
import org.eclipse.handly.model.impl.IElementImpl;
import org.eclipse.handly.model.impl.ISourceElementImpl;
import org.eclipse.handly.model.impl.ISourceFileImpl;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.snapshot.StaleSnapshotException;
import org.eclipse.handly.util.Property;
import org.eclipse.handly.util.TextRange;

/**
 * Provides methods for generic access to elements of a Handly-based model.
 * <p>
 * Methods annotated as "handle-only" do not require underlying elements
 * to exist. Methods that require underlying elements to exist throw a
 * <code>CoreException</code> when the underlying element is missing.
 * </p>
 * <p>
 * Note that despite having a dependency on {@link IResource} and {@link IFile}
 * this class can safely be used even when <code>org.eclipse.core.resources</code>
 * bundle is not available. This is based on the "outward impression" of late
 * resolution of symbolic references a JVM must provide according to the JVMS.
 * </p>
 *
 * @see IElement
 */
public class Elements
{
    /**
     * Returns the name of the element, or <code>null</code>
     * if the element has no name. This is a handle-only method.
     *
     * @param element not <code>null</code>
     * @return the element name, or <code>null</code> if the element has no name
     */
    public static String getName(IElement element)
    {
        return ((IElementImpl)element).hName();
    }

    /**
     * Returns the immediate parent of the element,
     * or <code>null</code> if the element has no parent.
     * This is a handle-only method.
     *
     * @param element not <code>null</code>
     * @return the parent element, or <code>null</code> if the element has
     *  no parent
     */
    public static IElement getParent(IElement element)
    {
        return ((IElementImpl)element).hParent();
    }

    /**
     * Returns the root element for the element.
     * Returns the element itself if it has no parent.
     * This is a handle-only method.
     *
     * @param element not <code>null</code>
     * @return the root element (never <code>null</code>)
     */
    public static IElement getRoot(IElement element)
    {
        return ((IElementImpl)element).hRoot();
    }

    /**
     * Returns the element's closest ancestor that has the given type.
     * Returns <code>null</code> if no such ancestor can be found.
     * This is a handle-only method.
     *
     * @param element not <code>null</code>
     * @param ancestorType the given type (not <code>null</code>)
     * @return the closest ancestor element that has the given type,
     *  or <code>null</code> if no such ancestor can be found
     */
    public static <T> T getAncestor(IElement element, Class<T> ancestorType)
    {
        return ((IElementImpl)element).hAncestor(ancestorType);
    }

    /**
     * Returns the model that owns the element. This is a handle-only method.
     * <p>
     * Note that the relationship between an element and its owing model does
     * not change over the lifetime of an element.
     * </p>
     *
     * @param element not <code>null</code>
     * @return the element's model (never <code>null</code>)
     * @throws IllegalStateException if the model is no longer accessible
     */
    public static IModel getModel(IElement element)
    {
        return ((IElementImpl)element).hModel();
    }

    /**
     * Returns the innermost resource enclosing the element, or <code>null</code>
     * if the element is not enclosed in a workspace resource.
     * This is a handle-only method.
     * <p>
     * Note that it is safe to call this method and test the return value
     * for <code>null</code> even when <code>org.eclipse.core.resources</code>
     * bundle is not available.
     * </p>
     *
     * @param element not <code>null</code>
     * @return the innermost resource enclosing the element, or <code>null</code>
     *  if the element is not enclosed in a workspace resource
     */
    public static IResource getResource(IElement element)
    {
        return ((IElementImpl)element).hResource();
    }

    /**
     * Returns the path to the innermost resource enclosing the element.
     * If the element is enclosed in a workspace resource, the path returned
     * is the full, absolute path to the underlying resource, relative to
     * the workspace. Otherwise, the path returned is the absolute path to
     * a file or to a folder in the file system.
     * This is a handle-only method.
     *
     * @param element not <code>null</code>
     * @return the path to the innermost resource enclosing the element
     *  (never <code>null</code>)
     */
    public static IPath getPath(IElement element)
    {
        return ((IElementImpl)element).hPath();
    }

    /**
     * Returns whether the element exists in the model.
     * <p>
     * Handles may or may not be backed by an actual element. Handles that are
     * backed by an actual element are said to "exist". It is always the case
     * that if an element exists, then its parent also exists (provided
     * it has one) and includes that element as one of its children.
     * It is therefore possible to navigate to any existing element
     * from the root element along a chain of existing elements.
     * </p>
     *
     * @param element not <code>null</code>
     * @return <code>true</code> if the element exists in the model, and
     *  <code>false</code> if the element does not exist
     */
    public static boolean exists(IElement element)
    {
        return ((IElementImpl)element).hExists();
    }

    /**
     * Returns the immediate children of the element. Unless otherwise specified
     * by the implementing element, the children are in no particular order.
     *
     * @param element not <code>null</code>
     * @return the immediate children of the element (never <code>null</code>).
     *  Clients <b>must not</b> modify the returned array.
     * @throws CoreException if the element does not exist or if an
     *  exception occurs while accessing its corresponding resource
     */
    public static IElement[] getChildren(IElement element) throws CoreException
    {
        return ((IElementImpl)element).hChildren();
    }

    /**
     * Returns the immediate children of the element that have the given type.
     * Unless otherwise specified by the implementing element, the children are
     * in no particular order.
     *
     * @param element not <code>null</code>
     * @param childType the given type (not <code>null</code>)
     * @return the immediate children of the element that have the given type
     *  (never <code>null</code>). Clients <b>must not</b> modify the returned
     *  array.
     * @throws CoreException if the element does not exist or if an
     *  exception occurs while accessing its corresponding resource
     */
    public static <T> T[] getChildren(IElement element, Class<T> childType)
        throws CoreException
    {
        return ((IElementImpl)element).hChildren(childType);
    }

    /**
     * Debugging purposes. Returns a string representation of the element.
     * Note that the format options specified in the given context serve as
     * a hint that implementations may or may not fully support.
     * <p>
     * Implementations are advised to support common hints defined in
     * {@link org.eclipse.handly.util.ToStringOptions ToStringOptions} and
     * interpret the format style as follows:
     * </p>
     * <ul>
     * <li>{@link org.eclipse.handly.util.ToStringOptions.FormatStyle#FULL FULL}
     * - A full representation that lists ancestors and children.</li>
     * <li>{@link org.eclipse.handly.util.ToStringOptions.FormatStyle#LONG LONG}
     * - A long representation that lists children but not ancestors.</li>
     * <li>{@link org.eclipse.handly.util.ToStringOptions.FormatStyle#MEDIUM MEDIUM}
     * - A compact representation that lists ancestors but not children.</li>
     * <li>{@link org.eclipse.handly.util.ToStringOptions.FormatStyle#SHORT SHORT}
     * - A minimal representation that does not list ancestors or children.</li>
     * </ul>
     *
     * @param element not <code>null</code>
     * @param context not <code>null</code>
     * @return a string representation of the element (never <code>null</code>)
     */
    public static String toString(IElement element, IContext context)
    {
        return ((IElementImpl)element).hToString(context);
    }

    /**
     * Returns whether the given elements are equal and have the same parent.
     *
     * @param e1 the first element (not <code>null</code>)
     * @param e2 the second element (may be <code>null</code>)
     * @return <code>true</code> if the given elements are equal and have
     *  the same parent, <code>false</code> otherwise
     */
    public static boolean equalsAndSameParent(IElement e1, IElement e2)
    {
        if (!e1.equals(e2))
            return false;

        return Objects.equals(getParent(e1), getParent(e2));
    }

    /**
     * Returns the smallest element that includes the given source position,
     * or <code>null</code> if the given position is not within the source range
     * of the given element. If no finer grained element is found at the
     * position, the given element itself is returned.
     *
     * @param context a source element (not <code>null</code>)
     * @param position a source position (0-based)
     * @param base a snapshot on which the given position is based,
     *  or <code>null</code> if the snapshot is unknown or doesn't matter
     * @return the innermost element enclosing the given source position,
     *  or <code>null</code> if none (including the given element itself)
     * @throws CoreException if the given element does not exist or if an
     *  exception occurs while accessing its corresponding resource
     * @throws StaleSnapshotException if snapshot inconsistency is detected,
     *  i.e. the given element's current structure and properties are based on
     *  a different snapshot
     */
    public static ISourceElement getSourceElementAt(ISourceElement context,
        int position, ISnapshot base) throws CoreException
    {
        return ((ISourceElementImpl)context).hSourceElementAt(position, base);
    }

    /**
     * Returns the smallest element that includes the given position,
     * or <code>null</code> if the given position is not within the source
     * range of the given element, or if the given element does not exist or
     * an exception occurs while accessing its corresponding resource, or if
     * snapshot inconsistency is detected. If no finer grained element is
     * found at the position, the given element itself is returned.
     *
     * @param context a source element (not <code>null</code>)
     * @param position a source position (0-based)
     * @param base a snapshot on which the given position is based,
     *  or <code>null</code> if the snapshot is unknown or doesn't matter
     * @return the innermost element enclosing the given source position,
     *  or <code>null</code> if none (including the given element itself)
     */
    public static ISourceElement getSourceElementAt2(ISourceElement context,
        int position, ISnapshot base)
    {
        try
        {
            return getSourceElementAt(context, position, base);
        }
        catch (CoreException e)
        {
            if (!exists(context))
                ; // this is considered normal
            else
                Activator.log(e.getStatus());
        }
        catch (StaleSnapshotException e)
        {
            // ignore
        }
        return null;
    }

    /**
     * Returns an object holding cached structure and properties for the
     * source element.
     *
     * @param element not <code>null</code>
     * @return {@link ISourceElementInfo} for the element
     *  (never <code>null</code>)
     * @throws CoreException if the element does not exist or if an
     *  exception occurs while accessing its corresponding resource
     */
    public static ISourceElementInfo getSourceElementInfo(
        ISourceElement element) throws CoreException
    {
        return ((ISourceElementImpl)element).hSourceElementInfo();
    }

    /**
     * Returns an object holding cached structure and properties for the
     * source element, or {@link #NO_SOURCE_ELEMENT_INFO} if source info is not
     * available.
     *
     * @param element not <code>null</code>
     * @return {@link ISourceElementInfo} for the element
     *  (never <code>null</code>)
     */
    public static ISourceElementInfo getSourceElementInfo2(
        ISourceElement element)
    {
        try
        {
            return getSourceElementInfo(element);
        }
        catch (CoreException e)
        {
            if (!exists(element))
                ; // this is considered normal
            else
                Activator.log(e.getStatus());
        }
        return NO_SOURCE_ELEMENT_INFO;
    }

    /**
     * A 'null object' indicating that no info is available for source element.
     * <p>
     * The instance methods return either <code>null</code> (if allowed
     * by the method contract) or an appropriate 'null object' (such as
     * a zero-length array).
     * </p>
     * @see ISourceElementInfo
     */
    public static final ISourceElementInfo NO_SOURCE_ELEMENT_INFO =
        new NoSourceElementInfo();

    private static class NoSourceElementInfo
        implements ISourceElementInfo
    {
        static final ISourceConstruct[] NO_CHILDREN = new ISourceConstruct[0];

        @Override
        public ISnapshot getSnapshot()
        {
            return null;
        }

        @Override
        public <T> T get(Property<T> property)
        {
            return null;
        }

        @Override
        public ISourceConstruct[] getChildren()
        {
            return NO_CHILDREN;
        }

        @Override
        public TextRange getFullRange()
        {
            return null;
        }

        @Override
        public TextRange getIdentifyingRange()
        {
            return null;
        }
    };

    /**
     * Returns the source file that contains the given element,
     * or <code>null</code> if the given element is not contained in a
     * source file. Returns the given element itself if it is a source file.
     *
     * @param element not <code>null</code>
     * @return the source file containing the given element,
     *  or <code>null</code> if none
     */
    public static ISourceFile getSourceFile(ISourceElement element)
    {
        if (element instanceof ISourceFile)
            return (ISourceFile)element;
        else
            return getAncestor(element, ISourceFile.class);
    }

    /**
     * Ensures that, if the given element is contained in a source file,
     * the source file is reconciled. Note that the call may result in
     * change of existence status for the given element: if the element
     * did not exist before, it may be brought into existence; conversely,
     * if the element existed, it may cease to exist.
     *
     * @param element not <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired
     * @return <code>true</code> if the call completed successfully,
     *  <code>false</code> in case of a failure
     * @throws OperationCanceledException if this method is canceled
     */
    public static boolean ensureReconciled(ISourceElement element,
        IProgressMonitor monitor)
    {
        ISourceFile sourceFile = getSourceFile(element);
        if (sourceFile != null)
        {
            try
            {
                reconcile(sourceFile, false, monitor);
            }
            catch (CoreException e)
            {
                Activator.log(e.getStatus());
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the underlying {@link IFile}, or <code>null</code>
     * if the source file has no underlying file in the workspace.
     * This is a handle-only method.
     * <p>
     * This method returns the same value as {@link #getResource(IElement)},
     * but saves a downcast.
     * </p>
     *
     * @param sourceFile not <code>null</code>
     * @return the underlying <code>IFile</code>, or <code>null</code>
     *  if the source file has no underlying file in the workspace
     */
    public static IFile getFile(ISourceFile sourceFile)
    {
        return ((ISourceFileImpl)sourceFile).hFile();
    }

    /**
     * Returns whether the source file is a working copy.
     *
     * @param sourceFile not <code>null</code>
     * @return <code>true</code> if the source file is a working copy,
     *  <code>false</code> otherwise
     */
    public static boolean isWorkingCopy(ISourceFile sourceFile)
    {
        return ((ISourceFileImpl)sourceFile).hIsWorkingCopy();
    }

    /**
     * Returns whether the source file needs reconciling.
     * A source file needs reconciling if it is a working copy and
     * its buffer has been modified since the last time it was reconciled.
     *
     * @param sourceFile not <code>null</code>
     * @return <code>true</code> if the source file needs reconciling,
     *  <code>false</code> otherwise
     */
    public static boolean needsReconciling(ISourceFile sourceFile)
    {
        return ((ISourceFileImpl)sourceFile).hNeedsReconciling();
    }

    /**
     * Makes the working copy consistent with its buffer by updating
     * the element's structure and properties as necessary. Does nothing
     * if the source file is not in working copy mode. The boolean argument
     * allows to force reconciling even if the working copy is already
     * consistent with its buffer.
     *
     * @param sourceFile not <code>null</code>
     * @param force indicates whether reconciling has to be performed
     *  even if the working copy is already consistent with its buffer
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired
     * @throws CoreException if the working copy cannot be reconciled
     * @throws OperationCanceledException if this method is canceled
     */
    public static void reconcile(ISourceFile sourceFile, boolean force,
        IProgressMonitor monitor) throws CoreException
    {
        ((ISourceFileImpl)sourceFile).hReconcile(force, monitor);
    }

    /**
     * Returns the buffer opened for the source file. Note that buffers may
     * be shared by multiple clients, so the returned buffer may have unsaved
     * changes if it has been modified by another client.
     * <p>
     * The client takes (potentially shared) ownership of the returned buffer
     * and is responsible for releasing it when finished. The buffer will be
     * disposed only after it is released by every owner. The buffer must not
     * be accessed by clients which don't own it.
     * </p>
     *
     * @param sourceFile not <code>null</code>
     * @return the buffer opened for the source file (never <code>null</code>)
     * @throws CoreException if the source file does not exist
     *  or if its contents cannot be accessed
     * @see IBuffer
     */
    public static IBuffer getBuffer(ISourceFile sourceFile) throws CoreException
    {
        return ((ISourceFileImpl)sourceFile).hBuffer();
    }

    /**
     * Returns the buffer opened for the source file. Note that buffers may
     * be shared by multiple clients, so the returned buffer may have unsaved
     * changes if it has been modified by another client.
     * <p>
     * The client takes (potentially shared) ownership of the returned buffer
     * and is responsible for releasing it when finished. The buffer will be
     * disposed only after it is released by every owner. The buffer must not
     * be accessed by clients which don't own it.
     * </p>
     * <p>
     * If <code>create == false</code> and there is no buffer currently
     * opened for the source file, <code>null</code> is returned.
     * </p>
     *
     * @param sourceFile not <code>null</code>
     * @param create indicates whether a new buffer should be created
     *  if none already exists for the source file
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired
     * @return the buffer opened for the source file, or <code>null</code>
     *  if <code>create == false</code> and there is no buffer currently opened
     *  for the source file
     * @throws CoreException if the source file does not exist
     *  or if its contents cannot be accessed
     * @throws OperationCanceledException if this method is canceled
     * @see IBuffer
     */
    public static IBuffer getBuffer(ISourceFile sourceFile, boolean create,
        IProgressMonitor monitor) throws CoreException
    {
        return ((ISourceFileImpl)sourceFile).hBuffer(create, monitor);
    }

    private Elements()
    {
    }
}
