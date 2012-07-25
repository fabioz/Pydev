/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.util.HashSet;
import java.util.Map;

import org.python.pydev.parser.visitors.scope.ASTEntry;

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

    protected Class getProcessUnderTest() {
        return PyRenameGlobalProcess.class;
    }

    public void testRename1() throws Exception {
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renameglobal.renglobal", 0, 8);
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); //the current module must also be there
        assertEquals(3, references.get(CURRENT_MODULE_IN_REFERENCES).size());
    }

    public void testRename2() throws Exception {
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renameglobal2.bar2", 2, 1);
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); //the current module must also be there
        assertContains(1, 18, references.get(CURRENT_MODULE_IN_REFERENCES));
        assertContains(3, 1, references.get(CURRENT_MODULE_IN_REFERENCES));
        assertContains(6, 1, references.get("reflib.renameglobal2.bar1"));
    }

}
