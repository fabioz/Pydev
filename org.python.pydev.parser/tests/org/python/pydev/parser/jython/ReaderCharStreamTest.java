/*
 * Created on Apr 27, 2006
 */
package org.python.pydev.parser.jython;

import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;

public class ReaderCharStreamTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ReaderCharStreamTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testIt() throws Exception {
        String initialDoc = 
            "a\n" +
            "bc\n";
        StringReader inString = new StringReader(initialDoc);
        CharStream in = new ReaderCharStream(inString);
        
        doTests(in);
        
        in = new FastCharStream(initialDoc);
        doTests(in);
    }

    /**
     * @param in
     * @throws IOException
     */
    private void doTests(CharStream in) throws IOException {
        assertEquals('a', in.BeginToken());
        checkStart(in,1,1);
        assertEquals(1, in.getEndColumn());
        assertEquals(1, in.getEndLine());
        assertEquals("a", in.GetImage());
        assertEquals("a", new String(in.GetSuffix(1)));
        char[] cs = new char[2];
        cs[1] = 'a';
        assertEquals(new String(cs), new String(in.GetSuffix(2)));
        cs = new char[3];
        cs[2] = 'a';
        assertEquals(new String(cs), new String(in.GetSuffix(3)));
        
        assertEquals('\n', in.readChar());
        checkStart(in,1,1);
        assertEquals(2, in.getEndColumn());
        assertEquals(1, in.getEndLine());
        assertEquals("a\n", in.GetImage());
        assertEquals("\n", new String(in.GetSuffix(1)));
        assertEquals("a\n", new String(in.GetSuffix(2)));
       
        assertEquals('b', in.readChar());
        checkStart(in,1,1);
        assertEquals(1, in.getEndColumn());
        assertEquals(2, in.getEndLine());
        assertEquals("a\nb", in.GetImage());
        
        assertEquals('c', in.readChar());
        checkStart(in,1,1);
        assertEquals(2, in.getEndColumn());
        assertEquals(2, in.getEndLine());
        assertEquals("a\nbc", in.GetImage());
        
        in.backup(1);
        assertEquals("a\nb", in.GetImage());
        assertEquals(1, in.getEndColumn());
        assertEquals(2, in.getEndLine());
        
        
        assertEquals('c', in.readChar());
        assertEquals("a\nbc", in.GetImage());
        checkStart(in,1,1);
        assertEquals(2, in.getEndColumn());
        assertEquals(2, in.getEndLine());
        
        assertEquals('\n', in.readChar());
        checkStart(in,1,1);
        assertEquals(3, in.getEndColumn());
        assertEquals(2, in.getEndLine());
        
        try {
            in.readChar();
            fail("Expected exception");
        } catch (IOException e) {
            //ok
        }
        assertEquals(3, in.getEndColumn());
        assertEquals(2, in.getEndLine());
    }

    /**
     * @param in
     */
    private void checkStart(CharStream in, int line, int col) {
        assertEquals(1, in.getBeginColumn());
        assertEquals(1, in.getBeginLine());
    }

}
