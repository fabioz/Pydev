/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.refactoring.wizards.rename.PyRenameImportProcess;

public class RenameModuleRefactoringTest extends RefactoringRenameTestBase {

    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = false;
            RenameModuleRefactoringTest test = new RenameModuleRefactoringTest();
            test.setUp();
            test.testRenameModuleInWorkspace3();
            test.tearDown();

            junit.textui.TestRunner.run(RenameModuleRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class getProcessUnderTest() {
        return PyRenameImportProcess.class;
    }

    public void testRenameModuleInWorkspace() throws Exception {
        //importer.py and importer2.py are the same:
        //
        //import mod1
        //from mod1 import submod1

        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renamemodule.importer", 0, 8);
        assertEquals(3, references.size());

        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES));
        assertEquals(4, references.get(CURRENT_MODULE_IN_REFERENCES).size());

        assertTrue(references.containsKey("reflib.renamemodule.mod1.__init__")); //module renamed 
        assertEquals(1, references.get("reflib.renamemodule.mod1.__init__").size());

        assertTrue(references.containsKey("reflib.renamemodule.importer2"));
        assertEquals(4, references.get("reflib.renamemodule.importer2").size());
    }

    public void testRenameModuleInWorkspace2() throws Exception {
        //importer.py and importer2.py are the same:
        //
        //import mod1
        //from mod1 import submod1

        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renamemodule.importer", 1, 18);
        checkSubMod1References(references);
        assertTrue(references.containsKey("reflib.renamemodule.importer2"));
        assertTrue(references.containsKey("reflib.renamemodule.importer3"));
        assertTrue(references.containsKey("reflib.renamemodule.importer4"));
        assertTrue(references.containsKey("reflib.renamemodule.importer5"));
        assertTrue(references.containsKey("reflib.renamemodule.mod1.submod1")); //module renamed 
    }

    public void testRenameModuleInWorkspace3() throws Exception {
        //from reflib.renamemodule.mod1 import submod1

        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renamemodule.importer5", 0, 40);
        checkSubMod1References(references);
        assertTrue(references.containsKey("reflib.renamemodule.importer"));
        assertTrue(references.containsKey("reflib.renamemodule.importer2"));
        assertTrue(references.containsKey("reflib.renamemodule.importer3"));
        assertTrue(references.containsKey("reflib.renamemodule.importer4"));
        assertTrue(references.containsKey("reflib.renamemodule.mod1.submod1")); //module renamed 
    }

    private void checkSubMod1References(Map<String, HashSet<ASTEntry>> references) {
        assertEquals(6, references.size());

        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES));
        assertEquals(1, references.get(CURRENT_MODULE_IN_REFERENCES).size());

        for (Collection<ASTEntry> values : references.values()) {
            assertEquals(1, values.size());
        }
    }

}
