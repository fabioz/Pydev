/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Dec 10, 2006
 * @author Fabio
 */
package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.util.HashSet;
import java.util.Map;

import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.refactoring.wizards.rename.PyRenameFunctionProcess;

/**
 * Class that should test the renaming of classes within a number of modules in
 * the workspace.
 * 
 * @author Fabio
 */
public class RenameFunctionRefactoringTest extends RefactoringRenameTestBase {

    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = true;
            RenameFunctionRefactoringTest test = new RenameFunctionRefactoringTest();
            test.setUp();
            test.testRename1();
            test.tearDown();

            junit.textui.TestRunner.run(RenameFunctionRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class getProcessUnderTest() {
        return PyRenameFunctionProcess.class;
    }

    public void testRename1() throws Exception {
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renamefunction.renfoo", 0, 8);
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); //the current module must also be there
        assertTrue(references.containsKey("reflib.renamefunction.accessfoo"));

        assertFalse(references.containsKey("reflib.renamefunction.renfoo")); //the current module does not have a separated key here
        assertFalse(references.containsKey("reflib.renamefunction.__init__"));

        //the modules with a duplicate definition here should not be in the results.
        //CHANGE: Now, access even in those places (duck typing in python can
        //make it valid).
        assertTrue(references.containsKey("reflib.renamefunction.accessdup"));
        assertTrue(references.containsKey("reflib.renamefunction.duprenfoo"));

        assertEquals(4, references.get(CURRENT_MODULE_IN_REFERENCES).size());
        assertContains(1, 5, references.get(CURRENT_MODULE_IN_REFERENCES));
        assertContains(4, 7, references.get(CURRENT_MODULE_IN_REFERENCES));
        assertContains(5, 14, references.get(CURRENT_MODULE_IN_REFERENCES));
        assertContains(6, 15, references.get(CURRENT_MODULE_IN_REFERENCES));

        assertEquals(4, references.get("reflib.renamefunction.accessfoo").size());
        assertContains(1, 20, references.get("reflib.renamefunction.accessfoo"));
        assertContains(4, 7, references.get("reflib.renamefunction.accessfoo"));
        assertContains(5, 17, references.get("reflib.renamefunction.accessfoo"));
        assertContains(7, 5, references.get("reflib.renamefunction.accessfoo"));

        assertEquals(8, references.size());
    }

    public void testRename2() throws Exception {
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renamefunction.accessfoo", 0,
                22);
        assertTrue(references.containsKey("reflib.renamefunction.accessfoo") == false); //the current module does not have a separated key here
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); //the current module must also be there
        assertTrue(references.containsKey("reflib.renamefunction.renfoo")); //the module where it is actually defined
        checkProcessors();
    }

    public void testRename3() throws Exception {
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renameparameter.methoddef", 1,
                6);
        assertTrue(references.containsKey("reflib.renameparameter.methodaccess"));
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES));
        assertEquals(4, references.get("reflib.renameparameter.methodaccess").size());
        assertEquals(1, references.get(CURRENT_MODULE_IN_REFERENCES).size());
        checkProcessors();
    }

    public void testRename4() throws Exception {
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renamefunction.classfunc", 1,
                8);
        assertEquals(1, references.size());
        assertEquals(2, references.get(CURRENT_MODULE_IN_REFERENCES).size());
        checkProcessors();
    }

}
