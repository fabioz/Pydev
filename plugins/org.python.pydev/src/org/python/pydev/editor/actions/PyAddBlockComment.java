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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Preferences;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.actions.PyFormatStd.FormatStd;
import org.python.pydev.editor.commentblocks.CommentBlocksPreferences;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * Creates a comment block.  Comment blocks are slightly different than regular comments 
 * created in that they provide a distinguishing element at the beginning and end as a 
 * separator.  In this case, it is a string of <code>=======</code> symbols to strongly
 * differentiate this comment block.
 * 
 * @author Fabio Zadrozny
 * @author Parhaum Toofanian
 */
public class PyAddBlockComment extends AbstractBlockCommentAction {

    private boolean defaultClassNameBehaviour;
    private boolean defaultFunctionNameBehaviour;
    private FormatStd std;

    public PyAddBlockComment(FormatStd std) {
        //default
        this.std = std;
    }

    public PyAddBlockComment() {
        this(null);
    }

    /**
     * For tests: assigns the default values
     */
    PyAddBlockComment(FormatStd std, int defaultCols, boolean alignLeft, boolean classNameBehaviour,
            boolean functionNameBehaviour) {
        super(defaultCols, alignLeft);
        this.std = std;
        this.defaultClassNameBehaviour = classNameBehaviour;
        this.defaultFunctionNameBehaviour = functionNameBehaviour;
    }

    @Override
    protected void revealSelEndLine(PySelection ps) {
        getTextEditor().selectAndReveal(ps.getEndLine().getOffset(), 0);
    }

    protected boolean getUseClassNameBehaviour() {
        if (SharedCorePlugin.inTestMode()) {
            return defaultClassNameBehaviour;
        }

        Preferences prefs = PydevPlugin.getDefault().getPluginPreferences();
        return prefs.getBoolean(CommentBlocksPreferences.MULTI_BLOCK_COMMENT_SHOW_ONLY_CLASS_NAME);
    }

    protected boolean getUseFunctionNameBehaviour() {
        if (SharedCorePlugin.inTestMode()) {
            return defaultFunctionNameBehaviour;
        }

        Preferences prefs = PydevPlugin.getDefault().getPluginPreferences();
        return prefs.getBoolean(CommentBlocksPreferences.MULTI_BLOCK_COMMENT_SHOW_ONLY_FUNCTION_NAME);
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
        FastStringBuffer tempBuffer = new FastStringBuffer();

        // If they selected a partial line, count it as a full one
        ps.selectCompleteLine();

        try {
            String fullCommentLine;
            String endLineDelim = ps.getEndLineDelim();

            int startLineIndex = ps.getStartLineIndex();
            int endLineIndex = ps.getEndLineIndex();

            boolean classBehaviour = false;
            if (startLineIndex == endLineIndex && getUseClassNameBehaviour()) {
                if (ps.isInClassLine()) {
                    //just get the class name
                    classBehaviour = true;
                }
            }

            boolean functionBehaviour = false;
            if (startLineIndex == endLineIndex && getUseFunctionNameBehaviour()) {
                if (ps.isInFunctionLine(false)) {
                    //just get the class name
                    functionBehaviour = true;
                }
            }

            // Start of block

            if (classBehaviour || functionBehaviour) {
                String line = ps.getLine(startLineIndex);

                int classIndex;
                int tokLen;

                if (classBehaviour) {
                    classIndex = line.indexOf("class ");
                    tokLen = 6;
                } else {
                    classIndex = line.indexOf("def ");
                    tokLen = 4;
                }

                fullCommentLine = getFullCommentLine(classIndex, tempBuffer);
                String spacesBefore;
                if (classIndex > 0) {
                    spacesBefore = line.substring(0, classIndex);
                } else {
                    spacesBefore = "";
                }

                strbuf.append(spacesBefore + "#").append(fullCommentLine).append(endLineDelim);
                String initialLine = line;
                line = line.substring(classIndex + tokLen);
                FastStringBuffer className = new FastStringBuffer();
                for (int i = 0; i < line.length(); i++) {
                    char cN = line.charAt(i);
                    if (Character.isJavaIdentifierPart(cN)) {
                        className.append(cN);
                    } else {
                        break;
                    }
                }

                strbuf.append(spacesBefore);
                strbuf.append("# ");
                strbuf.append(className);
                strbuf.append(endLineDelim);

                strbuf.append(spacesBefore);
                strbuf.append("#").append(fullCommentLine);
                strbuf.append(endLineDelim);
                strbuf.append(initialLine);

            } else {
                List<String> lines = new ArrayList<String>();

                int minCharsBefore = Integer.MAX_VALUE;
                for (int i = startLineIndex; i <= endLineIndex; i++) {
                    String line = ps.getLine(i);
                    minCharsBefore = Math.min(minCharsBefore, PySelection.getFirstCharPosition(line));
                    lines.add(line);
                }

                String firstLine = lines.get(0);
                String lastLine = lines.get(lines.size() - 1);

                String strBefore = firstLine.substring(0, minCharsBefore);
                fullCommentLine = getFullCommentLine(getLenOfStrConsideringTabEditorLen(strBefore), tempBuffer.clear());
                strbuf.append(strBefore).append("#").append(fullCommentLine).append(endLineDelim);

                String spacesInStartComment = null;
                FormatStd std = this.std != null ? this.std : PyFormatStd.getFormat(getPyEdit());
                if (std.spacesInStartComment != 0) {
                    if (std.spacesInStartComment < 0) {
                        //Negative means that we manage it manually!
                        spacesInStartComment = StringUtils.createSpaceString(1);

                    } else {
                        spacesInStartComment = StringUtils.createSpaceString(std.spacesInStartComment);
                    }
                }

                // For each line, comment them out
                for (int i = startLineIndex; i <= endLineIndex; i++) {
                    String line = ps.getLine(i);
                    strbuf.append(line.substring(0, minCharsBefore));
                    strbuf.append("#");
                    line = line.substring(minCharsBefore);
                    strbuf.append(spacesInStartComment);
                    strbuf.append(line);
                    strbuf.append(endLineDelim);
                }
                // End of block
                String strAfter = firstLine.substring(0, minCharsBefore);
                fullCommentLine = getFullCommentLine(getLenOfStrConsideringTabEditorLen(strAfter), tempBuffer.clear());
                strbuf.append(lastLine.substring(0, minCharsBefore)).append("#").append(fullCommentLine);
            }

            int startOffset = ps.getStartLine().getOffset();
            String str = strbuf.toString();
            // Replace the text with the modified information
            ps.getDoc().replace(startOffset, ps.getSelLength(), str);

            return new Tuple<Integer, Integer>(startOffset + str.length(), 0);
        } catch (Exception e) {
            e.printStackTrace();
            beep(e);
        }

        // In event of problems, return null
        return null;
    }

    @Override
    protected String getPreferencesNameForChar() {
        return CommentBlocksPreferences.MULTI_BLOCK_COMMENT_CHAR;
    }

    /**
     * Currently returns a string with the comment block.  
     * 
     * @return Comment line string, or a default one if Preferences are null
     */
    protected String getFullCommentLine(int subtract, FastStringBuffer buffer) {
        Tuple<Integer, Character> colsAndChar = getColsAndChar();
        int cols = colsAndChar.o1 - subtract;
        char c = colsAndChar.o2;

        buffer.clear();
        for (int i = 0; i < cols - 1; i++) {
            buffer.append(c);
        }
        return buffer.toString();
    }
}
