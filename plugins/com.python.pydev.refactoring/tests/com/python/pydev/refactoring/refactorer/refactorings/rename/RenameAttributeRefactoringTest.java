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
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameattribute.attr2", 1, 8);
        assertEquals(""
                + "reflib.renameattribute.attr1\n"
                + "  ASTEntry<attrInstance (NameTok L=3 C=14)>\n"
                + "    Line: 2          self.attrInstance = 1 -->         self.new_name = 1\n"
                + "\n"
                + "reflib.renameattribute.attr2\n"
                + "  ASTEntry<attrInstance (Attribute L=2 C=7)>\n"
                + "    Line: 1      a.attrInstance = 10 -->     a.new_name = 10\n"
                + "  ASTEntry<attrInstance (Name L=3 C=6)>\n"
                + "    Line: 2      #attrInstance comment -->     #new_name comment\n"
                + "  ASTEntry<attrInstance (Name L=4 C=6)>\n"
                + "    Line: 3      'attrInstance comment' -->     'new_name comment'\n"
                + "\n"
                + "", asStr(references));
    }
}
