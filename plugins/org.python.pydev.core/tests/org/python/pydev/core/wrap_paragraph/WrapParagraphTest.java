package org.python.pydev.core.wrap_paragraph;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.ReplaceEdit;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.shared_core.string.StringUtils;

import junit.framework.TestCase;

public class WrapParagraphTest extends TestCase {

    public void testParagrapher() throws MalformedTreeException, BadLocationException {
        String txt = "# aaaa bbbb ccccc ddddd";
        IDocument doc = new Document(txt);
        PySelection ps = new PySelection(doc);
        Paragrapher p = new Paragrapher(ps, 12);
        assertNull(p.getValidErrorInPos());
        ReplaceEdit replaceEdit = p.getReplaceEdit();
        replaceEdit.apply(doc);
        assertEquals("# aaaa bbbb\n"
                + "# ccccc\n"
                + "# ddddd", StringUtils.replaceNewLines(doc.get(), "\n"));

    }

    public void testParagrapher2() throws MalformedTreeException, BadLocationException {
        String txt = "# aaaa bbbb\n# ccccc ddddd";
        IDocument doc = new Document(txt);
        PySelection ps = new PySelection(doc);
        Paragrapher p = new Paragrapher(ps, 12);
        assertNull(p.getValidErrorInPos());
        ReplaceEdit replaceEdit = p.getReplaceEdit();
        replaceEdit.apply(doc);
        assertEquals("# aaaa bbbb\n"
                + "# ccccc\n"
                + "# ddddd", StringUtils.replaceNewLines(doc.get(), "\n"));

    }

    public void testParagrapher3() throws MalformedTreeException, BadLocationException {
        String txt = "'''\naaaa bbbb\nccccc ddddd eeee\n'''";
        IDocument doc = new Document(txt);
        PySelection ps = new PySelection(doc, 5);
        Paragrapher p = new Paragrapher(ps, 12);
        assertNull(p.getValidErrorInPos());
        ReplaceEdit replaceEdit = p.getReplaceEdit();
        replaceEdit.apply(doc);
        assertEquals("'''\n"
                + "aaaa bbbb\n"
                + "ccccc ddddd\n"
                + "eeee\n"
                + "'''", StringUtils.replaceNewLines(doc.get(), "\n"));

    }

}
