/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 15, 2006
 */
package org.python.copiedfromeclipsesrc;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.PythonCodeReader;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class PythonCodeReaderTest extends TestCase {

    public static void main(String[] args) {
        try {
            PythonCodeReaderTest t = new PythonCodeReaderTest();
            t.setUp();
            t.testBackwardCurrentStatement3();
            t.tearDown();
            junit.textui.TestRunner.run(PythonCodeReaderTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private PythonCodeReader reader;
    private Document doc;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testForward() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe");
        reader.configureForwardReader(doc, 0, doc.getLength(), true, true, true);
        assertEquals('f', reader.read());
        assertEquals('e', reader.read());
        assertEquals(PythonCodeReader.EOF, reader.read());
    }

    public void testBackward() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe");
        reader.configureBackwardReader(doc, doc.getLength(), true, true, true);
        assertEquals('e', reader.read());
        assertEquals('f', reader.read());
        assertEquals(PythonCodeReader.EOF, reader.read());
    }

    public void testBackwardLiterals() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe\n'lit'\n");
        reader.configureBackwardReader(doc, doc.getLength(), true, true, true);
        assertEquals('\n', (char) reader.read());
        assertEquals('\n', (char) reader.read());
        assertEquals('e', (char) reader.read());
        assertEquals('f', (char) reader.read());
        assertEquals(PythonCodeReader.EOF, reader.read());
    }

    public void testBackwardLiterals2() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe\n\"lit\"\n");
        reader.configureBackwardReader(doc, doc.getLength(), true, true, true);
        assertEquals('\n', (char) reader.read());
        assertEquals('\n', (char) reader.read());
        assertEquals('e', (char) reader.read());
        assertEquals('f', (char) reader.read());
        assertEquals(PythonCodeReader.EOF, reader.read());
    }

    public void testBackwardLiterals3() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe\n'''lit'''\n");
        reader.configureBackwardReader(doc, doc.getLength(), true, true, true);
        assertEquals('\n', (char) reader.read());
        assertEquals('\n', (char) reader.read());
        assertEquals('e', (char) reader.read());
        assertEquals('f', (char) reader.read());
        assertEquals(PythonCodeReader.EOF, reader.read());
    }

    public void testBackwardLiterals4() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe\n'''li't'''\n");
        reader.configureBackwardReader(doc, doc.getLength(), true, true, true);
        assertEquals('\n', (char) reader.read());
        assertEquals('\n', (char) reader.read());
        assertEquals('e', (char) reader.read());
        assertEquals('f', (char) reader.read());
        assertEquals(PythonCodeReader.EOF, reader.read());
    }

    public void testBackwardLiterals5() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("''\n");
        reader.configureBackwardReader(doc, doc.getLength(), true, true, true);
        assertEquals('\n', (char) reader.read());
        assertEquals(PythonCodeReader.EOF, reader.read());
    }

    public void testBackwardLiterals6() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("''''\n");
        reader.configureBackwardReader(doc, doc.getLength(), true, true, true);
        assertEquals('\n', (char) reader.read());
        assertEquals(PythonCodeReader.EOF, reader.read());
    }

    public void testBackwardComments() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe\n#foo");
        reader.configureBackwardReader(doc, doc.getLength(), true, true, true);
        assertEquals('#', (char) reader.read());
        assertEquals('\n', (char) reader.read());
        assertEquals('e', (char) reader.read());
        assertEquals('f', (char) reader.read());
        assertEquals(PythonCodeReader.EOF, reader.read());
    }

    public void testForwardComments() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe\n#too\nh");
        reader.configureForwardReader(doc, 0, doc.getLength(), true, true, true);
        assertEquals('f', (char) reader.read());
        assertEquals('e', (char) reader.read());
        assertEquals('\n', (char) reader.read());
        assertEquals('#', (char) reader.read());
        assertEquals('\n', (char) reader.read());
        assertEquals('h', (char) reader.read());
        assertEquals(PythonCodeReader.EOF, reader.read());
    }

    public void testForwardLiteral() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe\n'too'\nh");
        reader.configureForwardReader(doc, 0, doc.getLength(), true, true, true);
        assertEquals('f', (char) reader.read());
        assertEquals('e', (char) reader.read());
        assertEquals('\n', (char) reader.read());
        assertEquals('\n', (char) reader.read());
        assertEquals('h', (char) reader.read());
        assertEquals(PythonCodeReader.EOF, reader.read());
    }

    public void testForwardLiteral2() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe\n'''too'''\nh");
        reader.configureForwardReader(doc, 0, doc.getLength(), true, true, true);
        assertEquals('f', (char) reader.read());
        assertEquals('e', (char) reader.read());
        assertEquals('\n', (char) reader.read());
        assertEquals('\n', (char) reader.read());
        assertEquals('h', (char) reader.read());
        assertEquals(PythonCodeReader.EOF, reader.read());
    }

    public void testForwardCurrentStatement() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("a = 10\n" +
                "def m1(self): pass");
        reader.configureForwardReader(doc, 0, doc.getLength(), true, true, true);
        FastStringBuffer buf = new FastStringBuffer();
        int c;
        while ((c = reader.read()) != PythonCodeReader.EOF) {
            buf.append((char) c);
        }
        assertEquals("a = 10\n", buf.toString());
    }

    public void testBackwardCurrentStatement() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("a = 10\n" +
                "def m1(self):\n" +
                "   a = 10");
        reader.configureBackwardReader(doc, doc.getLength(), true, true, true);
        FastStringBuffer buf = new FastStringBuffer();
        int c;
        while ((c = reader.read()) != PythonCodeReader.EOF) {
            buf.append((char) c);
        }
        buf.reverse();
        assertEquals(" m1(self):\n   a = 10", buf.toString());
    }

    public void testBackwardCurrentStatement2() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("" +
                "titleEnd = ('''\n" +
                "            [#''')" + //should wrap to the start
                "");
        reader.configureBackwardReader(doc, doc.getLength(), true, true, true);
        FastStringBuffer buf = new FastStringBuffer();
        int c;
        while ((c = reader.read()) != PythonCodeReader.EOF) {
            buf.append((char) c);
        }
        buf.reverse();
        assertEquals("titleEnd = ()", buf.toString());
    }

    public void testBackwardCurrentStatement3() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("" +
                "titleEnd = ('''\n" +
                "# inside string" +
                "            [#''') #actual" + //should wrap to the start
                "");
        reader.configureBackwardReader(doc, doc.getLength(), true, true, true);
        FastStringBuffer buf = new FastStringBuffer();
        int c;
        while ((c = reader.read()) != PythonCodeReader.EOF) {
            buf.append((char) c);
        }
        buf.reverse();
        assertEquals("titleEnd = () #", buf.toString());
    }

}
