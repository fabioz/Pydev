/*
 * Created on Feb 1, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.plugin.PythonNature;

/**
 * Tests here have no dependency on the pythonpath.
 * 
 * @author Fabio Zadrozny
 */
public class ASTManagerTest extends TestCase {

    private CompletionState state;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ASTManagerTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCompletion(){
        ASTManager manager = new ASTManager();
        PythonNature nature = new PythonNature();
        String token = "C";
        int line = 6;
        int col = 11;
        
		String sDoc = ""+
		"class C:             \n" +  
		"                     \n" +    
		"    def makeit(self):\n" +     
		"        pass         \n" +     
		"                     \n" +       
		"class D(C.:          \n" +  
		"                     \n" +    
		"    def a(self):     \n" +   
		"        pass         \n";
		
        Document doc = new Document(sDoc);
        IToken[] comps = null;
        state = new CompletionState(line,col, token, nature);
        comps = manager.getCompletionsForToken(doc, state);
        assertEquals(1, comps.length);
        assertEquals("makeit", comps[0].getRepresentation());
        
        
		sDoc = ""+
		"import unittest       \n" +          
		"                      \n" +    
		"class Classe1:        \n" +       
		"                      \n" +          
		"    def makeit(self): \n" +          
		"        self.makeit   \n" +             
		"                      \n" +      
		"                      \n" +       
		"class Test(unit       \n";
		
		line = 8;
		col = 16;
		token = "";
		doc = new Document(sDoc);
        state = new CompletionState(line,col, token, nature);
        comps = manager.getCompletionsForToken(doc, state);
        assertEquals(3, comps.length);
        assertIsIn("unittest", comps);
        assertIsIn("Classe1", comps);
        assertIsIn("Test", comps);
        
		sDoc = ""+
		"import unittest       \n" +          
		"                      \n" +    
		"class Classe1:        \n" +       
		"                      \n" +          
		"    def makeit(self): \n" +          
		"        self.makeit   \n" +             
		"                      \n" +      
		"                      \n" +       
		"class Test(unit       \n" +
		"                      \n" +
		"def meth1():          \n" +
		"    pass              \n";
		
		line = 8;
		col = 16;
		token = "";
		doc = new Document(sDoc);
        state = new CompletionState(line,col, token, nature);
        comps = manager.getCompletionsForToken(doc, state);
        assertEquals(4, comps.length);
        assertIsIn("unittest", comps);
        assertIsIn("Classe1", comps);
        assertIsIn("Test", comps);
        assertIsIn("meth1", comps);
        
		sDoc = ""+
		"import unittest       \n" +          
		"                      \n" +    
		"class Classe1:        \n" +       
		"                      \n" +          
		"    def makeit(self): \n" +          
		"        self.makeit   \n" +             
		"                      \n" +      
		"                      \n" +       
		"class Test(unit       \n" +
		"                      \n" +
		"    def meth1():      \n" +
		"        pass          \n";
		
		line = 8;
		col = 16;
		token = "";
		doc = new Document(sDoc);
        state = new CompletionState(line,col, token, nature);
        comps = manager.getCompletionsForToken(doc, state);
        assertEquals(3, comps.length);
        assertIsIn("unittest", comps);
        assertIsIn("Classe1", comps);
        assertIsIn("Test", comps);
        
		sDoc = ""+
		"class Classe1:       \n" +       
		"                     \n" +         
		"    def foo(self):   \n" +          
		"        self.a = 1   \n" +      
		"        self.        \n" +      
		"                     \n";
		
		line = 5;
		col = 13;
		token = "Classe1";
		doc = new Document(sDoc);
        state = new CompletionState(line,col, token, nature);
        comps = manager.getCompletionsForToken(doc, state);
        assertEquals(2, comps.length);
        assertIsIn("a", comps);
        assertIsIn("foo", comps);
        
		sDoc = ""+
		"class Classe1:       \n" +       
		"                     \n" +         
		"    def foo(self):   \n" +          
		"        self.a = 2   \n" +      
		"                     \n" +      
		"    test = foo       \n" +      
		"                     \n" +      
		"Classe1.             \n";        
		
		line = 8;
		col = 9;
		token = "Classe1";
		doc = new Document(sDoc);
        state = new CompletionState(line,col, token, nature);
        comps = manager.getCompletionsForToken(doc, state);
        assertEquals(3, comps.length);
        assertIsIn("foo", comps);
        assertIsIn("a", comps);
        assertIsIn("test", comps);
        
    }

    /**
     * @param string
     * @param comps
     */
    public static void assertIsIn(String string, IToken[] comps) {
        boolean found = false;
        for (int i = 0; i < comps.length; i++) {
            if(string.equals(comps[i].getRepresentation())){
                found = true;
            }
        }
        assertTrue("The searched token ("+string+")was not found in the completions", found);
    }
    
}
