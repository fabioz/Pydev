/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.io.File;
import java.util.HashSet;
import java.util.Map;

import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.refactoring.wizards.rename.PyRenameGlobalProcess;

public class RenameGlobalRefactoringTest extends RefactoringRenameTestBase {
    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = false;
            RenameGlobalRefactoringTest test = new RenameGlobalRefactoringTest();
            test.setUp();
            test.testRename2();
            test.tearDown();

            junit.textui.TestRunner.run(RenameGlobalRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class<PyRenameGlobalProcess> getProcessUnderTest() {
        return null;
    }

    public void testRename1() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameglobal.renglobal", 0, 8);
        assertEquals(""
                + "reflib.renameglobal.renglobal\n"
                + "  ASTEntry<bar (Name L=2 C=1)>\n"
                + "    Line: 1  bar = 10 --> new_name = 10\n"
                + "  ASTEntry<bar (Name L=3 C=7)>\n"
                + "    Line: 2  print(bar) --> print(new_name)\n"
                + "  ASTEntry<bar (NameTok L=1 C=8)>\n"
                + "    Line: 0  global bar --> global new_name\n"
                + "\n"
                + "", asStr(references));

    }

    public void testRename2() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameglobal2.bar2", 2, 1);
        assertEquals(""
                + "reflib.renameglobal2.bar1\n"
                + "  ASTEntry<Bar1 (Name L=6 C=1)>\n"
                + "    Line: 5  Bar1 = BadPickleGet --> new_name = BadPickleGet\n"
                + "\n"
                + "reflib.renameglobal2.bar2\n"
                + "  ASTEntry<Bar1 (Name L=3 C=1)>\n"
                + "    Line: 2  Bar1 --> new_name\n"
                + "  ASTEntry<Bar1 (NameTok L=1 C=19)>\n"
                + "    Line: 0  from .bar1 import Bar1 --> from .bar1 import new_name\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRename3() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameglobal3.bar", 2, 10);
        assertEquals(""
                + "reflib.renameglobal3.bar\n"
                + "  ASTEntry<SOME_CONSTANT (NameTok L=3 C=9)>\n"
                + "    Line: 2  a = foo.SOME_CONSTANT --> a = foo.new_name\n"
                + "\n"
                + "reflib.renameglobal3.foo\n"
                + "  ASTEntry<SOME_CONSTANT (Name L=1 C=1)>\n"
                + "    Line: 0  SOME_CONSTANT = 'constant' --> new_name = 'constant'\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRename4() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameglobal3.foo", 0, 5);
        assertEquals(""
                + "reflib.renameglobal3.bar\n"
                + "  ASTEntry<SOME_CONSTANT (Attribute L=3 C=9)>\n"
                + "    Line: 2  a = foo.SOME_CONSTANT --> a = foo.new_name\n"
                + "\n"
                + "reflib.renameglobal3.foo\n"
                + "  ASTEntry<SOME_CONSTANT (Name L=1 C=1)>\n"
                + "    Line: 0  SOME_CONSTANT = 'constant' --> new_name = 'constant'\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRename5() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameglobal4.foo", 0, 5);
        assertEquals(""
                + "reflib.renameglobal4.bar\n"
                + "  ASTEntry<SOME_CONSTANT (Attribute L=5 C=17)>\n"
                + "    Line: 4          a = foo.SOME_CONSTANT -->         a = foo.new_name\n"
                + "\n"
                + "reflib.renameglobal4.foo\n"
                + "  ASTEntry<SOME_CONSTANT (Name L=1 C=1)>\n"
                + "    Line: 0  SOME_CONSTANT = 'constant' --> new_name = 'constant'\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRename6() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameglobal4.bar", 4, 20);
        assertEquals(""
                + "reflib.renameglobal4.bar\n"
                + "  ASTEntry<SOME_CONSTANT (NameTok L=5 C=17)>\n"
                + "    Line: 4          a = foo.SOME_CONSTANT -->         a = foo.new_name\n"
                + "\n"
                + "reflib.renameglobal4.foo\n"
                + "  ASTEntry<SOME_CONSTANT (Name L=1 C=1)>\n"
                + "    Line: 0  SOME_CONSTANT = 'constant' --> new_name = 'constant'\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRename7() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameglobal5.foo", 0, 5);
        assertEquals(""
                + "reflib.renameglobal5.bar\n"
                + "  ASTEntry<SOME_CONSTANT (Attribute L=8 C=19)>\n"
                + "    Line: 7          \"\"\" + foo.SOME_CONSTANT + \"abcdefg\" -->         \"\"\" + foo.new_name + \"abcdefg\"\n"
                + "\n"
                + "reflib.renameglobal5.foo\n"
                + "  ASTEntry<SOME_CONSTANT (Name L=1 C=1)>\n"
                + "    Line: 0  SOME_CONSTANT = 'constant' --> new_name = 'constant'\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRename8() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameglobal5.bar", 7, 24);
        assertEquals(""
                + "reflib.renameglobal5.bar\n"
                + "  ASTEntry<SOME_CONSTANT (NameTok L=8 C=19)>\n"
                + "    Line: 7          \"\"\" + foo.SOME_CONSTANT + \"abcdefg\" -->         \"\"\" + foo.new_name + \"abcdefg\"\n"
                + "\n"
                + "reflib.renameglobal5.foo\n"
                + "  ASTEntry<SOME_CONSTANT (Name L=1 C=1)>\n"
                + "    Line: 0  SOME_CONSTANT = 'constant' --> new_name = 'constant'\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRename9() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameglobal6.foo", 0, 5);
        assertEquals(""
                + "reflib.renameglobal6.bar\n"
                + "  ASTEntry<SOME_CONSTANT (Attribute L=8 C=15)>\n"
                + "    Line: 7          + foo.SOME_CONSTANT -->         + foo.new_name\n"
                + "\n"
                + "reflib.renameglobal6.foo\n"
                + "  ASTEntry<SOME_CONSTANT (Name L=1 C=1)>\n"
                + "    Line: 0  SOME_CONSTANT = 'constant' --> new_name = 'constant'\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRename10() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameglobal6.bar", 7, 24);
        assertEquals(""
                + "reflib.renameglobal6.bar\n"
                + "  ASTEntry<SOME_CONSTANT (NameTok L=8 C=15)>\n"
                + "    Line: 7          + foo.SOME_CONSTANT -->         + foo.new_name\n"
                + "\n"
                + "reflib.renameglobal6.foo\n"
                + "  ASTEntry<SOME_CONSTANT (Name L=1 C=1)>\n"
                + "    Line: 0  SOME_CONSTANT = 'constant' --> new_name = 'constant'\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRename11() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameglobal7.foo", 0, 5);
        assertEquals(""
                + "reflib.renameglobal7.bar\n"
                + "  ASTEntry<SOME_CONSTANT (Attribute L=5 C=17)>\n"
                + "    Line: 4          a = foo.SOME_CONSTANT + foo.SOME_CONSTANT \\ -->         a = foo.new_name + foo.SOME_CONSTANT \\\n"
                + "  ASTEntry<SOME_CONSTANT (Attribute L=5 C=37)>\n"
                + "    Line: 4          a = foo.SOME_CONSTANT + foo.SOME_CONSTANT \\ -->         a = foo.SOME_CONSTANT + foo.new_name \\\n"
                + "  ASTEntry<SOME_CONSTANT (Attribute L=6 C=15)>\n"
                + "    Line: 5          + foo.SOME_CONSTANT -->         + foo.new_name\n"
                + "\n"
                + "reflib.renameglobal7.foo\n"
                + "  ASTEntry<SOME_CONSTANT (Name L=1 C=1)>\n"
                + "    Line: 0  SOME_CONSTANT = 'constant' --> new_name = 'constant'\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRename12() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameglobal7.bar", 4, 21);
        assertEquals(""
                + "reflib.renameglobal7.bar\n"
                + "  ASTEntry<SOME_CONSTANT (NameTok L=5 C=17)>\n"
                + "    Line: 4          a = foo.SOME_CONSTANT + foo.SOME_CONSTANT \\ -->         a = foo.new_name + foo.SOME_CONSTANT \\\n"
                + "  ASTEntry<SOME_CONSTANT (NameTok L=5 C=37)>\n"
                + "    Line: 4          a = foo.SOME_CONSTANT + foo.SOME_CONSTANT \\ -->         a = foo.SOME_CONSTANT + foo.new_name \\\n"
                + "  ASTEntry<SOME_CONSTANT (NameTok L=6 C=15)>\n"
                + "    Line: 5          + foo.SOME_CONSTANT -->         + foo.new_name\n"
                + "\n"
                + "reflib.renameglobal7.foo\n"
                + "  ASTEntry<SOME_CONSTANT (Name L=1 C=1)>\n"
                + "    Line: 0  SOME_CONSTANT = 'constant' --> new_name = 'constant'\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRename13() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameglobal8.foo", 0, 5);
        assertEquals(""
                + "reflib.renameglobal8.bar\n"
                + "  ASTEntry<SOME_CONSTANT (Attribute L=5 C=17)>\n"
                + "    Line: 4          a = foo.SOME_CONSTANT == 'something' -->         a = foo.new_name == 'something'\n"
                + "\n"
                + "reflib.renameglobal8.foo\n"
                + "  ASTEntry<SOME_CONSTANT (Name L=1 C=1)>\n"
                + "    Line: 0  SOME_CONSTANT = 'constant' --> new_name = 'constant'\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRename14() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameglobal8.bar", 4, 20);
        assertEquals(""
                + "reflib.renameglobal8.bar\n"
                + "  ASTEntry<SOME_CONSTANT (NameTok L=5 C=17)>\n"
                + "    Line: 4          a = foo.SOME_CONSTANT == 'something' -->         a = foo.new_name == 'something'\n"
                + "\n"
                + "reflib.renameglobal8.foo\n"
                + "  ASTEntry<SOME_CONSTANT (Name L=1 C=1)>\n"
                + "    Line: 0  SOME_CONSTANT = 'constant' --> new_name = 'constant'\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRename15() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameglobal9.foo", 0, 5);
        assertEquals(""
                + "reflib.renameglobal9.bar\n"
                + "  ASTEntry<SOME_CONSTANT (Attribute L=5 C=17)>\n"
                + "    Line: 4          a = foo.SOME_CONSTANT == 'something' or foo.SOME_CONSTANT == 'another' -->         a = foo.new_name == 'something' or foo.SOME_CONSTANT == 'another'\n"
                + "  ASTEntry<SOME_CONSTANT (Attribute L=5 C=53)>\n"
                + "    Line: 4          a = foo.SOME_CONSTANT == 'something' or foo.SOME_CONSTANT == 'another' -->         a = foo.SOME_CONSTANT == 'something' or foo.new_name == 'another'\n"
                + "\n"
                + "reflib.renameglobal9.foo\n"
                + "  ASTEntry<SOME_CONSTANT (Name L=1 C=1)>\n"
                + "    Line: 0  SOME_CONSTANT = 'constant' --> new_name = 'constant'\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRename16() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameglobal9.bar", 4, 20);
        assertEquals(""
                + "reflib.renameglobal9.bar\n"
                + "  ASTEntry<SOME_CONSTANT (NameTok L=5 C=17)>\n"
                + "    Line: 4          a = foo.SOME_CONSTANT == 'something' or foo.SOME_CONSTANT == 'another' -->         a = foo.new_name == 'something' or foo.SOME_CONSTANT == 'another'\n"
                + "  ASTEntry<SOME_CONSTANT (NameTok L=5 C=53)>\n"
                + "    Line: 4          a = foo.SOME_CONSTANT == 'something' or foo.SOME_CONSTANT == 'another' -->         a = foo.SOME_CONSTANT == 'something' or foo.new_name == 'another'\n"
                + "\n"
                + "reflib.renameglobal9.foo\n"
                + "  ASTEntry<SOME_CONSTANT (Name L=1 C=1)>\n"
                + "    Line: 0  SOME_CONSTANT = 'constant' --> new_name = 'constant'\n"
                + "\n"
                + "", asStr(references));
    }
}
