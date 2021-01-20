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
        testContent(uncommentedContent, "##a = 10", 0, 0, 0);
        testContent(uncommentedContent, "##a = 10", 0, 0, 1);
        testContent(uncommentedContent, "## a = 10", 0, 1, 1);
        testContent(uncommentedContent, "## a = 10", 0, 1, 0, " a = 10");
    }

    public void testLineCommentUncommentSpaced() throws Exception {
        String uncommentedContent = "   a = 10";
        String commentedContet = "   ## a = 10";
        testContent(uncommentedContent, commentedContet);
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
        testContent(uncommentedContent, commentedContent);
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
        testContent(uncommentedContent, commentedContent, 1);
    }

    private void testContent(String uncommentedContent, String commentedContent) throws BadLocationException {
        testContent(uncommentedContent, commentedContent, 0);
    }

    private void testContent(String uncommentedContent, String commentedContent, int startLine)
            throws BadLocationException {
        testContent(uncommentedContent, commentedContent, startLine, 1, 1);
    }

    private void testContent(String uncommentedContent, String commentedContent, int startLine,
            int commentSpacesInStart, int uncommentSpacesInStart) throws BadLocationException {
        testContent(uncommentedContent, commentedContent, startLine, commentSpacesInStart, uncommentSpacesInStart,
                uncommentedContent);
    }

    private void testContent(String uncommentedContent, String commentedContent, int startLine,
            int commentSpacesInStart, int uncommentSpacesInStart, String expectedUncomment)
            throws BadLocationException {
        TextSelectionUtils ts = createTextSelectionUtils(uncommentedContent, startLine);
        new LineCommentAction(ts, "##", commentSpacesInStart).execute();
        assertEquals(commentedContent, ts.getDoc().get());
        ts = createTextSelectionUtils(commentedContent, startLine);
        new LineUncommentAction(ts, "##", uncommentSpacesInStart).execute();
        assertEquals(expectedUncomment, ts.getDoc().get());
    }

    private TextSelectionUtils createTextSelectionUtils(String content, int startLine) {
        TextSelectionUtils ts = new TextSelectionUtils(new Document(content), 0);
        int startOffset = ts.getLineOffset(startLine);
        ts.setSelection(startOffset, content.length());
        return ts;
    }
}
