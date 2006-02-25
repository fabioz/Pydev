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

    private AssistAssign assist;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AssistAssignTest.class);
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
    
    
    public void testSimple3() throws BadLocationException {
        String d = ""+
        "from testAssist import assist\n" +
        "a = assist.NewMethod(a,b)";
        
        Document doc = new Document(d);
        
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);
        
        assertEquals(false, assist.isValid(ps, sel, null, d.length()));
        
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

    private void assertContains(String string, List<ICompletionProposal> props) {
        for (ICompletionProposal proposal : props) {
            System.out.println(proposal.getDisplayString());
            if(proposal.getDisplayString().equals(string)){
                return;
            }
        }
        fail("not found");
    }
}
