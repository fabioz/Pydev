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

import com.python.pydev.refactoring.wizards.rename.PyRenameSelfAttributeProcess;

public class RenameSelfRefactoringTest extends RefactoringRenameTestBase {

    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = false;
            RenameSelfRefactoringTest test = new RenameSelfRefactoringTest();
            test.setUp();
            test.testRenameSelf();
            test.tearDown();

            junit.textui.TestRunner.run(RenameSelfRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class<PyRenameSelfAttributeProcess> getProcessUnderTest() {
        return PyRenameSelfAttributeProcess.class;
    }

    public void testRenameSelf() throws Exception {
        //Line 0 = "def Method1(param1, param2=None):"
        //rename param1
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameself.renameselfclass",
                2, 14);
        assertEquals(""
                + "reflib.renameself.renameselfclass\n"
                + "  ASTEntry<instance1 (Name L=5 C=10)>\n"
                + "    Line: 4          #instance1 comment -->         #new_name comment\n"
                + "  ASTEntry<instance1 (Name L=6 C=10)>\n"
                + "    Line: 5          'instance1 string' -->         'new_name string'\n"
                + "  ASTEntry<instance1 (NameTok L=3 C=14)>\n"
                + "    Line: 2          self.instance1 = 1 -->         self.new_name = 1\n"
                + "\n"
                + "reflib.renameself.renameselfclass2\n"
                + "  ASTEntry<instance1 (Name L=10 C=2)>\n"
                + "    Line: 9  'instance1 string' --> 'new_name string'\n"
                + "  ASTEntry<instance1 (Name L=9 C=2)>\n"
                + "    Line: 8  #instance1 comment --> #new_name comment\n"
                + "  ASTEntry<instance1 (NameTok L=4 C=14)>\n"
                + "    Line: 3          self.instance1 = 1 -->         self.new_name = 1\n"
                + "  ASTEntry<instance1 (NameTok L=8 C=20)>\n"
                + "    Line: 7  RenameSelfClass2().instance1 = 2 --> RenameSelfClass2().new_name = 2\n"
                + "\n"
                + "", asStr(references));
    }
}
