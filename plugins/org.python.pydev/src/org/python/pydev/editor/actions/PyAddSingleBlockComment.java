/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 01/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.commentblocks.CommentBlocksPreferences;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class PyAddSingleBlockComment extends AbstractBlockCommentAction {

    public PyAddSingleBlockComment() {
        //default
    }

    /**
     * For tests: assigns the default values
     */
    PyAddSingleBlockComment(int defaultCols, boolean alignLeft) {
        super(defaultCols, alignLeft);
    }

    /**
     * Performs the action with a given PySelection
     * 
     * @param ps Given PySelection
     * @return boolean The success or failure of the action
     */
    @Override
    public Tuple<Integer, Integer> perform(PySelection ps) {
        // What we'll be replacing the selected text with
        FastStringBuffer strbuf = new FastStringBuffer();

        // If they selected a partial line, count it as a full one
        ps.selectCompleteLine();

        int i;
        try {
            // For each line, comment them out
            for (i = ps.getStartLineIndex(); i <= ps.getEndLineIndex(); i++) {
                String line = StringUtils.rightTrim(ps.getLine(i));
                if (getAlignRight()) {
                    strbuf.append(getRightAlignedFullCommentLine(line));
                    strbuf.append(line.trim());
                    if (i != ps.getEndLineIndex()) {
                        strbuf.append(ps.getEndLineDelim());
                    }
                } else {
                    Tuple<Integer, Character> colsAndChar = getColsAndChar();
                    int cols = colsAndChar.o1;
                    char c = colsAndChar.o2;

                    FastStringBuffer buffer = makeBufferToIndent(line, cols);
                    int lenOfStrWithTabsAsSpaces = getLenOfStrConsideringTabEditorLen(buffer.toString());
                    int diff = lenOfStrWithTabsAsSpaces - buffer.length();

                    buffer.append("# ");
                    buffer.append(line.trim());
                    buffer.append(' ');
                    while (buffer.length() + diff < cols) {
                        buffer.append(c);
                    }
                    strbuf.append(buffer);
                    if (i != ps.getEndLineIndex()) {
                        strbuf.append(ps.getEndLineDelim());
                    }
                }
            }

            int startOffset = ps.getStartLine().getOffset();
            String str = strbuf.toString();
            // Replace the text with the modified information
            ps.getDoc().replace(startOffset, ps.getSelLength(), str);
            return new Tuple<Integer, Integer>(startOffset + str.length(), 0);
        } catch (Exception e) {
            beep(e);
        }

        // In event of problems, return null
        return null;
    }

    private boolean getAlignRight() {
        if (SharedCorePlugin.inTestMode()) {
            return this.alignRight;
        }

        PydevPlugin plugin = PydevPlugin.getDefault();
        return plugin.getPluginPreferences().getBoolean(CommentBlocksPreferences.SINGLE_BLOCK_COMMENT_ALIGN_RIGHT);
    }

    @Override
    protected String getPreferencesNameForChar() {
        return CommentBlocksPreferences.SINGLE_BLOCK_COMMENT_CHAR;
    }

    /**
     * Currently returns a string with the comment block.
     * 
     * @param line
     * 
     * @return Comment line string, or a default one if Preferences are null
     */
    protected String getRightAlignedFullCommentLine(String line) {
        Tuple<Integer, Character> colsAndChar = getColsAndChar();
        int cols = colsAndChar.o1;
        char c = colsAndChar.o2;

        FastStringBuffer buffer = makeBufferToIndent(line, cols);
        int lenOfStrWithTabsAsSpaces = getLenOfStrConsideringTabEditorLen(buffer.toString());
        int diff = lenOfStrWithTabsAsSpaces - buffer.length();

        buffer.append("#");
        for (int i = 0; i + line.length() + diff < cols - 2; i++) {
            buffer.append(c);
        }
        buffer.append(" ");
        return buffer.toString();
    }

    private FastStringBuffer makeBufferToIndent(String line, int cols) {
        FastStringBuffer buffer = new FastStringBuffer(cols);
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '\t' || ch == ' ') {
                buffer.append(ch);
            } else {
                break;
            }
        }
        return buffer;
    }

}
