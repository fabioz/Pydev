package org.python.pydev.editor.codecompletion;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;

public class PythonCompletionCalltipsTest  extends CodeCompletionTestsBase {

    public static void main(String[] args) {
        
      try {
          //DEBUG_TESTS_BASE = true;
          PythonCompletionCalltipsTest test = new PythonCompletionCalltipsTest();
          test.setUp();
          test.testCalltips6();
          test.tearDown();
          System.out.println("Finished");

          junit.textui.TestRunner.run(PythonCompletionCalltipsTest.class);
      } catch (Exception e) {
          e.printStackTrace();
      } catch(Error e){
          e.printStackTrace();
      }
    }

    /*
     * @see TestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(false);
        codeCompletion = new PyCodeCompletion();
    }

    /*
     * @see TestCase#tearDown()
     */
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
    }


    public void testCalltips1() throws CoreException, BadLocationException {
        String s;
        s = "" +
        "GLOBAL_VAR = 10\n" + //this variable should not show in the return
        "def m1(a, b):\n" +
        "    print a, b\n" +
        "\n" +
        "m1(a, b)"; //we'll request a completion inside the parentesis to check for calltips. For calltips, we
                //should get the activation token as an empty string and the qualifier as "m1", 
                //so, the completion that should return is "m1(a, b)", with the information context
                //as "a, b". 
                //
                //
                //
                //The process of getting the completions actually starts at:
                //org.python.pydev.editor.codecompletion.PyCodeCompletion#getCodeCompletionProposals
        
        ICompletionProposal[] proposals = requestCompl(s, s.length()-5, -1, new String[] {});
        
        
        if(false){ //make true to see which proposals were returned.
            for (ICompletionProposal proposal : proposals) {
                System.out.println(proposal.getDisplayString());
            }
        }
        
        assertEquals(1, proposals.length); //now, here's the first part of the failing test: we can only have one
                                           //returned proposal: m1(a, b)
        
        //check if the returned proposal is there
        ICompletionProposal prop = proposals[0];
        assertEquals("m1(a, b)", prop.getDisplayString());
        PyCompletionProposal p4 = (PyCompletionProposal) prop;
        assertTrue(p4.isAutoInsertable());
        assertEquals(PyCompletionProposal.ON_APPLY_JUST_SHOW_CTX_INFO, p4.onApplyAction);
        
        //the display string for the context 'context' and 'information' should be the same
        PyCalltipsContextInformation contextInformation = (PyCalltipsContextInformation) prop.getContextInformation();
        assertEquals("a, b", contextInformation.getContextDisplayString());
        assertEquals("a, b", contextInformation.getInformationDisplayString());
        
        //now, this proposal has one interesting thing about it: it should actually not change the document
        //where it is applied (it is there just to show the calltip). 
        //
        //To implement that, when we see that it is called inside some parenthesis, we should create a subclass of 
        //PyCompletionProposal that will have its apply method overriden, so that nothing happens here (the calltips will
        //still be shown)
        Document doc = new Document();
        prop.apply(doc);
        assertEquals("", doc.get());
    }


    
    public void testCalltips2() throws CoreException, BadLocationException {
        String s;
        s = "" +
        "GLOBAL_VAR = 10\n" + 
        "def m1(a, b):\n" +
        "    print a, b\n" +
        "def m1Other(a, b):\n" + //this one should not show, as we're returning it for calltip purposes only 
        "    print a, b\n" +
        "\n" +
        "m1()"; 
        ICompletionProposal[] proposals = requestCompl(s, s.length()-1, -1, new String[] {"m1(a, b)"});
        assertEquals(1, proposals.length); 
    }
    
    public void testCalltips3() throws CoreException, BadLocationException {
        String s;
        s = "" +
        "def m1(a, b):\n" +
        "    print a, b\n" +
        "m1()";  
        PyContextInformationValidator validator = new PyContextInformationValidator();
        int requestOffset = s.length()-1;
        ICompletionProposal[] proposals = requestCompl(s, requestOffset, -1, new String[] {});
        assertEquals(1, proposals.length); 
        PyCalltipsContextInformation contextInformation = (PyCalltipsContextInformation) proposals[0].getContextInformation();
        
        validator.install(contextInformation, new Document(s), requestOffset);
        assertFalse(validator.isContextInformationValid(0));
        assertTrue(validator.isContextInformationValid(requestOffset));
        assertFalse(validator.isContextInformationValid(requestOffset+1));
    }
    
    public void testCalltips4() throws CoreException, BadLocationException {
        String s;
        s = "" +
        "def m1(a, b):\n" +
        "    print a, b\n" +
        "m1(a,b)";  
        int requestOffset = s.length()-4;
        ICompletionProposal[] proposals = requestCompl(s, requestOffset, -1, new String[] {});
        assertEquals(1, proposals.length); 
        PyContextInformationValidator validator = new PyContextInformationValidator();
        PyCalltipsContextInformation contextInformation = (PyCalltipsContextInformation) proposals[0].getContextInformation();
        
        validator.install(contextInformation, new Document(s), requestOffset);
        assertFalse(validator.isContextInformationValid(requestOffset-1));
        assertTrue(validator.isContextInformationValid(requestOffset));
        assertTrue(validator.isContextInformationValid(requestOffset+3));
        assertFalse(validator.isContextInformationValid(requestOffset+4));
    }

    public void testCalltips5() throws Exception {
        String s0 = 
            "class TestCase(object):\n" +
            "    def __init__(self, a, b):\n" +
            "        pass\n" +
            "    \n" +
            "TestCase(%s)";
        
        String s = StringUtils.format(s0, "");
        ICompletionProposal[] proposals = requestCompl(s, s.length()-1, -1, new String[] {});
        assertEquals(1, proposals.length); 
        PyCompletionProposal p = (PyCompletionProposal) proposals[0];
        assertEquals("TestCase(a, b)", p.getDisplayString());
        
        
        Document document = new Document(s);
        p.apply(document);
        assertEquals(StringUtils.format(s0, "a, b"), document.get());
    }
    
    public void testCalltips6() throws Exception {
        String s = 
            "from extendable import calltips\n" +
            "calltips.";
        
        requestCompl(s, s.length(), 1, new String[] {"method1(a, b)"});
    }
        
}
