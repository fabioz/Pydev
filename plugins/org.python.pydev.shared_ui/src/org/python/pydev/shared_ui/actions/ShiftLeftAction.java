/******************************************************************************
* Copyright (C) 2009-2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_ui.actions;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;

public class ShiftLeftAction {

    /**
     * Performs the action with a given PySelection
     * 
     * @param ps Given PySelection
     * @param indentPrefs 
     * @return the new selection
     * @throws BadLocationException 
     */
    public static void perform(TextSelectionUtils ps, int tabWidth) throws BadLocationException {
        int endLineIndex = ps.getEndLineIndex();
        int startLineIndex = ps.getStartLineIndex();

        // If they selected a partial line, count it as a full one
        ps.selectCompleteLine();

        String selectedText = ps.getSelectedText();
        List<String> ret = StringUtils.splitInLines(selectedText);

        int tabWidthToUse = tabWidth;

        //Calculate the tab width we should use
        for (String line : ret) {
            String lineIndent = TextSelectionUtils.getIndentationFromLine(line);

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
