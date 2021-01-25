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
        Document doc = new Document("a = 10;");

        TextSelectionUtils ts = new TextSelectionUtils(doc, 0);

        LineCommentAction comment = new LineCommentAction(ts, "##", 1);

        LineUncommentAction uncomment = new LineUncommentAction(ts, "##", 1);

        assertEquals("## a = 10", comment.commentLines("a = 10").toString());
        assertEquals("a = 10", uncomment.uncommentLines("## a = 10").toString());
        assertEquals("a = 10", uncomment.uncommentLines("##a = 10").toString());
        assertEquals(" a = 10", uncomment.uncommentLines("##  a = 10").toString());
    }

    public void testLineCommentUncommentSpaced() throws Exception {
        Document doc = new Document("a = 10;");

        TextSelectionUtils ts = new TextSelectionUtils(doc, 0);

        LineCommentAction comment = new LineCommentAction(ts, "##", 1);

        LineUncommentAction uncomment = new LineUncommentAction(ts, "##", 1);

        assertEquals("    ## a = 10", comment.commentLines("    a = 10").toString());
        assertEquals("    a = 10", uncomment.uncommentLines("    ## a = 10").toString());
        assertEquals("   a = 10", uncomment.uncommentLines("   ##a = 10").toString());
        assertEquals(" a = 10", uncomment.uncommentLines(" ## a = 10").toString());
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
        testContent(uncommentedContent, commentedContent, 0);
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

    private void testContent(String uncommentedContent, String commentedContent, int startLine)
            throws BadLocationException {
        TextSelectionUtils ts = createTextSelectionUtils(uncommentedContent, startLine);
        new LineCommentAction(ts, "##", 1).execute();
        assertEquals(commentedContent, ts.getDoc().get());
        ts = createTextSelectionUtils(commentedContent, startLine);
        new LineUncommentAction(ts, "##", 1).execute();
        assertEquals(uncommentedContent, ts.getDoc().get());
    }

    private TextSelectionUtils createTextSelectionUtils(String content, int startLine) {
        TextSelectionUtils ts = new TextSelectionUtils(new Document(content), 0);
        int startOffset = ts.getLineOffset(startLine);
        ts.setSelection(startOffset, content.length());
        return ts;
    }
}
