/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.util.ArrayList;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.ast.refactoring.RefactoringInfo;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestCaseUtils;
import org.python.pydev.core.autoedit.DefaultIndentPrefs;
import org.python.pydev.core.autoedit.TestIndentPrefs;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.editor.codecompletion.proposals.DefaultCompletionProposalFactory;
import org.python.pydev.shared_core.string.CoreTextSelection;
import org.python.pydev.shared_core.string.ICoreTextSelection;

import com.python.pydev.analysis.refactoring.tdd.AbstractPyCreateAction;
import com.python.pydev.analysis.refactoring.tdd.PyCreateMethodOrField;

public class PyCreateMethodTest extends TestCaseUtils {

    public static void main(String[] args) {
        try {
            PyCreateMethodTest test = new PyCreateMethodTest();
            test.setUp();
            test.testPyCreateMethodWithTabs();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyCreateMethodTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CompletionProposalFactory.set(new DefaultCompletionProposalFactory());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        CompletionProposalFactory.set(null);
    }

    private static final IGrammarVersionProvider PY_27_ONLY_GRAMMAR_VERSION_PROVIDER = new IGrammarVersionProvider() {

        @Override
        public int getGrammarVersion() throws MisconfigurationException {
            return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_5;
        }

        @Override
        public AdditionalGrammarVersionsToCheck getAdditionalGrammarVersions() throws MisconfigurationException {
            return null;
        }
    };

    public void testPyCreateMethodGlobal() {
        PyCreateMethodOrField pyCreateMethod = new PyCreateMethodOrField();

        String source = "MyMethod()";
        IDocument document = new Document(source);
        ICoreTextSelection selection = new CoreTextSelection(document, 0, 0);
        RefactoringInfo info = new RefactoringInfo(document, selection, PY_27_ONLY_GRAMMAR_VERSION_PROVIDER);

        ExecutePyCreate.execute(pyCreateMethod, info, AbstractPyCreateAction.LOCATION_STRATEGY_BEFORE_CURRENT);

        assertContentsEqual("" +
                "def MyMethod():\n" +
                "    pass\n" +
                "\n" +
                "\n" +
                "MyMethod()" +
                "",
                document.get());
    }

    public void testPyCreateMethodGlobalParams() {
        PyCreateMethodOrField pyCreateMethod = new PyCreateMethodOrField();

        String source = "MyMethod(a, b())";
        IDocument document = new Document(source);
        ICoreTextSelection selection = new CoreTextSelection(document, 0, 0);
        RefactoringInfo info = new RefactoringInfo(document, selection, PY_27_ONLY_GRAMMAR_VERSION_PROVIDER);

        ExecutePyCreate.execute(pyCreateMethod, info, AbstractPyCreateAction.LOCATION_STRATEGY_BEFORE_CURRENT);

        assertContentsEqual("" +
                "def MyMethod(a, b):\n" +
                "    pass\n" +
                "\n" +
                "\n"
                +
                "MyMethod(a, b())" +
                "", document.get());
    }

    public void testPyCreateMethodGlobal1() {
        PyCreateMethodOrField pyCreateMethod = new PyCreateMethodOrField();

        String source = "a = MyMethod()";
        IDocument document = new Document(source);
        ICoreTextSelection selection = new CoreTextSelection(document, 5, 0);
        RefactoringInfo info = new RefactoringInfo(document, selection, PY_27_ONLY_GRAMMAR_VERSION_PROVIDER);

        ExecutePyCreate.execute(pyCreateMethod, info, AbstractPyCreateAction.LOCATION_STRATEGY_END);

        assertContentsEqual("" +
                "a = MyMethod()\n" +
                "\n" +
                "def MyMethod():\n" +
                "    pass\n" +
                "\n"
                +
                "\n" +
                "", document.get());
    }

    public void testPyCreateMethodInEmptyDoc() {
        PyCreateMethodOrField pyCreateMethod = new PyCreateMethodOrField();

        String source = "";
        IDocument document = new Document(source);
        ICoreTextSelection selection = new CoreTextSelection(document, 5, 0);
        RefactoringInfo info = new RefactoringInfo(document, selection, PY_27_ONLY_GRAMMAR_VERSION_PROVIDER);

        ExecutePyCreate.execute(pyCreateMethod, info, "MyMethod", new ArrayList<String>(),
                AbstractPyCreateAction.LOCATION_STRATEGY_END);

        assertContentsEqual("" +
                "def MyMethod():\n" +
                "    pass\n" +
                "\n" +
                "\n" +
                "", document.get());

        document.set("");
        ExecutePyCreate.execute(pyCreateMethod, info, "MyMethod2", new ArrayList<String>(),
                AbstractPyCreateAction.LOCATION_STRATEGY_BEFORE_CURRENT);

        assertContentsEqual("" +
                "def MyMethod2():\n" +
                "    pass\n" +
                "\n" +
                "\n" +
                "", document.get());
    }

    public void testPyCreateMethodInClass() {
        PyCreateMethodOrField pyCreateMethod = new PyCreateMethodOrField();

        String source = "" +
                "class A(object):\n" +
                "    '''comment'''\n" +
                "\n" +
                "A.MyMethod(a, b())";
        IDocument document = new Document(source);
        ICoreTextSelection selection = new CoreTextSelection(document, document.getLength() - "hod(a, b())".length(),
                0);
        RefactoringInfo info = new RefactoringInfo(document, selection, PY_27_ONLY_GRAMMAR_VERSION_PROVIDER);

        pyCreateMethod.setCreateInClass("A");
        pyCreateMethod.setCreateAs(PyCreateMethodOrField.CLASSMETHOD);
        ExecutePyCreate.execute(pyCreateMethod, info, AbstractPyCreateAction.LOCATION_STRATEGY_END);

        assertContentsEqual("" +
                "" +
                "class A(object):\n" +
                "    '''comment'''\n" +
                "\n" +
                "    \n"
                +
                "    @classmethod\n" +
                "    def MyMethod(cls, a, b):\n" +
                "        pass\n"
                +
                "    \n" +
                "    \n" +
                "\n" +
                "A.MyMethod(a, b())" +
                "", document.get());
    }

    public void testPyCreateMethodInSelfWithDecorator() {
        PyCreateMethodOrField pyCreateMethod = new PyCreateMethodOrField();

        String source = "" +
                "class A(object):\n" +
                "    @decorator\n" +
                "    def m1(self):\n" +
                "        self.m2()";
        IDocument document = new Document(source);
        ICoreTextSelection selection = new CoreTextSelection(document, document.getLength() - "2()".length(), 0);
        RefactoringInfo info = new RefactoringInfo(document, selection, PY_27_ONLY_GRAMMAR_VERSION_PROVIDER);

        pyCreateMethod.setCreateInClass("A");
        pyCreateMethod.setCreateAs(PyCreateMethodOrField.BOUND_METHOD);
        ExecutePyCreate.execute(pyCreateMethod, info, AbstractPyCreateAction.LOCATION_STRATEGY_BEFORE_CURRENT);

        String expected = "" +
                "class A(object):\n" +
                "\n" +
                "    \n" +
                "    def m2(self):\n"
                +
                "        pass\n" +
                "    \n" +
                "    \n" +
                "    @decorator\n" +
                "    def m1(self):\n"
                +
                "        self.m2()";

        assertContentsEqual(expected, document.get());
    }

    public void testPyCreateMethod() {
        PyCreateMethodOrField pyCreateMethod = new PyCreateMethodOrField();

        String source = "" +
                "class A(object):\n" +
                "\n" +
                "\n" +
                "\n" +
                "    def m1(self):\n" +
                "        self.m2()";
        IDocument document = new Document(source);
        ICoreTextSelection selection = new CoreTextSelection(document, document.getLength() - "2()".length(), 0);
        RefactoringInfo info = new RefactoringInfo(document, selection, PY_27_ONLY_GRAMMAR_VERSION_PROVIDER);

        pyCreateMethod.setCreateInClass("A");
        pyCreateMethod.setCreateAs(PyCreateMethodOrField.BOUND_METHOD);
        ExecutePyCreate.execute(pyCreateMethod, info, AbstractPyCreateAction.LOCATION_STRATEGY_BEFORE_CURRENT);

        String expected = "" +
                "class A(object):\n" +
                "\n" +
                "\n" +
                "\n" +
                "    def m2(self):\n"
                +
                "        pass\n" +
                "    \n" +
                "    \n" +
                "    def m1(self):\n" +
                "        self.m2()";

        assertContentsEqual(expected, document.get());
    }

    public void testPyCreateMethodWithTabs() {
        DefaultIndentPrefs.set(new TestIndentPrefs(false, 4));
        IDocument document;
        String expected;
        try {
            PyCreateMethodOrField pyCreateMethod = new PyCreateMethodOrField();

            String source = "" +
                    "class A(object):\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "\tdef m1(self):\n" +
                    "\t\tself.m2()";
            document = new Document(source);
            ICoreTextSelection selection = new CoreTextSelection(document, document.getLength() - "2()".length(), 0);
            RefactoringInfo info = new RefactoringInfo(document, selection, PY_27_ONLY_GRAMMAR_VERSION_PROVIDER);

            pyCreateMethod.setCreateInClass("A");
            pyCreateMethod.setCreateAs(PyCreateMethodOrField.BOUND_METHOD);
            ExecutePyCreate.execute(pyCreateMethod, info, AbstractPyCreateAction.LOCATION_STRATEGY_BEFORE_CURRENT);

            expected = "" +
                    "class A(object):\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "\tdef m2(self):\n"
                    +
                    "\t\tpass\n" +
                    "\t\n" +
                    "\t\n" +
                    "\tdef m1(self):\n" +
                    "\t\tself.m2()";
        } finally {
            DefaultIndentPrefs.set(null);
        }

        assertContentsEqual(expected, document.get());
    }

}
