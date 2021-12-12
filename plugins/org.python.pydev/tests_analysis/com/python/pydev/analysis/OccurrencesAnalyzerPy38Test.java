package com.python.pydev.analysis;

import java.io.File;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.python.pydev.ast.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.autoedit.TestIndentPrefs;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.shared_core.io.FileUtils;

public class OccurrencesAnalyzerPy38Test extends AnalysisTestsBase {

    public static void main(String[] args) {
        try {
            OccurrencesAnalyzerPy38Test analyzer2 = new OccurrencesAnalyzerPy38Test();
            analyzer2.setUp();
            analyzer2.testWalrusOperatorInIf2();
            analyzer2.tearDown();
            System.out.println("finished");

            junit.textui.TestRunner.run(OccurrencesAnalyzerPy38Test.class);
            System.out.println("finished all");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    @Override
    protected String getSystemPythonpathPaths() {
        String paths;
        paths = TestDependent.getCompletePythonLib(true, isPython3Test());
        if (TestDependent.PYTHON38_QT5_PACKAGES != null) {
            paths += "|" + TestDependent.PYTHON38_QT5_PACKAGES;
        }
        return paths;
    }

    private int initialGrammar;

    @Override
    public void setUp() throws Exception {
        initialGrammar = GRAMMAR_TO_USE_FOR_PARSING;
        GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_8;

        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        GRAMMAR_TO_USE_FOR_PARSING = initialGrammar;
        ParseException.verboseExceptions = true;
        super.tearDown();
    }

    @Override
    protected boolean isPython3Test() {
        return true;
    }

    public void testWalrusOperatorInIf() throws Exception {
        doc = new Document("" +
                ""
                + "def test():\n"
                + "    return 10\n"
                + "\n"
                + "if a := test() > 20:\n" +
                "    pass\n" +
                "");
        checkNoError();
    }

    public void testWalrusOperatorInIf2() throws Exception {
        doc = new Document("" +
                ""
                + "def test():\n"
                + "    return 10\n"
                + "\n"
                + "def foo():\n"
                + "    if a := test() > 20:\n" +
                "        pass\n" +
                "");
        checkNoError();
    }

    public void testWithBelowDefinedWithFuture() throws Exception {
        doc = new Document("" +
                "from __future__ import annotations\n" +
                "\n" +
                "class Foo:\n" +
                "    def __init__(self, bar: Bar):\n" +
                "        self.bar = bar\n" +
                "\n" +
                "class Bar:\n"
                + "    pass");
        checkNoError();
    }

    public void testWithBelowDefined() throws Exception {
        doc = new Document("" +
                "class Foo:\n" +
                "    def __init__(self, bar: Bar):\n" +
                "        self.bar = bar\n" +
                "\n" +
                "class Bar:\n"
                + "    pass");
        checkError("Undefined variable: Bar");
    }

    public void testClassWithReferenceToItself() throws Exception {
        doc = new Document("" +
                "def gen():\n" +
                "    class C:\n" +
                "        def m(self):\n" +
                "            return 42\n" +
                "        def mm(self):\n" +
                "            return C.m");
        checkNoError();
    }

    public void testClassSelfReferenceWithFutureImported() throws Exception {
        doc = new Document("from __future__ import annotations\n" +
                "\n" +
                "class A():\n" +
                "\n" +
                "    def b(self) -> A:\n" +
                "        return self");
        checkNoError();
    }

    public void testClassSelfReferenceWithoutFutureImported() throws Exception {
        doc = new Document("" +
                "class A():\n" +
                "\n" +
                "    def b(self) -> A:\n" +
                "        return self");
        checkError("Undefined variable: A");
    }

    public void testStaticClassVariable2() throws Exception {
        analyzer = new OccurrencesAnalyzer();
        File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC + "static_class_variables/mod2.py");
        Document doc = new Document(FileUtils.getFileContents(file));
        msgs = analyzer.analyzeDocument(nature,
                (SourceModule) AbstractModule.createModule("static_class_variables.mod2", file, nature, true), prefs,
                doc,
                new NullProgressMonitor(), new TestIndentPrefs(true, 4));

        printMessages(msgs, 0);
    }

    public void testQt() throws Exception {
        if (TestDependent.PYTHON38_QT5_PACKAGES != null) {
            doc = new Document("import PyQt5.QtGui\n"
                    + "print(PyQt5.QtGui.QColor.red)\n" +
                    "\n");
            analyzer = new OccurrencesAnalyzer();
            checkNoError();
        }
    }

    public void testQt2() throws Exception {
        if (TestDependent.PYTHON38_QT5_PACKAGES != null) {
            doc = new Document("import PyQt5.QtWidgets\n"
                    + "print(PyQt5.QtWidgets.QWidget.__init__)\n" +
                    "\n");
            analyzer = new OccurrencesAnalyzer();
            checkNoError();
        }
    }
}
