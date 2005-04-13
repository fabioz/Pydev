/*
 * Created on Apr 12, 2005
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
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.SourceModuleProposal;

/**
 * @author Fabio Zadrozny
 */
public class AssistCreateInModuleTest extends CodeCompletionTestsBase{

    private IAssistProps assist;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AssistCreateInModuleTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        restorePythonPath(false);
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
    public void testAssistMethod() throws BadLocationException {
        assist = new AssistCreateMethodInModule();
		String d = ""+
		"from testAssist import assist\n" +
		"assist.NewMethod(a,b)";

		Document doc = new Document(d);
		PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

		assertEquals(true, assist.isValid(ps, sel));
		List props = assist.getProps(ps, null, null, nature, null);
		assertEquals(1, props.size());
		SourceModuleProposal p = (SourceModuleProposal) props.get(0);
		
		String res = "\n" +
		"def NewMethod(a,b):\n" +
		"    '''\n"+
		"    @param a:\n"+
		"    @param b:\n"+
		"    '''\n"+
		"    ";
		
		assertEquals(res, p.getReplacementStr());
		assertEquals("testAssist.assist", p.module.getName());
    }

    /**
     * @throws BadLocationException
     * 
     */
    public void testAssistClass() throws BadLocationException {
        assist = new AssistCreateClassInModule();
		String d = ""+
		"from testAssist import assist\n" +
		"assist.NewClass(a,b)";

		Document doc = new Document(d);
		PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

		assertEquals(true, assist.isValid(ps, sel));
		List props = assist.getProps(ps, null, null, nature, null);
		assertEquals(1, props.size());
		SourceModuleProposal p = (SourceModuleProposal) props.get(0);
		
		String res = "\n" +
		"class NewClass(object):\n" +
		"    '''\n"+
		"    '''\n"+
		"    \n"+
		"    def __init__(self, a, b):\n"+
		"        '''\n"+
		"        @param a:\n"+
		"        @param b:\n"+
		"        '''\n"+
		"        ";
		assertEquals(res, p.getReplacementStr());
		assertEquals("testAssist.assist", p.module.getName());
    }

    /**
     * @throws BadLocationException
     * 
     */
    public void testAssistMethodInClass() throws BadLocationException {
        assist = new AssistCreateMethodInClass();
		String d = ""+
		"from testAssist import assist\n" +
		"ex = assist.ExistingClass()\n" +
		"ex.newMethod(c,d)";

		Document doc = new Document(d);
		PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

		assertEquals(true, assist.isValid(ps, sel));
		List props = assist.getProps(ps, null, null, nature, null);
		assertEquals(1, props.size());
		SourceModuleProposal p = (SourceModuleProposal) props.get(0);
		
		String res = "\n" +
		"    def newMethod(self, c, d):\n"+
		"        '''\n"+
		"        @param c:\n"+
		"        @param d:\n"+
		"        '''\n"+
		"        ";
		assertEquals(res, p.getReplacementStr());
		assertEquals("testAssist.assist", p.module.getName());
		assertEquals("ExistingClass", p.className);
		assertEquals(SourceModuleProposal.ADD_TO_LAST_CLASS_LINE, p.addTo);
    }
}
