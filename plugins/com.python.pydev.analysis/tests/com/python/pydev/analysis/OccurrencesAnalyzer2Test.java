/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 21, 2006
 * @author Fabio
 */
package com.python.pydev.analysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.autoedit.TestIndentPrefs;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.callbacks.ICallbackListener;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;

import com.python.pydev.analysis.messages.IMessage;

public class OccurrencesAnalyzer2Test extends AnalysisTestsBase {

    public static void main(String[] args) {
        try {
            OccurrencesAnalyzer2Test analyzer2 = new OccurrencesAnalyzer2Test();
            analyzer2.setUp();
            analyzer2.testParameterAnalysis27a();
            analyzer2.tearDown();
            System.out.println("finished");

            junit.textui.TestRunner.run(OccurrencesAnalyzer2Test.class);
            System.out.println("finished all");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        prefs.severityForArgumentsMismatch = IMarker.SEVERITY_ERROR;
    }

    public void testErrorNotShownOnDynamicClass() {
        doc = new Document("from extendable.noerr import importer\n" +
                "print importer.getWithAttr.whatever\n");
        checkNoError();

    }

    public void testErrorNotShownOnDynamicClass2() {
        doc = new Document("from extendable.noerr import importer\n" +
                "print importer.getWithAttr.whatever.other\n");
        checkNoError();

    }

    public void testErrorNotShownOnDynamicClass3() {
        doc = new Document("from extendable.noerr import importer\n" +
                "print importer.childGetWithAttr.whatever\n"
                +
                "print importer.childGetWithAttr.whatever.other\n");
        checkNoError();

    }

    public void testErrorNotShownOnClassFromMethod() {
        doc = new Document("from extendable.noerr import importer\n" +
                "print importer.logger.debug('10')\n");
        checkNoError();

    }

    public void testErrorNotShownOnNoneClass() {
        doc = new Document("from extendable.noerr import importer\n" +
                "print importer.initialNone.foo\n");
        checkNoError();

    }

    public void testErrorNotShownOnDynamicClass4() {
        doc = new Document("from extendable.noerr import importer\n" +
                "print importer.globals_struct.bar\n");
        checkNoError();

    }

    public void testErrorNotShownOnDynamicClass5() {
        doc = new Document("from extendable.noerr import importer\n" +
                "print importer.Struct.bar\n");
        checkNoError();
    }

    public void testErrorNotShownOnDynamicClass6() {
        doc = new Document("from extendable.noerr.importer import WithGetAttr2\n" +
                "print WithGetAttr2.anything\n");
        checkNoError();
    }

    public void testErrorNotShownOnDynamicClass7() {
        doc = new Document("from extendable.noerr.importer import Struct\n" +
                "print Struct.anything\n");
        checkNoError();
    }

    public void testErrorNotShownOnDynamicClass8() {
        doc = new Document("from extendable.noerr.importer import StructSub\n" +
                "print StructSub.anything\n");
        checkNoError();
    }

    public void testErrorShownOnInitialSetClass() {
        doc = new Document("from extendable.noerr import importer\n" +
                "print importer.initialSet.m1\n"
                +
                "print importer.initialSet.m2\n"//has error
        );
        IMessage[] messages = checkError(1);
        assertEquals("Undefined variable from import: m2", messages[0].getMessage());
    }

    public void testNoErrorPathInPackage() {
        doc = new Document("import extendable\n" +
                "print extendable.__path__\n");
        checkNoError();
    }

    public void testErrorPathNotInModule() {
        doc = new Document("from extendable import static\n" +
                "print static.__path__\n");
        IMessage[] messages = checkError(1);
        assertEquals("Undefined variable from import: __path__", messages[0].getMessage());
    }

    public void testErrorPathNotInModule2() {
        doc = new Document("from extendable import * #@UnusedWildImport\n" +
                "__path__\n");

        //__path__ does not come on "import *" 
        IMessage[] messages = checkError(1);
        assertEquals("Undefined variable: __path__", messages[0].getMessage());
    }

    public void testNoEffectInException() {
        doc = new Document("def raise_exception():\n" +
                "    x = None\n" +
                "    raise Exception, '%(number)s' % {\n"
                +
                "        'number': x is None,\n" +
                "    }\n");
        checkNoError();
    }

    public void testNoEffectInGenExp() {
        doc = new Document("for val in (3 in (1, 2), 'something else'):\n" +
                "    print 'val was', val\n");
        checkNoError();
    }

    public void testPathFound() throws IOException, MisconfigurationException {

        analyzer = new OccurrencesAnalyzer();
        File file = new File(TestDependent.TEST_PYSRC_LOC +
                "extendable/with_path.py");
        Document doc = new Document(FileUtils.getFileContents(file));
        msgs = analyzer.analyzeDocument(nature,
                (SourceModule) AbstractModule.createModule("extendable.with_path", file, nature, true), prefs, doc,
                new NullProgressMonitor(), new TestIndentPrefs(true, 4));

        printMessages(msgs, 0);
    }

    public void testPathFound2() throws IOException, MisconfigurationException {

        analyzer = new OccurrencesAnalyzer();
        File file = new File(TestDependent.TEST_PYSRC_LOC +
                "extendable/__init__.py");
        Document doc = new Document(FileUtils.getFileContents(file));
        msgs = analyzer.analyzeDocument(nature,
                (SourceModule) AbstractModule.createModule("extendable.__init__", file, nature, true), prefs, doc,
                new NullProgressMonitor(), new TestIndentPrefs(true, 4));

        printMessages(msgs, 0);
    }

    public void testInitDef() throws IOException {
        doc = new Document("from extendable import help\n" +
                "print help.about\n");
        checkNoError();

    }

    public void testNoStaticComplain() throws IOException {
        doc = new Document("class SomeClass(object):\n" +
                "    @staticmethod\n" +
                "    def m1(cls):\n"
                +
                "        pass\n");
        checkNoError();
    }

    public void testNoStaticComplain2() throws IOException {
        doc = new Document("class SomeClass(object):\n" +
                "    @staticmethod\n" +
                "    def m1():\n" +
                "        pass\n");
        checkNoError();
    }

    public void testNoStaticComplain3() throws IOException {
        doc = new Document("class SomeClass(object):\n" +
                "    @staticmethod\n" +
                "    def m1(self):\n"
                +
                "        pass\n");
        checkNoError();
    }

    public void testInnerAndOuterScopeReference() throws IOException {
        doc = new Document("EnumType = lambda: EnumType\n" +
                "print(EnumType())\n");
        checkNoError();
    }

    public void testInnerAndOuterScopeReference2() throws IOException {
        doc = new Document("def m1():\n" +
                "    c = lambda: a\n" +
                "    a = 10\n" +
                "    print c\n" +
                "\n");
        checkNoError();
    }

    public void testAssignNotAcknowledged() throws IOException {
        doc = new Document("def m1():\n" +
                "    c = object()\n" +
                "    c.x += 1\n" +
                "\n");
        checkNoError();
    }

    public void testNoLeakageInGenerator() throws IOException {
        doc = new Document("(a for a in range(5))\n" +
                "print a\n");
        checkError(1);
    }

    public void testLeakageInListComp() throws IOException {
        doc = new Document("[b for b in range(5)]\n" +
                "print b\n");
        checkNoError();
    }

    public void testLeakageInListComp2() throws IOException {
        doc = new Document("[x for x in [y for y in range(3)]]\n" +
                "print x, y\n");
        checkNoError();
    }

    public void testListCompFalsePositive() throws IOException {
        doc = new Document("alist = []\n" +
                "blist = []\n"
                +
                "clist = [c for c in (a + b for b in blist for a in alist)]\n");
        checkNoError();
    }

    public void testParameterAnalysis() throws IOException {
        doc = new Document("def m1():\n" +
                "    pass\n" +
                "m1(20)");
        IMessage[] messages = checkError(1);
        assertContainsMsg("m1: arguments don't match", messages);
    }

    public void testParameterAnalysis2() throws IOException {
        doc = new Document("def m1(a):\n" +
                "    pass\n" +
                "m1()");
        IMessage[] messages = checkError(1);
        assertContainsMsg("m1: arguments don't match", messages);
    }

    public void testParameterAnalysis3() throws IOException {
        doc = new Document("def m1(*args):\n" +
                "    pass\n" +
                "m1()");
        checkNoError();
    }

    public void testParameterAnalysis4() throws IOException {
        doc = new Document("def m1(**args):\n" +
                "    pass\n" +
                "m1()");
        checkNoError();
    }

    public void testParameterAnalysis5() throws IOException {
        doc = new Document("def m1(a=10):\n" +
                "    pass\n" +
                "m1()");
        checkNoError();
    }

    public void testParameterAnalysis6() throws IOException {
        doc = new Document("def m1(a=10):\n" +
                "    pass\n" +
                "m1(a=20)");
        checkNoError();
    }

    public void testParameterAnalysis7() throws IOException {
        doc = new Document("def m1(a=10):\n" +
                "    pass\n" +
                "m1(b=20)");
        checkError(1);
    }

    public void testParameterAnalysis8() throws IOException {
        doc = new Document("from extendable.calltips.mod1.sub1 import method1\n" + //method1(a, b)
                "method1(10)");
        IMessage[] errors = checkError(1);
        IMessage msg = errors[0];
        assertEquals(msg.getMessage(), "method1: arguments don't match");
        assertEquals(msg.getStartCol(doc), 8);
        assertEquals(msg.getEndCol(doc), 12);
        assertEquals(msg.getStartLine(doc), 2);
        assertEquals(msg.getEndLine(doc), 2);
    }

    public void testParameterAnalysis8a() throws IOException {
        doc = new Document("from extendable.calltips.mod1.sub1 import method1\n" +
                "method1(\n" +
                "       10)");
        IMessage[] errors = checkError(1);
        IMessage msg = errors[0];
        assertEquals(msg.getMessage(), "method1: arguments don't match");
        assertEquals(msg.getStartCol(doc), 8);
        assertEquals(msg.getEndCol(doc), 11);
        assertEquals(msg.getStartLine(doc), 2);
        assertEquals(msg.getEndLine(doc), 3);
    }

    public void testParameterAnalysis9() throws IOException {
        doc = new Document("from extendable.calltips.mod1.sub1 import method1\n" +
                "method1(10, 20)");
        checkNoError();
    }

    public void testParameterAnalysis10() throws IOException {
        doc = new Document("def m1(a):\n" +
                "    pass\n" +
                "\n" +
                "d={'a':20}\n" +
                "m1(**d)");
        checkNoError();
    }

    public void testParameterAnalysis11() throws IOException {
        doc = new Document("def m1(a):\n" +
                "    pass\n" +
                "\n" +
                "d=[20]\n" +
                "m1(*d)");
        checkNoError();
    }

    public void testParameterAnalysis12() throws IOException {
        doc = new Document("def m1(a, **kwargs):\n" +
                "    pass\n" +
                "\n" +
                "d=[20]\n" +
                "m1(10, *d)");
        checkError(1);
    }

    public void testParameterAnalysis13() throws IOException {
        doc = new Document("def m1(a, *args):\n" +
                "    pass\n" +
                "\n" +
                "d=[20]\n" +
                "m1(10, *d)");
        checkNoError();
    }

    public void testParameterAnalysis14() throws IOException {
        doc = new Document("def m1(a=10, **args):\n" +
                "    pass\n" +
                "\n" +
                "d=[20]\n" +
                "m1(*d)");
        checkNoError();
    }

    public void testParameterAnalysis15() throws IOException {
        int original = GRAMMAR_TO_USE_FOR_PARSING;
        GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_0;
        try {
            doc = new Document("def w(a=10, *, b):\n" +
                    "    pass\n" +
                    "\n" +
                    "w(20, b=10)\n");
            checkNoError();
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = original;
        }
    }

    public void testParameterAnalysis16() throws IOException {
        int original = GRAMMAR_TO_USE_FOR_PARSING;
        GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_0;
        try {
            doc = new Document("def w(a=10, *, b):\n" +
                    "    pass\n" +
                    "\n" +
                    "w(20, 10)\n" //b must be keyword parameter
            );
            checkError(1);
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = original;
        }
    }

    public void testParameterAnalysis17() throws IOException {
        doc = new Document("class Foo:\n" +
                "    def __init__(self, a):\n" +
                "        pass\n" +
                "Foo()\n");
        checkError(1);
    }

    public void testParameterAnalysis17a() throws IOException {
        doc = new Document("class Foo:\n" +
                "    def __init__(self, a):\n" +
                "        pass\n" +
                "Foo(10)\n");
        checkNoError();
    }

    public void testParameterAnalysis18() throws IOException {
        doc = new Document("from testOtherImports.f2 import SomeOtherTest\n" + //class with __init__ == __init__(self, a, b)
                "SomeOtherTest()\n");
        checkError("SomeOtherTest: arguments don't match");
    }

    public void testParameterAnalysis19() throws IOException {
        doc = new Document("from extendable.parameters_check.check import Foo\n" + //class with __init__ == __init__(self, a, b)
                "foo = Foo(10, 20)\n" +
                "foo.Method()\n" //Method(self, a)
        );
        checkError("foo.Method: arguments don't match");
    }

    public void testParameterAnalysis19a() throws IOException {
        doc = new Document("from extendable.parameters_check.check import Foo\n" + //class with __init__ == __init__(self, a, b)
                "foo = Foo(10, 20)\n" +
                "Foo.Method(foo, 10)\n" //Method(self, a)
        );
        checkNoError();
    }

    public void testParameterAnalysis19b() throws IOException {
        doc = new Document("from extendable.parameters_check.check import Foo\n" + //class with __init__ == __init__(self, a, b)
                "Foo.Method(10)\n" //Method(self, a)
        );
        checkError("Foo.Method: arguments don't match");
    }

    public void testParameterAnalysis19c() throws IOException {
        doc = new Document("from extendable.parameters_check import Foo\n" + //class with __init__ == __init__(self, a, b)
                "foo = Foo(10, 20)\n" +
                "Foo.Method(foo, 10)\n" //Method(self, a)
        );
        checkNoError();
    }

    public void testParameterAnalysis19d() throws IOException {
        doc = new Document("from extendable.parameters_check import Foo\n" + //class with __init__ == __init__(self, a, b)
                "foo = Foo(10, 20)\n" +
                "foo.Method(10)\n" //Method(self, a)
        );
        checkNoError();
    }

    public void testParameterAnalysis19e() throws IOException {
        doc = new Document("from extendable.parameters_check import Foo\n" + //class with __init__ == __init__(self, a, b)
                "foo = Foo(10, 20)\n" +
                "Foo.Method(10)\n" //Method(self, a)
        );
        checkError("Foo.Method: arguments don't match");
    }

    public void testParameterAnalysis19f() throws IOException {
        doc = new Document("from extendable.parameters_check import Foo\n" + //class with __init__ == __init__(self, a, b)
                "foo = Foo(10, 20)\n" +
                "foo.Method(foo, 10)\n" //Method(self, a)
        );
        checkError("foo.Method: arguments don't match");
    }

    public void testParameterAnalysis20() throws IOException {
        doc = new Document("from extendable.parameters_check.check import Foo\n" + //class with __init__ == __init__(self, a, b)
                "foo = Foo(10, 20)\n" +
                "foo.Method(10)\n");
        checkNoError();
    }

    public void testParameterAnalysis21() throws IOException {
        doc = new Document("class Bar(object):\n" +
                "    @classmethod\n" +
                "    def Method(cls, a, b):\n"
                +
                "        pass\n" +
                "Bar.Method(10, 20)\n");
        checkNoError();
    }

    public void testParameterAnalysis22() throws IOException {
        doc = new Document("class Bar(object):\n" +
                "    @classmethod\n" +
                "    def Method(cls, a, b):\n"
                +
                "        pass\n" +
                "Bar.Method(20)\n");
        checkError("Bar.Method: arguments don't match");
    }

    public void testParameterAnalysis23() throws IOException {
        doc = new Document("class Bar(object):\n" +
                "    def Method(cls, a, b):\n" +
                "        pass\n"
                +
                "    Method = classmethod(Method)\n" +
                "Bar.Method(20, 20, 20)\n");
        checkError("Bar.Method: arguments don't match");
    }

    public void testParameterAnalysis22a() throws IOException {
        doc = new Document("class Bar(object):\n" +
                "    @staticmethod\n" +
                "    def Method(cls, a, b):\n"
                +
                "        pass\n" +
                "Bar.Method(20, 21)\n");
        checkError("Bar.Method: arguments don't match");
    }

    public void testParameterAnalysis23a() throws IOException {
        doc = new Document("class Bar(object):\n" +
                "    def Method(cls, a, b):\n" +
                "        pass\n"
                +
                "    Method = staticmethod(Method)\n" +
                "Bar.Method(20, 20, 20)\n");
        checkNoError();
    }

    public void testParameterAnalysis23b() throws IOException {
        doc = new Document("class Bar(object):\n" +
                "    @staticmethod\n" +
                "    def Method(cls, a, b):\n"
                +
                "        pass\n" +
                "Bar.Method(20, 20, 20)\n");
        checkNoError();
    }

    public void testParameterAnalysis24() throws IOException {
        doc = new Document("from extendable.parameters_check import Foo\n"
                + //class with __init__ == __init__(self, a, b)
                "\n" +
                "class X(object):\n" +
                "\n" +
                "    def __init__(self, a, b):\n"
                +
                "        Foo.__init__(self, a, b)\n" +
                "\n" +
                "\n" +
                "\n" +
                "class B(object):\n" +
                "\n"
                +
                "    def __init__(self, a, b, c):\n" +
                "        pass\n" +
                "\n" +
                "    @classmethod\n"
                +
                "    def Create(cls):\n" +
                "        return B(1, 2, 3)\n");
        checkNoError();
    }

    public void testParameterAnalysis24a() throws IOException {
        doc = new Document("class B(object):\n" +
                "\n" +
                "    def __init__(self, a, b, c):\n" +
                "        pass\n" +
                "\n"
                +
                "    @classmethod\n" +
                "    def Create(cls):\n" +
                "        return B(1, 2)\n");
        checkError("B: arguments don't match");
    }

    public void testParameterAnalysis25() throws IOException {
        doc = new Document("class Bar(object):\n" +
                "\n" +
                "    def __init__(self):\n" +
                "        pass\n" +
                "\n"
                +
                "class Foo(Bar):\n" +
                "    pass\n" +
                "\n" +
                "Foo()\n" +
                "Foo()\n");
        checkNoError();
    }

    public void testParameterAnalysis26() throws IOException {
        doc = new Document("class Foo(object):\n" +
                "    def Method(self):\n" +
                "        pass\n" +
                "\n"
                +
                "    def Method2(self):\n" +
                "        self.Method()\n");
        checkNoError();
    }

    public void testParameterAnalysis26a() throws IOException {
        doc = new Document("class Foo(object):\n" +
                "    def Method(self):\n" +
                "        pass\n" +
                "\n"
                +
                "    def Method2(self):\n" +
                "        self.Method(1)\n");
        checkError("self.Method: arguments don't match");
    }

    public void testParameterAnalysis27() throws IOException {
        doc = new Document("class Bounds(object):\n" +
                "\n" +
                "    def Method(self):\n" +
                "        pass\n" +
                "\n"
                +
                "class Bar(object):\n" +
                "\n" +
                "    def __init__(self):\n" +
                "        self.bounds = Bounds()\n"
                +
                "\n" +
                "    def testGetDiagonalLength(self):\n" +
                "        self.bounds.Method()\n" +
                "\n");
        checkNoError();
    }

    public void testParameterAnalysis27a() throws IOException {
        doc = new Document("class Bounds(object):\n" +
                "\n" +
                "    def Method(self):\n" +
                "        pass\n" +
                "\n"
                +
                "class Bar(object):\n" +
                "\n" +
                "    def __init__(self):\n" +
                "        self.Bounds = Bounds\n" +
                "\n"
                +
                "    def testGetDiagonalLength(self):\n" +
                "        self.Bounds.Method()\n" +
                "\n");
        checkError("self.Bounds.Method: arguments don't match");
    }

    List<String> findDefinitionDone = new ArrayList<String>();
    private ICallbackListener<ICompletionState> listener = new ICallbackListener<ICompletionState>() {

        public Object call(ICompletionState obj) {
            findDefinitionDone.add(obj.getActivationToken());
            return null;
        }
    };

    private void registerOnFindDefinitionListener() {
        SourceModule.onFindDefinition = new CallbackWithListeners<ICompletionState>();
        SourceModule.onFindDefinition.registerListener(listener);
    }

    private void unregisterFindDefinitionListener(String... expected) {
        SourceModule.onFindDefinition = null;
        if (expected.length != findDefinitionDone.size()) {
            fail(StringUtils.format(
                    "Expected: %s (%s) find definition call(s). Found: %s (%s)", expected.length,
                    Arrays.asList(expected), findDefinitionDone.size(), findDefinitionDone));
        }
    }

    public void testParameterAnalysisOptimization() throws IOException {
        registerOnFindDefinitionListener();
        try {
            doc = new Document("def method():\n" +
                    "    \n" +
                    "method()\n" + //
                    "method()\n");
            checkNoError();
        } finally {
            unregisterFindDefinitionListener();
        }
    }

    public void testParameterAnalysisOptimization2() throws IOException {
        registerOnFindDefinitionListener();
        try {
            doc = new Document("def method():\n" +
                    "    \n" +
                    "b = method\n" +
                    "b()\n" + //
                    "b()\n");
            checkNoError();
        } finally {
            unregisterFindDefinitionListener();
        }
    }

    public void testParameterAnalysisOptimization3() throws IOException {
        registerOnFindDefinitionListener();
        try {
            doc = new Document("class Foo:\n" +
                    "    def __init__(self, a):\n" +
                    "        pass\n" +
                    "Foo()\n");
            checkError("Foo: arguments don't match");
        } finally {
            unregisterFindDefinitionListener();
        }
    }

    public void testParameterAnalysisOptimization4() throws IOException {
        registerOnFindDefinitionListener();
        try {
            doc = new Document("class Foo:\n" +
                    "    def __init__(self, a):\n" +
                    "        pass\n" +
                    "Bar = Foo\n"
                    +
                    "Bar(1)\n");
            checkNoError();
        } finally {
            unregisterFindDefinitionListener();
        }
    }

    public void testParameterAnalysisOptimization5a() throws IOException {
        registerOnFindDefinitionListener();
        try {
            doc = new Document("from extendable.parameters_check.check import Foo\n" + //class with __init__ == __init__(self, a, b)
                    "foo = Foo(10, 20)\n" +
                    "foo.Method(10)\n");
            checkNoError();
        } finally {
            unregisterFindDefinitionListener("Foo", "foo.Method", "foo"); //TODO: This must be improved!
        }
    }

    public void testParameterAnalysisOptimization5() throws IOException {
        prefs.severityForArgumentsMismatch = IMarker.SEVERITY_INFO; //Nothing will be analyzed and the checks should be skipped!
        registerOnFindDefinitionListener();
        try {
            doc = new Document("from extendable.parameters_check.check import Foo\n" + //class with __init__ == __init__(self, a, b)
                    "foo = Foo(10, 20, 20)\n" +
                    "foo.Method(10, 30)\n" //error here, but check is disabled!
            );
            checkNoError();
        } finally {
            unregisterFindDefinitionListener();
        }
    }

    public void testParameterAnalysisOptimization6() throws IOException {
        registerOnFindDefinitionListener();
        try {
            doc = new Document("from extendable.parameters_check import check\n" + //class with __init__ == __init__(self, a, b)
                    "foo = check.Foo(10, 20, 20)\n" +
                    "foo.Method(10, 20)\n");
            checkError("foo.Method: arguments don't match");
        } finally {
            unregisterFindDefinitionListener("", "check.Foo", "foo.Method", "foo");
        }
    }

    //    public void testNonDefaultAfterDefault() throws IOException{
    //        doc = new Document(
    //                "def m1(a=20, 20):\n"+ //non-default after default
    //                "    pass\n"
    //        );
    //        checkError(1);
    //    }
}
