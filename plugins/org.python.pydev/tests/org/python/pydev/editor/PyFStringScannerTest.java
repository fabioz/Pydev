package org.python.pydev.editor;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.rules.IToken;
import org.python.pydev.editor.ColorCacheAndStyleForTesting.TextAttr;
import org.python.pydev.shared_core.string.FastStringBuffer;

import junit.framework.TestCase;

public class PyFStringScannerTest extends TestCase {

    public void testFStringTokenScanner1() {
        check(
                "f'a {b} a'",
                "" +
                        "unicode:0:2\n" +
                        "unicode:2:2\n" +
                        "string:4:3\n" +
                        "unicode:7:2\n" +
                        "unicode:9:1\n" +
                        "");
    }

    //It's possible that the damage comes from a non-full damager 
    //if the damage was done in another partition, so, we need to 
    //cover for this situation.
    public void testFStringTokenScanner1a() {
        check(
                "f'''",
                "" +
                        "unicode:0:4\n" +
                        "");
    }

    public void testFStringTokenScanner1b() {
        check(
                "f'''{",
                "" +
                        "unicode:0:4\n" +
                        "string:4:1\n" +
                        "");
    }

    public void testFStringTokenScanner1c() {
        check(
                "f'''{}a",
                "" +
                        "unicode:0:4\n" +
                        "string:4:2\n" +
                        "unicode:6:1\n" +
                        "");
    }

    public void testFStringTokenScanner2() {
        check(
                "f'{b}'",
                "" +
                        "unicode:0:2\n" +
                        "string:2:3\n" +
                        "unicode:5:1\n" +
                        "");
    }

    public void testFStringTokenScanner2a() {
        check(
                "  f'  {b}'  ", //Note: checking on a substring
                "" +
                        "unicode:2:2\n" +
                        "unicode:4:2\n" +
                        "string:6:3\n" +
                        "unicode:9:1\n" +
                        "",
                2,
                8);
    }

    public void testFStringTokenScanner3() {
        check(
                "f'{bsn'",
                "" +
                        "unicode:0:2\n" +
                        "string:2:4\n" +
                        "unicode:6:1\n" +
                        "");
    }

    public void testFStringTokenScanner4() {
        check(
                "f'aa{bsn",
                "" +
                        "unicode:0:2\n" +
                        "unicode:2:2\n" +
                        "string:4:4\n" +
                        "");
    }

    public void testFStringTokenScanner5() {
        check(
                "f'a{b}a{b}a",
                "" +
                        "unicode:0:2\n" +
                        "unicode:2:1\n" +
                        "string:3:3\n" +
                        "unicode:6:1\n" +
                        "string:7:3\n" +
                        "unicode:10:1\n" +
                        "");

    }

    public void testFStringTokenScanner6() {
        check(
                "f\"{'{b}'}\"",
                "" +
                        "unicode:0:2\n" +
                        "string:2:7\n" +
                        "unicode:9:1\n" +
                        "");

    }

    public void testFStringTokenScanner8() {
        check(
                "f'{}{}",
                "" +
                        "unicode:0:2\n" +
                        "string:2:2\n" +
                        "string:4:2\n" +
                        "");

    }

    public void testFStringTokenScanner9() {
        check(
                "f'th\n{an}",
                "" +
                        "unicode:0:2\n" +
                        "unicode:2:3\n" +
                        "string:5:4\n" +
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
