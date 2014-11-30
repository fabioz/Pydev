/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author: ptoofani
 * Created: June 2004
 */

package org.python.pydev.editor.actions;

import java.util.HashSet;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * Removes a comment block. Comment blocks are slightly different than regular
 * comments created in that they provide a distinguishing element at the
 * beginning and end as a separator. In this case, it is a string of
 * <code>=======</code> symbols to strongly differentiate this comment block.
 * 
 * This will handle regular comment blocks as well by removing the # token at
 * the head of each line, but will also remove the block separators if they are
 * present.
 * 
 * Changes in 1.2.7: if any line of a block comment is selected, all 'adjacent' lines
 * will have its comment removed (without the need to select the whole lines
 * 
 * @author Parhaum Toofanian
 * @author Fabio Zadrozny
 */
public class PyRemoveBlockComment extends PyAddBlockComment {

    /**
     * Grabs the selection information and performs the action.
     */
    @Override
    public void run(IAction action) {
        try {
            if (!canModifyEditor()) {
                return;
            }

            // Select from text editor
            PyEdit pyEdit = getPyEdit();

            PySelection ps = new PySelection(getTextEditor());

            // Perform the action
            Tuple<Integer, Integer> repRegion = perform(ps);
            if (repRegion == null) {
                return;
            }

            // Put cursor at the first area of the selection
            pyEdit.selectAndReveal(repRegion.o1, repRegion.o2);
        } catch (Exception e) {
            beep(e);
        }
    }

    @Override
    public Tuple<Integer, Integer> perform(PySelection ps) {
        int startLineIndex = getStartIndex(ps);
        int endLineIndex = getEndIndex(ps);
        if (startLineIndex == -1 || endLineIndex == -1) {
            if (startLineIndex == -1 && endLineIndex == -1) {
                return null;
            } else if (startLineIndex == -1) {
                startLineIndex = endLineIndex;
            } else {
                endLineIndex = startLineIndex;
            }
        }
        return this.performUncommentBlock(ps, startLineIndex, endLineIndex);
    }

    /**
     * Performs the action with a given PySelection
     * 
     * @param ps Given PySelection
     * @return boolean The success or failure of the action
     */
    public Tuple<Integer, Integer> performUncommentBlock(TextSelectionUtils ps, int startLineIndex, int endLineIndex) {
        // What we'll be replacing the selected text with
        FastStringBuffer strbuf = new FastStringBuffer();

        try {
            //discover 1st line that starts the block comment
            int i;

            // For each line, uncomment it
            for (i = startLineIndex; i <= endLineIndex; i++) {
                boolean addDelim = true;
                String spacesBefore = "";
                String line = ps.getLine(i);
                int lineLen = line.length();
                for (int j = 0; j < lineLen; j++) {
                    char c = line.charAt(j);
                    if (c == '#') {
                        //ok, it starts with # (so, remove the whitespaces before it)
                        if (j > 0) {
                            spacesBefore = line.substring(0, j);
                        }
                        line = line.substring(j);
                        break;
                    } else {
                        if (!Character.isWhitespace(c)) {
                            break;
                        }
                    }
                }
                if (line.startsWith("#")) {
                    line = line.substring(1);
                }

                //get the chars used in block-comments
                AbstractBlockCommentAction[] acts = new AbstractBlockCommentAction[] { new PyAddSingleBlockComment(),
                        new PyAddBlockComment() };
                HashSet<Character> chars = new HashSet<Character>();
                for (int j = 0; j < acts.length; j++) {
                    AbstractBlockCommentAction action = acts[j];
                    chars.add(action.getColsAndChar().o2);
                }

                if (line.length() > 0) {
                    boolean removedChar = false;
                    char lastChar = '\0';
                    for (int j = 0; j < line.length(); j++) {
                        lastChar = line.charAt(j);
                        if (!chars.contains(lastChar)) {
                            break;
                        } else {
                            removedChar = true;
                            line = line.substring(1);
                            j--;
                        }
                    }
                    if (line.length() == 0 && removedChar) {
                        addDelim = false;
                    }
                    if (removedChar && lastChar == ' ') {
                        line = line.substring(1);
                    }
                }

                if (addDelim) {
                    strbuf.append(spacesBefore);
                    strbuf.append(line);
                    String lineDelimiter = ps.getDoc().getLineDelimiter(i);
                    if (lineDelimiter != null) {
                        strbuf.append(lineDelimiter);
                    }
                }
            }

            //Ok, at this point things should be correct, but make sure than on uncomment,
            //the code goes to a proper indent position (remove spaces we may have added when creating a block).
            String string = strbuf.toString();
            List<String> lines = StringUtils.splitInLines(string);
            Tuple<Integer, String> posAndLine = new Tuple<Integer, String>(-1, "");
            for (String line : lines) {
                //Don't consider empty lines
                if (line.trim().length() > 0) {
                    int firstCharPosition = PySelection.getFirstCharPosition(line);
                    if (firstCharPosition < posAndLine.o1 || posAndLine.o1 < 0) {
                        posAndLine.o1 = firstCharPosition;
                        posAndLine.o2 = line;
                    }
                }
            }
            if (posAndLine.o1 > 0) {
                String sub = posAndLine.o2.substring(0, posAndLine.o1);
                if (sub.endsWith("\t") && sub.startsWith(" ")) {
                    for (int j = 0; j < sub.length(); j++) {
                        if (Character.isWhitespace(sub.charAt(j)) && sub.charAt(j) != '\t') {

                        } else {
                            sub = sub.substring(0, j);
                        }
                    }
                }
                if (sub.endsWith(" ")) { //If it ends with a tab, we won't change anything (only spaces are removed -- which we may have introduced)
                    boolean allEqual = true;
                    for (String line : lines) {
                        if (!line.startsWith(sub)) {
                            //Don't consider empty lines
                            if (line.trim().length() > 0) {
                                allEqual = false;
                            }
                            break;
                        }
                    }
                    if (allEqual) {
                        if (sub.startsWith("\t")) {
                            //Tabs based indent: remove any ending spaces (and at this point we know a string ends with a space)
                            int j;
                            for (j = sub.length() - 1; j >= 0; j--) {
                                char c = sub.charAt(j);
                                if (c != ' ') {
                                    j++;
                                    break;
                                }
                            }
                            String newSub = sub.substring(0, j);
                            strbuf.clear();
                            for (String line : lines) {
                                strbuf.append(newSub);
                                strbuf.append(line.substring(sub.length()));
                            }

                        } else {
                            IIndentPrefs indentPrefs;
                            if (targetEditor instanceof PyEdit) {
                                PyEdit pyEdit = (PyEdit) targetEditor;
                                indentPrefs = pyEdit.getIndentPrefs();
                            } else {
                                indentPrefs = DefaultIndentPrefs.get(null);
                            }

                            String indentationString = indentPrefs.getIndentationString();

                            if (indentationString.equals("\t")) {
                                strbuf.clear();
                                for (String line : lines) {
                                    if (line.startsWith(sub)) { //must check as we may have empty lines
                                        strbuf.append(line.substring(sub.length()));
                                    } else {
                                        strbuf.append(line);
                                    }
                                }
                            } else {
                                int subLen = sub.length();
                                int indentLen = indentationString.length();
                                int mod = subLen % indentLen;
                                if (mod != 0) {
                                    String substring = sub.substring(subLen - mod, subLen);
                                    boolean onlyWhitespaces = true;
                                    for (int k = 0; k < substring.length(); k++) {
                                        if (substring.charAt(k) != ' ') {
                                            onlyWhitespaces = false;
                                            break;
                                        }
                                    }
                                    if (onlyWhitespaces) {
                                        String newSub = sub.substring(0, subLen - mod);
                                        strbuf.clear();
                                        for (String line : lines) {
                                            if (line.trim().length() == 0) {
                                                strbuf.append(line);

                                            } else {
                                                strbuf.append(newSub);
                                                strbuf.append(line.substring(sub.length()));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Replace the text with the modified information
            int startLineOffset = ps.getLineOffset(startLineIndex);
            int endLineOffset = ps.getEndLineOffset(endLineIndex);
            String endLineDelimiter = ps.getDoc().getLineDelimiter(endLineIndex);
            int endLen = 0;
            if (endLineDelimiter != null) {
                endLen = endLineDelimiter.length();
                endLineOffset += endLen;
            }
            String str = strbuf.toString();
            ps.getDoc().replace(startLineOffset, endLineOffset - startLineOffset, str);
            int len = str.length();
            if (len > endLen) {
                len -= endLen;
            }
            return new Tuple<Integer, Integer>(startLineOffset, len);
        } catch (Exception e) {
            beep(e);
        }

        // In event of problems, return null
        return null;
    }

    private int getEndIndex(PySelection ps) {
        int endLineIndex = -1;
        int i = ps.getEndLineIndex();
        while (true) {
            String line = ps.getLine(i);
            if (PySelection.isCommentLine(line)) {
                endLineIndex = i;
            } else {
                break;
            }
            i++;
        }
        return endLineIndex;
    }

    private int getStartIndex(PySelection ps) {
        int startLineIndex = -1;
        int i = ps.getStartLineIndex();
        while (true) {
            String line = ps.getLine(i);
            if (PySelection.isCommentLine(line)) {
                startLineIndex = i;
            } else {
                break;
            }
            i--;
        }
        return startLineIndex;
    }

}
