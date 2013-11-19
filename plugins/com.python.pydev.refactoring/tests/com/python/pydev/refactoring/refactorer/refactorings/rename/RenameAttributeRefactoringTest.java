/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.util.HashSet;
import java.util.Map;

import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.refactoring.wizards.rename.PyRenameAttributeProcess;

public class RenameAttributeRefactoringTest extends RefactoringRenameTestBase {

    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = false;
            RenameAttributeRefactoringTest test = new RenameAttributeRefactoringTest();
            test.setUp();
            test.testRenameAttribute();
            test.tearDown();

            junit.textui.TestRunner.run(RenameAttributeRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class<PyRenameAttributeProcess> getProcessUnderTest() {
        return PyRenameAttributeProcess.class;
    }

    public void testRenameAttribute() throws Exception {
        //Line 1 = "    a.attrInstance = 10"
        //rename attrInstance
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renameattribute.attr2", 1, 8);
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES));
        assertTrue(references.containsKey("reflib.renameattribute.attr1"));
        assertEquals(3, references.get(CURRENT_MODULE_IN_REFERENCES).size());
        assertEquals(1, references.get("reflib.renameattribute.attr1").size());
    }
}
