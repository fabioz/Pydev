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

import com.python.pydev.refactoring.wizards.rename.PyRenameLocalProcess;

public class RenameLocalRefactoringTest extends RefactoringRenameTestBase {

    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = false;
            RenameLocalRefactoringTest test = new RenameLocalRefactoringTest();
            test.setUp();
            test.testRenameLocal();
            test.tearDown();

            junit.textui.TestRunner.run(RenameLocalRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class<PyRenameLocalProcess> getProcessUnderTest() {
        return PyRenameLocalProcess.class;
    }

    public void testRenameLocal() throws Exception {
        //Line 1 = "    aa = 10"
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renamelocal.local1", 1, 5);
        assertEquals(""
                + "reflib.renamelocal.local1\n"
                + "  ASTEntry<aa (Name L=2 C=5)>\n"
                + "    Line: 1      aa = 10 -->     new_name = 10\n"
                + "  ASTEntry<aa (Name L=3 C=11)>\n"
                + "    Line: 2      print aa -->     print new_name\n"
                + "\n"
                + "", asStr(references));
    }
}