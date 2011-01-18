package com.python.pydev.refactoring.tdd;

import java.util.ArrayList;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestCaseUtils;
import org.python.pydev.refactoring.core.base.RefactoringInfo;

public class PyCreateClassTest extends TestCaseUtils {

    public static void main(String[] args) {
        try {
            PyCreateClassTest test = new PyCreateClassTest();
            test.setUp();
            test.testPyCreateClassWithParameters();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyCreateClassTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    

    public void testPyCreateClassInSameModule() throws Exception {
        PyCreateClass pyCreateClass = new PyCreateClass();
        
        String source = "MyClass()";
        IDocument document = new Document(source);
        ITextSelection selection = new TextSelection(document, 0, 0);
        RefactoringInfo info = new RefactoringInfo(document, selection, new IGrammarVersionProvider() {
            
            public int getGrammarVersion() throws MisconfigurationException {
                return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
            }
        });

        pyCreateClass.execute(info, PyCreateClass.LOCATION_STRATEGY_BEFORE_CURRENT);
        
        assertContentsEqual("" +
        		"class MyClass(${object}):\n" +
        		"    ${pass}${cursor}\n" +
        		"\n" +
        		"\n" +
        		"MyClass()" +
        		"", document.get());
    }
    
    
    public void testPyCreateClassWithParameters() throws Exception {
        PyCreateClass pyCreateClass = new PyCreateClass();
        
        String source = "MyClass(aa, bb, 10)";
        IDocument document = new Document(source);
        ITextSelection selection = new TextSelection(document, 0, 0);
        RefactoringInfo info = new RefactoringInfo(document, selection, new IGrammarVersionProvider() {
            
            public int getGrammarVersion() throws MisconfigurationException {
                return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
            }
        });
        
        pyCreateClass.execute(info, PyCreateClass.LOCATION_STRATEGY_BEFORE_CURRENT);
        
        assertContentsEqual("" +
                "class MyClass(${object}):\n" +
                "    \n" +
                "    def __init__(self, ${aa}, ${bb}, ${param2}):\n" +
                "        ${pass}${cursor}\n" +
                "\n" +
                "\n" +
                "MyClass(aa, bb, 10)" +
                "", document.get());
    }
    
    public void testPyCreateClassWithParameters2() throws Exception {
        PyCreateClass pyCreateClass = new PyCreateClass();
        
        String source = "MyClass(aa, bb, MyFoo())";
        IDocument document = new Document(source);
        ITextSelection selection = new TextSelection(document, 0, 0);
        RefactoringInfo info = new RefactoringInfo(document, selection, new IGrammarVersionProvider() {
            
            public int getGrammarVersion() throws MisconfigurationException {
                return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
            }
        });
        
        pyCreateClass.execute(info, PyCreateClass.LOCATION_STRATEGY_BEFORE_CURRENT);
        
        assertContentsEqual("" +
                "class MyClass(${object}):\n" +
                "    \n" +
                "    def __init__(self, ${aa}, ${bb}, ${my_foo}):\n" +
                "        ${pass}${cursor}\n" +
                "\n" +
                "\n" +
                "MyClass(aa, bb, MyFoo())" +
                "", document.get());
    }
    
    
    
    public void testPyCreateClassInSameModule2() throws Exception {
        PyCreateClass pyCreateClass = new PyCreateClass();
        
        String source = "" +
        		"import foo\n" +
        		"\n" +
        		"class Bar(object):\n" +
        		"    def m1(self):\n" +
        		"        MyClass()\n";
        IDocument document = new Document(source);
        ITextSelection selection = new TextSelection(document, source.length()-4, 0);
        RefactoringInfo info = new RefactoringInfo(document, selection, new IGrammarVersionProvider() {
            
            public int getGrammarVersion() throws MisconfigurationException {
                return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
            }
        });
        
        pyCreateClass.execute(info, PyCreateClass.LOCATION_STRATEGY_BEFORE_CURRENT);
        
        assertContentsEqual(
                "" +
                "import foo\n" +
                "\n" +
                "\n" +
                "class MyClass(${object}):\n" +
                "    ${pass}${cursor}\n" +
                "\n" +
                "\n" +
                "class Bar(object):\n" +
                "    def m1(self):\n" +
                "        MyClass()\n",
                document.get());
    }
    
    
    public void testPyCreateClassInSameModule3() throws Exception {
        PyCreateClass pyCreateClass = new PyCreateClass();
        
        String source = "" +
        "import foo\n" +
        "\n" +
        "class Foo(object):\n" +
        "    pass\n" +
        "\n" +
        "class Bar(object):\n" +
        "    def m1(self):\n" +
        "        MyClass()\n";
        IDocument document = new Document(source);
        ITextSelection selection = new TextSelection(document, source.length()-4, 0);
        RefactoringInfo info = new RefactoringInfo(document, selection, new IGrammarVersionProvider() {
            
            public int getGrammarVersion() throws MisconfigurationException {
                return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
            }
        });
        
        pyCreateClass.execute(info, PyCreateClass.LOCATION_STRATEGY_BEFORE_CURRENT);
        
        assertContentsEqual(
                "" +
                "import foo\n" +
                "\n" +
                "class Foo(object):\n" +
                "    pass\n" +
                "\n" +
                "\n" +
                "class MyClass(${object}):\n" +
                "    ${pass}${cursor}\n" +
                "\n" +
                "\n" +
                "class Bar(object):\n" +
                "    def m1(self):\n" +
                "        MyClass()\n",
                document.get());
    }
    
    
    public void testPyCreateClassEndOfFile() throws Exception {
        PyCreateClass pyCreateClass = new PyCreateClass();
        
        String source = "" +
        "import foo\n" +
        "\n" +
        "class Foo(object):\n" +
        "    pass\n" +
        "\n" +
        "class Bar(object):\n" +
        "    def m1(self):\n" +
        "        MyClass()\n";
        IDocument document = new Document(source);
        ITextSelection selection = new TextSelection(document, source.length()-4, 0);
        RefactoringInfo info = new RefactoringInfo(document, selection, new IGrammarVersionProvider() {
            
            public int getGrammarVersion() throws MisconfigurationException {
                return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
            }
        });
        
        pyCreateClass.execute(info, PyCreateClass.LOCATION_STRATEGY_END);
        
        assertContentsEqual(
                "" +
                "import foo\n" +
                "\n" +
                "class Foo(object):\n" +
                "    pass\n" +
                "\n" +
                "class Bar(object):\n" +
                "    def m1(self):\n" +
                "        MyClass()\n"+
                "\n" +
                "class MyClass(${object}):\n" +
                "    ${pass}${cursor}\n" +
                "\n"+
                "\n"+
                "\n",
                document.get());
    }
    
    public void testPyCreateClassEndOfFile2() throws Exception {
        PyCreateClass pyCreateClass = new PyCreateClass();
        
        String source = "";
        IDocument document = new Document(source);
        ITextSelection selection = new TextSelection(document, 0, 0);
        RefactoringInfo info = new RefactoringInfo(document, selection, new IGrammarVersionProvider() {
            
            public int getGrammarVersion() throws MisconfigurationException {
                return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
            }
        });
        pyCreateClass.createProposal(info, "Foo", PyCreateClass.LOCATION_STRATEGY_END, new ArrayList<String>()).apply(document);
        
        
        assertContentsEqual(
                "" +
                "class Foo(${object}):\n" +
                "    ${pass}${cursor}\n" +
                "\n"+
                "\n",
                document.get());
    }
    
    
}
