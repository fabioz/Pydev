/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.PyEdit;

/**
 * This action was created so that we can make the shift left even if there are less characters in the line than
 * the expected indent (the default shift left won't do the dedent in that case).
 */
public class PyShiftLeft extends PyAction {

    /**
     * Grabs the selection information and performs the action.
     * 
     * Note that setting the rewrite session and undo/redo must be done from the caller.
     */
    public void run(IAction action) {
        try {
            if (!canModifyEditor()) {
                return;
            }

            PyEdit pyEdit = (PyEdit) getTextEditor();
            IIndentPrefs indentPrefs = pyEdit.getIndentPrefs();
            PySelection ps = new PySelection(pyEdit);
            perform(ps, indentPrefs);
        } catch (Exception e) {
            beep(e);
        }
    }

    /**
     * Performs the action with a given PySelection
     * 
     * @param ps Given PySelection
     * @param indentPrefs 
     * @return the new selection
     * @throws BadLocationException 
     */
    public void perform(PySelection ps, IIndentPrefs indentPrefs) throws BadLocationException {
        int endLineIndex = ps.getEndLineIndex();
        int startLineIndex = ps.getStartLineIndex();

        // If they selected a partial line, count it as a full one
        ps.selectCompleteLine();

        String selectedText = ps.getSelectedText();
        List<String> ret = StringUtils.splitInLines(selectedText);

        int tabWidth = indentPrefs.getTabWidth();
        int tabWidthToUse = tabWidth;

        //Calculate the tab width we should use
        for (String line : ret) {
            String lineIndent = PySelection.getIndentationFromLine(line);

            if (lineIndent.length() > 0) {
                if (lineIndent.startsWith("\t")) {
                    //Tab will be treated by removing the whole tab, so, just go on
                } else {
                    //String with spaces... let's see if we have less spaces than we have the tab width
                    int spaces = 0;
                    for (int i = 0; i < lineIndent.length(); i++) {
                        char c = lineIndent.charAt(i);
                        if (c == ' ') {
                            spaces += 1;
                        } else {
                            break; //ok, found all spaces available
                        }
                    }
                    if (spaces > 0) {
                        tabWidthToUse = Math.min(spaces, tabWidthToUse);
                    }
                }
            }
        }

        String defaultIndentStr = StringUtils.createSpaceString(tabWidthToUse);

        //Note that we remove the contents line by line just erasing the needed chars
        //(if we did a full replacement in the end, the cursor wouldn't end in the correct position
        //and trying to set the cursor later also changed the editor scroll position).
        IDocument doc = ps.getDoc();
        for (int i = startLineIndex; i <= endLineIndex; i++) {
            String line = ps.getLine(i);
            if (line.startsWith("\t")) {
                doc.replace(ps.getLineOffset(i), 1, "");

            } else if (line.startsWith(defaultIndentStr)) {
                doc.replace(ps.getLineOffset(i), defaultIndentStr.length(), "");
            }
        }
    }
}
