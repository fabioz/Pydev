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
    
    public void testIt2() throws Exception {
    	String s = new String(new char[]{'\n',34,34,34,'\n',97,'\n',34,34,34});
        //
        //"""
        //a
        //"""
    	
    	StringReader inString = new StringReader(s);
    	CharStream in = new ReaderCharStream(inString);
    	checkCvsStream(in);
    	
    	in = new FastCharStream(s.toCharArray());
    	checkCvsStream(in);
    	
    	
    }

	private void checkCvsStream(CharStream in) throws IOException {
		assertEquals(10,in.BeginToken());
		in.backup(0);
		assertEquals("\n",new String(in.GetSuffix(1)));
		in.backup(1);
		
		assertEquals(10,in.readChar());
    	assertEquals(34,in.readChar());
    	assertEquals(34,in.readChar());
    	assertEquals(34,in.readChar());
    	assertEquals(10,in.readChar());
    	
    	assertEquals(97,in.readChar());
    	
    	assertEquals(10,in.readChar());
    	assertEquals(34,in.readChar());
    	assertEquals(34,in.readChar());
    	assertEquals(34,in.readChar());
    	try {
			in.readChar();
			fail("Expectend end");
		} catch (IOException e) {
			// expected
		}
	}
    public void testIt() throws Exception {
        String initialDoc = 
            "a\n" +
            "bc\n";
        StringReader inString = new StringReader(initialDoc);
        CharStream in = new ReaderCharStream(inString);
        doTests(in);

        inString = new StringReader(initialDoc);
        in = new ReaderCharStream(inString);
        doTests2(in);
        
        in = new FastCharStream(initialDoc.toCharArray());
        doTests(in);
        
        in = new FastCharStream(initialDoc.toCharArray());
        doTests2(in);
    }

    private void doTests2(CharStream in) throws IOException {
		assertEquals('a', in.readChar());
		assertEquals("a", in.GetImage());
		
    	assertEquals('\n',in.BeginToken());
    	assertEquals('b', in.readChar());
    	assertEquals("\nb", in.GetImage());
    	
    	in.backup(1);
    	assertEquals("\n", in.GetImage());
    	
    	assertEquals('b',in.BeginToken());
    	assertEquals("b", in.GetImage());
    	
    	assertEquals('c',in.BeginToken());
    	assertEquals("c", in.GetImage());
    	
    	assertEquals('\n',in.BeginToken());
    	assertEquals("\n", in.GetImage());
    	
    	try {
			in.BeginToken();
			fail("expected exception");
		} catch (IOException e) {
			// expected
		}
    	assertEquals("\n", in.GetImage());
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
        in.backup(0);
        assertEquals(3, in.getEndColumn());
        assertEquals(2, in.getEndLine());
        
        
        assertEquals("bc\n", new String(in.GetSuffix(3)));
        
        cs = new char[6];
        cs[1] = 'a';
        cs[2] = '\n';
        cs[3] = 'b';
        cs[4] = 'c';
        cs[5] = '\n';
        final String suf = new String(in.GetSuffix(6));
		assertEquals(new String(cs), suf);
        in.backup(4);
        
        assertEquals("a", in.GetImage());
        assertEquals('\n', in.readChar());
        assertEquals("a\n", in.GetImage());
        assertEquals(2, in.getEndColumn());
        assertEquals(1, in.getEndLine());
    }

    /**
     * @param in
     */
    private void checkStart(CharStream in, int line, int col) {
        assertEquals(1, in.getBeginColumn());
        assertEquals(1, in.getBeginLine());
    }

}
