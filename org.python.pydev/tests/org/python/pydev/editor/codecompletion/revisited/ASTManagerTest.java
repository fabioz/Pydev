/*
 * Created on Feb 1, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.plugin.BundleInfo;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * Tests here have no dependency on the pythonpath.
 * 
 * @author Fabio Zadrozny
 */
public class ASTManagerTest extends CodeCompletionTestsBase {

    private CompletionState state;
    private String token;
    private int line;
    private int col;
    private String sDoc;
    private Document doc;
    private IToken[] comps = null;


    /**
     * @return Returns the manager.
     */
    private ICodeCompletionASTManager getManager() {
        return (ICodeCompletionASTManager) nature.getAstManager();
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;

        PydevPlugin.setPythonInterpreterManager(new PythonInterpreterManagerStub(preferences));
        nature = createNature();
        ASTManager manager = new ASTManager();
        nature.setAstManager(manager);
        manager.setNature(nature);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        BundleInfo.setBundleInfo(null);
    }

    public void testCompletion(){
        token = "C";
        line = 6;
        col = 11;
        sDoc = ""+
        		"class C:             \n" +  
        		"                     \n" +    
        		"    def makeit(self):\n" +     
        		"        pass         \n" +     
        		"                     \n" +       
        		"class D(C.:          \n" +  
        		"                     \n" +    
        		"    def a(self):     \n" +   
        		"        pass         \n";
        doc = new Document(sDoc);
        state = new CompletionState(line,col, token, nature);
        comps = getManager().getCompletionsForToken(doc, state);
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
        comps = getManager().getCompletionsForToken(doc, state);
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
        comps = getManager().getCompletionsForToken(doc, state);
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
        comps = getManager().getCompletionsForToken(doc, state);
        assertEquals(3, comps.length);
        assertIsIn("unittest", comps);
        assertIsIn("Classe1", comps);
        assertIsIn("Test", comps);
        
		sDoc = ""+
		"class Classe1:       \n" +       
		"                     \n" +         
		"    def foo(self):   \n" +
		"        ignoreThis=0 \n" +          
		"        self.a = 1   \n" +      
		"        self.        \n" +      
		"                     \n";
		
		line = 6;
		col = 13;
		token = "Classe1";
		doc = new Document(sDoc);
        state = new CompletionState(line,col, token, nature);
        comps = getManager().getCompletionsForToken(doc, state);
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
        comps = getManager().getCompletionsForToken(doc, state);
        assertEquals(3, comps.length);
        assertIsIn("foo", comps);
        assertIsIn("a", comps);
        assertIsIn("test", comps);

        
        
		sDoc = ""+
		"class LinkedList:                      \n"+
		"    def __init__(self,content='Null'): \n" +
		"        if not content:                \n"+
		"            self.first=content         \n"+
		"            self.last=content          \n"+
		"        else:                          \n"+
		"            self.first='Null'          \n"+
		"            self.last='Null'           \n"+
		"        self.content=content           \n"+
		"        self.                          \n";
		
		line = 9;
		col = 9;
		token = "LinkedList";
		doc = new Document(sDoc);
        state = new CompletionState(line,col, token, nature);
        comps = getManager().getCompletionsForToken(doc, state);
        assertIsIn("first", comps);
        assertIsIn("last", comps);
        assertIsIn("content", comps);
        
    }

    public void testRecursion(){
        token = "B";
        line = 0;
        col = 0;
        sDoc = ""+
			"class A(B):pass          \n" +    
			"class B(A):pass          \n";
        doc = new Document(sDoc);
        state = new CompletionState(line,col, token, nature);
        comps = getManager().getCompletionsForToken(doc, state);
        assertEquals(0, comps.length); //no tokens returned
        
    }
    
    public void testRelative(){
        super.restorePythonPath(false);
        token = "Test1";
        line = 1;
        col = 0;
        sDoc = ""+
			"from testlib.unittest.relative import Test1 \n" +    
			"\n";
        doc = new Document(sDoc);
        state = new CompletionState(line,col, token, nature);
        comps = getManager().getCompletionsForToken(doc, state);
        assertEquals(1, comps.length);
        assertIsIn("test1", comps);
        
    }
    
    public void testLocals(){
        token = "";
        line = 2;
        sDoc = ""+
            "contentsCopy = applicationDb.getContentsCopy()\n" +    
            "database.Database.fromContentsCopy(self, cont)";
        col = sDoc.length()-3;
        doc = new Document(sDoc);
        state = new CompletionState(line,col, token, nature);
        comps = getManager().getCompletionsForToken(doc, state);
        assertEquals(1, comps.length );
        assertIsIn("contentsCopy", comps);
    }
    
    public void testLocals2(){
        token = "";
        line = 2;
        col = 10;
        sDoc = ""+
        		"def met(par1, par2):          \n" +    
        		"    print                     \n";
        doc = new Document(sDoc);
        state = new CompletionState(line,col, token, nature);
        comps = getManager().getCompletionsForToken(doc, state);
        assertEquals(3, comps.length );
        assertIsIn("par1", comps);
        assertIsIn("par2", comps);
        assertIsIn("met", comps);

    
        token = "";
        line = 3;
        col = 13;
        sDoc = ""+
        		"class C:                         \n" +    
        		"    def met(self, par1, par2):   \n" +    
        		"        print                    \n";
        doc = new Document(sDoc);
        state = new CompletionState(line,col, token, nature);
        comps = getManager().getCompletionsForToken(doc, state);
        assertEquals(4, comps.length );
        assertIsIn("par1", comps);
        assertIsIn("par2", comps);
        assertIsIn("self", comps);
        assertIsIn("C", comps);

        
        token = "";
        line = 4;
        col = 13;
        sDoc = ""+
        		"class C:                         \n" +    
        		"    def met(self, par1, par2):   \n" +
        		"        loc1 = 10                \n" +    
        		"        print                    \n";
        doc = new Document(sDoc);
        state = new CompletionState(line,col, token, nature);
        comps = getManager().getCompletionsForToken(doc, state);
        assertEquals(5, comps.length );
        assertIsIn("par1", comps);
        assertIsIn("loc1", comps);
        assertIsIn("par2", comps);
        assertIsIn("self", comps);
        assertIsIn("C", comps);

        
        token = "";
        line = 4;
        col = 13;
        sDoc = ""+
        		"class C:                         \n" +    
        		"    def met(self, par1, par2):   \n" +
        		"        loc1 = 10                \n" +    
        		"        print                    \n" +
        		"        ignoreLineAfter = 5      \n";
        doc = new Document(sDoc);
        state = new CompletionState(line,col, token, nature);
        comps = getManager().getCompletionsForToken(doc, state);
        assertEquals(5, comps.length );
        assertIsIn("par1", comps);
        assertIsIn("loc1", comps);
        assertIsIn("par2", comps);
        assertIsIn("self", comps);
        assertIsIn("C", comps);
    }
    
    
    /**
     * @param string
     * @param comps
     */
    public static void assertIsIn(String string, IToken[] comps) {
    	StringBuffer buffer = new StringBuffer("Available: \n");
        boolean found = false;
        for (int i = 0; i < comps.length; i++) {
            String rep = comps[i].getRepresentation();
			if(string.equals(rep)){
                found = true;
            }
			buffer.append(rep);
			buffer.append("\n");
        }
        
        assertTrue("The searched token ("+string+")was not found in the completions. "+buffer, found);
    }
    
    public static void main(String[] args)  {
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        

        try {
            ASTManagerTest test = new ASTManagerTest();
            test.setUp();
            test.testLocals();
            test.tearDown();

            junit.textui.TestRunner.run(ASTManagerTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
