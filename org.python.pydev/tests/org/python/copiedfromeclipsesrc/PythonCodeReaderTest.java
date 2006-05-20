/*
 * Created on Mar 15, 2006
 */
package org.python.copiedfromeclipsesrc;

import org.eclipse.jface.text.Document;

import junit.framework.TestCase;

public class PythonCodeReaderTest extends TestCase {

    public static void main(String[] args) {
        try {
            PythonCodeReaderTest t = new PythonCodeReaderTest();
            t.setUp();
            t.testBackwardComments();
            t.tearDown();
            junit.textui.TestRunner.run(PythonCodeReaderTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private PythonCodeReader reader;
    private Document doc;

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testForward() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe");
        reader.configureForwardReader(doc, 0, doc.getLength(), true, true);
        assertEquals('f',reader.read());
        assertEquals('e',reader.read());
        assertEquals(PythonCodeReader.EOF,reader.read());
    }
    
    public void testBackward() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe");
        reader.configureBackwardReader(doc, doc.getLength(), true, true);
        assertEquals('e',reader.read());
        assertEquals('f',reader.read());
        assertEquals(PythonCodeReader.EOF,reader.read());
    }
    
    public void testBackwardLiterals() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe\n'lit'\n");
        reader.configureBackwardReader(doc, doc.getLength(), true, true);
        assertEquals('\n',(char)reader.read());
        assertEquals('\n',(char)reader.read());
        assertEquals('e',(char)reader.read());
        assertEquals('f',(char)reader.read());
        assertEquals(PythonCodeReader.EOF,reader.read());
    }
    
    public void testBackwardLiterals2() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe\n\"lit\"\n");
        reader.configureBackwardReader(doc, doc.getLength(), true, true);
        assertEquals('\n',(char)reader.read());
        assertEquals('\n',(char)reader.read());
        assertEquals('e',(char)reader.read());
        assertEquals('f',(char)reader.read());
        assertEquals(PythonCodeReader.EOF,reader.read());
    }
    
    public void testBackwardLiterals3() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe\n'''lit'''\n");
        reader.configureBackwardReader(doc, doc.getLength(), true, true);
        assertEquals('\n',(char)reader.read());
        assertEquals('\n',(char)reader.read());
        assertEquals('e',(char)reader.read());
        assertEquals('f',(char)reader.read());
        assertEquals(PythonCodeReader.EOF,reader.read());
    }
    
    public void testBackwardLiterals4() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe\n'''li't'''\n");
        reader.configureBackwardReader(doc, doc.getLength(), true, true);
        assertEquals('\n',(char)reader.read());
        assertEquals('\n',(char)reader.read());
        assertEquals('e',(char)reader.read());
        assertEquals('f',(char)reader.read());
        assertEquals(PythonCodeReader.EOF,reader.read());
    }
    
    public void testBackwardLiterals5() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("''\n");
        reader.configureBackwardReader(doc, doc.getLength(), true, true);
        assertEquals('\n',(char)reader.read());
        assertEquals(PythonCodeReader.EOF,reader.read());
    }
    
    public void testBackwardLiterals6() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("''''\n");
        reader.configureBackwardReader(doc, doc.getLength(), true, true);
        assertEquals('\n',(char)reader.read());
        assertEquals(PythonCodeReader.EOF,reader.read());
    }
    
    public void testBackwardComments() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe\n#foo");
        reader.configureBackwardReader(doc, doc.getLength(), true, true);
        assertEquals('#',(char)reader.read());
        assertEquals('\n',(char)reader.read());
        assertEquals('e',(char)reader.read());
        assertEquals('f',(char)reader.read());
        assertEquals(PythonCodeReader.EOF,reader.read());
    }
    
    public void testForwardComments() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe\n#too\nh");
        reader.configureForwardReader(doc, 0, doc.getLength(), true, true);
        assertEquals('f',(char)reader.read());
        assertEquals('e',(char)reader.read());
        assertEquals('\n',(char)reader.read());
        assertEquals('#',(char)reader.read());
        assertEquals('\n',(char)reader.read());
        assertEquals('h',(char)reader.read());
        assertEquals(PythonCodeReader.EOF,reader.read());
    }
    
    public void testForwardLiteral() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe\n'too'\nh");
        reader.configureForwardReader(doc, 0, doc.getLength(), true, true);
        assertEquals('f',(char)reader.read());
        assertEquals('e',(char)reader.read());
        assertEquals('\n',(char)reader.read());
        assertEquals('\n',(char)reader.read());
        assertEquals('h',(char)reader.read());
        assertEquals(PythonCodeReader.EOF,reader.read());
    }
    
    public void testForwardLiteral2() throws Exception {
        reader = new PythonCodeReader();
        doc = new Document("fe\n'''too'''\nh");
        reader.configureForwardReader(doc, 0, doc.getLength(), true, true);
        assertEquals('f',(char)reader.read());
        assertEquals('e',(char)reader.read());
        assertEquals('\n',(char)reader.read());
        assertEquals('\n',(char)reader.read());
        assertEquals('h',(char)reader.read());
        assertEquals(PythonCodeReader.EOF,reader.read());
    }
    
    
}
