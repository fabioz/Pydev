/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author: fabioz
 * Created: January 2004
 */

package org.python.pydev.editor.actions;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.actions.BaseAction;

/**
 * @author Fabio Zadrozny
 * 
 * Superclass of all our actions. Contains utility functions.
 * 
 * Subclasses should implement run(IAction action) method.
 */
public abstract class PyAction extends BaseAction implements IEditorActionDelegate {

    protected PyAction() {
        super();
    }

    protected PyAction(String text, int style) {
        super(text, style);
    }

    public static String getDelimiter(IDocument doc) {
        return PySelection.getDelimiter(doc);
    }

    /**
     * @return python editor.
     */
    protected PyEdit getPyEdit() {
        if (targetEditor instanceof PyEdit) {
            return (PyEdit) targetEditor;
        } else {
            throw new RuntimeException("Expecting PyEdit editor. Found:" + targetEditor.getClass().getName());
        }
    }

    /**
     * @return true if the contents of the editor may be changed. Clients MUST call this before actually
     * modifying the editor.
     */
    protected boolean canModifyEditor() {
        ITextEditor editor = getTextEditor();
        return BaseAction.canModifyEditor(editor);
    }

    /**
     * Are we in the first char of the line with the offset passed?
     * @param doc
     * @param cursorOffset
     */
    protected void isInFirstVisibleChar(IDocument doc, int cursorOffset) {
        try {
            IRegion region = doc.getLineInformationOfOffset(cursorOffset);
            int offset = region.getOffset();
            String src = doc.get(offset, region.getLength());
            if ("".equals(src)) {
                return;
            }
            int i = 0;
            while (i < src.length()) {
                if (!Character.isWhitespace(src.charAt(i))) {
                    break;
                }
                i++;
            }
            setCaretPosition(offset + i - 1);
        } catch (BadLocationException e) {
            beep(e);
            return;
        }
    }

    /**
     * Returns the position of the last non whitespace char in the current line.
     * @param doc
     * @param cursorOffset
     * @return position of the last character of the line (returned as an absolute
     *            offset)
     * 
     * @throws BadLocationException
     */
    protected int getLastCharPosition(IDocument doc, int cursorOffset) throws BadLocationException {
        IRegion region;
        region = doc.getLineInformationOfOffset(cursorOffset);
        int offset = region.getOffset();
        String src = doc.get(offset, region.getLength());

        int i = src.length();
        boolean breaked = false;
        while (i > 0) {
            i--;
            //we have to break if we find a character that is not a whitespace or a tab.
            if (Character.isWhitespace(src.charAt(i)) == false && src.charAt(i) != '\t') {
                breaked = true;
                break;
            }
        }
        if (!breaked) {
            i--;
        }
        return (offset + i);
    }

    /**
     * Goes to first char of the line.
     * @param doc
     * @param cursorOffset
     */
    protected void gotoFirstChar(IDocument doc, int cursorOffset) {
        try {
            IRegion region = doc.getLineInformationOfOffset(cursorOffset);
            int offset = region.getOffset();
            setCaretPosition(offset);
        } catch (BadLocationException e) {
            beep(e);
        }
    }

    /**
     * Goes to the first visible char.
     * @param doc
     * @param cursorOffset
     */
    protected void gotoFirstVisibleChar(IDocument doc, int cursorOffset) {
        try {
            setCaretPosition(PySelection.getFirstCharPosition(doc, cursorOffset));
        } catch (BadLocationException e) {
            beep(e);
        }
    }

    /**
     * Goes to the first visible char.
     * @param doc
     * @param cursorOffset
     */
    protected boolean isAtFirstVisibleChar(IDocument doc, int cursorOffset) {
        try {
            return PySelection.getFirstCharPosition(doc, cursorOffset) == cursorOffset;
        } catch (BadLocationException e) {
            return false;
        }
    }

    //================================================================
    // HELPER FOR DEBBUGING... 
    //================================================================

    /*
     * Beep...humm... yeah....beep....ehehheheh
     */
    protected static void beep(Exception e) {
        try {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay().beep();
        } catch (Throwable x) {
            //ignore, workbench has still not been created
        }
        Log.log(e);
    }

    /**
     * 
     */
    public static String getLineWithoutComments(String sel) {
        return sel.replaceAll("#.*", "");
    }

    /**
     * 
     */
    public static String getLineWithoutComments(PySelection ps) {
        return getLineWithoutComments(ps.getCursorLineContents());
    }

    public static String lowerChar(String s, int pos) {
        char[] ds = s.toCharArray();
        ds[pos] = Character.toLowerCase(ds[pos]);
        return new String(ds);
    }

    /**
     * @param string
     * @param j
     * @return
     */
    public static boolean stillInTok(String string, int j) {
        char c = string.charAt(j);

        return c != '\n' && c != '\r' && c != ' ' && c != '.' && c != '(' && c != ')' && c != ',' && c != ']'
                && c != '[' && c != '#' && c != '\'' && c != '"';
    }

    /**
     * @param ps the selection that contains the document
     */
    protected void revealSelEndLine(PySelection ps) {
        // Put cursor at the first area of the selection
        int docLen = ps.getDoc().getLength() - 1;
        IRegion endLine = ps.getEndLine();
        if (endLine != null) {
            int curOffset = endLine.getOffset();
            getTextEditor().selectAndReveal(curOffset < docLen ? curOffset : docLen, 0);
        }
    }

    /**
     * @return a set with the currently opened files in the PyEdit editors.
     */
    public static Set<IFile> getOpenFiles() {
        Set<IFile> ret = new HashSet<IFile>();
        IWorkbenchWindow activeWorkbenchWindow = EditorUtils.getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return ret;
        }

        IWorkbenchPage[] pages = activeWorkbenchWindow.getPages();
        for (int i = 0; i < pages.length; i++) {
            IEditorReference[] editorReferences = pages[i].getEditorReferences();

            for (int j = 0; j < editorReferences.length; j++) {
                IEditorReference iEditorReference = editorReferences[j];
                if (!PyEdit.EDITOR_ID.equals(iEditorReference.getId())) {
                    continue; //Only PyDev editors...
                }
                try {
                    IEditorInput editorInput = iEditorReference.getEditorInput();
                    if (editorInput == null) {
                        continue;
                    }
                    IFile file = (IFile) editorInput.getAdapter(IFile.class);
                    if (file != null) {
                        ret.add(file);
                    }
                } catch (Exception e1) {
                    Log.log(e1);
                }
            }
        }
        return ret;
    }

}
