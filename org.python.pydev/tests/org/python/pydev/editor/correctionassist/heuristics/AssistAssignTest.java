/*
 * Created on Apr 13, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.codingstd.ICodingStd;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.actions.PyAction;

/**
 * @author Fabio Zadrozny
 */
public class AssistAssignTest extends TestCase {


    
    static class NonCamelCodingStd implements ICodingStd{

        public boolean localsAndAttrsCamelcase() {
            return false;
        }
        
    }
    
    
    static class CamelCodingStd implements ICodingStd{
        
        public boolean localsAndAttrsCamelcase() {
            return true;
        }
        
    }

    
    private static final boolean DEBUG = false;
	private AssistAssign assist;

    public static void main(String[] args) {
        try{
            AssistAssignTest test = new AssistAssignTest();
            test.setUp();
            test.testSimple9();
            test.tearDown();
            junit.textui.TestRunner.run(AssistAssignTest.class);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        assist = new AssistAssign(new CamelCodingStd());
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSimple() throws BadLocationException {
		String d = ""+
		"from testAssist import assist\n" +
		"assist.NewMethod(a,b)";

		Document doc = new Document(d);
		
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);
        
		assertEquals(true, assist.isValid(ps, sel, null, d.length()));
		List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
		assertEquals(2, props.size());
        
    }
    
    
    public void testSimple2() throws BadLocationException {
        String d = ""+
        "from testAssist import assist\n" +
        "assist.NewMethod(a = 1, b = 2)";
        
        Document doc = new Document(d);
        
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);
        
        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (newMethod)", props);
    }
    
    public void testSimpleUnderline() throws BadLocationException {
        String d = ""+
        "from testAssist import assist\n" +
        "assist._NewMethod(a = 1, b = 2)";
        
        Document doc = new Document(d);
        
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);
        
        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (_newMethod)", props);
    }
    
    public void testSimple3() throws BadLocationException {
        String d = ""+
        "from testAssist import assist\n" +
        "a = assist.NewMethod(a,b)";
        
        Document doc = new Document(d);
        
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);
        
        assertEquals(false, assist.isValid(ps, sel, null, d.length()));
        
    }
        
    public void testCodingStd() throws BadLocationException {
        assist = new AssistAssign(new NonCamelCodingStd());
        String d = ""+
        "from testAssist import assist\n" +
        "assist.NewMethod(a = 1, b = 2)";
        
        Document doc = new Document(d);
        
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);
        
        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (new_method)", props);
    }
    
    public void testCodingStd2() throws BadLocationException {
        assist = new AssistAssign(new NonCamelCodingStd());
        String d = ""+
        "from testAssist import assist\n" +
        "assist._NewMethod(a = 1, b = 2)";
        
        Document doc = new Document(d);
        
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);
        
        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (_new_method)", props);
    }
    
    public void testSimple4() throws BadLocationException {
        String d = ""+
        "def m1():\n" +
        "   foo";
        
        Document doc = new Document(d);
        
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);
        
        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (foo)", props);
    }
    
    public void testSimple5() throws BadLocationException {
        String d = ""+
        "def m1():\n" +
        "   1+1";
        
        Document doc = new Document(d);
        
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);
        
        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (result)", props);
    }
    
    public void testSimple6() throws BadLocationException {
        String d = ""+
        "def m1():\n" +
        "   a = 1";
        
        Document doc = new Document(d);
        
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);
        
        assertEquals(false, assist.isValid(ps, sel, null, d.length()));
    }

    public void testSimple7() throws BadLocationException {
        String d = ""+
        "def m1():\n" +
        "   ALL_UPPERCASE";
        
        Document doc = new Document(d);
        
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);
        
        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (all_uppercase)", props);
    }
    
    public void testSimple8() throws BadLocationException {
        assist = new AssistAssign(new NonCamelCodingStd());
        
        String d = ""+
        "def m1():\n" +
        "   IKVMClass";
        
        Document doc = new Document(d);
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);
        
        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (ikvmclass)", props);
    }
    
    public void testSimple9() throws BadLocationException {
        assist = new AssistAssign(new NonCamelCodingStd());
        
        String d = ""+
        "def m1():\n" +
        "   IKVMClassBBBar";
        
        Document doc = new Document(d);
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);
        
        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (ikvmclass_bbbar)", props);
    }
    
    
    private void assertContains(String string, List<ICompletionProposal> props) {
        StringBuffer buffer = new StringBuffer("Available: \n");
        
        for (ICompletionProposal proposal : props) {
        	if(DEBUG){
        		System.out.println(proposal.getDisplayString());
        	}
            if(proposal.getDisplayString().equals(string)){
                return;
            }
            buffer.append(proposal.getDisplayString());
            buffer.append("\n");
        }
        fail(string+" not found. "+buffer);
    }
}
