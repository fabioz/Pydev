/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 1, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings.renamelocal;

import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.shared_core.SharedCorePlugin;

public class RenameLocalVariableRefactoringTest extends RefactoringLocalTestBase {

    public static void main(String[] args) {
        try {
            DEBUG = true;
            RenameLocalVariableRefactoringTest test = new RenameLocalVariableRefactoringTest();
            test.setUp();
            test.testRenameImportLocally3();
            test.tearDown();

            junit.textui.TestRunner.run(RenameLocalVariableRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = true;
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
    }

    private String getDefaultDocStr() {
        return "" +
                "def method():\n" +
                "    %s = 2\n" +
                "    print %s\n" +
                "";
    }

    public void testRenameErr() throws Exception {
        int line = 2;
        int col = 10;
        checkRename(getDefaultDocStr(), line, col, "bb", true, false, "aaa bb");
    }

    public void testNoCommentsRename() throws Exception {
        int line = 6;
        int col = 27;
        String str = "#bar (no rename)\n" +
                "class RenameFunc2:\n" +
                "    '''\n" +
                "        bar (no rename)\n"
                +
                "    '''\n" +
                "    \n" +
                "    def RenameFunc2(self, %s):\n" +
                "        '''\n" +
                "            %s\n"
                +
                "        '''\n" +
                "        #%s\n" +
                "        \n" +
                "    def Other(self):\n"
                +
                "        #bar (no rename)\n" +
                "        pass\n" +
                "";

        checkRename(str, line, col, "bar", false, true);
    }

    public void testAttrRename() throws Exception {
        int line = 2;
        int col = 14;
        String str = "class Bar(object):\n" +
                "    def _GetFoo(self):\n" +
                "        self.%s()\n"
                +
                "    def %s(self):\n" +
                "        pass\n" +
                "";

        checkRename(str, line, col, "_foo", false, true);
    }

    public void testRenameAttribute2() throws Exception {
        int line = 5;
        int col = 5;
        String str = "class Foo(object):\n" +
                "    \n" +
                "    def %s(self):\n" +
                "        pass\n" +
                "        \n"
                +
                "Foo.%s\n" +
                "\n" +
                "";

        checkRename(str, line, col, "Foo", false, true);
    }

    public void testRenameClass3() throws Exception {
        int line = 0;
        int col = 7;
        String str = "class %s:\n" +
                "    \n" +
                "    def Test(self):\n" +
                "        print self.Test\n" +
                "        \n"
                +
                "";

        checkRename(str, line, col, "Test", false, true);
    }

    public void testRenameAttribute1() throws Exception {
        int line = 0;
        int col = 7;
        String str = "class %s:\n" +
                "    \n" +
                "    def A(self):\n" +
                "        %s()._DoImportSimulation()\n" +
                "";

        checkRename(str, line, col, "ActionProvider", false, true);
    }

    public void testCommentRename() throws Exception {
        int line = 3;
        int col = 10;
        String str = "class A(object):\n" +
                "    def %s(self):\n" +
                "        object.%s(self)\n" +
                "        #%s\n" +
                "";

        checkRename(str, line, col, "foo_", false, true);
    }

    public void testRenameImportFromReference() throws Exception {
        int line = 1;
        int col = 11;
        String str = "from testrec2.core import %s\n" +
                "class Foo(%s.Leaf):\n" +
                "    def setUp(self):\n"
                +
                "        %s.Leaf.setUp(self)";

        checkRename(str, line, col, "noleaf", false, true);
    }

    public void testRenameParam() throws Exception {
        //the difference is that in this one we find the leaf module and in the first we don't
        int line = 2;
        int col = 24;
        String str = "class MyProjectHandler(object):\n" +
                "    def testProjectManager(self):\n"
                +
                "        def MyClose(s, %s):\n" +
                "            pass\n"
                +
                "        foo(MyProjectHandler, Close = MyClose)\n" +
                "";

        checkRename(str, line, col, "project_id", false, true);
    }

    public void testRenameImportFromReference2() throws Exception {
        //the difference is that in this one we find the leaf module and in the first we don't
        int line = 1;
        int col = 11;
        String str = "from testrec2.core import %s\n" +
                "class Foo(%s.Leaf):\n" +
                "    def setUp(self):\n"
                +
                "        %s.Leaf.setUp(self)";

        checkRename(str, line, col, "leaf", false, true);
    }

    public void testRenameInstance2() throws Exception {
        String str = "" +
                "class Foo:\n" +
                "    def m1(self,%s):\n" + //we want to target only the bb in this method and not in the next
                "        print %s\n" +
                "    def m2(self,bb):\n" +
                "        return bb\n" +
                "\n";
        int line = 2;
        int col = 16;
        checkRename(str, line, col, "foo", false, true);
    }

    public void testRenameParameter() throws Exception {
        String str = "" +
                "class Foo:\n" +
                "    def m1(self,%s):\n" + //we want to target only the parameter foo in this method and not the attribute
                "        print %s\n" +
                "        print self.foo" +
                "\n";
        int line = 1;
        int col = 16;
        checkRename(str, line, col, "foo", false, true);
    }

    public void testRenameParameter5() throws Exception {
        String str = "" +
                "class Test:\n" +
                "    \n" +
                "    def call(self, %s):\n"
                +
                "        '''@param %s: entuhoen'''\n" +
                "        \n" +
                "    def testing(self):\n" +
                "        '''\n"
                +
                "            @param here: etnhuon\n" +
                "        '''\n" +
                "        here = 19\n"
                +
                "        print 'here'\n";

        int line = 3;
        int col = 19;
        checkRename(str, line, col, "here", false, true);
    }

    public void testRenameMethod() throws Exception {
        String str = "" +
                "class Test:\n" +
                "    \n" +
                "    def %s(self):\n" +
                "        pass\n" +
                "        \n";

        int line = 2;
        int col = 9;
        checkRename(str, line, col, "Test", false, true);
    }

    public void testRenameMethod2() throws Exception {
        String str = "" +
                "class Test:\n" +
                "    \n" +
                "    def %s(self, here):\n"
                +
                "        '''@param here: entuhoen'''\n" +
                "        \n" +
                "    %s = staticmethod(%s)\n" +
                "\n";

        int line = 2;
        int col = 9;
        checkRename(str, line, col, "Test", false, true);
    }

    public void testRenameClass2() throws Exception {
        String str = "" +
                "class %s:\n" +
                "    \n" +
                "    def Test(self, here):\n"
                +
                "        '''@param here: entuhoen'''\n" +
                "        \n" +
                "    Test = staticmethod(Test)\n" +
                "\n";

        int line = 0;
        int col = 7;
        checkRename(str, line, col, "Test", false, true);
    }

    public void testRenameParameter4() throws Exception {
        String str = "" +
                "'''\n" +
                "foo\n" +
                "'''\n" +
                "class Foo:\n" +
                "    def m1(self,%s):\n"
                +
                "        '''%s'''\n" +
                "        print %s\n" +
                "\n";
        int line = 4;
        int col = 17;
        checkRename(str, line, col, "foo", false, true);
    }

    public void testRenameParameter2() throws Exception {
        String str = "" +
                "class Foo:\n" +
                "    def m1(self,%s):\n" + //we want to target only the parameter foo in this method and not the attribute
                "        print %s\n" +
                "        print %s.bla" +
                "\n";
        int line = 1;
        int col = 16;
        checkRename(str, line, col, "foo", false, true);
    }

    public void testRenameParameter3() throws Exception {
        String str = "" +
                "def m1(foo):\n" +
                "    if foo is None:\n" +
                "        pass\n" +
                "    \n" +
                "#m2\n"
                +
                "def m2(%s):\n" +
                "    print %s\n" +
                "\n";
        int line = 5;
        int col = 7;
        checkRename(str, line, col, "foo", false, true);
    }

    public void testRenameLocalMethod() throws Exception {
        String str = "" +
                "def Foo():\n" +
                "    def %s():\n" +
                "        pass\n" +
                "    if %s(): pass\n" +
                "\n" +
                "";
        int line = 1;
        int col = 9;
        checkRename(str, line, col, "met1", false, true);
    }

    public void testRenameLocalMethod2() throws Exception {
        String str = "" +
                "def %s():\n" +
                "    def mm():\n" +
                "        print %s()\n" +
                "\n" +
                "";
        int line = 0;
        int col = 5;
        checkRename(str, line, col, "met1", false, true);
    }

    public void testRenameImportLocally() throws Exception {
        String str = "" +
                "from foo import %s\n" +
                "def mm():\n" +
                "    print %s\n" +
                "\n" +
                "";
        int line = 0;
        int col = 17;
        checkRename(str, line, col, "bla", false, true);
    }

    public void testRenameImportLocally2() throws Exception {
        String str = "" +
                "import %s\n" +
                "def run():\n" +
                "    print %s.getopt()\n" +
                "\n" +
                "";
        int line = 0;
        int col = 10;
        checkRename(str, line, col, "getopt", false, true);
    }

    public void testRenameImportLocally3() throws Exception {
        String str = "" +
                "import %s\n" +
                "def run():\n" +
                "    print %s\n" +
                "\n" +
                "";
        int line = 0;
        int col = 10;
        checkRename(str, line, col, "sys", false, true);
    }

    public void testRenameImportLocally4() throws Exception {
        String str = "" +
                "from extendable.constants import %s\n" +
                "def run():\n" +
                "    print %s\n"
                +
                "    #comment %s\n" +
                "    'string %s'" +
                "\n" +
                "";
        int line = 2;
        int col = 11;
        checkRename(str, line, col, "CONSTANT1", false, true);
    }

    public void testRenameMethodImportLocally() throws Exception {
        String str = "" +
                "from testAssist.assist.ExistingClass import %s\n" +
                "def run():\n" +
                "    print %s\n" +
                "\n"
                +
                "";
        int line = 2;
        int col = 11;
        checkRename(str, line, col, "existingMethod", false, true);
    }

    public void testRenameMethodImportLocally2() throws Exception {
        String str = "" +
                "from testAssist.assist import ExistingClass\n" +
                "def run():\n"
                +
                "    print ExistingClass.%s\n" +
                "\n" +
                "";
        int line = 2;
        int col = 26;
        checkRename(str, line, col, "existingMethod", false, true);
    }

    public void testRenameClassImportLocally() throws Exception {
        String str = "" +
                "from testAssist.assist import %s\n" +
                "def run():\n" +
                "    print %s\n" +
                "\n" +
                "";
        int line = 2;
        int col = 11;
        checkRename(str, line, col, "ExistingClass", false, true);
    }

    public void testRenameMethodLocally() throws Exception {
        String str = "" +
                "class Foo:\n" +
                "    def %s(self):\n" +
                "        tup = 10\n" +
                "        tup[1].append(1)\n"
                +
                "";
        int line = 1;
        int col = 9;
        checkRename(str, line, col, "foo", false, true);
    }

    public void testRenameLocalAttr() throws Exception {
        String str = "" +
                "class Foo:\n" +
                "    def foo(self):\n" +
                "        %s = [[],[]]\n"
                +
                "        %s[1].append(1)\n" +
                "";
        int line = 2;
        int col = 9;
        checkRename(str, line, col, "tup", false, true);
    }

    public void testRenameLocalAttr2() throws Exception {
        String str = "" +
                "'''\n" +
                "tup\n"
                + //should not rename 'global' comments when found in a local scope
                "'''\n" +
                "class Foo:\n" +
                "    def foo(self):\n" +
                "        '''@param %s: bbbbb xxxxx'''\n"
                +
                "        %s = 10\n" +
                "        print %s\n" +
                "";
        int line = 6;
        int col = 9;
        checkRename(str, line, col, "tup", false, true);
    }

    public void testRenameAttribute() throws Exception {
        String str = "" +
                "class Foo(object):\n" +
                "    def %s(self):\n" +
                "        pass\n" +
                "    \n"
                +
                "foo = Foo()\n" +
                "foo.%s()\n" +
                "";
        int line = 1;
        int col = 9;
        checkRename(str, line, col, "met1", false, true);
    }

    public void testRenameUndefined() throws Exception {
        String str = "" +
                "from a import %s\n" +
                "print %s\n" +
                "";
        int line = 0;
        int col = 15;
        checkRename(str, line, col, "foo", false, true);
    }

    public void testNotFoundAttr() throws Exception {
        //TODO: the problem here is that when the attribute is not found,
        //we need to change the definition too.
        String str = "" +
                "class Foo:\n" +
                "    def %s(self, foo):\n" +
                "        print foo.%s\n" +
                "";
        int line = 2;
        int col = 19;
        checkRename(str, line, col, "met1", false, true);
    }

    public void testCall() throws Exception {
        String str = "" +
                "class A(object):\n" +
                "    %s( A, self ).__init__( a,b )\n" +
                "\n" +
                "\n" +
                "\n" +
                "";
        int line = 1;
        int col = 5;
        checkRename(str, line, col, "super", false, true);
    }

    public void testMultiStr() throws Exception {
        String str = "" +
                "%s = str(1)+ 'foo2'\n" +
                "print %s\n" +
                "";
        int line = 0;
        int col = 1;
        checkRename(str, line, col, "ss", false, true);
    }

    public void testDict() throws Exception {
        String str = "" +
                "%s = {}\n" +
                "print %s[1]\n" +
                "";
        int line = 0;
        int col = 1;
        checkRename(str, line, col, "ddd", false, true);
    }

    public void testRenameInstance() throws Exception {
        String str = getDefaultDocStr();
        int line = 2;
        int col = 10;

        checkRename(str, line, col);
    }

    public void testImport() throws Exception {
        String str = "" +
                "import os.%s\n" +
                "print os.%s\n" +
                "";
        int line = 1;
        int col = 10;
        checkRename(str, line, col, "path", false, true);
    }

    public void testLocalNotGotten() throws Exception {
        String str = "" +
                "def m1():\n" +
                "    foo.%s = 10\n" + //accessing this should not affect the locals (not taking methods into account)
                "    print foo.%s\n" +
                "    bla = 20\n" +
                "    print bla\n" +
                "";

        int line = 1;
        int col = 9;
        checkRename(str, line, col, "bla", false, true);
    }

    public void testLocalNotGotten2() throws Exception {
        String str = "" +
                "def m1():\n" +
                "    print foo.%s\n" + //accessing this should not affect the locals (not taking methods into account)
                "    print bla\n" +
                "";

        int line = 1;
        int col = 15;
        checkRename(str, line, col, "bla", false, true);
    }

    public void testLocalGotten() throws Exception {
        String str = "" +
                "def m1():\n" +
                "    print foo.bla\n" +
                "    print %s\n" + //should only affect the locals
                "";

        int line = 2;
        int col = 11;
        checkRename(str, line, col, "bla", false, true);
    }

    public void testRenameSelf() throws Exception {
        String str = "" +
                "def checkProps(%s):\n" +
                "    getattr(%s).value\n" +
                "\n";

        int line = 0;
        int col = 17;
        checkRename(str, line, col, "fff", false, true);
    }

    public void testRenameClassVar() throws Exception {
        String str = "" +
                "class Foo:\n" +
                "    %s = ''\n" +
                "    def toSimulator(self):\n" +
                "        print self.%s\n"
                +
                "";

        int line = 3;
        int col = 21;
        checkRename(str, line, col, "vlMolecularWeigth", false, true);
    }

    public void testRenameClass() throws Exception {
        String str = "" +
                "class %s(object):\n" +
                "    \n" +
                "    def SlotImportSimulation(cls):\n"
                +
                "        %s()._DoSlotImportSimulation()\n" +
                "\n" +
                "\n";
        int line = 0;
        int col = 7;
        checkRename(str, line, col, "ActionProvider", false, true);
    }

    public void testRenameNonLocal() throws Exception {
        String str = "" +
                "import b    \n" +
                "print b.%s\n" +
                "class C2:\n" +
                "    def m1(self):\n"
                +
                "        barr = 10\n" + //should not rename the local
                "\n" +
                "";

        int line = 1;
        int col = 9;
        checkRename(str, line, col, "barr", false, true);
    }

    public void testRenameNonLocal2() throws Exception {
        String str = "" +
                "def m1():\n" +
                "    bar = 10\n" +
                "    bar.%s\n" + //selected (Foo)
                "    foop = 20\n" +
                "";

        int line = 2;
        int col = 9;
        checkRename(str, line, col, "foop", false, true);
    }

    public void testRename1() throws Exception {
        String str = "" +
                "import pprint\n" +
                "def myHook():\n" +
                "    pprint.%s()\n" +
                "";

        int line = 2;
        int col = 12;
        checkRename(str, line, col, "PrettyPrinter", false, true);
    }

    public void testRename2() throws Exception {
        String str = "" +
                "import bla as %s\n" +
                "raise %s.ffff(msg)\n" +
                "\n" +
                "";

        int line = 0;
        int col = 15;
        checkRename(str, line, col, "fooo", false, true);
    }

    public void testRename3() throws Exception {
        String str = "" +
                "def m1(a):\n" +
                "    a.data.%s\n" +
                "";

        int line = 1;
        int col = 12;
        checkRename(str, line, col, "fooo", false, true);
    }

    public void testRenameParamFromCall() throws Exception {
        String str = "" +
                "def m1():\n" +
                "    def m2(%s):\n" +
                "        m1(%s=%s-1)\n" +
                "";

        int line = 2;
        int col = 12;
        checkRename(str, line, col, "fooo", false, true);
    }

    public void testRenameParamFromCall2() throws Exception {
        String str = "" +
                "def m1():\n" +
                "    def m2(%s):\n" +
                "        m1(%s=%s-1)\n" +
                "";

        int line = 1;
        int col = 12;
        checkRename(str, line, col, "fooo", false, true);
    }

    public void testRenameParamDocs() throws Exception {
        String str = "" +
                "tok = 10\n" +
                "def m1(%s=tok):\n" + //only get the tok that is a parameter the docs and comments
                "    '@param %s: this is %s'\n" +
                "    #checking %s right?\n" +
                "";

        checkRename(str, 1, 7, "tok", false, true);
    }

    public void testRenameParamDocs2() throws Exception {
        String str = "" +
                "tok = 10\n" +
                "#checking tok right?\n" + //not renamed (out of context)
                "def m1(%s=tok):\n" + //only get the tok that is a parameter the docs and comments
                "    pass\n" +
                "";

        checkRename(str, 2, 7, "tok", false, true);
    }

    public void testRenameComment() throws Exception {
        String str = "" +
                "%s = 10\n" +
                "#checking %s right?\n" +
                "def m1(a=%s):\n" +
                "    pass\n" +
                "";

        checkRename(str, 1, 11, "tok", false, true);
    }

    public void testRenameString() throws Exception {
        String str = "" +
                "%s = 10\n" +
                "'''\n" +
                "%s\n" +
                "%s\n" +
                "'''\n" +
                "";

        checkRename(str, 0, 1, "tok", false, true);
    }

    public void testRenameString2() throws Exception {
        String str = "" +
                "%s = 10\r\n" +
                "'''\r\n" +
                "%s\r\n" +
                "\r\n" +
                "\r\n" +
                "%s\r\n" +
                "\r\n" +
                "'''\r\n" +
                "";

        checkRename(str, 0, 1, "tok", false, true);
    }

    public void testRenameParam2() throws Exception {
        String str = "" +
                "class DefaultProcessFactory(object):\n" +
                "    \n" +
                "    def _DoCreateIt(self, %s):\n"
                +
                "        pass\n" +
                "        \n" +
                "    def CreateIt(self, a):\n"
                +
                "        for root_info in a.GetRootInfos():\n" +
                "            self._DoCreateIt(root_info)\n" +
                "";

        checkRename(str, 2, 27, "root_info", false, true);
    }

    public void testRenameParam3() throws Exception {
        String str = "" +
                "class Foo:\n" +
                "    def ListFiles(%s):\n" +
                "        pass\n" +
                "    def testCases(self):\n"
                +
                "        self.ListFiles\n" +
                "";

        checkRename(str, 1, 19, "self", false, true);
    }

    public void testRenameParam4() throws Exception {
        String str = "" +
                "class Foo:\n" +
                "    def ListFiles(self, %s):\n" +
                "        pass\n"
                +
                "    def testCases(self):\n" +
                "        self.ListFiles(%s=10)\n" +
                "";

        checkRename(str, 1, 25, "xxx", false, true);
    }

    public void testRenameParam5() throws Exception {
        String str = "" +
                "class Foo:\n" +
                "    def ListFiles(self, %s):\n" +
                "        pass\n"
                +
                "    def testCases(self):\n" +
                "        self.ListFiles(bar)\n" +
                "";

        checkRename(str, 1, 25, "xxx", false, true);
    }

    public void testRenameParam6() throws Exception {
        String str = "" +
                "class Foo:\n" +
                "    def ListFiles(self, %s):\n" +
                "        pass\n"
                +
                "    def testCases(self):\n" +
                "        bar = 10\n" +
                "        self.ListFiles(%s=bar)\n" +
                "";

        checkRename(str, 1, 25, "bar", false, true);
    }

    public void testRenameParam7() throws Exception {
        String str = "" +
                "class Foo:\n" +
                "    def ListFiles(self, %s):\n" +
                "        pass\n"
                +
                "    def testCases(self):\n" +
                "        bar = 10\n" +
                "        self.ListFiles(%s=bar)\n" +
                "";

        checkRename(str, 5, 24, "bar", false, true);
    }

    public void testRenameParam8() throws Exception {
        String str = "" +
                "class Foo:\n" +
                "    def testCases(self):\n" +
                "        bar = 10\n"
                +
                "        self.ListFiles(%s=bar)\n" +
                "";

        checkRename(str, 3, 24, "bar", false, true);
    }

    public void testRenameParam9() throws Exception {
        String str = "" +
                "def foo(%s):\n" +
                "    foo(%s = 1)\n" +
                "";

        //expecting to fail (not done) -- see ScopeAnalysis.getLocalOccurrences
        checkRename(str, 0, 9, "days", false, true);
    }

    public void testRenameParam10() throws Exception {
        //CURRENTLY EXPECTED TO FAIL (NOT FINISHED)
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }
        String str = "" +
                "def foo():\n" +
                "    %s=10\n" +
                "    foo(days = %s)\n" +
                "";

        try {
            checkRename(str, 1, 5, "days", false, true);
        } catch (Throwable e) {
            fail("Expected to fail!");
        }
    }

}
