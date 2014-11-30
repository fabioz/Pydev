/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author: Fabio Zadrozny
 * Created: July 2004
 */
package org.python.pydev.editor.actions;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PythonPairMatcher;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.editor.autoedit.PyAutoIndentStrategy;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.editor.ITextViewerExtensionAutoEditions;

/**
 * @author Fabio Zadrozny
 * 
 * Makes a backspace happen...
 * 
 * We can:
 * - go to the indentation from some uncommented previous line (if we
 *   only have whitespaces in the current line).
 * - erase all whitespace characters until we find some character.
 * - erase a single character.
 */
public class PyBackspace extends PyAction {

    private IIndentPrefs prefs;
    private int dontEraseMoreThan = -1;

    public void setIndentPrefs(IIndentPrefs prefs) {
        this.prefs = prefs;
    }

    public IIndentPrefs getIndentPrefs() {
        if (this.prefs == null) {
            this.prefs = getPyEdit().getIndentPrefs();
        }
        return this.prefs;
    }

    public void perform(PySelection ps) {
        // Perform the action
        try {
            ITextSelection textSelection = ps.getTextSelection();

            if (textSelection.getLength() != 0) {
                eraseSelection(ps);
                return;
            }

            int lastCharPosition = getLastCharPosition(ps.getDoc(), ps.getLineOffset());

            int cursorOffset = textSelection.getOffset();

            IRegion lastCharRegion = ps.getDoc().getLineInformationOfOffset(lastCharPosition + 1);
            //System.out.println("cursorOffset: "+ cursorOffset);
            //System.out.println("lastCharPosition: "+lastCharPosition);
            //System.out.println("lastCharRegion.getOffset(): "
            // +lastCharRegion.getOffset());
            //          IRegion cursorRegion =
            // ps.doc.getLineInformationOfOffset(cursorOffset);

            if (cursorOffset == lastCharRegion.getOffset()) {
                //System.out.println("We are in the beginning of the line.");
                //in this situation, we are in the first character of the
                // line...
                //so, we have to get the end of the other line and delete it.
                if (cursorOffset != 0) {
                    // the first line.
                    eraseLineDelimiter(ps);
                }
            } else if (cursorOffset <= lastCharPosition) {
                //System.out.println("cursorOffset <= lastCharPosition");
                //this situation is:
                //    |a (delete to previous indentation - considers cursor
                // position)
                //or
                //    as | as (delete single char)
                //or
                //  | a (delete to previous indentation - considers cursor
                // position)
                //so, we have to treat it carefully
                eraseToPreviousIndentation(ps, false, lastCharRegion);
            } else if (lastCharRegion.getOffset() == lastCharPosition + 1) {
                //System.out.println("Only whitespaces in the line.");
                //in this situation, this line only has whitespaces,
                //so, we have to erase depending on the previous indentation.
                eraseToPreviousIndentation(ps, true, lastCharRegion);
            } else {

                if (cursorOffset - lastCharPosition == 1) {
                    //System.out.println("Erase single char.");
                    //last char and cursor are in the same line.
                    //this situation is:
                    //    a|
                    eraseSingleChar(ps);

                } else if (cursorOffset - lastCharPosition > 1) {
                    //this situation is:
                    //    a |
                    //System.out.println("Erase until last char is found.");
                    eraseUntilLastChar(ps, lastCharPosition);
                }
            }
        } catch (Exception e) {
            beep(e);
        }
    }

    /**
     * Makes a backspace happen...
     * 
     * We can:
     * - go to the indentation from some uncommented previous line (if
     *   we only have whitespaces in the current line).
     *  - erase all whitespace characters until we find some character.
     *  - erase a single character.
     */
    @Override
    public void run(IAction action) {
        OfflineActionTarget adapter = (OfflineActionTarget) getPyEdit().getAdapter(OfflineActionTarget.class);
        if (adapter != null) {
            if (adapter.isInstalled()) {
                adapter.removeLastCharSearchAndUpdateStatus();
                return;
            }
        }
        if (!canModifyEditor()) {
            return;
        }

        PySelection ps = new PySelection(getTextEditor());
        perform(ps);
    }

    /**
     * @param ps
     * @param hasOnlyWhitespaces
     * @param lastCharRegion 
     * @throws BadLocationException
     */
    private void eraseToPreviousIndentation(PySelection ps, boolean hasOnlyWhitespaces, IRegion lastCharRegion)
            throws BadLocationException {
        String lineContentsToCursor = ps.getLineContentsToCursor();
        if (hasOnlyWhitespaces) {
            //System.out.println("only whitespaces");
            eraseToIndentation(ps, lineContentsToCursor);
        } else {
            //System.out.println("not only whitespaces");
            //this situation is:
            //    |a (delete to previous indentation - considers cursor position)
            //
            //or
            //
            //    as | as (delete single char)
            //
            //so, we have to treat it carefully
            //TODO: use the conditions above and not just erase a single
            // char.

            if (PySelection.containsOnlyWhitespaces(lineContentsToCursor)) {
                eraseToIndentation(ps, lineContentsToCursor);

            } else {
                eraseSingleChar(ps);
            }
        }
    }

    /**
     * 
     * @param ps
     * @throws BadLocationException
     */
    private void eraseSingleChar(PySelection ps) throws BadLocationException {
        ITextSelection textSelection = ps.getTextSelection();

        int replaceLength = 1;
        int replaceOffset = textSelection.getOffset() - replaceLength;
        IDocument doc = ps.getDoc();

        if (replaceOffset >= 0 && replaceOffset + replaceLength < doc.getLength()) {
            char c = doc.getChar(replaceOffset);
            if (c == '(' || c == '[' || c == '{') {
                //When removing a (, check if we have to delete the corresponding ) too.
                char peer = StringUtils.getPeer(c);
                if (replaceOffset + replaceLength < doc.getLength()) {
                    char c2 = doc.getChar(replaceOffset + 1);
                    if (c2 == peer) {
                        //Ok, there's a closing one right next to it, now, what we have to do is
                        //check if the user was actually removing that one because there's an opening
                        //one without a match.
                        //To do that, we go backwards in the document searching for an opening match and then
                        //search its match. If it's found, it means we can delete both, otherwise, this
                        //delete will make things correct.

                        //Create a matcher only matching this char
                        PythonPairMatcher pythonPairMatcher = new PythonPairMatcher(new char[] { c, peer });
                        int openingPeerOffset = pythonPairMatcher.searchForAnyOpeningPeer(replaceOffset, doc);
                        if (openingPeerOffset == -1) {
                            replaceLength += 1;
                        } else {
                            int closingPeerOffset = pythonPairMatcher.searchForClosingPeer(openingPeerOffset, c, peer,
                                    doc);
                            if (closingPeerOffset != -1) {
                                //we have a match, so, things are balanced and we can delete the next
                                replaceLength += 1;
                            }
                        }
                    }
                }

            } else if (c == '\'' || c == '"') {
                //when removing a ' or ", check if we have to delete another ' or " too.
                Tuple<String, String> beforeAndAfterMatchingChars = ps.getBeforeAndAfterMatchingChars(c);
                int matchesBefore = beforeAndAfterMatchingChars.o1.length();
                int matchesAfter = beforeAndAfterMatchingChars.o2.length();
                if (matchesBefore == 1 && matchesBefore == matchesAfter) {
                    replaceLength += 1;
                }
            }
        }

        makeDelete(doc, replaceOffset, replaceLength);
    }

    /**
     * 
     * @param ps
     * @throws BadLocationException
     */
    private void eraseLineDelimiter(PySelection ps) throws BadLocationException {

        ITextSelection textSelection = ps.getTextSelection();

        int length = getDelimiter(ps.getDoc()).length();
        int offset = textSelection.getOffset() - length;

        //System.out.println("Replacing offset: "+(offset) +" lenght: "+
        // (length));
        makeDelete(ps.getDoc(), offset, length);
    }

    /**
     * 
     * @param ps
     * @throws BadLocationException
     */
    private void eraseSelection(PySelection ps) throws BadLocationException {
        ITextSelection textSelection = ps.getTextSelection();

        makeDelete(ps.getDoc(), textSelection.getOffset(), textSelection.getLength());
    }

    /**
     * @param ps
     * @param lastCharPosition
     * @throws BadLocationException
     */
    private void eraseUntilLastChar(PySelection ps, int lastCharPosition) throws BadLocationException {
        ITextSelection textSelection = ps.getTextSelection();
        int cursorOffset = textSelection.getOffset();

        int offset = lastCharPosition + 1;
        int length = cursorOffset - lastCharPosition - 1;
        //System.out.println("Replacing offset: "+(offset) +" lenght: "+
        // (length));
        makeDelete(ps.getDoc(), offset, length);
    }

    /**
     * TODO: Make use of the indentation gotten previously. This implementation
     * just uses the indentation string and erases the number of chars from it.
     * 
     * @param ps
     * @param indentation this is in number of characters.
     * @throws BadLocationException
     */
    private void eraseToIndentation(PySelection ps, String lineContentsToCursor) throws BadLocationException {
        final int cursorOffset = ps.getAbsoluteCursorOffset();
        final int cursorLine = ps.getCursorLine();
        final int lineContentsToCursorLen = lineContentsToCursor.length();

        if (lineContentsToCursorLen > 0) {
            char c = lineContentsToCursor.charAt(lineContentsToCursorLen - 1);
            if (c == '\t') {
                eraseSingleChar(ps);
                return;
            }
        }

        String indentationString = getIndentPrefs().getIndentationString();

        int replaceLength;
        int replaceOffset;

        final int indentationLength = indentationString.length();
        final int modLen = lineContentsToCursorLen % indentationLength;

        if (modLen == 0) {
            replaceOffset = cursorOffset - indentationLength;
            replaceLength = indentationLength;
        } else {
            replaceOffset = cursorOffset - modLen;
            replaceLength = modLen;
        }

        IDocument doc = ps.getDoc();
        if (cursorLine > 0) {
            IRegion prevLineInfo = doc.getLineInformation(cursorLine - 1);
            int prevLineEndOffset = prevLineInfo.getOffset() + prevLineInfo.getLength();
            Tuple<Integer, Boolean> tup = PyAutoIndentStrategy.determineSmartIndent(prevLineEndOffset, doc, prefs);
            Integer previousContextSmartIndent = tup.o1;
            if (previousContextSmartIndent > 0 && lineContentsToCursorLen > previousContextSmartIndent) {
                int initialLineOffset = cursorOffset - lineContentsToCursorLen;
                if (replaceOffset < initialLineOffset + previousContextSmartIndent) {
                    int newReplaceOffset = initialLineOffset + previousContextSmartIndent + 1;
                    if (newReplaceOffset != cursorOffset) {
                        replaceOffset = newReplaceOffset;
                        replaceLength = cursorOffset - replaceOffset;
                    }
                }
            }
        }

        //now, check what we're actually removing here... we can only remove chars if they are the
        //same, so, if we have a replace for '\t ', we should only remove the ' ', and not the '\t'
        if (replaceLength > 1) {
            String strToReplace = doc.get(replaceOffset, replaceLength);
            char prev = 0;
            for (int i = strToReplace.length() - 1; i >= 0; i--) {
                char c = strToReplace.charAt(i);
                if (prev != 0) {
                    if (c != prev) {
                        replaceOffset += (i + 1);
                        replaceLength -= (i + 1);
                        break;
                    }
                }
                prev = c;
            }
        }

        makeDelete(doc, replaceOffset, replaceLength);
    }

    private void makeDelete(IDocument doc, int replaceOffset, int replaceLength) throws BadLocationException {
        if (replaceOffset < dontEraseMoreThan) {
            int delta = dontEraseMoreThan - replaceOffset;
            replaceOffset = dontEraseMoreThan;
            replaceLength -= delta;
            if (replaceLength <= 0) {
                return;
            }
        }
        doc.replace(replaceOffset, replaceLength, "");
    }

    public void setDontEraseMoreThan(int offset) {
        this.dontEraseMoreThan = offset;
    }

    /**
     * Creates a handler that will properly treat backspaces considering python code.
     */
    public static VerifyKeyListener createVerifyKeyListener(final TextViewer viewer, final PyEdit edit) {
        return new VerifyKeyListener() {

            public void verifyKey(VerifyEvent event) {
                if ((event.doit && event.character == SWT.BS && event.stateMask == 0 && viewer != null && viewer
                        .isEditable())) { //isBackspace
                    boolean blockSelection = false;
                    try {
                        blockSelection = viewer.getTextWidget().getBlockSelection();
                    } catch (Throwable e) {
                        //that's OK (only available in eclipse 3.5)
                    }
                    if (!blockSelection) {
                        if (viewer instanceof ITextViewerExtensionAutoEditions) {
                            ITextViewerExtensionAutoEditions autoEditions = (ITextViewerExtensionAutoEditions) viewer;
                            if (!autoEditions.getAutoEditionsEnabled()) {
                                return;
                            }
                        }

                        ISelection selection = viewer.getSelection();
                        if (selection instanceof ITextSelection) {
                            //Only do our custom backspace if we're not in block selection mode.
                            PyBackspace pyBackspace = new PyBackspace();
                            if (edit != null) {
                                pyBackspace.setEditor(edit);
                            } else {
                                IAdaptable adaptable;
                                if (viewer instanceof IAdaptable) {
                                    adaptable = (IAdaptable) viewer;
                                } else {
                                    adaptable = new IAdaptable() {

                                        @Override
                                        public Object getAdapter(Class adapter) {
                                            return null;
                                        }
                                    };
                                }
                                pyBackspace.setIndentPrefs(new DefaultIndentPrefs(adaptable));
                            }
                            PySelection ps = new PySelection(viewer.getDocument(), (ITextSelection) selection);
                            pyBackspace.perform(ps);
                            event.doit = false;
                        }
                    }
                }
            }
        };
    }

}