/**
 * Copyright (c) 2013-2015 by Brainwy Software Ltda. All Rights Reserved.
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

import com.python.pydev.refactoring.wizards.rename.PyRenameAnyLocalProcess;

public class RenameBuiltinRefactoringTest extends RefactoringRenameTestBase {

    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = false;
            RenameBuiltinRefactoringTest test = new RenameBuiltinRefactoringTest();
            test.setUp();
            test.testRenameLocal();
            test.tearDown();

            junit.textui.TestRunner.run(RenameBuiltinRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class getProcessUnderTest() {
        return PyRenameAnyLocalProcess.class;
    }

    public void testRenameLocal() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renamebuiltin.f2", 3, 4); //AssertionError
        assertEquals(""
                + "reflib.renamebuiltin.f1\n"
                + "  ASTEntry<AssertionError (Name L=1 C=5)>\n"
                + "    Line: 0  a = AssertionError --> a = new_name\n"
                + "\n"
                + "reflib.renamebuiltin.f2\n"
                + "  ASTEntry<AssertionError (Name L=4 C=5)>\n"
                + "    Line: 3      AssertionError -->     new_name\n"
                + "\n", asStr(references));
    }
}