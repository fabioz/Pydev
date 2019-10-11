/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis;

import static org.python.pydev.ast.analysis.IAnalysisPreferences.TYPE_DUPLICATED_SIGNATURE;
import static org.python.pydev.ast.analysis.IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE;
import static org.python.pydev.ast.analysis.IAnalysisPreferences.TYPE_UNUSED_IMPORT;
import static org.python.pydev.ast.analysis.IAnalysisPreferences.TYPE_UNUSED_VARIABLE;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.python.pydev.ast.analysis.messages.CompositeMessage;
import org.python.pydev.ast.analysis.messages.IMessage;
import org.python.pydev.ast.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.autoedit.TestIndentPrefs;
import org.python.pydev.shared_core.io.FileUtils;

public class OccurrencesAnalyzerTest extends AnalysisTestsBase {

    public static void main(String[] args) {
        try {
            OccurrencesAnalyzerTest analyzer2 = new OccurrencesAnalyzerTest();
            analyzer2.setUp();
            // analyzer2.testNoDuplicateOnTypingOverride();
            analyzer2.tearDown();
            System.out.println("finished");

            junit.textui.TestRunner.run(OccurrencesAnalyzerTest.class);
            System.out.println("finished all");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public void testUnusedImports() {

        prefs.severityForUnusedImport = IMarker.SEVERITY_ERROR;
        prefs.severityForUnusedWildImport = IMarker.SEVERITY_ERROR;
        doc = new Document("import testlib\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(1, msgs.length);
        assertEquals("Unused import: testlib", msgs[0].getMessage());
        assertEquals(IMarker.SEVERITY_ERROR, msgs[0].getSeverity());
        assertEquals(TYPE_UNUSED_IMPORT, msgs[0].getType());

        //-----------------
        doc = new Document("import testlib\nprint testlib");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(0, msgs.length);

        //-----------------
        sDoc = "from testlib.unittest import *";
        doc = new Document(sDoc);
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals(IMarker.SEVERITY_ERROR, msgs[0].getSeverity());
        assertEquals(6, msgs[0].getStartCol(doc));
        assertEquals(31, msgs[0].getEndCol(doc));
        assertEquals(
                "Unused in wild import: AnotherTest, GUITest, TestCase, TestCaseAlias, anothertest, guitestcase, main, t, testcase",
                msgs[0].getMessage());

        //-----------------
        prefs.severityForUnusedImport = IMarker.SEVERITY_WARNING;
        prefs.severityForUnusedWildImport = IMarker.SEVERITY_WARNING;
        sDoc = "from testlib.unittest import *\nprint TestCase";
        doc = new Document(sDoc);
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(1, msgs.length);
        assertEquals(IMarker.SEVERITY_WARNING, msgs[0].getSeverity());
        assertEquals(
                "Unused in wild import: AnotherTest, GUITest, TestCaseAlias, anothertest, guitestcase, main, t, testcase",
                msgs[0].getMessage());
        //        assertEquals("TestCase", msgs[0].getAdditionalInfo().get(0));

        //-----------------
        prefs.severityForUnusedImport = IMarker.SEVERITY_INFO;
        prefs.severityForUnusedWildImport = IMarker.SEVERITY_INFO;

        sDoc = "from testlib.unittest import *\nprint TestCase\nprint testcase";
        doc = new Document(sDoc);
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        //even in ignore mode, we get the message
        assertEquals(1, msgs.length);
        assertEquals("Unused in wild import: AnotherTest, GUITest, TestCaseAlias, anothertest, guitestcase, main, t",
                msgs[0].getMessage());
        //        assertEquals("TestCase", msgs[0].getAdditionalInfo().get(0));
        //        assertEquals("testcase", msgs[0].getAdditionalInfo().get(1));
    }

    public void testUnusedImportsSupressed() {
        int original = CompositeMessage.MAXIMUM_NUMBER_OF_INTERNAL_MESSAGES;
        CompositeMessage.MAXIMUM_NUMBER_OF_INTERNAL_MESSAGES = 3;
        try {
            doc = new Document("from testlib.unittest import *\n");
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();

            printMessages(msgs, 1);
            assertTrue(msgs[0].getMessage().endsWith("... others suppressed"));
        } finally {
            CompositeMessage.MAXIMUM_NUMBER_OF_INTERNAL_MESSAGES = original;
        }
    }

    public void testSpacesAndTabsMix() throws Exception {
        doc = new Document("def a():\n" +
                "        print 'a'\n" +
                "\tprint 'b'\n"); //this is valid in python, but not usually wanted...
        checkError(1);
    }

    public void testIconsistendIndent() throws Exception {
        doc = new Document("def a():\n" +
                "     print 'a'\n" + //5 spaces (user config == 4)
                "\n" +
                "def b():\n" +
                "    print 'b'\n" + //ok
                "    print 'c'\n" + //ok
                "");
        checkError(1);
    }

    public void testListComp() throws Exception {
        doc = new Document("print [a for a in range(3)][0].split()");
        checkNoError();
    }

    public void testMetaclass() {
        doc = new Document("class MyMetaclass(type):\n" +
                "    def __init__(cls, name, bases, dct): #@UnusedVariable\n"
                +
                "        pass\n" +
                "\n");
        checkNoError();
    }

    public void testBuiltinsWithoutImport() {
        //to use __builtin__, it has to be imported, but
        //__builtins__ is always directly there
        doc = new Document("print __builtins__\n" +
                "\n");
        checkNoError();
    }

    public void testListComp2() {
        doc = new Document("a = [1, 2]\n" +
                "b = [1, 3]\n" +
                "for found in [found for found in a if found not in b]:\n"
                +
                "    print found\n");
        checkNoError();

    }

    public void testOsPath() {
        doc = new Document("from os.path import *#@UnusedWildImport\n" +
                "print exists\n");
        checkNoError();

    }

    public void testFalseUnused() {
        doc = new Document("def m1():\n" +
                "    name = ''\n" +
                "    getattr(1, name).text().latin1\n");
        checkNoError();
    }

    public void testNoUnusedWithLocals() {
        doc = new Document("def m1():\n" +
                "    name = ''\n" +
                "    print locals()\n");
        checkNoError();
    }

    public void testDelete() {
        doc = new Document("def m1():\n" +
                "    del foo\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);

    }

    public void testAugAssign() {

        doc = new Document("def m1():\n" +
                "    foo|=1\n");
        checkAug(1);

        doc = new Document("def m1():\n" +
                "    foo+=1\n");
        checkAug(1);

        doc = new Document("def m1():\n" +
                "    foo*=1\n");
        checkAug(1);

        doc = new Document("def m1():\n" +
                "    print foo|1\n");
        checkAug(1);

        doc = new Document("def m1():\n" +
                "    foo = 10\n" +
                "    foo += 20");
        checkAug(0);
    }

    private void checkAug(int errors) {
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, errors);
    }

    public void testFromFutureImport() {
        doc = new Document("from __future__ import generators\n");
        checkNoError();

    }

    public void testMetaclassImport() {
        doc = new Document("from psyco.classes import __metaclass__ #@UnresolvedImport\n");
        checkNoError();

    }

    public void testFromFutureImport2() {
        doc = new Document("from __future__ import generators\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc2();

        printMessages(msgs, 0);

    }

    private IMessage[] analyzeDoc2() {
        try {
            return analyzer.analyzeDocument(nature,
                    AbstractModule.createModuleFromDoc("foo", null, doc, nature, true), prefs, doc,
                    new NullProgressMonitor(), new TestIndentPrefs(true, 4));
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public void testWrongLine() throws MisconfigurationException {
        doc = new Document("ExportMethodTransient(True,\n" +
                "                      0,\n"
                +
                "                      1).DoExport()\n" +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc2();

        printMessages(msgs, 1);
        assertEquals(1, msgs[0].getStartLine(doc));
        assertEquals(1, msgs[0].getEndLine(doc));
        assertEquals(1, msgs[0].getStartCol(doc));
        assertEquals(22, msgs[0].getEndCol(doc));
    }

    public void testClassVar() {
        doc = new Document("class Foo:\n" +
                "    x = 1\n" +
                "    def m1(self):\n" +
                "        print x\n" + //should access with self.x or Foo.x
                "        print Foo.x\n" +
                "        print self.x\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Undefined variable: x", msgs[0].getMessage());
    }

    public void testImportWithTryExcept() {
        doc = new Document("try:\n" +
                "    import foo\n" +
                "except ImportError:\n" +
                "    foo = None\n");
        checkNoError();
    }

    public void testImportWithTryExcept2() {
        doc = new Document("try:\n" +
                "    import foo\n" +
                "except:\n" +
                "    foo = None\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Unresolved import: foo", msgs[0].getMessage());
    }

    public void testClsInNew() {
        doc = new Document("class C2:\n" +
                "    def __new__(cls):\n" +
                "        print cls\n" +
                "");
        checkNoError();
    }

    public void testMsgInNew() {
        doc = new Document("class C2:\n" +
                "    def __new__(foo):\n" +
                "        print foo\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Method '__new__' should have self or cls as first parameter", msgs[0].getMessage());
    }

    public void testClsInsteadOfSelf() {
        doc = new Document("class C2:\n" +
                "    @str\n" +
                "    def foo(cls):\n" +
                "        print cls\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertContainsMsg("Method 'foo' should have self as first parameter", msgs);
        assertEquals(9, msgs[0].getStartCol(doc));
        assertEquals(3, msgs[0].getStartLine(doc));

    }

    public void testConsiderAsGlobals() {
        doc = new Document("print considerGlobal");
        checkNoError();

    }

    public void testConsiderAsGlobalsWithAttributeAccess() {
        doc = new Document("print considerGlobal.anything");
        checkNoError();

    }

    public void testConsiderUnusedImportWithinTryExcept() throws Exception {

        doc = new Document("try:\n" +
                "    import cStringIO as foo\n" +
                "except ImportError:\n"
                +
                "    import StringIO as foo\n" +
                "print foo\n" +
                "");
        checkNoError();

    }

    public void testUnusedImports2() {

        doc = new Document("from simpleimport import *\n" +
                "print xml");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertContainsMsg("Unused in wild import: xml.dom, xml.dom.domreg", msgs);
        //        assertEquals("xml", msgs[0].getAdditionalInfo().get(0)); //this is the used import
    }

    public void testUnusedImports2a() {

        doc = new Document("import os.path");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);

        IMessage message = assertContainsMsg("Unused import: os.path", msgs);
        assertEquals(8, message.getStartCol(doc));
        assertEquals(15, message.getEndCol(doc));

    }

    public void testUnusedImports2b() {

        doc = new Document("\n\nfrom testlib.unittest import *");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);

        assertEquals(6, msgs[0].getStartCol(doc));
        assertEquals(31, msgs[0].getEndCol(doc));

    }

    public void testUnusedImports3() {

        doc = new Document("import os.path as otherthing\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(1, msgs.length);
        assertContainsMsg("Unused import: otherthing", msgs);
        assertEquals(8, msgs[0].getStartCol(doc));
        assertEquals(29, msgs[0].getEndCol(doc));
    }

    public void testUnusedImports3a() {

        doc = new Document("import os.path as otherthing, unittest\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 2);
        IMessage message = assertContainsMsg("Unused import: otherthing", msgs);
        assertEquals(8, message.getStartCol(doc));
        assertEquals(29, message.getEndCol(doc));

        message = assertContainsMsg("Unused import: unittest", msgs);
        assertEquals(31, message.getStartCol(doc));
        assertEquals(39, message.getEndCol(doc));

    }

    public void testUnusedImports3b() {

        doc = new Document("from testlib import unittest, __init__\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 2);
        IMessage message = assertContainsMsg("Unused import: unittest", msgs);
        assertEquals(21, message.getStartCol(doc));
        assertEquals(29, message.getEndCol(doc));

        message = assertContainsMsg("Unused import: __init__", msgs);
        assertEquals(31, message.getStartCol(doc));
        assertEquals(39, message.getEndCol(doc));

    }

    public void testUnusedImports4() {

        doc = new Document("def m():\n" +
                "    import os.path as otherthing\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(1, msgs.length);
        assertContainsMsg("Unused import: otherthing", msgs);
        assertEquals(12, msgs[0].getStartCol(doc));
        assertEquals(33, msgs[0].getEndCol(doc));
    }

    public void testUnusedImports5() {

        doc = new Document("from definitions import methoddef\n" +
                "@methoddef.decorator1\n" +
                "def m1():pass\n" +
                "\n"
                +
                "");
        checkNoError();
    }

    public void testCompiledUnusedImports5() {

        if (TestDependent.PYTHON_WXPYTHON_PACKAGES != null) {
            doc = new Document("from wxPython.wx import wxButton\n" +
                    "");
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();

            printMessages(msgs, 1);
            assertContainsMsg("Unused import: wxButton", msgs);
        }
    }

    public void testCompiledWx() {

        if (TestDependent.PYTHON_WXPYTHON_PACKAGES != null) {
            //            CompiledModule.TRACE_COMPILED_MODULES = true;
            doc = new Document("from wx import glcanvas\n" +
                    "print glcanvas.GLCanvas\n" +
                    "");
            checkNoError();
        }
    }

    public void testImportNotFound() {

        doc = new Document("def m():\n" +
                "    import invalidImport\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(2, msgs.length);
        assertContainsMsg("Unresolved import: invalidImport", msgs);
        assertContainsMsg("Unused import: invalidImport", msgs);
    }

    public void testImportNotFound2() {

        doc = new Document("import invalidImport\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(2, msgs.length);
        assertContainsMsg("Unresolved import: invalidImport", msgs);
        assertContainsMsg("Unused import: invalidImport", msgs);
    }

    public void testImportNotFound3() {

        doc = new Document("import os.notDefined\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 2);
        assertContainsMsg("Unused import: os.notDefined", msgs);
        assertContainsMsg("Unresolved import: os.notDefined", msgs);
    }

    public void testImportNotFound9() {

        doc = new Document("from os import path, notDefined\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 3);
        assertContainsMsg("Unresolved import: notDefined", msgs);
        assertContainsMsg("Unused import: notDefined", msgs);
        assertContainsMsg("Unused import: path", msgs);
    }

    public void testMultilineImport() {

        doc = new Document("from os import (pathNotDef1,\n" +
                "                notDefined)\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        IMessage message;

        printMessages(msgs, 4);

        message = assertContainsMsg("Unresolved import: pathNotDef1", msgs);
        assertEquals(1, message.getStartLine(doc));

        message = assertContainsMsg("Unresolved import: notDefined", msgs);
        assertEquals(2, message.getStartLine(doc));
        assertEquals(2, message.getEndLine(doc));

        assertEquals(17, message.getStartCol(doc));
        assertEquals(27, message.getEndCol(doc));

        assertContainsMsg("Unused import: notDefined", msgs);
        assertContainsMsg("Unused import: pathNotDef1", msgs);
    }

    public void testImportNotFound4() {

        doc = new Document("import os as otherThing\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertContainsMsg("Unused import: otherThing", msgs);
    }

    public void testImportNotFound6() {

        doc = new Document("import os\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        IMessage msg = assertContainsMsg("Unused import: os", msgs);
        assertEquals(1, msg.getStartLine(doc));
    }

    public void testImportNotFound7() {

        doc = new Document("import encodings.latin_1\n" +
                "print encodings.latin_1\n" +
                "");
        checkNoError();
    }

    public void testRelImport() throws Exception {

        analyzer = new OccurrencesAnalyzer();
        File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC +
                "relative/__init__.py");
        Document doc = new Document(FileUtils.getFileContents(file));
        msgs = analyzer.analyzeDocument(nature,
                (SourceModule) AbstractModule.createModule("relative.__init__", file, nature, true), prefs, doc,
                new NullProgressMonitor(), new TestIndentPrefs(true, 4));

        //no unused import message is generated
        printMessages(msgs, 0);
    }

    public void testImportNotFound8() throws Exception {

        analyzer = new OccurrencesAnalyzer();
        File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC +
                "testenc/encimport.py");
        Document doc = new Document(FileUtils.getFileContents(file));
        msgs = analyzer.analyzeDocument(nature,
                (SourceModule) AbstractModule.createModule("testenc.encimport", file, nature, true), prefs, doc,
                new NullProgressMonitor(), new TestIndentPrefs(true, 4));

        printMessages(msgs, 0);
    }

    public void testUnusedWildRelativeImport() throws Exception {

        analyzer = new OccurrencesAnalyzer();
        File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC +
                "testOtherImports/f1.py");
        Document doc = new Document(FileUtils.getFileContents(file));
        msgs = analyzer.analyzeDocument(nature,
                (SourceModule) AbstractModule.createModule("testOtherImports.f1", file, nature, true), prefs, doc,
                new NullProgressMonitor(), new TestIndentPrefs(true, 4));

        printMessages(msgs, 1);
        assertContainsMsg("Unused in wild import: SomeOtherTest, Test", msgs);
    }

    public void testImportNotFound5() {

        doc = new Document("from os import path as otherThing\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertContainsMsg("Unused import: otherThing", msgs);
    }

    public void testImportFound1() {

        doc = new Document("from os import path\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertContainsMsg("Unused import: path", msgs);
    }

    public void testReimport4() {

        doc = new Document("from testlib.unittest.relative import toimport\n"
                +
                "from testlib.unittest.relative import toimport\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 3);
        assertContainsMsg("Unused import: toimport", msgs, 1);
        assertContainsMsg("Unused import: toimport", msgs, 2);
        assertContainsMsg("Import redefinition: toimport", msgs);
        HashSet<IMessage> set = new HashSet<IMessage>();
        for (IMessage m : msgs) {
            set.add(m);
        }
        assertEquals(2, set.size()); //that's because we actually only have those 2 messages (but one appears 2 times)
    }

    public void testRelativeNotUndefined() throws Exception {

        analyzer = new OccurrencesAnalyzer();
        File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC +
                "testlib/unittest/relative/testrelative.py");
        Document doc = new Document(FileUtils.getFileContents(file));
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModule(
                "testlib.unittest.relative.testrelative", file, nature, true), prefs, doc, new NullProgressMonitor(),
                new TestIndentPrefs(true, 4));

        printMessages(msgs, 0);
    }

    public void testRelativeNotUndefined2() throws Exception {

        analyzer = new OccurrencesAnalyzer();
        File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC +
                "relative/mod2.py");
        Document doc = new Document(FileUtils.getFileContents(file));
        msgs = analyzer.analyzeDocument(nature,
                (SourceModule) AbstractModule.createModule("relative.mod2", file, nature, true), prefs, doc,
                new NullProgressMonitor(), new TestIndentPrefs(true, 4));

        printMessages(msgs, 1);
        assertEquals("Unused import: mod1", msgs[0].getMessage());
    }

    public void testLambda() {

        doc = new Document("a = lambda b: callit(b)\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertContainsMsg("Undefined variable: callit", msgs);
    }

    public void testLambda2() {

        doc = new Document("a = lambda c,*b: callit(c, *b)\n" +
                "\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertContainsMsg("Undefined variable: callit", msgs);
    }

    public void testReimport() {

        doc = new Document("import os \n" +
                "import os \n" +
                "print os  \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 2);
        IMessage message = assertContainsMsg("Import redefinition: os", msgs, 2);
        assertContainsMsg("Unused import: os", msgs, 1);
        assertEquals(8, message.getStartCol(doc));
        assertEquals(10, message.getEndCol(doc));
        assertEquals(2, message.getStartLine(doc));
    }

    public void testReimport2() {

        doc = new Document("import os \n" +
                "import os \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 3);
        assertContainsMsg("Import redefinition: os", msgs);
        assertContainsMsg("Unused import: os", msgs, 1);
        assertContainsMsg("Unused import: os", msgs, 2);
        HashSet<IMessage> set = new HashSet<IMessage>();
        for (IMessage m : msgs) {
            set.add(m);
        }
        assertEquals(2, set.size()); //that's because we actually only have those 2 messages (but one appears 2 times)

    }

    public void testReimport5() {

        doc = new Document("import os \n" +
                "print os  \n" +
                "import os \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 2);
        assertContainsMsg("Import redefinition: os", msgs, 3);
        assertContainsMsg("Unused import: os", msgs, 3);

    }

    public void testReimport3() {

        doc = new Document("import os      \n" +
                "def m1():      \n" +
                "    import os  \n" +
                "    print os   \n" +
                "\n"
                +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 2);
        assertEquals(2, msgs.length);
        assertContainsMsg("Import redefinition: os", msgs, 3);
        assertContainsMsg("Unused import: os", msgs, 1);
    }

    public void testUnusedVariable2() {

        //ignore the self
        doc = new Document("class Class1:         \n" +
                "    def met1(self, a):\n" +
                "        print 'foo'");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Unused parameter: a", msgs[0].getMessage());

    }

    public void testNoUnusedVariable() {
        doc = new Document("class Class1:         \n" +
                "    @classmethod\n" +
                "    def met1(cls):\n" +
                "        pass");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);

    }

    public void testUnusedVariable() {
        doc = new Document("def m1():    \n" +
                "    a = 1      ");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(1, msgs.length);
        assertEquals(TYPE_UNUSED_VARIABLE, msgs[0].getType());
        assertEquals("Unused variable: a", msgs[0].getMessage());

        doc = new Document("a = 1;print a");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(0, msgs.length);

    }

    public void test2UnusedVariables() {
        doc = new Document("def m1():          \n" +
                "    result = 10    \n" +
                "    \n" +
                "    if False:      \n"
                +
                "        result = 20\n" +
                "        \n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(2, msgs.length);
    }

    public void test3UnusedVariables() {
        doc = new Document("def m1():              \n" +
                "    dummyResult = 10   \n" +
                "    \n"
                +
                "    if False:          \n" +
                "        dummy2 = 20    \n" +
                "        \n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(0, msgs.length);
    }

    public void test4UnusedVariables() {
        doc = new Document("notdefined.aa().bb.cc\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals(1, msgs[0].getStartCol(doc));
    }

    public void test5UnusedVariables() {
        doc = new Document("notdefined.aa[10].bb.cc\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals(1, msgs[0].getStartCol(doc));
    }

    public void testNotUnusedVariable() {
        doc = new Document("def m1():          \n" +
                "    result = 10    \n" +
                "    \n" +
                "    if False:      \n"
                +
                "        result = 20\n" +
                "    \n" +
                "    print result   \n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);

    }

    public void testNotUnusedVariable4() {
        doc = new Document("def m1():             \n" +
                "    result = 10       \n" +
                "    \n"
                +
                "    while result > 0: \n" +
                "        result = 0    \n" +
                "        \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);

    }

    public void testNotUnusedVariable5() {
        doc = new Document("def m():         \n" +
                "    try:         \n" +
                "        c = 'a'  \n"
                +
                "    except:      \n" +
                "        c = 'b'  \n" +
                "    print c      \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);

    }

    public void testNotUnusedVariable6() {
        doc = new Document("def m():         \n" +
                "    try:         \n" +
                "        c = 'a'  \n"
                +
                "    finally:     \n" +
                "        c = 'b'  \n" +
                "    print c      \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);

    }

    public void testNotUnusedVariable7() {
        doc = new Document("def m(a, b):                 \n" +
                "    raise RuntimeError('err')\n" +
                "");
        checkNoError();
        assertEquals(0, msgs.length);

    }

    public void testNotUnusedVariable8() {
        doc = new Document("def m(a, b):                 \n" +
                "    '''test'''               \n"
                +
                "    raise RuntimeError('err')\n" +
                "");
        checkNoError();
        assertEquals(0, msgs.length);

    }

    public void testUnusedVariable8() {
        doc = new Document("def outer(show=True):     \n" +
                "    def inner(show):      \n"
                +
                "        print show        \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals(1, msgs.length);
        assertContainsMsg("Unused parameter: show", msgs, 1);
    }

    public void testUnusedVariable9() {
        doc = new Document("def outer(show=True):        \n" +
                "    def inner(show=show):    \n"
                +
                "        print 'foo'          \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals(1, msgs.length);
        assertContainsMsg("Unused parameter: show", msgs, 2);
    }

    public void testUnusedVariable10() {
        doc = new Document("def outer(show):        \n" +
                "    def inner(show):    \n" +
                "        print 'foo'     \n"
                +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 2);
        assertEquals(2, msgs.length);
        assertContainsMsg("Unused parameter: show", msgs, 1);
        assertContainsMsg("Unused parameter: show", msgs, 2);
    }

    private IMessage[] analyzeDoc() {
        SourceModule mod;
        try {
            mod = AbstractModule.createModuleFromDoc(null, null, doc, nature, true);
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
        if (mod.parseError != null) {
            throw new RuntimeException(mod.parseError);
        }
        return analyzer.analyzeDocument(nature, mod, prefs, doc, new NullProgressMonitor(),
                new TestIndentPrefs(true, 4));
    }

    public void testUnusedVariable6() throws Exception {
        doc = new Document("def m():         \n" +
                "    try:         \n" +
                "        c = 'a'  \n"
                +
                "    finally:     \n" +
                "        c = 'b'  \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 2);
        assertEquals(2, msgs.length);

    }

    public void testUnusedVariable7() throws Exception {
        doc = new Document("def m( a, b ):       \n" +
                "    def m1( a, b ):  \n" +
                "        print a, b   \n"
                +
                "    if a:            \n" +
                "        print 'ok'   \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals(1, msgs.length);
        assertContainsMsg("Unused parameter: b", msgs, 1);

    }

    public void testNotUnusedVariable2() {
        doc = new Document("def GetValue( option ):         \n" +
                "    return int( option ).Value()\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);

    }

    public void testNotUnusedVariable3() {
        doc = new Document("def val(i):    \n" +
                "    i = i + 1  \n" +
                "    print i    \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testUndefinedVar() {
        doc = new Document("def GetValue():         \n" +
                "    return int( option ).Value()\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals(1, msgs.length);
        assertContainsMsg("Undefined variable: option", msgs);
    }

    public void testScopes() {
        //2 messages with token with same name
        doc = new Document("def m1():       \n" +
                "    def m2():   \n" +
                "        print a \n" +
                "    a = 10        ");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testScopes2() {
        doc = new Document("class Class1:              \n" +
                "    c = 1                  \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testScopes3() {
        doc = new Document("class Class1:              \n" +
                "    def __init__( self ):  \n"
                +
                "        print Class1       \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testScopes4() {
        doc = new Document("def rec():           \n" +
                "    def rec2():      \n" +
                "        return rec2  \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testScopes5() {
        doc = new Document("class C:       \n" +
                "    class I:   \n" +
                "        print I\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertContainsMsg("Undefined variable: I", msgs);
    }

    public void testScopes5a() {
        doc = new Document("class Internal:\n" +
                "    print Internal\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertContainsMsg("Undefined variable: Internal", msgs);
    }

    public void testScopes6() {
        doc = new Document("def ok():          \n" +
                "    print col      \n" +
                "def rowNotEmpty(): \n"
                +
                "    col = 1        \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 2);
        assertContainsMsg("Undefined variable: col", msgs, 2);
        assertContainsMsg("Unused variable: col", msgs, 4);
    }

    public void testScopes7() {
        doc = new Document("def ok():          \n" +
                "    def call():    \n" +
                "        call2()    \n" +
                "    \n"
                +
                "    def call2():   \n" +
                "        pass\n" +
                "");
        checkNoError();
    }

    public void testScopes8() {
        doc = new Document("def m1():                      \n" +
                "    print (str(undef)).lower() \n" +
                "    \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertContainsMsg("Undefined variable: undef", msgs, 2);

    }

    public void testScopes9() {
        doc = new Document("def m1():                      \n" +
                "    undef = 10                 \n"
                +
                "    print (str(undef)).lower() \n" +
                "    \n" +
                "");
        checkNoError();

    }

    public void testScopes10() {
        doc = new Document("class C:\n" +
                "    def m1(self):\n" +
                "        print m2\n" + //should give error, as we are inside the method (and not in the class scope)
                "    def m2(self):\n" +
                "        print m1\n" + //should give error, as we are inside the method (and not in the class scope)
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 2);

    }

    public void testSameName() {
        //2 messages with token with same name
        doc = new Document("def m1():\n" +
                "    a = 1\n" +
                "    a = 2");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(2, msgs.length);
    }

    public void testVarArgs() {
        doc = new Document("def m1(*args): \n" +
                "    print args   ");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(0, msgs.length);
    }

    public void testVarArgsNotUsed() {
        doc = new Document("\n" +
                "def m1(*args): \n" +
                "    print 'foo'  ");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(1, msgs.length);
        assertContainsMsg("Unused parameter: args", msgs, 2);
        assertEquals(9, msgs[0].getStartCol(doc));
        assertEquals(13, msgs[0].getEndCol(doc));
    }

    public void testKwArgs() {
        doc = new Document("def m1(**kwargs): \n" +
                "    print kwargs    ");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testKwArgs2() {
        doc = new Document("def m3():             \n" +
                "    def m1(**kwargs): \n" +
                "        print 'foo'     ");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals(1, msgs.length);
        assertContainsMsg("Unused parameter: kwargs", msgs, 2);
        assertEquals(14, msgs[0].getStartCol(doc));
        assertEquals(20, msgs[0].getEndCol(doc));
    }

    public void testOtherScopes() {
        //2 messages with token with same name
        doc = new Document("def m1(  aeee  ): \n" +
                "    print 'foo'   \n" +
                "def m2(  afff  ): \n"
                +
                "    print 'foo'     ");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(2, msgs.length);
    }

    public void testUndefinedVariable() {
        //2 messages with token with same name
        doc = new Document("print a");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(1, msgs.length);
        assertEquals(TYPE_UNDEFINED_VARIABLE, msgs[0].getType());
        assertEquals("Undefined variable: a", msgs[0].getMessage());
    }

    public void testUndefinedVariable2() {
        doc = new Document("a = 10      \n" + //global scope - does not give msg
                "def m1():   \n" +
                "    a = 20  \n" +
                "    print a \n" +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testDecoratorUndefined() {
        doc = new Document("@notdefined \n" +
                "def m1():   \n" +
                "    pass    \n" +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();
        printMessages(msgs, 1);
        IMessage msg = msgs[0];
        assertEquals("Undefined variable: notdefined", msg.getMessage());
        assertEquals(2, msg.getStartCol(doc));
        assertEquals(1, msg.getStartLine(doc));
    }

    public void testClassDecoratorUndefined() {
        doc = new Document("@notdefined \n" +
                "class Foo:   \n" +
                "    pass    \n" +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();
        printMessages(msgs, 1);
        IMessage msg = msgs[0];
        assertEquals("Undefined variable: notdefined", msg.getMessage());
        assertEquals(2, msg.getStartCol(doc));
        assertEquals(1, msg.getStartLine(doc));
    }

    public void testUndefinedVariable3() {
        doc = new Document("a = 10      \n" + //global scope - does not give msg
                "def m1():   \n" +
                "    a = 20  \n" +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals(1, msgs.length);
        assertEquals(TYPE_UNUSED_VARIABLE, msgs[0].getType());
        assertEquals("Unused variable: a", msgs[0].getMessage());
        assertEquals(3, msgs[0].getStartLine(doc));
    }

    public void testOk() {
        //all ok...
        doc = new Document("import os   \n" +
                "            \n" +
                "def m1():   \n" +
                "    print os\n" +
                "            \n"
                +
                "print m1    \n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testImportAfter() {
        doc = new Document("def met():          \n" +
                "    print os.path   \n" +
                "import os.path      \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testImportAfter2() {
        doc = new Document("def met():          \n" +
                "    print os.path   \n" +
                "import os           \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testImportPartial() {
        doc = new Document("import os.path   \n" +
                "print os         \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(1, msgs.length);
        assertContainsMsg("Unused import: os.path", msgs);
    }

    public void testImportAs() {
        doc = new Document("import os.path as bla   \n" +
                "print bla               \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testImportAs2() {
        doc = new Document("import os.path as bla   \n" +
                "print os.path           \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 2);
        assertEquals(2, msgs.length);
        assertContainsMsg("Undefined variable: os", msgs);
        assertContainsMsg("Unused import: bla", msgs);
    }

    public void testImportAs3() {
        doc = new Document("import os.path as bla   \n" +
                "print os                \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(2, msgs.length);
        assertContainsMsg("Undefined variable: os", msgs);
        assertContainsMsg("Unused import: bla", msgs);
    }

    public void testAttributeImport() {
        //all ok...
        doc = new Document("import os.path      \n" +
                "print os.path       \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testGlobal() {
        //no need to warn if global variable is unused (and it should be defined at the global definition)
        doc = new Document("def m():                         \n" +
                "    global __progress            \n"
                +
                "    __progress = __progress + 1  \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testGlobal2() {
        //no need to warn if global variable is unused (and it should be defined at the global definition)
        doc = new Document("def m():                         \n" +
                "    global __progress            \n"
                +
                "    __progress = 1               \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testGlobal3() {
        doc = new Document("global typ\n" +
                "typ = 10\n");
        checkNoError();
    }

    public void testAttributeImportAccess() {
        //all ok...
        doc = new Document("import os           \n" +
                "print os.path       \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testAttributeAccessMsg() {
        //all ok...
        doc = new Document("s.a = 10            \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(1, msgs.length);
        assertEquals("Undefined variable: s", msgs[0].getMessage());
    }

    public void testAttributeAccess() {
        //all ok...
        doc = new Document("def m1():               \n" +
                "    class Stub():pass   \n" +
                "    s = Stub()          \n"
                +
                "    s.a = 10            \n" +
                "    s.b = s.a           \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testAttributeAccess2() {
        //all ok...
        doc = new Document("class TestCase:                                    \n"
                +
                "    def test(self):                                \n"
                +
                "        db = 10                                    \n"
                +
                "        comp = db.select(1)                        \n"
                +
                "        aa.bbb.cccc[comp.id].hasSimulate = True    \n" +
                "        \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        //comp should be used
        //aa undefined (check pos)
        assertNotContainsMsg("Unused variable: comp", msgs);
        assertContainsMsg("Undefined variable: aa", msgs, 5);
        assertEquals(1, msgs.length);
        assertEquals(9, msgs[0].getStartCol(doc));
    }

    public void testAttributeErrorPos() {
        //all ok...
        doc = new Document("print message().bla\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals(1, msgs.length);
        assertEquals("Undefined variable: message", msgs[0].getMessage());
        assertEquals(7, msgs[0].getStartCol(doc));
        assertEquals(14, msgs[0].getEndCol(doc));
    }

    public void testAttributeErrorPos2() {
        //all ok...
        doc = new Document("lambda x: os.rmdir( x )\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals(1, msgs.length);
        assertEquals("Undefined variable: os", msgs[0].getMessage());
        assertEquals(11, msgs[0].getStartCol(doc));
        assertEquals(13, msgs[0].getEndCol(doc));
    }

    public void testAttributeErrorPos3() {
        //all ok...
        doc = new Document("os.rmdir( '' )\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals(1, msgs.length);
        assertEquals("Undefined variable: os", msgs[0].getMessage());
        assertEquals(1, msgs[0].getStartCol(doc));
        assertEquals(3, msgs[0].getEndCol(doc));
    }

    public void testImportAttr() {
        //all ok...
        doc = new Document("import os.path                 \n" +
                "if os.path.isfile( '' ):pass   \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testSelf() {
        //all ok...
        doc = new Document("class C:            \n" +
                "    def m1(self):   \n" +
                "        print self  \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testDefinitionLater() {
        doc = new Document("def m1():     \n" +
                "    print m2()\n" +
                "    \n" +
                "def m2():     \n" +
                "    pass      \n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testDefinitionLater2() {
        doc = new Document("def m():                \n" +
                "    AroundContext().m1()\n" +
                "                        \n"
                +
                "class AroundContext:    \n" +
                "    def m1(self):       \n" +
                "        pass            \n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);

    }

    public void testNotDefinedLater() {
        doc = new Document("def m1():     \n" +
                "    print m2()\n" +
                "    \n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(1, msgs.length);
    }

    public void testNotDefinedLater2() {
        doc = new Document("def m1():     \n" +
                "    print c   \n" +
                "    c = 10    \n" +
                "    \n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(2, msgs.length);
    }

    public void testUndefinedVariableBuiltin() {
        doc = new Document("print False");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(0, msgs.length);
    }

    public void testUndefinedVariableBuiltin2() {
        doc = new Document("print __file__" //source folder always has the builtin __file__
        );
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testUndefinedVariableFromBuiltinModule() {
        doc = new Document("import os\n" +
                "print os.__file__\n" +
                "print os.__name__\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testUndefinedVariableFromSourceModule() {
        doc = new Document("import testlib\n" +
                "print testlib.__file__\n" +
                "print testlib.__name__");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testUndefinedVariableBuiltin3() {
        doc = new Document("print [].__str__" //[] is a builtin
        );
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testSelfAttribute() {
        doc = new Document("class C:                          \n" +
                "    def m2(self):                 \n"
                +
                "        self.m1 = ''              \n" +
                "        print self.m1.join('a').join('b')   \n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testBuiltinAcess() {
        doc = new Document("print file.read");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(0, msgs.length);
    }

    public void testDictAcess() {
        doc = new Document("def m1():\n" +
                "    k = {}                   \n" +
                "    print k[0].append(10)   ");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testAttribute1() {
        doc = new Document("def m1():\n" +
                "    file( 10, 'r' ).read()");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(0, msgs.length);
    }

    public void testAttributeFloat() {
        doc = new Document("def m1():\n" +
                "    v = 1.0.__class__\n" +
                "    print v            ");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testAttributeString() {
        doc = new Document("def m1():\n" +
                "    v = 'r'.join('a')\n" +
                "    print v            ");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testAttributeString2() {
        doc = new Document("def m1():\n" +
                "    v = 'r.a.s.b'.join('a')\n" +
                "    print v            ");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testUnusedInFor() {
        doc = new Document("def test():\n" +
                "    for a in range(10):\n" + //a is unused
                "        pass");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
    }

    public void testUnusedInFor2() {
        doc = new Document("def problemFunct():\n" +
                "    msg='initialised'\n" +
                "    for i in []:\n"
                +
                "        msg='success at %s' % i\n" +
                "    return msg\n");
        checkNoError();
    }

    public void testTupleVar() {
        doc = new Document("def m1():\n" +
                "    print (0,0).__class__");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testStaticNoSelf() {
        doc = new Document("class C:\n" +
                "    @staticmethod\n" +
                "    def m():\n" +
                "        pass\n" +
                "\n" +
                "\n"
                +
                "");
        checkNoError();
    }

    public void testClassMethodCls() {
        doc = new Document("class C:\n" +
                "    @classmethod\n" +
                "    def m(cls):\n" +
                "        print cls\n" +
                "\n"
                +
                "\n" +
                "");
        checkNoError();
    }

    public void testClassMethodCls2() {
        doc = new Document("class C:\n" +
                "    def m(cls):\n" +
                "        print cls\n" +
                "    m = classmethod(m)" +
                "\n"
                +
                "\n" +
                "");
        checkNoError();
    }

    public void testClassMethodCls3() {
        doc = new Document("class C:\n" +
                "    def m():\n" +
                "        pass\n" +
                "    m = classmethod(m)" +
                "\n" +
                "\n"
                +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Method 'm' should have cls as first parameter", msgs[0].toString());
    }

    public void testStaticNoSelf2() {
        doc = new Document("class C:\n" +
                "    def m():\n" +
                "        pass\n" +
                "    m = staticmethod(m)\n" +
                "\n" +
                "");
        checkNoError();
    }

    public void testNoSelf() {
        doc = new Document("class C:\n" +
                "    def m():\n" +
                "        pass\n" +
                "\n" +
                "\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Method 'm' should have self as first parameter", msgs[0].getMessage());
    }

    public void testTupleVar2() {
        doc = new Document("def m1():\n" +
                "    print (10 / 10).__class__");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs);
        assertEquals(0, msgs.length);
    }

    public void testDuplicatedSignature() {
        //2 messages with token with same name
        doc = new Document("class C:             \n" +
                "    def m1(self):pass\n" +
                "    def m1(self):pass\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(1, msgs.length);
        assertEquals(TYPE_DUPLICATED_SIGNATURE, msgs[0].getType());
        assertEquals("Duplicated signature: m1", msgs[0].getMessage());
        assertEquals(9, msgs[0].getStartCol(doc));

        //ignore
        prefs.severityForDuplicatedSignature = IMarker.SEVERITY_INFO;
        doc = new Document("class C:             \n" +
                "    def m1(self):pass\n" +
                "    def m1(self):pass\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(1, msgs.length); //it is created, but in ignore mode
        assertEquals(IMarker.SEVERITY_INFO, msgs[0].getSeverity());

    }

    public void testDuplicatedSignatureNotOnProperty() {
        doc = new Document("class C:             \n" +
                "    @property\n" +
                "    def m1(self):pass\n"
                +
                "    @m1.setter\n" +
                "    def m1(self):pass\n" +
                "    @m1.deleter\n" +
                "    def m1(self):pass\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testUndefinedWithTab() {
        doc = new Document("def m():\n" +
                "\tprint a\n" +
                "\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        IMessage msg = assertContainsMsg("Undefined variable: a", msgs);
        assertEquals(8, msg.getStartCol(doc));
    }

    public void testUnusedVar() {
        doc = new Document("def test(data):\n" +
                "    return str(data)[0].strip()\n" +
                "\n");
        checkNoError();
    }

    public void testUndefinedVar1() {
        doc = new Document("return (data)[0].strip()");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Undefined variable: data", msgs[0].getMessage());
    }

    public void testNotUnusedParameter() {
        doc = new Document("def a(x):pass");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testUnusedParameter() {
        doc = new Document("def a(x):print 'foo'");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Unused parameter: x", msgs[0].getMessage());
    }

    public void testUnusedParameter2() {
        doc = new Document("def a(*x):print 'foo'");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Unused parameter: x", msgs[0].getMessage());
    }

    public void testUnusedParameter3() {
        doc = new Document("def a(**x):print 'foo'");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Unused parameter: x", msgs[0].getMessage());
    }

    public void testEmptyDict() {
        doc = new Document("for k,v in {}.iteritmes(): print k,v");
        checkNoError();
    }

    public void testDirectDictAccess() {
        doc = new Document("def Load(self):\n" +
                "    #Is giving Unused variable: i\n"
                +
                "    for i in xrange(10):    \n" +
                "        coerce(dict[i].text.strip())\n");
        checkNoError();
    }

    public void testDefinedInClassAndInLocal() {
        doc = new Document("class MyClass(object):\n" +
                "    foo = 10\n" +
                "    \n" +
                "    def mystery(self):\n"
                +
                "        for foo in range(12):\n" +
                "            print foo\n" +
                "\n");
        checkNoError();
    }

    public void testDefinedInClassAndInLocal2() {
        doc = new Document("class MyClass(object):\n" +
                "    options = [i for i in range(10)]\n" +
                "    \n"
                +
                "    def mystery(self):\n" +
                "        for i in range(12):\n"
                +
                "            print i #should not be undefined!\n" +
                "\n");
        checkNoError();
    }

    public void testColError() {
        doc = new Document("print function()[0].strip()");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Undefined variable: function", msgs[0].getMessage());
        assertEquals(7, msgs[0].getStartCol(doc));
    }

    public void testColError2() {
        doc = new Document("" +
                "class Foo(object):\n" +
                "    def  m1(self):\n" +
                "        pass\n"
                +
                "    def  m1(self):\n" +
                "        pass\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Duplicated signature: m1", msgs[0].getMessage());
        assertEquals(10, msgs[0].getStartCol(doc));
    }

    public void testColError3() {
        doc = new Document("" +
                "class  Foo(object):\n" +
                "    pass\n" +
                "class  Foo(object):\n" +
                "    pass\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Duplicated signature: Foo", msgs[0].getMessage());
        assertEquals(8, msgs[0].getStartCol(doc));
    }

    public void testDupl2() {
        doc = new Document("" +
                "if True:\n" +
                "    def f(self):\n" +
                "        pass\n" +
                "else:\n"
                +
                "    def f(self):\n" +
                "        pass\n" +
                "\n" +
                "");
        checkNoError();
    }

    public void testNoEffect() {
        doc = new Document("" +
                "2 == 1\n" + //has no effect
                "if 2 == 1: pass\n" + //has effect
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Statement apppears to have no effect", msgs[0].getMessage());
    }

    public void testNoEffect2() {
        doc = new Document("" +
                "a = 5\n" +
                "a == 1\n" + //has no effect
                "if a == 1: pass\n" + //has effect
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Statement apppears to have no effect", msgs[0].getMessage());
    }

    public void testNoEffect3() {
        doc = new Document("" +
                "a = 5\n" +
                "if a == 1: \n" + //has effect
                "    a == 1\n" + //has no effect
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Statement apppears to have no effect", msgs[0].getMessage());
    }

    public void testNoEffectOk() {
        doc = new Document("" +
                "assert 10 == 5\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testNoEffectOk2() {
        doc = new Document("" +
                "a = 10\n" +
                "a = a == 5\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testNoEffectOk3() {
        doc = new Document("" +
                "a = 10\n" +
                "a += a == 5\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testNoEffectOk4() {
        doc = new Document("" +
                "print 10 == 5\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testNoEffectOk5() {
        doc = new Document("" +
                "def check(a):\n" +
                "    pass\n" +
                "check(1 in [1,2])\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testNoEffectOk6() {
        doc = new Document("" +
                "def check():\n" +
                "    return 1 == 2\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testNoEffectOk7() {
        doc = new Document("" +
                "def check():\n" +
                "    yield 1 == 2\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testInternalClassDefinition() {
        doc = new Document("" +
                "class Container:\n" +
                "    \n" +
                "    def Method(self):\n" +
                "        \n"
                +
                "        class ClassSuper:\n" +
                "            pass\n" +
                "        class ClassSub(ClassSuper):\n"
                +
                "            pass\n" +
                "    \n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testInvalidStatementNoEffect() {
        doc = new Document("" +
                "@property('a' in [1])\n" +
                "@property(lambda: 'a' in [1])\n" +
                "def test_hallo():\n"
                +
                "    lambda: 'a' in [1]\n" +
                "    return 'Hallo'\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testInternalClassDefinition2() {
        doc = new Document("" +
                "class Obj:\n" +
                "    \n" +
                "    class EmptyObj:\n" +
                "        pass\n" +
                "\n"
                +
                "    EmptyObj\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testStarExp() throws Throwable {
        doc = new Document("" +
                "a, *b = [1, 2, 3]\n" +
                "a, b");

        int original = GRAMMAR_TO_USE_FOR_PARSING;
        GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_5;
        try {
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();
            printMessages(msgs, 0);
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = original;
        }
    }

    public void testNotDefinedInAlll() {
        String s = "from extendable.all_check import *\n" +
                "print ThisDoesnt\n" +
                "print ThisGoes\n"
                +
                "print ThisGoesToo\n" +
                "";

        doc = new Document(s);
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Undefined variable: ThisDoesnt", msgs[0].toString());
    }

    public void testHasAttrDefinesIt() {
        String s = "import extendable\n" +
                "if hasattr(extendable, 'errrrrrr'):\n" +
                "    extendable.errrrrrr\n" +
                "";

        doc = new Document(s);
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testHasAttrDefinesIt2() {
        String s = "import extendable\n" +
                "\n" +
                "def m1():\n" +
                "    if hasattr(extendable, 'bbb'):\n"
                +
                "        extendable.bbb\n" + //this is ok
                "        \n" +
                "def m2():\n" + //but this should give an error
                "    extendable.bbb\n" +
                "";

        doc = new Document(s);
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
    }

    public void testNoImportRedefinition() throws Exception {
        String s = "import extendable.all_check\n" +
                "import extendable.all_check2\n" +
                "print extendable.all_check\n"
                +
                "print extendable.all_check2\n" +
                "";

        doc = new Document(s);
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testRecursionCondition() throws Exception {

        analyzer = new OccurrencesAnalyzer();
        File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC +
                "extendable/recursion_on_non_existent/unexistent_import.py");
        Document doc = new Document(FileUtils.getFileContents(file));
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModule(
                "extendable.recursion_on_non_existent.unexistent_import", file, nature, true), prefs, doc,
                new NullProgressMonitor(), new TestIndentPrefs(true, 4));

        //unused and unresolved
        printMessages(msgs, 2);
    }

    public void testRelativeWithWildCard() throws Exception {

        analyzer = new OccurrencesAnalyzer();
        File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC +
                "extendable/relative_wildcard/mymod2.py");
        Document doc = new Document(FileUtils.getFileContents(file));
        msgs = analyzer.analyzeDocument(nature,
                (SourceModule) AbstractModule.createModule("extendable.relative_wildcard.mymod2", file, nature, true),
                prefs, doc, new NullProgressMonitor(), new TestIndentPrefs(true, 4));

        printMessages(msgs, 0);
    }

    public void testModuleNotFoundOnRelativeAndFullMixed() throws Exception {

        analyzer = new OccurrencesAnalyzer();
        File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC +
                "extendable/relative_and_full_mixed/pluginstestcaseext.py");
        Document doc = new Document(FileUtils.getFileContents(file));
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModule(
                "extendable.relative_and_full_mixed.pluginstestcaseext", file, nature, true), prefs, doc,
                new NullProgressMonitor(), new TestIndentPrefs(true, 4));

        printMessages(msgs, 0);
    }

    public void testModuleNotFoundOnRelativeAbsolute() throws Exception {

        analyzer = new OccurrencesAnalyzer();
        File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC +
                "extendable/relative_absolute_import/__init__.py");
        Document doc = new Document(FileUtils.getFileContents(file));
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModule(
                "extendable.relative_absolute_import.__init__", file, nature, true), prefs, doc,
                new NullProgressMonitor(), new TestIndentPrefs(true, 4));

        printMessages(msgs, 0);
    }

    public void testImportErrorPattern() throws Exception {

        analyzer = new OccurrencesAnalyzer();
        File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC +
                "extendable/redefinition_in_import/check_access.py");
        Document doc = new Document(FileUtils.getFileContents(file));
        msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModule(
                "extendable.redefinition_in_import.check_access.py", file, nature, true), prefs, doc,
                new NullProgressMonitor(), new TestIndentPrefs(true, 4));

        printMessages(msgs, 0);
    }

    public void testPython30() {
        int initial = GRAMMAR_TO_USE_FOR_PARSING;
        try {
            GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_5;
            doc = new Document("def func(arg, *, arg2=None):\n" +
                    "    arg, arg2");
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();

            printMessages(msgs);
            assertEquals(0, msgs.length);
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = initial;
        }
    }

    public void testPython30UnusedParameter() {
        int initial = GRAMMAR_TO_USE_FOR_PARSING;
        try {
            GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_5;
            doc = new Document("class Class1:         \n" +
                    "    def met1(self, *, a=2):\n" +
                    "        self.x = 20");
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();

            printMessages(msgs, 1);
            assertEquals("Unused parameter: a", msgs[0].getMessage());
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = initial;
        }
    }

    public void testDictComprehension() {
        int initial = GRAMMAR_TO_USE_FOR_PARSING;
        try {
            GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_5;
            doc = new Document("class Bar(object):\n" +
                    "    def __init__(self, row):\n"
                    +
                    "        self.__dict__.update({'Input.'+k: v for k,v in row.items()})\n" +
                    "");
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();

            printMessages(msgs, 0);
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = initial;
        }
    }

    public void testSetComprehension() {
        int initial = GRAMMAR_TO_USE_FOR_PARSING;
        try {
            GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_5;
            doc = new Document("class Bar(object):\n" +
                    "    def __init__(self, row):\n"
                    +
                    "        self.__dict__.update({v for v in row.items()})\n"
                    +
                    "        self.__dict__.update({v for v in (w for w in row.keys())})\n" + //all works well here
                    "        self.__dict__.update({v for v in jj for jj in row.keys()})\n" + //jj is not defined in this case
                    "");
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();

            printMessages(msgs, 2);
            assertContainsMsg("Unused variable: jj", msgs);
            assertContainsMsg("Undefined variable: jj", msgs);
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = initial;
        }
    }

    public void testElipsis() {
        int initial = GRAMMAR_TO_USE_FOR_PARSING;
        try {
            GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_5;
            doc = new Document("class Bar:\n" +
                    "    ...\n" +
                    "");
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();

            printMessages(msgs, 0);
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = initial;
        }
    }

    public void testBoolInClassConstructor() {
        int initial = GRAMMAR_TO_USE_FOR_PARSING;
        try {
            GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_5;
            doc = new Document("class Bar(A or B):\n" +
                    "    ...\n" +
                    "");
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();

            printMessages(msgs, 2); //A and B not defined.
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = initial;
        }
    }

    public void testDictAccess() throws Exception {

        doc = new Document("import unittest\n" +
                "unittest.__dict__\n");
        checkNoError();
    }

    public void testNoQa() throws Exception {
        doc = new Document("print(a) #noqa\n");
        checkNoError();
    }

    public void testNoQa2() throws Exception {
        doc = new Document("print(a) #noqa: foo\n");
        msgs = analyzeDoc();
        printMessages(msgs, 1);
    }

    public void testNoQa3() throws Exception {
        doc = new Document("print(a) #noqa: undefined-variable\n");
        checkNoError();
    }

    public void testBuiltInAssignment() {
        int initial = GRAMMAR_TO_USE_FOR_PARSING;
        try {
            GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_5;
            doc = new Document("import os as list \n" +
                    "def Foo():\n" +
                    "	id = 10 \n" +
                    "	for tuple in range(3):	\n"
                    +
                    "		val = tuple \n" +
                    "	try: \n" +
                    "		val += val/0 \n" +
                    "	except ZeroDivisionError as range: \n"
                    +
                    "		pass \n" +
                    "");
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();

            printMessages(msgs, 5); // list, id, tuple and range are reserved
                                    // keywords.
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = initial;
        }
    }

    public void testBuiltInAssignment2() {
        doc = new Document("def m1(*list, **dict):\n" +
                "    pass");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 2); // list, dict
    }

    public void testBuiltInAssignment3() {
        doc = new Document("def list():\n" +
                "    pass\n" +
                "class dict:\n" +
                "    pass\n" +
                "global id");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 3); // list, dict, id
    }

    public void testBuiltInAssignment4() {
        int initial = GRAMMAR_TO_USE_FOR_PARSING;
        try {
            GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_5;
            doc = new Document("def Method(a:lambda list:None) -> lambda list:None:\n" +
                    "    pass\n" +
                    "");
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();

            printMessages(msgs, 2);
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = initial;
        }
    }

    public void testBuiltInAssignment5() {
        doc = new Document("class A:\n" +
                "    def open(self):\n" +
                "        pass" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testBuiltInAssignment6() {
        doc = new Document("class A:\n" +
                "    id = 10\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testBuiltInAssignment7() {
        doc = new Document("def call(**kwargs):\n" +
                "    pass\n" +
                "call(id=10)\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testDefaultMarksAsUsed() {
        int initial = GRAMMAR_TO_USE_FOR_PARSING;
        try {
            GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_5;
            doc = new Document("import threading\n" +
                    "def method(*, t=threading):\n"
                    + "    print(t)\n" +
                    "");
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();

            printMessages(msgs, 0);
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = initial;
        }
    }

    public void testAnnotationNotImported() {
        int initial = GRAMMAR_TO_USE_FOR_PARSING;
        try {
            GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_5;
            doc = new Document("\n" +
                    "def method(*, t=threading):\n"
                    + "    print(t)\n" +
                    "");
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();

            printMessages(msgs, 1);
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = initial;
        }
    }

    public void testAnnotationMarksAsUsed() {
        int initial = GRAMMAR_TO_USE_FOR_PARSING;
        try {
            GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_5;
            doc = new Document("\n" +
                    "def method(*, t:threading=None):\n"
                    + "    print(t)\n" +
                    "");
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();

            printMessages(msgs, 1);
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = initial;
        }
    }

    public void testDefaultFromScope() {
        int initial = GRAMMAR_TO_USE_FOR_PARSING;
        try {
            GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_5;
            doc = new Document("\n" +
                    "class F(object):\n" +
                    "    \n" +
                    "    another = 1\n" +
                    "    \n" +
                    "    def method(self, *, a=another):\n" +
                    "        print(a)\n" +
                    "");
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();

            printMessages(msgs, 0);
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = initial;
        }
    }

    public void testAnnotationFromScope() {
        int initial = GRAMMAR_TO_USE_FOR_PARSING;
        try {
            GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_5;
            doc = new Document("\n" +
                    "class F(object):\n" +
                    "    \n" +
                    "    another = 1\n" +
                    "    \n" +
                    "    def method(self, *, a:another):\n" +
                    "        print(a)\n" +
                    "");
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();

            printMessages(msgs, 0);
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = initial;
        }
    }

    public void testRelativeOnPy3() throws IOException, MisconfigurationException {
        int initial = GRAMMAR_TO_USE_FOR_PARSING;
        try {
            GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_5;
            analyzer = new OccurrencesAnalyzer();
            File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC +
                    "extendable/grammar3/sub1.py");
            Document doc = new Document(FileUtils.getFileContents(file));
            msgs = analyzer.analyzeDocument(nature,
                    (SourceModule) AbstractModule.createModule("extendable.grammar3.sub1", file, nature, true), prefs,
                    doc, new NullProgressMonitor(), new TestIndentPrefs(true, 4));

            printMessages(msgs, 1); //Errors in Python 3.x
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = initial;
        }
    }

    public void testRelativeOnPy2() throws IOException, MisconfigurationException {
        analyzer = new OccurrencesAnalyzer();
        File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC +
                "extendable/grammar3/sub1.py");
        Document doc = new Document(FileUtils.getFileContents(file));
        msgs = analyzer.analyzeDocument(nature,
                (SourceModule) AbstractModule.createModule("extendable.grammar3.sub1", file, nature, true), prefs, doc,
                new NullProgressMonitor(), new TestIndentPrefs(true, 4));

        printMessages(msgs, 0); //No errors in Python 2.x
    }

    public void testImportSelf() throws IOException, MisconfigurationException {
        analyzer = new OccurrencesAnalyzer();
        File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC +
                "importself/__init__.py");
        Document doc = new Document(FileUtils.getFileContents(file));
        msgs = analyzer.analyzeDocument(nature,
                (SourceModule) AbstractModule.createModule("importself.__init__", file, nature, true), prefs, doc,
                new NullProgressMonitor(), new TestIndentPrefs(true, 4));

        printMessages(msgs, 0); //No errors in Python 2.x
    }

    public void testImportSelf2() throws IOException, MisconfigurationException {
        analyzer = new OccurrencesAnalyzer();
        File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC +
                "importself/importself2.py");
        Document doc = new Document(FileUtils.getFileContents(file));
        msgs = analyzer.analyzeDocument(nature,
                (SourceModule) AbstractModule.createModule("importself.importself2", file, nature, true), prefs, doc,
                new NullProgressMonitor(), new TestIndentPrefs(true, 4));

        printMessages(msgs, 1); //Unused import
        assertContainsMsg("Unused import: importself.importself2", msgs);
    }

    public void testReportSingleErrorOnAttributeAccessWithCalls() {
        doc = new Document(""
                + "NotDefined.object.Check(\n"
                + "    ).Foo(\n"
                + "    ).Bar(\n"
                + "    )\n"
                + "");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals(1, msgs[0].getStartLine(doc));
    }

    public void testRelativeImport() throws IOException, MisconfigurationException {
        analyzer = new OccurrencesAnalyzer();
        File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC +
                "mod/mod1/test_relative.py");
        Document doc = new Document(FileUtils.getFileContents(file));
        msgs = analyzer.analyzeDocument(nature,
                (SourceModule) AbstractModule.createModule("mod.mod1.test_relative", file, nature, true), prefs, doc,
                new NullProgressMonitor(), new TestIndentPrefs(true, 4));

        printMessages(msgs, 0); //No errors in Python 2.x
    }
}
