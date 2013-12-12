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

import com.python.pydev.refactoring.wizards.rename.PyRenameGlobalProcess;

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
                + "    Line: 2  print bar --> print new_name\n"
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
                + "  ASTEntry<Bar1 (NameTok L=1 C=18)>\n"
                + "    Line: 0  from bar1 import Bar1 --> from bar1 import new_name\n"
                + "\n"
                + "", asStr(references));
    }

}
