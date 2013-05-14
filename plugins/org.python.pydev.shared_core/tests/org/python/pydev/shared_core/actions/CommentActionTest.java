package org.python.pydev.shared_core.actions;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.shared_core.string.TextSelectionUtils;

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
}
