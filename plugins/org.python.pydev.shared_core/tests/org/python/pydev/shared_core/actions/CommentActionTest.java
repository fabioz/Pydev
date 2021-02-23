/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.actions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.python.pydev.shared_core.string.TextSelectionUtils;

import junit.framework.TestCase;

public class CommentActionTest extends TestCase {

    public void testLineCommentUncomment() throws Exception {
        String uncommentedContent = "a = 10";
        checkContent(uncommentedContent, "##a = 10", 0, 0, 0, true);
        checkContent(uncommentedContent, "##a = 10", 0, 0, 1, true);
        checkContent(uncommentedContent, "## a = 10", 0, 1, 1, true);
        checkContent(uncommentedContent, "## a = 10", 0, 1, 0, " a = 10", true);
    }

    public void testLineCommentUncommentSpaced() throws Exception {
        String uncommentedContent = "   a = 10";
        String commentedContet = "   ## a = 10";
        checkContent(uncommentedContent, commentedContet, true);
    }

    public void testMultiLineCommentSpaced1() throws Exception {
        String uncommentedContent = ""
                + "def method():\n" +
                "    if True:\n" +
                "        a = 10";
        String commentedContent = ""
                + "## def method():\n" +
                "    ## if True:\n" +
                "        ## a = 10";
        checkContent(uncommentedContent, commentedContent, true);
    }

    public void testMultiLineCommentSpaced2() throws Exception {
        String uncommentedContent = ""
                + "def method():\n" +
                "    if True:\n" +
                "        a = 10";
        String commentedContent = ""
                + "def method():\n" +
                "    ## if True:\n" +
                "        ## a = 10";
        checkContent(uncommentedContent, commentedContent, 1, true);
    }

    public void testMultiLineCommentSpaced3() throws Exception {
        String uncommentedContent = "" +
                "    if True:\n" +
                "\n" +
                "        a = 10";

        String commentedContent = "" +
                "    ## if True:\n" +
                "    ##\n" +
                "        ## a = 10";

        String expectedUncommentedContent = "" +
                "    if True:\n" +
                "    \n" +
                "        a = 10";
        checkContent(uncommentedContent, commentedContent, 0, 1, 1, expectedUncommentedContent, true);
    }

    public void testMultiLineCommentSpaced4() throws Exception {
        String uncommentedContent = "" +
                "    \n" +
                "\n" +
                "    if True:\n" +
                "\n" +
                "        a = 10";

        String commentedContent = "" +
                "    ##\n" +
                "    ##\n" +
                "    ## if True:\n" +
                "    ##\n" +
                "        ## a = 10";

        String expectedUncommentedContent = "" +
                "    \n" +
                "    \n" +
                "    if True:\n" +
                "    \n" +
                "        a = 10";
        checkContent(uncommentedContent, commentedContent, 0, 1, 1, expectedUncommentedContent, true);
    }

    public void testSingleEmptyLine() throws Exception {
        String uncommentedContent = "\n"
                + "\n";

        String commentedContent = "\n"
                + "##\n";

        String expectedUncommentedContent = "\n"
                + "\n";
        checkContent(uncommentedContent, commentedContent, 1, 1, 1, expectedUncommentedContent, true);
    }

    public void testMultipleEmptyLine() throws Exception {
        String uncommentedContent = "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n";

        String commentedContent = "##\n"
                + "##\n"
                + "##\n"
                + "##\n"
                + "##\n";

        String expectedUncommentedContent = "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n";
        checkContent(uncommentedContent, commentedContent, 0, 1, 1, expectedUncommentedContent, true);
    }

    public void testSingleEmptyLine2() throws Exception {
        String uncommentedContent = "\n"
                + "\n";

        String commentedContent = "\n"
                + "## \n";

        String expectedUncommentedContent = "\n"
                + "\n";
        checkContent(uncommentedContent, commentedContent, 1, 1, 1, expectedUncommentedContent, false);
    }

    private void checkContent(String uncommentedContent, String commentedContent, boolean addCommentsAtIndent)
            throws BadLocationException {
        checkContent(uncommentedContent, commentedContent, 0, addCommentsAtIndent);
    }

    private void checkContent(String uncommentedContent, String commentedContent, int startLine,
            boolean addCommentsAtIndent)
            throws BadLocationException {
        checkContent(uncommentedContent, commentedContent, startLine, 1, 1, addCommentsAtIndent);
    }

    private void checkContent(String uncommentedContent, String commentedContent, int startLine,
            int commentSpacesInStart, int uncommentSpacesInStart, boolean addCommentsAtIndent)
            throws BadLocationException {
        checkContent(uncommentedContent, commentedContent, startLine, commentSpacesInStart, uncommentSpacesInStart,
                uncommentedContent, addCommentsAtIndent);
    }

    private void checkContent(String uncommentedContent, String commentedContent, int startLine,
            int commentSpacesInStart, int uncommentSpacesInStart, String expectedUncomment, boolean addCommentsAtIndent)
            throws BadLocationException {
        TextSelectionUtils ts = createTextSelectionUtils(uncommentedContent, startLine);
        String addCommentsOption = null;
        if (addCommentsAtIndent) {
            addCommentsOption = LineCommentOption.ADD_COMMENTS_INDENT_LINE_ORIENTED;
        } else {
            addCommentsOption = LineCommentOption.ADD_COMMENTS_LINE_START;
        }
        new LineCommentAction(ts, "##", commentSpacesInStart, addCommentsOption).execute();
        assertEquals(commentedContent, ts.getDoc().get());
        ts = createTextSelectionUtils(commentedContent, startLine);
        new LineUncommentAction(ts, "##", uncommentSpacesInStart).execute();
        assertEquals(expectedUncomment, ts.getDoc().get());
    }

    private TextSelectionUtils createTextSelectionUtils(String content, int startLine) {
        TextSelectionUtils ts = new TextSelectionUtils(new Document(content), 0);
        int startOffset = ts.getLineOffset(startLine);
        ts.setSelection(startOffset, content.length() + 1);
        return ts;
    }
}
