/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Dec 12, 2006
 * @author Fabio
 */
package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.util.HashSet;
import java.util.Map;

import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.refactoring.wizards.rename.PyRenameClassProcess;

public class RefactoringLocalToken extends RefactoringRenameTestBase {
    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = false;
            RefactoringLocalToken test = new RefactoringLocalToken();
            test.setUp();
            test.testRename2();
            test.tearDown();

            junit.textui.TestRunner.run(RefactoringLocalToken.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    protected Class getProcessUnderTest() {
        return PyRenameClassProcess.class;
    }

    public void testRename1() throws Exception {
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renameclass.renfoo", 0, 8);
        assertTrue(references.containsKey("reflib.renameclass.renfoo") == false); //the current module does not have a separated key here
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); //the current module must also be there

        assertTrue(references.containsKey("reflib.renameclass.__init__") == false);

        //the modules with a duplicate definition here should not be in the results.
        assertTrue(references.containsKey("reflib.renameclass.accessdup"));
        assertTrue(references.containsKey("reflib.renameclass.duprenfoo"));
    }

    public void testRename2() throws Exception {
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renameclass.accessfoo", 0, 22);
        assertTrue(references.containsKey("reflib.renameclass.accessfoo") == false); //the current module does not have a separated key here
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); //the current module must also be there
        assertContains(references, "reflib.renameclass.renfoo"); //the module where it is actually defined
    }

}
