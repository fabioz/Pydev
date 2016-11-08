package org.python.pydev.editor;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.rules.IToken;
import org.python.pydev.editor.ColorCacheAndStyleForTesting.TextAttr;
import org.python.pydev.shared_core.string.FastStringBuffer;

import junit.framework.TestCase;

public class PyFStringScannerTest extends TestCase {

    public void testFStringTokenScanner() {
        check(
                "a {b} a",
                "" +
                        "unicode:0:2\n" +
                        "string:2:1\n" +
                        "code:3:1\n" +
                        "string:4:1\n" +
                        "unicode:5:2\n" +
                        "");
    }

    public void testFStringTokenScanner2() {
        check(
                "{b}",
                "" +
                        "string:0:1\n" +
                        "code:1:1\n" +
                        "string:2:1\n" +
                        "");
    }

    public void testFStringTokenScanner2a() {
        check(
                "  {b}",
                "" +
                        "string:2:1\n" +
                        "code:3:1\n" +
                        "string:4:1\n" +
                        "",
                2,
                3);
    }

    public void testFStringTokenScanner3() {
        check(
                "{bsn",
                "" +
                        "unicode:0:4\n" +
                        "");
    }

    public void testFStringTokenScanner4() {
        check(
                "aa{bsn",
                "" +
                        "unicode:0:2\n" +
                        "unicode:2:4\n" +
                        "");
    }

    public void testFStringTokenScanner5() {
        check(
                "a{b}a{b}a",
                "" +
                        "unicode:0:1\n" +

                        "string:1:1\n" +
                        "code:2:1\n" +
                        "string:3:1\n" +

                        "unicode:4:1\n" +

                        "string:5:1\n" +
                        "code:6:1\n" +
                        "string:7:1\n" +

                        "unicode:8:1\n" +
                        "");

    }

    public void testFStringTokenScanner6() {
        check(
                "{'{b}'}",
                "" +
                        "string:0:1\n" +
                        "code:1:5\n" +
                        "string:2:1\n" +
                        "");

    }

    private void check(String initialContent, String expected) {
        check(initialContent, expected, 0, initialContent.length());
    }

    private void check(String initialContent, String expected, int startOffset, int len) {
        Document document = new Document(initialContent);
        ColorCacheAndStyleForTesting colorCache = new ColorCacheAndStyleForTesting();
        PyFStringScanner pyStringScanner = new PyFStringScanner(colorCache);
        pyStringScanner.setRange(document, startOffset, len);
        FastStringBuffer buf = new FastStringBuffer();
        while (true) {
            IToken nextToken = pyStringScanner.nextToken();
            if (nextToken.isEOF()) {
                break;
            }
            int tokenOffset = pyStringScanner.getTokenOffset();
            int tokenLength = pyStringScanner.getTokenLength();
            String data = ((TextAttr) nextToken.getData()).data;
            buf.append(data).append(':').append(tokenOffset)
                    .append(':').append(tokenLength).append('\n');
        }
        String string = buf.toString();
        assertEquals(expected, string);
    }

}
