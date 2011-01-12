package com.python.pydev.refactoring.tdd;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestCaseUtils;
import org.python.pydev.refactoring.core.base.RefactoringInfo;

public class PyCreateMethodTest extends TestCaseUtils {
    
    public static void main(String[] args) {
        try {
            PyCreateMethodTest test = new PyCreateMethodTest();
            test.setUp();
            test.testPyCreateMethodInClass();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyCreateMethodTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testPyCreateMethodGlobal() {
        PyCreateMethod pyCreateMethod = new PyCreateMethod();
        
        String source = "MyMethod()";
        IDocument document = new Document(source);
        ITextSelection selection = new TextSelection(document, 0, 0);
        RefactoringInfo info = new RefactoringInfo(document, selection, new IGrammarVersionProvider() {
            
            public int getGrammarVersion() throws MisconfigurationException {
                return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
            }
        });

        pyCreateMethod.execute(info, PyCreateAction.LOCATION_STRATEGY_BEFORE_CURRENT);
        
        assertContentsEqual("" +
                "def MyMethod():\n" +
                "    ${pass}${cursor}\n" +
                "\n" +
                "\n" +
                "MyMethod()" +
                "", document.get());
    }
    
    public void testPyCreateMethodGlobalParams() {
        PyCreateMethod pyCreateMethod = new PyCreateMethod();
        
        String source = "MyMethod(a, b())";
        IDocument document = new Document(source);
        ITextSelection selection = new TextSelection(document, 0, 0);
        RefactoringInfo info = new RefactoringInfo(document, selection, new IGrammarVersionProvider() {
            
            public int getGrammarVersion() throws MisconfigurationException {
                return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
            }
        });
        
        pyCreateMethod.execute(info, PyCreateAction.LOCATION_STRATEGY_BEFORE_CURRENT);
        
        assertContentsEqual("" +
                "def MyMethod(${a}, ${b}):\n" +
                "    ${pass}${cursor}\n" +
                "\n" +
                "\n" +
                "MyMethod(a, b())" +
                "", document.get());
    }
    
    public void testPyCreateMethodInClass() {
        PyCreateMethod pyCreateMethod = new PyCreateMethod();
        
        String source = "" +
        		"class A(object):\n" +
        		"    '''comment'''\n" +
        		"\n" +
        		"A.MyMethod(a, b())";
        IDocument document = new Document(source);
        ITextSelection selection = new TextSelection(document, document.getLength()-"hod(a, b())".length(), 0);
        RefactoringInfo info = new RefactoringInfo(document, selection, new IGrammarVersionProvider() {
            
            public int getGrammarVersion() throws MisconfigurationException {
                return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
            }
        });
        
        pyCreateMethod.setCreateInClass("A");
        pyCreateMethod.setCreateAs(PyCreateMethod.CLASSMETHOD);
        pyCreateMethod.execute(info, PyCreateAction.LOCATION_STRATEGY_END);
        
        assertContentsEqual("" +
                "" +
                "class A(object):\n" +
                "    '''comment'''\n" +
                "    \n" +
                "    @classmethod\n" +
                "    def MyMethod(cls, ${a}, ${b}):\n" +
                "        ${pass}${cursor}\n" +
                "    \n" +
                "    \n" +
                "\n" +
                "A.MyMethod(a, b())" +
                "", document.get());
    }
    
}
