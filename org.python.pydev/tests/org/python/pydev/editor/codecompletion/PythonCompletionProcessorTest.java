/*
 * Created on Mar 8, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;

/**
 * @author Fabio Zadrozny
 */
public class PythonCompletionProcessorTest extends CodeCompletionTestsBase {

    private PyCodeCompletion codeCompletion;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PythonCompletionProcessorTest.class);
//      try {
//          PythonCompletionProcessorTest test = new PythonCompletionProcessorTest();
//	      test.setUp();
//	      test.testCompleteCompletion();
//	      test.tearDown();
//	  } catch (Exception e) {
//	      e.printStackTrace();
//	  } catch(Error e){
//	      e.printStackTrace();
//	  }
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        codeCompletion = new PyCodeCompletion(false);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    
	public void testCompleteCompletion() throws CoreException, BadLocationException{
        IDocument doc = new Document("import testl");
        int documentOffset = 12;
        CompletionRequest request = new CompletionRequest(null, 
                nature, doc, documentOffset,
                codeCompletion);

        List props = codeCompletion.getCodeCompletionProposals(request);
        ICompletionProposal[] codeCompletionProposals = codeCompletion.onlyValidSorted(props, request.qualifier);
        assertEquals(1, codeCompletionProposals.length);
        assertEquals("testlib", codeCompletionProposals[0].getDisplayString());
        
    }
    
    public void testGetActTok(){
        String strs[];
        
        strs = codeCompletion.getActivationTokenAndQual(new Document(""), 0);
        assertEquals("", strs[0]);
        assertEquals("", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("self.assertEquals( DECAY_COEF, t.item(0, C).text())"), 42);
        assertEquals("" , strs[0]);
        assertEquals("C", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("self.assertEquals( DECAY_COEF, t.item(0,C).text())"), 41);
        assertEquals("" , strs[0]);
        assertEquals("C", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("m = met(self.c, self.b)"), 14);
        assertEquals("self." , strs[0]);
        assertEquals("c", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("[a,b].ap"), 8);
        assertEquals("list." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("{a:1,b:2}.ap"), 12);
        assertEquals("dict." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("''.ap"), 5);
        assertEquals("str." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("\"\".ap"), 5);
        assertEquals("str." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("ClassA.someMethod.ap"), 20);
        assertEquals("ClassA.someMethod." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("ClassA.someMethod().ap"), 22);
        assertEquals("ClassA.someMethod()." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("ClassA.someMethod( a, b ).ap"), 28);
        assertEquals("ClassA.someMethod()." , strs[0]);
        assertEquals("ap", strs[1]);
        
        
    }


}
