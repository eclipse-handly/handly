/*******************************************************************************
 * Copyright (c) 2014, 2016 1C-Soft LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *     (inspired by Eclipse LTK work)
 *******************************************************************************/
package org.eclipse.handly.refactoring;

import static org.eclipse.handly.context.Contexts.EMPTY_CONTEXT;
import static org.eclipse.handly.model.Elements.getBuffer;
import static org.eclipse.handly.model.Elements.getFile;
import static org.eclipse.handly.model.Elements.getPath;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.handly.buffer.BufferChange;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.buffer.IBufferChange;
import org.eclipse.handly.buffer.SaveMode;
import org.eclipse.handly.internal.Activator;
import org.eclipse.handly.model.ISourceFile;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.snapshot.NonExpiringSnapshot;
import org.eclipse.handly.snapshot.StaleSnapshotException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChangeGroup;
import org.eclipse.ltk.core.refactoring.TextEditChangeGroup;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditCopier;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.text.edits.TextEditProcessor;

/**
 * A {@link TextEditBasedChange} that operates on a {@link ISourceFile}.
 */
public class SourceFileChange
    extends TextEditBasedChange
{
    /*
     * A special object denoting all edits managed by the change. This even
     * includes those edits not managed by a group.
     */
    private static final TextEditBasedChangeGroup[] ALL_EDITS =
        new TextEditBasedChangeGroup[0];

    private ISourceFile sourceFile;
    private TextEdit edit;
    private ISnapshot base;
    private SaveMode saveMode = SaveMode.KEEP_SAVED_STATE;
    private TextEditCopier copier;

    /**
     * Creates a change that initially contains only an empty root edit.
     *
     * @param name the change's name, mainly used to render the change in the UI
     * @param sourceFile the source file this change works on
     */
    public SourceFileChange(String name, ISourceFile sourceFile)
    {
        this(name, sourceFile, new MultiTextEdit());
    }

    /**
     * Creates a change with the given edit tree. The structure of the tree
     * may be modified later.
     *
     * @param name the change's name, mainly used to render the change in the UI
     * @param sourceFile the source file this change works on
     * @param edit the root of the change's edit tree
     */
    public SourceFileChange(String name, ISourceFile sourceFile, TextEdit edit)
    {
        super(name);
        if ((this.sourceFile = sourceFile) == null)
            throw new IllegalArgumentException();
        if ((this.edit = edit) == null)
            throw new IllegalArgumentException();
        setTextType(getPath(sourceFile).getFileExtension());
    }

    /**
     * @return the root of the change's edit tree (never <code>null</code>)
     */
    public TextEdit getEdit()
    {
        return edit;
    }

    /**
     * Inserts the given edit into the change's edit tree.
     *
     * @param edit the edit to insert - must not be <code>null</code>
     * @throws MalformedTreeException if edit can't be inserted
     */
    public void addEdit(TextEdit edit)
    {
        insert(this.edit, edit);
    }

    /**
     * Inserts the given edits into the change's edit tree.
     *
     * @param edits the edits to insert - must not be <code>null</code>
     * @throws MalformedTreeException if edits can't be inserted
     */
    public void addEdits(TextEdit[] edits)
    {
        for (TextEdit edit : edits)
            addEdit(edit);
    }

    /**
     * Inserts the edits of the given group into the change's edit tree
     * and then {@link TextEditBasedChange#addChangeGroup(TextEditBasedChangeGroup)
     * adds} the group itself to the change.
     *
     * @param group the group to add - must not be <code>null</code>
     * @throws MalformedTreeException if edits can't be inserted
     */
    public void addGroupedEdits(TextEditBasedChangeGroup group)
    {
        addEdits(group.getTextEdits());
        addChangeGroup(group);
    }

    /**
     * Inserts the edits of the given group into the change's edit tree
     * and then {@link TextEditBasedChange#addTextEditGroup(TextEditGroup)
     * adds} the group itself to the change.
     *
     * @param group the group to add - must not be <code>null</code>
     * @throws MalformedTreeException if edits can't be inserted
     */
    public void addGroupedEdits(TextEditGroup group)
    {
        addGroupedEdits(new TextEditBasedChangeGroup(this, group));
    }

    /**
     * Sets the snapshot on which the change is based.
     *
     * @param base the snapshot on which the change is based,
     *  or <code>null</code> if unknown
     */
    public void setBase(ISnapshot base)
    {
        this.base = base;
    }

    /**
     * Returns the snapshot on which the change's edit tree is based,
     * or <code>null</code> if the snapshot is unknown.
     *
     * @return the snapshot on which the change is based,
     *  or <code>null</code> if unknown
     */
    public ISnapshot getBase()
    {
        return base;
    }

    /**
     * Sets the save mode of the change.
     *
     * @param saveMode indicates whether the buffer is to be saved
     *  after the change has been successfully applied
     */
    public void setSaveMode(SaveMode saveMode)
    {
        this.saveMode = saveMode;
    }

    /**
     * Returns the save mode associated with the change.
     *
     * @return the change's save mode
     */
    public SaveMode getSaveMode()
    {
        return saveMode;
    }

    @Override
    public void dispose()
    {
        edit = null;
        base = null;
        copier = null;
        super.dispose();
    }

    @Override
    public void initializeValidationData(IProgressMonitor pm)
    {
    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException,
        OperationCanceledException
    {
        RefactoringStatus result = new RefactoringStatus();

        if (base == null)
            return result; // OK

        try (IBuffer buffer = getBuffer(sourceFile, EMPTY_CONTEXT, pm))
        {
            if (!base.isEqualTo(buffer.getSnapshot()))
            {
                result.addFatalError(MessageFormat.format(
                    Messages.SourceFileChange_Cannot_apply_stale_change__0,
                    getPath(sourceFile).makeRelative()));
            }
        }
        return result;
    }

    @Override
    public Change perform(IProgressMonitor pm) throws CoreException
    {
        pm.beginTask("", 2); //$NON-NLS-1$
        try (
            IBuffer buffer = getBuffer(sourceFile, EMPTY_CONTEXT,
                new SubProgressMonitor(pm, 1)))
        {
            BufferChangeWithExcludes change = new BufferChangeWithExcludes(
                edit);
            change.setExcludes(getDisabledEdits());
            change.setBase(base);
            change.setStyle(IBufferChange.CREATE_UNDO
                | IBufferChange.UPDATE_REGIONS);
            change.setSaveMode(saveMode);

            IBufferChange undoChange;

            try
            {
                undoChange = buffer.applyChange(change, new SubProgressMonitor(
                    pm, 1));
            }
            catch (StaleSnapshotException e)
            {
                throw new CoreException(Activator.createErrorStatus(
                    MessageFormat.format(
                        Messages.SourceFileChange_Cannot_apply_stale_change__0,
                        getPath(sourceFile).makeRelative()), e));
            }

            return new UndoSourceFileChange(getName(), sourceFile, undoChange);
        }
        finally
        {
            pm.done();
        }
    }

    @Override
    public Object getModifiedElement()
    {
        return sourceFile;
    }

    @Override
    public Object[] getAffectedObjects()
    {
        IFile file = getFile(sourceFile);
        if (file == null)
            return null;
        return new Object[] { file };
    }

    @Override
    public String getCurrentContent(IProgressMonitor pm) throws CoreException
    {
        try (IBuffer buffer = getBuffer(sourceFile, EMPTY_CONTEXT, pm))
        {
            NonExpiringSnapshot snapshot = new NonExpiringSnapshot(buffer);
            return snapshot.getContents();
        }
    }

    @Override
    public String getCurrentContent(IRegion region,
        boolean expandRegionToFullLine, int surroundingLines,
        IProgressMonitor pm) throws CoreException
    {
        if (region == null)
            throw new IllegalArgumentException();
        if (expandRegionToFullLine && surroundingLines < 0)
            throw new IllegalArgumentException();
        String content = getCurrentContent(pm);
        if (content.length() < region.getOffset() + region.getLength())
            throw new IllegalArgumentException();
        return getDocumentContent(new Document(content), region,
            expandRegionToFullLine, surroundingLines);
    }

    @Override
    public String getPreviewContent(IProgressMonitor pm) throws CoreException
    {
        return getPreviewDocument(pm).get();
    }

    @Override
    public String getPreviewContent(TextEditBasedChangeGroup[] changeGroups,
        IRegion region, boolean expandRegionToFullLine, int surroundingLines,
        IProgressMonitor pm) throws CoreException
    {
        IRegion changeRegion = getRegion(changeGroups);

        Preview preview = getPreview(changeGroups, pm);

        int delta;
        if (preview.changeRegion == null) // all edits were delete edits so no new region
            delta = -changeRegion.getLength();
        else
            delta = preview.changeRegion.getLength() - changeRegion.getLength();

        return getDocumentContent(preview.document, new Region(
            region.getOffset(), region.getLength() + delta),
            expandRegionToFullLine, surroundingLines);
    }

    @Override
    public void setKeepPreviewEdits(boolean keep)
    {
        super.setKeepPreviewEdits(keep);

        if (!keep)
            copier = null;
    }

    /**
     * Returns the edit that got executed during preview generation
     * instead of the given original. The method requires that <code>
     * setKeepPreviewEdits</code> is set to <code>true</code> and that
     * a preview has been requested via one of the <code>getPreview*
     * </code> methods.
     * <p>
     * The method returns <code>null</code> if the original isn't managed
     * by this text change.
     * </p>
     *
     * @param original the original edit managed by this text change
     *
     * @return the edit executed during preview generation
     */
    public TextEdit getPreviewEdit(TextEdit original)
    {
        if (!getKeepPreviewEdits() || copier == null)
            throw new IllegalStateException();
        return copier.getCopy(original);
    }

    /**
     * Returns the edits that were executed during preview generation
     * instead of the given array of original edits. The method requires
     * that <code>setKeepPreviewEdits</code> is set to <code>true</code>
     * and that a preview has been requested via one of the <code>
     * getPreview*</code> methods.
     * <p>
     * The method returns an empty array if none of the original edits
     * is managed by this text change.
     * </p>
     *
     * @param originals an array of original edits managed by this text
     *  change
     *
     * @return an array of edits containing the corresponding edits
     *  executed during preview generation
     */
    public TextEdit[] getPreviewEdits(TextEdit[] originals)
    {
        if (!getKeepPreviewEdits() || copier == null)
            throw new IllegalStateException();

        if (originals == null || originals.length == 0)
            return new TextEdit[0];

        List<TextEdit> result = new ArrayList<TextEdit>(originals.length);
        for (TextEdit original : originals)
        {
            TextEdit copy = copier.getCopy(original);
            if (copy != null)
                result.add(copy);
        }
        return result.toArray(new TextEdit[result.size()]);
    }

    /**
     * Returns a document containing a preview of the text change. The
     * preview is computed by executing the all managed text edits. The
     * method considers the active state of the added {@link TextEditChangeGroup
     * text edit change groups}.
     *
     * @param pm a progress monitor to report progress or <code>null</code>
     *  if no progress reporting is desired
     * @return a document containing the preview of the text change
     *
     * @throws CoreException if the preview can't be created
     */
    public IDocument getPreviewDocument(IProgressMonitor pm)
        throws CoreException
    {
        return getPreview(ALL_EDITS, pm).document;
    }

    /*
     * Inserts {@link TextEdit} in edit's tree, trying not to break edit's tree well-formedness.
     *
     * @param parent edit's tree root (not <code>null</code>)
     * @param edit edit to insert (not <code>null</code>)
     * @throws MalformedTreeException if edit cannot be inserted
     */
    private static void insert(TextEdit parent, TextEdit edit)
    {
        if (edit == null)
            throw new IllegalArgumentException();
        if (!parent.hasChildren())
        {
            parent.addChild(edit);
            return;
        }
        TextEdit[] children = parent.getChildren();
        // First dive down to find the right parent.
        for (int i = 0; i < children.length; i++)
        {
            TextEdit child = children[i];
            if (covers(child, edit))
            {
                insert(child, edit);
                return;
            }
        }
        // We have the right parent. Now check if some of the children have to
        // be moved under the new edit since it is covering it.
        int removed = 0;
        for (int i = 0; i < children.length; i++)
        {
            TextEdit child = children[i];
            if (covers(edit, child))
            {
                parent.removeChild(i - removed++);
                edit.addChild(child);
            }
        }
        parent.addChild(edit);
    }

    private static boolean covers(TextEdit thisEdit, TextEdit otherEdit)
    {
        if (thisEdit.getLength() == 0) // an insertion point can't cover anything
            return false;

        int thisOffset = thisEdit.getOffset();
        int thisEnd = thisEdit.getExclusiveEnd();
        if (otherEdit.getLength() == 0)
        {
            int otherOffset = otherEdit.getOffset();
            return thisOffset < otherOffset && otherOffset < thisEnd;
        }
        else
        {
            int otherOffset = otherEdit.getOffset();
            int otherEnd = otherEdit.getExclusiveEnd();
            return thisOffset <= otherOffset && otherEnd <= thisEnd;
        }
    }

    /*
     * Adapted from TextEditBasedChange#getContent(...)
     */
    private String getDocumentContent(IDocument document, IRegion region,
        boolean expandRegionToFullLine, int surroundingLines)
        throws CoreException
    {
        try
        {
            if (expandRegionToFullLine)
            {
                int startLine = Math.max(document.getLineOfOffset(
                    region.getOffset()) - surroundingLines, 0);
                int endLine;
                if (region.getLength() == 0)
                {
                    // no lines are in the region, so remove one from the context,
                    // or else spurious changes show up that look like deletes from the source
                    if (surroundingLines == 0)
                    {
                        // empty: show nothing
                        return ""; //$NON-NLS-1$
                    }
                    endLine = Math.min(document.getLineOfOffset(
                        region.getOffset()) + surroundingLines - 1,
                        document.getNumberOfLines() - 1);
                }
                else
                {
                    endLine = Math.min(document.getLineOfOffset(
                        region.getOffset() + region.getLength() - 1)
                        + surroundingLines, document.getNumberOfLines() - 1);
                }

                int offset = document.getLineInformation(startLine).getOffset();
                IRegion endLineRegion = document.getLineInformation(endLine);
                int length = endLineRegion.getOffset()
                    + endLineRegion.getLength() - offset;
                return document.get(offset, length);
            }
            else
            {
                return document.get(region.getOffset(), region.getLength());
            }
        }
        catch (BadLocationException e)
        {
            throw new CoreException(Activator.createErrorStatus(e.getMessage(),
                e));
        }
    }

    private Collection<TextEdit> getDisabledEdits()
    {
        return Edits.DISABLED.of(getChangeGroups());
    }

    private Preview getPreview(TextEditBasedChangeGroup[] groups,
        IProgressMonitor pm) throws CoreException
    {
        IDocument document = new Document(getCurrentContent(pm));
        copier = new TextEditCopier(edit);
        try
        {
            createPreviewEditProcessor(document, groups).performEdits();
            return new Preview(document, getNewRegion(groups));
        }
        catch (BadLocationException e)
        {
            throw new CoreException(Activator.createErrorStatus(e.getMessage(),
                e));
        }
        finally
        {
            if (!getKeepPreviewEdits())
                copier = null;
        }
    }

    private TextEditProcessor createPreviewEditProcessor(IDocument document,
        TextEditBasedChangeGroup[] groups)
    {
        TextEdit copiedEdit = copier.perform();

        PreviewEditProcessor result = new PreviewEditProcessor(document,
            copiedEdit, getKeepPreviewEdits() ? TextEdit.UPDATE_REGIONS
                : TextEdit.NONE);
        if (groups == ALL_EDITS)
            result.setExcludes(mapEdits(getDisabledEdits()));
        else
            result.setIncludes(mapEdits(Edits.ENABLED.of(groups)));
        return result;
    }

    private IRegion getRegion(TextEditBasedChangeGroup[] groups)
    {
        if (groups == ALL_EDITS)
        {
            return edit.getRegion();
        }
        else
        {
            Collection<TextEdit> edits = Edits.ALL.of(groups);
            if (edits.isEmpty())
                return null;
            return TextEdit.getCoverage(edits.toArray(
                new TextEdit[edits.size()]));
        }
    }

    private IRegion getNewRegion(TextEditBasedChangeGroup[] groups)
    {
        if (groups == ALL_EDITS)
        {
            return copier.getCopy(edit).getRegion();
        }
        else
        {
            Collection<TextEdit> previewEdits = mapEdits(Edits.ALL.of(groups));
            if (previewEdits.isEmpty())
                return null;
            return TextEdit.getCoverage(previewEdits.toArray(
                new TextEdit[previewEdits.size()]));
        }
    }

    private Collection<TextEdit> mapEdits(Collection<TextEdit> edits)
    {
        List<TextEdit> result = new ArrayList<TextEdit>(edits.size());
        for (TextEdit edit : edits)
        {
            TextEdit copy = copier.getCopy(edit);
            if (copy != null)
                result.add(copy);
        }
        return result;
    }

    private static Set<TextEdit> flatten(Collection<TextEdit> edits)
    {
        Set<TextEdit> result = new HashSet<TextEdit>();
        for (TextEdit edit : edits)
        {
            flatten(result, edit);
        }
        return result;
    }

    private static void flatten(Set<TextEdit> result, TextEdit edit)
    {
        result.add(edit);
        TextEdit[] children = edit.getChildren();
        for (TextEdit child : children)
        {
            flatten(result, child);
        }
    }

    /*
     * Buffer change with the ability to selectively exclude single text edits.
     */
    private static class BufferChangeWithExcludes
        extends BufferChange
    {
        private Set<TextEdit> excludes;

        public BufferChangeWithExcludes(TextEdit edit)
        {
            super(edit);
        }

        public void setExcludes(Collection<TextEdit> excludes)
        {
            if (excludes == null)
                throw new IllegalArgumentException();
            this.excludes = flatten(excludes);
        }

        @Override
        public boolean contains(TextEdit edit)
        {
            if (!super.contains(edit))
                return false;
            if (excludes != null && excludes.contains(edit))
                return false;
            return true;
        }
    }

    /*
     * Text edit processor which has the ability to selectively include or
     * exclude single text edits.
     */
    private static class PreviewEditProcessor
        extends TextEditProcessor
    {
        private Set<TextEdit> excludes;
        private Set<TextEdit> includes;

        public PreviewEditProcessor(IDocument document, TextEdit root,
            int flags)
        {
            super(document, root, flags);
        }

        public void setIncludes(Collection<TextEdit> includes)
        {
            if (includes == null)
                throw new IllegalArgumentException();
            if (excludes != null)
                throw new IllegalStateException();
            this.includes = flatten(includes);
        }

        public void setExcludes(Collection<TextEdit> excludes)
        {
            if (excludes == null)
                throw new IllegalArgumentException();
            if (includes != null)
                throw new IllegalStateException();
            this.excludes = flatten(excludes);
        }

        @Override
        protected boolean considerEdit(TextEdit edit)
        {
            if (excludes != null && excludes.contains(edit))
                return false;
            if (includes != null && !includes.contains(edit))
                return false;
            return true;
        }
    }

    private static class Preview
    {
        public final IDocument document;
        public final IRegion changeRegion;

        public Preview(IDocument document, IRegion changeRegion)
        {
            this.document = document;
            this.changeRegion = changeRegion;
        }
    }

    private static enum Edits
    {
        ENABLED,
        DISABLED,
        ALL;

        public Collection<TextEdit> of(TextEditBasedChangeGroup[] groups)
        {
            Set<TextEdit> result = new HashSet<TextEdit>();
            for (TextEditBasedChangeGroup group : groups)
            {
                if (this == Edits.ALL || (this == Edits.DISABLED
                    ^ group.isEnabled()))
                {
                    result.addAll(Arrays.asList(group.getTextEdits()));
                }
            }
            return result;
        }
    }
}
