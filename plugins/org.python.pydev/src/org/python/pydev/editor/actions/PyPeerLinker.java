/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.editor.autoedit.PyAutoIndentStrategy;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.DocCmd;
import org.python.pydev.shared_ui.editor.ITextViewerExtensionAutoEditions;

/**
 * Something similar org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor.BracketInserter (but not too similar). 
 * 
 * @author Fabio Zadrozny
 */
public class PyPeerLinker {

    private IIndentPrefs prefs;

    public void setIndentPrefs(IIndentPrefs prefs) {
        this.prefs = prefs;
    }

    private int linkOffset;
    private int linkExitPos;
    private int linkLen;

    /**
     * Creates a handler that will properly treat backspaces considering python code.
     */
    public static VerifyKeyListener createVerifyKeyListener(final TextViewer viewer) {
        return new VerifyKeyListener() {

            private final PyPeerLinker pyPeerLinker = new PyPeerLinker();

            public void verifyKey(VerifyEvent event) {
                if (!event.doit) {
                    return;
                }
                switch (event.character) {
                    case '\'':
                    case '\"':
                    case '[':
                    case '{':
                    case '(':
                        break;
                    default:
                        return;
                }
                if (viewer != null && viewer.isEditable()) {
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

                            //Don't bother in getting the indent prefs from the editor: the default indent prefs are 
                            //always global for the settings we want.
                            pyPeerLinker.setIndentPrefs(new DefaultIndentPrefs(adaptable));
                            PySelection ps = new PySelection(viewer.getDocument(), (ITextSelection) selection);

                            if (pyPeerLinker.perform(ps, event.character, viewer)) {
                                event.doit = false;
                            }
                        }
                    }
                }
            }
        };
    }

    /**
     * @param ps
     */
    protected boolean perform(PySelection ps, final char c, TextViewer viewer) {
        linkOffset = -1;
        linkExitPos = -1;
        linkLen = 0;

        boolean literal = true;
        switch (c) {
            case '\'':
            case '\"':
                break;
            case '[':
            case '{':
            case '(':
                literal = false;
                break;
            default:
                return false;
        }

        if (literal) {
            if (!prefs.getAutoLiterals()) {
                return false;
            }
        } else {
            if (!prefs.getAutoParentesis()) {
                return false;
            }
        }

        try {
            IDocument doc = ps.getDoc();
            String contentType = ParsingUtils.getContentType(ps.getDoc(), ps.getAbsoluteCursorOffset());
            boolean isDefaultContext = contentType.equals(ParsingUtils.PY_DEFAULT);
            if (!isDefaultContext) {
                //not handled: leave it up to the auto-indent (if we're in link mode already it may delete the selected text and add a ', which is what we want).
                return false;
            }
            DocCmd docCmd = new DocCmd(ps.getAbsoluteCursorOffset(), ps.getSelLength(), "" + c);
            if (literal) {
                if (!handleLiteral(doc, docCmd, ps, isDefaultContext, prefs)) {
                    return false; //not handled
                }
            } else {
                if (!handleBrackets(ps, c, doc, docCmd, viewer)) {
                    return false; //not handled
                }
            }
            if (linkOffset == -1 || linkExitPos == -1) {
                return true; //it was handled (without the link)
            }

            if (prefs.getAutoLink()) {
                LinkedPositionGroup group = new LinkedPositionGroup();
                group.addPosition(new LinkedPosition(doc, linkOffset, linkLen, LinkedPositionGroup.NO_STOP));

                LinkedModeModel model = new LinkedModeModel();
                model.addGroup(group);
                model.forceInstall();

                if (viewer == null) {
                    return true; //don't actually do the link.
                }

                LinkedModeUI ui = new EditorLinkedModeUI(model, viewer);
                ui.setSimpleMode(true);
                IExitPolicy policy = new IExitPolicy() {

                    public ExitFlags doExit(LinkedModeModel model, VerifyEvent event, int offset, int length) {
                        //Yes, no special exit, if ' is entered again, let's do the needed treatment again instead of going 
                        //to the end (only <return> goes to the end).
                        //if (event.character == c) {
                        //    return new ExitFlags(ILinkedModeListener.UPDATE_CARET, false);
                        //}
                        return null;
                    }
                };
                ui.setExitPolicy(policy);
                ui.setExitPosition(viewer, linkExitPos, 0, Integer.MAX_VALUE);
                ui.setCyclingMode(LinkedModeUI.CYCLE_NEVER);
                ui.enter();
                IRegion newSelection = ui.getSelectedRegion();
                viewer.setSelectedRange(newSelection.getOffset(), newSelection.getLength());
            } else {
                viewer.setSelectedRange(linkOffset, linkLen);
            }

        } catch (Exception e) {
            Log.log(e);
        }
        return true;
    }

    private boolean handleBrackets(PySelection ps, final char c, IDocument doc, DocCmd docCmd, TextViewer viewer)
            throws BadLocationException {
        if (c == '(') {

            PyAutoIndentStrategy.handleParens(doc, docCmd, prefs);

            docCmd.doExecute(doc);

            //Note that this is done with knowledge on how the handleParens deals with the doc command (not meant as a
            //general thing to apply a doc command).
            if (docCmd.shiftsCaret) {
                //Regular stuff: just shift it and don't link
                if (viewer != null) {
                    viewer.setSelectedRange(docCmd.offset + docCmd.text.length(), 0);
                }
            } else {
                linkOffset = docCmd.caretOffset;
                linkLen = 0;
                linkExitPos = docCmd.offset + docCmd.text.length();
            }

        } else { //  [ or {
            char peer = StringUtils.getPeer(c);
            if (PyAutoIndentStrategy.shouldClose(ps, c, peer)) {
                int offset = ps.getAbsoluteCursorOffset();
                doc.replace(offset, ps.getSelLength(), StringUtils.getWithClosedPeer(c));
                linkOffset = offset + 1;
                linkLen = 0;
                linkExitPos = linkOffset + linkLen + 1;
            } else {
                //No link, just add the char and set the new selected range (if possible)
                docCmd.doExecute(doc);
                if (viewer != null) {
                    viewer.setSelectedRange(docCmd.offset + docCmd.text.length(), 0);
                }
            }
        }

        //Yes, in this situation, all cases are handled.
        return true;
    }

    /**
     * Called right after a ' or "
     * 
     * @return false if we should leave the handling to the auto-indent and true if it handled things properly here.
     */
    private boolean handleLiteral(IDocument document, DocumentCommand command, PySelection ps,
            boolean isDefaultContext, IIndentPrefs prefs) throws BadLocationException {
        int offset = ps.getAbsoluteCursorOffset();

        if (command.length > 0) {
            String selectedText = ps.getSelectedText();
            if (selectedText.indexOf('\r') != -1 || selectedText.indexOf('\n') != -1) {
                //we have a new line
                FastStringBuffer buf = new FastStringBuffer(selectedText.length() + 10);
                buf.appendN(command.text, 3);
                buf.append(selectedText);
                buf.appendN(command.text, 3);
                document.replace(offset, ps.getSelLength(), buf.toString());
                linkOffset = offset + 3;
                linkLen = selectedText.length();
                linkExitPos = linkOffset + linkLen + 3;
            } else {
                document.replace(offset, ps.getSelLength(), command.text + selectedText + command.text);
                linkOffset = offset + 1;
                linkLen = selectedText.length();
                linkExitPos = linkOffset + linkLen + 1;
            }
            return true;
        }
        char literalChar = command.text.charAt(0);

        try {
            char nextChar = ps.getCharAfterCurrentOffset();
            if (Character.isJavaIdentifierPart(nextChar)) {
                //we're just before a word (don't try to do anything in this case)
                //e.g. |var (| is cursor position)
                return false;
            }
        } catch (BadLocationException e) {
        }

        String cursorLineContents = ps.getCursorLineContents();
        if (cursorLineContents.indexOf(literalChar) == -1) {
            if (!isDefaultContext) {
                //only add additional chars if on default context. 
                return false;
            }
            document.replace(offset, ps.getSelLength(), command.text + command.text);
            linkOffset = offset + 1;
            linkLen = 0;
            linkExitPos = linkOffset + linkLen + 1;
            return true;
        }

        boolean balanced = isLiteralBalanced(cursorLineContents);

        Tuple<String, String> beforeAndAfterMatchingChars = ps.getBeforeAndAfterMatchingChars(literalChar);

        int matchesBefore = beforeAndAfterMatchingChars.o1.length();
        int matchesAfter = beforeAndAfterMatchingChars.o2.length();

        boolean hasMatchesBefore = matchesBefore != 0;
        boolean hasMatchesAfter = matchesAfter != 0;

        if (!hasMatchesBefore && !hasMatchesAfter) {
            //if it's not balanced, this char would be the closing char.
            if (balanced) {
                if (!isDefaultContext) {
                    //only add additional chars if on default context. 
                    return false;
                }
                document.replace(offset, ps.getSelLength(), command.text + command.text);
                linkOffset = offset + 1;
                linkLen = 0;
                linkExitPos = linkOffset + linkLen + 1;
                return true;
            }
        } else {
            //we're right after or before a " or '
            return false;
        }
        return false;
    }

    /**
     * @return true if the passed string has balanced ' and "
     */
    private boolean isLiteralBalanced(String cursorLineContents) {
        ParsingUtils parsingUtils = ParsingUtils.create(cursorLineContents, true);

        int offset = 0;
        int end = cursorLineContents.length();
        boolean balanced = true;
        while (offset < end) {
            char curr = cursorLineContents.charAt(offset++);
            if (curr == '"' || curr == '\'') {
                int eaten;
                try {
                    eaten = parsingUtils.eatLiterals(null, offset - 1) + 1;
                } catch (SyntaxErrorException e) {
                    balanced = false;
                    break;
                }
                if (eaten > offset) {
                    offset = eaten;
                }
            }
        }
        return balanced;
    }

    /**
     * In default namespace (used for testing)
     */

    int getLinkLen() {
        return linkLen;
    }

    int getLinkExitPos() {
        return linkExitPos;
    }

    int getLinkOffset() {
        return linkOffset;
    }

}