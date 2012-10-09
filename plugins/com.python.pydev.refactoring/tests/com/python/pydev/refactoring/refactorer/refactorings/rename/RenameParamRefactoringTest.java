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

import com.python.pydev.refactoring.wizards.rename.PyRenameParameterProcess;

public class RenameParamRefactoringTest extends RefactoringRenameTestBase {

    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = false;
            RenameParamRefactoringTest test = new RenameParamRefactoringTest();
            test.setUp();
            test.testRenameParameter2();
            test.tearDown();

            junit.textui.TestRunner.run(RenameParamRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class getProcessUnderTest() {
        return PyRenameParameterProcess.class;
    }

    public void testRenameParameter() throws Exception {
        //Line 1 = "def Method1(param1=param1, param2=None):"
        //rename param1
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renameparameter.methoddef", 1,
                12);
        assertEquals(2, references.size());
        assertTrue(references.containsKey("reflib.renameparameter.methodaccess"));
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES));
        assertEquals(2, references.get(CURRENT_MODULE_IN_REFERENCES).size());
        assertEquals(2, references.get("reflib.renameparameter.methodaccess").size());
    }

    public void testRenameParameter2() throws Exception {
        //    def mm(self, barparam):"
        //rename barparam
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renameparameter.methoddef2",
                1, 17);
        assertEquals(1, references.size());
        assertEquals(4, references.get(CURRENT_MODULE_IN_REFERENCES).size());
        assertContains(2, 18, references.get(CURRENT_MODULE_IN_REFERENCES));
        assertContains(4, 20, references.get(CURRENT_MODULE_IN_REFERENCES));
        assertContains(4, 38, references.get(CURRENT_MODULE_IN_REFERENCES));
        assertContains(7, 6, references.get(CURRENT_MODULE_IN_REFERENCES));
    }

}
