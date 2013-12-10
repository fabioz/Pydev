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
    protected Class<PyRenameParameterProcess> getProcessUnderTest() {
        return PyRenameParameterProcess.class;
    }

    public void testRenameParameter() throws Exception {
        //Line 1 = "def Method1(param1=param1, param2=None):"
        //rename param1
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameparameter.methoddef", 1,
                12);
        assertEquals(
                ""
                        + "reflib.renameparameter.methodaccess\n"
                        + "  ASTEntry<param1 (NameTok L=3 C=9)>\n"
                        + "    Line: 2  Method1(param1=10, param2=20) --> Method1(new_name=10, param2=20)\n"
                        + "  ASTEntry<param1 (NameTok L=5 C=9)>\n"
                        + "    Line: 4  Method1(param1=param1, param2=20) --> Method1(new_name=param1, param2=20)\n"
                        + "\n"
                        + "reflib.renameparameter.methoddef\n"
                        + "  ASTEntry<param1 (Name L=2 C=13)>\n"
                        + "    Line: 1  def Method1(param1=param1, param2=None): --> def Method1(new_name=param1, param2=None):\n"
                        + "  ASTEntry<param1 (Name L=3 C=11)>\n"
                        + "    Line: 2      print param1, param2 -->     print new_name, param2\n"
                        + "\n"
                        + "", asStr(references));
    }

    public void testRenameParameter2() throws Exception {
        //    def mm(self, barparam):"
        //rename barparam
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameparameter.methoddef2",
                1, 17);
        assertEquals(
                ""
                        + "reflib.renameparameter.methoddef2\n"
                        + "  ASTEntry<barparam (Name L=2 C=18)>\n"
                        + "    Line: 1      def mm(self, barparam): -->     def mm(self, new_name):\n"
                        + "  ASTEntry<barparam (Name L=4 C=20)>\n"
                        + "    Line: 3              @param barparam: this is barparam -->             @param new_name: this is barparam\n"
                        + "  ASTEntry<barparam (Name L=4 C=38)>\n"
                        + "    Line: 3              @param barparam: this is barparam -->             @param barparam: this is new_name\n"
                        + "  ASTEntry<barparam (NameTok L=7 C=6)>\n"
                        + "    Line: 6  f.mm(barparam=10) --> f.mm(new_name=10)\n"
                        + "\n"
                        + "", asStr(references));
    }

}
