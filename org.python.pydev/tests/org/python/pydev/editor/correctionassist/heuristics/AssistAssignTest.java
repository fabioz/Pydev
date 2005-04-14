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
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PySelection;

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

    /**
     * @throws BadLocationException
     * 
     */
    public void testIt() throws BadLocationException {
		String d = ""+
		"from testAssist import assist\n" +
		"assist.NewMethod(a,b)";

		Document doc = new Document(d);
		PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);
		assertEquals(true, assist.isValid(ps, sel));
		List props = assist.getProps(ps, null, null, null, null);
		assertEquals(2, props.size());

    }
}
