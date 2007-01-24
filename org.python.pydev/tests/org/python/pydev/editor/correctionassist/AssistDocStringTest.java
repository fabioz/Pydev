package org.python.pydev.editor.correctionassist;

/**
 * 
 */
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.correctionassist.docstrings.AssistDocString;

public class AssistDocStringTest extends TestCase {
    private AssistDocString assist;
    
    protected void setUp() throws Exception {
        super.setUp();
        assist = new AssistDocString();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }


	/**
	 * Testing the method isValid()
	 */
	public void testIsValid() {
		/**
		 * Dummy class for keeping data together.
		 */
		class TestEntry {
			public TestEntry(String declaration, boolean expectedResult) {
				this.declaration = declaration;
				this.expectedResult = expectedResult;
			}

			public String declaration;

			public boolean expectedResult;
		};

		TestEntry testData[] = { new TestEntry("def f():", true),
				new TestEntry("def  f() : ", true),
				new TestEntry("def f( x ):", true),
				new TestEntry("def f( x ): #comment", true),
				new TestEntry("def f( x, ):", false),
				new TestEntry("def f( ,x ):", false),
				new TestEntry("def f( x y ):", false),
				new TestEntry("def f( , ):", false),
				new TestEntry("def f( )", false),
				new TestEntry("def f(:", false),
				new TestEntry("def f):", false),
				new TestEntry("	def f(x, y,   z)  :", true),
				new TestEntry("class X:", true),
				new TestEntry("class    X(sfdsf.sdf):", true),
				new TestEntry("clas    X(sfdsf.sdf):", false),
				new TestEntry("	class    X(sfdsf.sdf):", false),
				new TestEntry("class X():", false) };

		for (int i = 0; i < testData.length; i++) {
			Document d = new Document(testData[i].declaration);

			PySelection ps = new PySelection(d, new TextSelection(d, testData[i].declaration.length(), 0));
			String sel = PyAction.getLineWithoutComments(ps);
			assertEquals(testData[i].expectedResult, assist.isValid(ps, sel, null,testData[i].declaration.length()));
		}
        
        assertTrue(AssistDocString.FunctionPattern.matcher("def f( x ): #comment").matches());
        assertTrue(AssistDocString.ClassPattern.matcher("class A( object ): #comment").matches());
        
	}
    
    public void testApply() throws Exception {
        Document doc = new Document("def foo(a): #comment");
        PySelection ps = new PySelection(doc, 0, doc.getLength());
        AssistDocString assist = new AssistDocString();
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, ps.getAbsoluteCursorOffset());
        assertEquals(1, props.size());
        props.get(0).apply(doc);
        assertEquals("def foo(a): #comment\r\n" +
                "    '''\r\n" +
                "    \r\n" +
                "    @param a:\r\n" +
                "    @type a:\r\n" +
                "    '''", doc.get());
    }

}
