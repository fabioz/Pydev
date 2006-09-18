/*
 * Created on Apr 13, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.actions.PyAction;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class AssistAssignTest extends TestCase {

    private static final boolean DEBUG = false;
	private AssistAssign assist;

    public static void main(String[] args) {
        try{
            AssistAssignTest test = new AssistAssignTest();
            test.setUp();
            test.testSimple4();
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
        assist = new AssistAssign();
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
		List props = assist.getProps(ps, null, null, null, null, d.length());
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
    
    public void testSimple3() throws BadLocationException {
        String d = ""+
        "from testAssist import assist\n" +
        "a = assist.NewMethod(a,b)";
        
        Document doc = new Document(d);
        
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);
        
        assertEquals(false, assist.isValid(ps, sel, null, d.length()));
        
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
