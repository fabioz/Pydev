/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import java.util.ResourceBundle;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.ILineRange;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.LineRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PyStringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.autoedit.PyAutoIndentStrategy;
import org.python.pydev.shared_core.utils.DocCmd;

/**
 * Base class for actions that do a move action (Alt+Up or Alt+Down).
 * 
 * Subclasses just need to decide whether to go up or down.
 * 
 * @author Fabio
 */
public abstract class PyMoveLineAction extends TextEditorAction {

    protected PyEdit pyEdit;

    protected PyMoveLineAction(ResourceBundle bundle, String prefix, PyEdit editor) {
        super(bundle, prefix, editor);
        this.pyEdit = editor;
        update();
    }

    @Override
    public void runWithEvent(Event event) {
        run();
    }

    @Override
    public void run() {
        // get involved objects
        if (pyEdit == null) {
            return;
        }

        if (!validateEditorInputState()) {
            return;
        }

        ISourceViewer viewer = pyEdit.getEditorSourceViewer();
        if (viewer == null) {
            return;
        }

        IDocument document = viewer.getDocument();
        if (document == null) {
            return;
        }

        StyledText widget = viewer.getTextWidget();
        if (widget == null) {
            return;
        }

        // get selection
        ITextSelection sel = (ITextSelection) viewer.getSelectionProvider().getSelection();
        move(pyEdit, viewer, document, sel);
    }

    public void move(PyEdit pyEdit, ISourceViewer viewer, IDocument document, ITextSelection sel) {
        if (sel.isEmpty()) {
            return;
        }

        ITextSelection skippedLine = getSkippedLine(document, sel);
        if (skippedLine == null) {
            return;
        }

        ITextSelection movingArea;
        try {
            try {
                movingArea = getMovingSelection(document, sel);
            } catch (BadLocationException e) {
                return; //selection is out of range
            }

            // if either the skipped line or the moving lines are outside the widget's
            // visible area, bail out
            if (!containedByVisibleRegion(movingArea, viewer) || !containedByVisibleRegion(skippedLine, viewer)) {
                return;
            }

            PySelection skippedPs = new PySelection(document, skippedLine);

            // get the content to be moved around: the moving (selected) area and the skipped line
            String moving = movingArea.getText();
            String skipped = skippedLine.getText();
            if (moving == null || skipped == null || document.getLength() == 0) {
                return;
            }

            String delim;
            String insertion;
            int offset;
            int length;
            ILineRange selectionBefore = getLineRange(document, movingArea);
            IRewriteTarget target = null;
            if (pyEdit != null) {
                target = (IRewriteTarget) pyEdit.getAdapter(IRewriteTarget.class);
                if (target != null) {
                    target.beginCompoundChange();
                    if (!getMoveUp()) {
                        //When going up we'll just do a single document change, so, there's
                        //no need to set the redraw.
                        target.setRedraw(false);
                    }
                }
            }
            ILineRange selectionAfter;
            boolean isStringPartition;
            try {
                if (getMoveUp()) {
                    //check partition in the start of the skipped line
                    isStringPartition = ParsingUtils.isStringPartition(document, skippedLine.getOffset());
                    delim = document.getLineDelimiter(skippedLine.getEndLine());
                    Assert.isNotNull(delim);
                    offset = skippedLine.getOffset();
                    length = moving.length() + delim.length() + skipped.length();
                } else {
                    //check partition in the start of the line after the skipped line
                    int offsetToCheckPartition;
                    if (skippedLine.getEndLine() == document.getNumberOfLines() - 1) {
                        offsetToCheckPartition = document.getLength() - 1; //check the last document char 
                    } else {
                        offsetToCheckPartition = skippedLine.getOffset() + skippedLine.getLength(); //that's always the '\n' of the line
                    }

                    isStringPartition = ParsingUtils.isStringPartition(document, offsetToCheckPartition);

                    delim = document.getLineDelimiter(movingArea.getEndLine());
                    Assert.isNotNull(delim);
                    offset = movingArea.getOffset();

                    //When going down, we need to remove the movingArea to compute the new indentation
                    //properly (otherwise we'd use that text being moved on the compute algorithm)
                    document.replace(movingArea.getOffset(), movingArea.getLength() + delim.length(), "");
                    length = skipped.length();
                    int pos = skippedPs.getAbsoluteCursorOffset() - (movingArea.getLength() + delim.length());
                    skippedPs.setSelection(pos, pos);
                }

                PyAutoIndentStrategy indentStrategy = null;
                if (pyEdit != null) {
                    indentStrategy = pyEdit.getAutoEditStrategy();
                }
                if (indentStrategy == null) {
                    indentStrategy = new PyAutoIndentStrategy(new IAdaptable() {

                        @Override
                        public Object getAdapter(Class adapter) {
                            return null;
                        }
                    });
                }

                if (!isStringPartition) {
                    if (indentStrategy.getIndentPrefs().getSmartLineMove()) {
                        String prevExpectedIndent = calculateNewIndentationString(document, skippedPs, indentStrategy);
                        if (prevExpectedIndent != null) {
                            moving = PyStringUtils.removeWhitespaceColumnsToLeftAndApplyIndent(moving,
                                    prevExpectedIndent, false);
                        }
                    }
                }
                if (getMoveUp()) {
                    insertion = moving + delim + skipped;

                } else {
                    insertion = skipped + delim + moving;
                }

                // modify the document
                document.replace(offset, length, insertion);

                if (getMoveUp()) {
                    selectionAfter = new LineRange(selectionBefore.getStartLine() - 1,
                            selectionBefore.getNumberOfLines());
                } else {
                    selectionAfter = new LineRange(selectionBefore.getStartLine() + 1,
                            selectionBefore.getNumberOfLines());
                }
            } finally {
                if (target != null) {
                    target.endCompoundChange();
                    if (!getMoveUp()) {
                        target.setRedraw(true);
                    }
                }
            }

            // move the selection along
            IRegion region = getRegion(document, selectionAfter);
            selectAndReveal(viewer, region.getOffset(), region.getLength());

        } catch (BadLocationException e) {
            Log.log(e);
            return;
        }
    }

    /**
     * This method will return the indentation that should be applied for the moving text.
     */
    private String calculateNewIndentationString(IDocument document, PySelection skippedPs,
            PyAutoIndentStrategy indentStrategy) throws BadLocationException {
        int cursorLine = skippedPs.getCursorLine();
        int line = cursorLine;
        if (getMoveUp()) {
            if (cursorLine == 0) {
                String cursorLineContents = skippedPs.getCursorLineContents();
                int firstCharPosition = PySelection.getFirstCharPosition(cursorLineContents);
                return cursorLineContents.substring(0, firstCharPosition);
            }
            line = cursorLine - 1;
            if (line < 0) {
                return null;
            }
        }
        //Go to a non-empty line!
        String line2 = skippedPs.getLine(line);
        while (line > 0 && (line2.startsWith("#") || line2.trim().length() == 0)) {
            line--;
            line2 = skippedPs.getLine(line);
        }

        DocumentCommand command = new DocCmd(skippedPs.getEndLineOffset(line), 0, "\n");
        indentStrategy.customizeDocumentCommand(document, command);
        return command.text.substring(1);
    }

    private ILineRange getLineRange(IDocument document, ITextSelection selection) throws BadLocationException {
        final int offset = selection.getOffset();
        int startLine = document.getLineOfOffset(offset);
        int endOffset = offset + selection.getLength();
        int endLine = document.getLineOfOffset(endOffset);
        final int nLines = endLine - startLine + 1;
        return new LineRange(startLine, nLines);
    }

    /**
     * Performs similar to AbstractTextEditor.selectAndReveal, but does not update
     * the viewers highlight area.
     *
     * @param viewer the viewer that we want to select on
     * @param offset the offset of the selection
     * @param length the length of the selection
     */
    private void selectAndReveal(ITextViewer viewer, int offset, int length) {
        if (viewer == null) {
            return; // in tests
        }
        // invert selection to avoid jumping to the end of the selection in st.showSelection()
        viewer.setSelectedRange(offset + length, -length);
        //viewer.revealRange(offset, length); // will trigger jumping
        StyledText st = viewer.getTextWidget();
        if (st != null)
        {
            st.showSelection(); // only minimal scrolling
        }
    }

    private IRegion getRegion(IDocument document, ILineRange lineRange) throws BadLocationException {
        final int startLine = lineRange.getStartLine();
        int offset = document.getLineOffset(startLine);
        final int numberOfLines = lineRange.getNumberOfLines();
        if (numberOfLines < 1) {
            return new Region(offset, 0);
        }
        int endLine = startLine + numberOfLines - 1;
        int endOffset;
        boolean blockSelectionModeEnabled = false;
        try {
            blockSelectionModeEnabled = ((AbstractTextEditor) getTextEditor()).isBlockSelectionModeEnabled();
        } catch (Throwable e) {
            //Ignore (not available before 3.5)
        }
        if (blockSelectionModeEnabled) {
            // in block selection mode, don't select the last delimiter as we count an empty selected line
            IRegion endLineInfo = document.getLineInformation(endLine);
            endOffset = endLineInfo.getOffset() + endLineInfo.getLength();
        } else {
            endOffset = document.getLineOffset(endLine) + document.getLineLength(endLine);
        }
        return new Region(offset, endOffset - offset);
    }

    protected abstract boolean getMoveUp();

    /*
     * @see org.eclipse.ui.texteditor.IUpdate#update()
     */
    @Override
    public void update() {
        super.update();

        if (isEnabled()) {
            setEnabled(canModifyEditor());
        }

    }

    /**
     * Computes the region of the skipped line given the text block to be moved. If
     * <code>fUpwards</code> is <code>true</code>, the line above <code>selection</code>
     * is selected, otherwise the line below.
     *
     * @param document the document <code>selection</code> refers to
     * @param selection the selection on <code>document</code> that will be moved.
     * @return the region comprising the line that <code>selection</code> will be moved over, without its terminating delimiter.
     */
    private ITextSelection getSkippedLine(IDocument document, ITextSelection selection) {
        int skippedLineN = (getMoveUp() ? selection.getStartLine() - 1 : selection.getEndLine() + 1);
        if (skippedLineN > document.getNumberOfLines()
                || ((skippedLineN < 0 || skippedLineN == document.getNumberOfLines()))) {
            return null;
        }
        try {
            IRegion line = document.getLineInformation(skippedLineN);
            return new TextSelection(document, line.getOffset(), line.getLength());
        } catch (BadLocationException e) {
            // only happens on concurrent modifications
            return null;
        }
    }

    /**
     * Given a selection on a document, computes the lines fully or partially covered by
     * <code>selection</code>. A line in the document is considered covered if
     * <code>selection</code> comprises any characters on it, including the terminating delimiter.
     * <p>Note that the last line in a selection is not considered covered if the selection only
     * comprises the line delimiter at its beginning (that is considered part of the second last
     * line).
     * As a special case, if the selection is empty, a line is considered covered if the caret is
     * at any position in the line, including between the delimiter and the start of the line. The
     * line containing the delimiter is not considered covered in that case.
     * </p>
     *
     * @param document the document <code>selection</code> refers to
     * @param selection a selection on <code>document</code>
     * @return a selection describing the range of lines (partially) covered by
     * <code>selection</code>, without any terminating line delimiters
     * @throws BadLocationException if the selection is out of bounds (when the underlying document has changed during the call)
     */
    private ITextSelection getMovingSelection(IDocument document, ITextSelection selection) throws BadLocationException {
        int low = document.getLineOffset(selection.getStartLine());
        int endLine = selection.getEndLine();
        int high = document.getLineOffset(endLine) + document.getLineLength(endLine);

        // get everything up to last line without its delimiter
        String delim = document.getLineDelimiter(endLine);
        if (delim != null) {
            high -= delim.length();
        }

        return new TextSelection(document, low, high - low);
    }

    /**
     * Checks if <code>selection</code> is contained by the visible region of <code>viewer</code>.
     * As a special case, a selection is considered contained even if it extends over the visible
     * region, but the extension stays on a partially contained line and contains only white space.
     *
     * @param selection the selection to be checked
     * @param viewer the viewer displaying a visible region of <code>selection</code>'s document.
     * @return <code>true</code>, if <code>selection</code> is contained, <code>false</code> otherwise.
     */
    private boolean containedByVisibleRegion(ITextSelection selection, ISourceViewer viewer) {
        if (viewer == null) {
            return true; //in tests
        }
        int min = selection.getOffset();
        int max = min + selection.getLength();
        IDocument document = viewer.getDocument();

        IRegion visible;
        if (viewer instanceof ITextViewerExtension5) {
            visible = ((ITextViewerExtension5) viewer).getModelCoverage();
        } else {
            visible = viewer.getVisibleRegion();
        }

        int visOffset = visible.getOffset();
        try {
            if (visOffset > min) {
                if (document.getLineOfOffset(visOffset) != selection.getStartLine()) {
                    return false;
                }
                if (!isWhitespace(document.get(min, visOffset - min))) {
                    showStatus();
                    return false;
                }
            }
            int visEnd = visOffset + visible.getLength();
            if (visEnd < max) {
                if (document.getLineOfOffset(visEnd) != selection.getEndLine()) {
                    return false;
                }
                if (!isWhitespace(document.get(visEnd, max - visEnd))) {
                    showStatus();
                    return false;
                }
            }
            return true;
        } catch (BadLocationException e) {
        }
        return false;
    }

    /**
     * Checks for white space in a string.
     *
     * @param string the string to be checked or <code>null</code>
     * @return <code>true</code> if <code>string</code> contains only white space or is
     * <code>null</code>, <code>false</code> otherwise
     */
    private boolean isWhitespace(String string) {
        return string == null ? true : string.trim().length() == 0;
    }

    /**
     * Displays information in the status line why a line move is not possible
     */
    private void showStatus() {
        ITextEditor textEditor = getTextEditor();
        IEditorStatusLine status = (IEditorStatusLine) textEditor.getAdapter(IEditorStatusLine.class);
        if (status == null) {
            return;
        }
        status.setMessage(false,
                "Move not possible - Uncheck \"Show Source of Selected Element Only\" to see the entire document", null);
    }
}
