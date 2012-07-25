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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.refactoring.wizards.rename.PyRenameClassProcess;

/**
 * Class that should test the renaming of classes within a number of modules in
 * the workspace.
 * 
 * TODO: fix faling test because it should not get 'onlystringrefs' 
 * 
 * @author Fabio
 */
public class RenameClassRefactoringTest extends RefactoringRenameTestBase {

    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = true;
            RenameClassRefactoringTest test = new RenameClassRefactoringTest();
            test.setUp();
            test.testRename1();
            test.tearDown();

            junit.textui.TestRunner.run(RenameClassRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    protected Class getProcessUnderTest() {
        return PyRenameClassProcess.class;
    }

    public void testRename1() throws Exception {
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renameclass.renfoo", 0, 8);
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); //the current module must also be there

        assertFalse(references.containsKey("reflib.renameclass.renfoo")); //the current module does not have a separated key here
        assertFalse(references.containsKey("reflib.renameclass.__init__"));

        //the modules with a duplicate definition here should not be in the results.
        assertTrue(references.containsKey("reflib.renameclass.accessdup"));
        assertTrue(references.containsKey("reflib.renameclass.duprenfoo"));

        assertEquals(4, references.get(CURRENT_MODULE_IN_REFERENCES).size());
        assertContains(1, 7, references.get(CURRENT_MODULE_IN_REFERENCES));
        assertContains(4, 7, references.get(CURRENT_MODULE_IN_REFERENCES));
        assertContains(6, 11, references.get(CURRENT_MODULE_IN_REFERENCES));
        assertContains(7, 10, references.get(CURRENT_MODULE_IN_REFERENCES));

        assertEquals(4, references.get("reflib.renameclass.accessfoo").size());
        assertContains(1, 20, references.get("reflib.renameclass.accessfoo"));
        assertContains(4, 7, references.get("reflib.renameclass.accessfoo"));
        assertContains(5, 11, references.get("reflib.renameclass.accessfoo"));
        assertContains(6, 9, references.get("reflib.renameclass.accessfoo"));

        assertEquals(8, references.size());

    }

    public void testRename2() throws Exception {
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renameclass.accessfoo", 0, 22);
        assertTrue(references.containsKey("reflib.renameclass.accessfoo") == false); //the current module does not have a separated key here
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); //the current module must also be there
        assertTrue(references.containsKey("reflib.renameclass.renfoo")); //the module where it is actually defined
    }

    public void testRenameLocalClass() throws Exception {
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renamelocaltoken.__init__", 1,
                12);
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES));
        assertEquals(1, references.size());
        Collection<ASTEntry> entries = references.get(CURRENT_MODULE_IN_REFERENCES);
        assertEquals(2, entries.size());
    }

    public void testRename3() throws Exception {
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renameclass2.defuser", 2, 8);
        assertTrue(references.containsKey("reflib.renameclass2.defuser") == false); //the current module does not have a separated key here
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); //the current module must also be there
        assertTrue(references.containsKey("reflib.renameclass2.sub1.__init__"));
        assertTrue(references.containsKey("reflib.renameclass2.sub1.defmod"));
        assertTrue(references.containsKey("reflib.renameclass2.defuser2"));
    }

    public void testRename4() throws Exception {
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renameclass.renkkk", 0, 8);
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); //the current module must also be there
        assertEquals(1, references.size());

        Collection<ASTEntry> refs = references.get(CURRENT_MODULE_IN_REFERENCES);
        for (ASTEntry entry : refs) {
            assertTrue((entry.node.beginColumn == 1 && entry.node.beginLine == 1)
                    || (entry.node.beginColumn == 9 && entry.node.beginLine == 4));
            assertEquals("ActionProvider", entry.getName());
        }
    }

}
