package org.python.pydev.editor.correctionassist;

/**
 * 
 */
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
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

        TestEntry testData[] = { 
                new TestEntry("    def f(x, y,   z)  :", true),
                new TestEntry("def f( x='' ): #comment", true),
                new TestEntry("def f( x=\"\" ): #comment", true),
                new TestEntry("def f( x=[] ): #comment", true),
                new TestEntry("def f( x={a:1} ): #comment", true),
                new TestEntry("def f( x=1, *args, **kwargs ): #comment", true),
                new TestEntry("def f():", true),
                new TestEntry("def  f() : ", true),
                new TestEntry("def f( x ):", true),
                new TestEntry("def f( x ): #comment", true),
                new TestEntry("class X:", true),
                new TestEntry("class    X(sfdsf.sdf):", true),
                new TestEntry("clas    X(sfdsf.sdf):", false),
                new TestEntry("    class    X(sfdsf.sdf):", true),
                new TestEntry("class X():", true) };

        for (int i = 0; i < testData.length; i++) {
            Document d = new Document(testData[i].declaration);

            PySelection ps = new PySelection(d, new TextSelection(d, testData[i].declaration.length(), 0));
            String sel = PyAction.getLineWithoutComments(ps);
            boolean expected = testData[i].expectedResult;
            boolean isValid = assist.isValid(ps, sel, null,testData[i].declaration.length());
            assertEquals(StringUtils.format("Expected %s was %s sel: %s", expected, isValid, sel), expected, isValid);
        }
        
        
    }
    
    public void testApply() throws Exception {
        String expected = "def foo(a): #comment\r\n" +
                          "    '''\r\n" +
                          "    \r\n" +
                          "    @param a:\r\n" +
                          "    @type a:\r\n" +
                          "    '''";
        check(expected, "def foo(a): #comment");
        
        
        expected = "def f( x, ):\r\n" +
        "    '''\r\n" +
        "    \r\n" +
        "    @param x:\r\n" +
        "    @type x:\r\n" +
        "    '''";
        check(expected, "def f( x, ):");
        
        
        expected = "def f( x y ):\r\n" +
        "    '''\r\n" +
        "    \r\n" +
        "    '''";
        check(expected, "def f( x y ):");
        
        expected = "def f( (x,y=10) ):\r\n" +
        "    '''\r\n" +
        "    \r\n" +
        "    @param x:\r\n" +
        "    @type x:\r\n" +
        "    @param y:\r\n" +
        "    @type y:\r\n" +
        "    '''";
        check(expected, "def f( (x,y=10) ):");
        
        
        expected = "def f( , ):\r\n" +
        "    '''\r\n" +
        "    \r\n" +
        "    '''";
        check(expected, "def f( , ):" );
        
        
        expected = "def f( ):\r\n" +
        "    '''\r\n" +
        "    \r\n" +
        "    '''";
        check(expected, "def f( ):"    );
        
        
        expected = "def f(:\r\n" +
        "    '''\r\n" +
        "    \r\n" +
        "    '''";
        check(expected, "def f(:"     );
        
        check("def f):", "def f):", 0     );
        
        

    }

    private void check(String expected, String initial) throws BadLocationException {
        check(expected, initial, 1);
    }
    private void check(String expected, String initial, int proposals) throws BadLocationException {
        Document doc = new Document(initial);
        PySelection ps = new PySelection(doc, 0, doc.getLength());
        AssistDocString assist = new AssistDocString();
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, ps.getAbsoluteCursorOffset());
        assertEquals(proposals, props.size());
        if(props.size() > 0){
            props.get(0).apply(doc);
            assertEquals(expected.replace("\r\n", "\n"), doc.get().replace("\r\n", "\n"));
        }
    }
    

}
