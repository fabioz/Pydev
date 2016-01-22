/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalTokensInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;

public class RenameFunctionRefactoringTest2 extends RefactoringRenameTestBase {

    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = true;
            RenameFunctionRefactoringTest2 test = new RenameFunctionRefactoringTest2();
            test.setUp();
            test.tearDown();

            junit.textui.TestRunner.run(RenameFunctionRefactoringTest2.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testRename5() throws Exception {
        List<IInfo> toks = AdditionalProjectInterpreterInfo.getTokensEqualTo("RenameFunc2", natureRefactoring,
                AbstractAdditionalTokensInfo.TOP_LEVEL | AbstractAdditionalTokensInfo.INNER);
        assertEquals(4, toks.size());

        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renamefunction2.renamefunc2",
                3, 19);
        assertEquals(
                ""
                        + "reflib.renamefunction2.dontrenamefunc2\n"
                        + "  ASTEntry<RenameFunc2 (Name L=6 C=1)>\n"
                        + "    Line: 5  RenameFunc2.Bar --> new_name.Bar\n"
                        + "  ASTEntry<RenameFunc2 (NameTok L=1 C=7)>\n"
                        + "    Line: 0  class RenameFunc2: --> class new_name:\n"
                        + "\n"
                        + "reflib.renamefunction2.renamefunc2\n"
                        + "  ASTEntry<RenameFunc2 (Name L=6 C=1)>\n"
                        + "    Line: 5  RenameFunc2.RenameFunc2 #and only the 2nd part of the access --> new_name.RenameFunc2 #and only the 2nd part of the access\n"
                        + "  ASTEntry<RenameFunc2 (NameTok L=1 C=7)>\n"
                        + "    Line: 0  class RenameFunc2: --> class new_name:\n"
                        + "  ASTEntry<RenameFunc2 (NameTok L=3 C=9)>\n"
                        + "    Line: 2      def RenameFunc2(self): #rename this method -->     def new_name(self): #rename this method\n"
                        + "  ASTEntry<RenameFunc2 (NameTok L=4 C=18)>\n"
                        + "    Line: 3          self.bar.RenameFunc2 -->         self.bar.new_name\n"
                        + "  ASTEntry<RenameFunc2 (NameTok L=6 C=13)>\n"
                        + "    Line: 5  RenameFunc2.RenameFunc2 #and only the 2nd part of the access --> RenameFunc2.new_name #and only the 2nd part of the access\n"
                        + "\n"
                        + "reflib.renamefunction2.renamefunc3\n"
                        + "  ASTEntry<RenameFunc2 (Name L=3 C=19)>\n"
                        + "    Line: 2  class RenameFunc3(RenameFunc2): --> class RenameFunc3(new_name):\n"
                        + "  ASTEntry<RenameFunc2 (Name L=8 C=1)>\n"
                        + "    Line: 7  RenameFunc2.RenameFunc2 #and only the 2nd part of the access --> new_name.RenameFunc2 #and only the 2nd part of the access\n"
                        + "  ASTEntry<RenameFunc2 (NameTok L=1 C=48)>\n"
                        + "    Line: 0  from reflib.renamefunction2.renamefunc2 import RenameFunc2 --> from reflib.renamefunction2.renamefunc2 import new_name\n"
                        + "  ASTEntry<RenameFunc2 (NameTok L=5 C=9)>\n"
                        + "    Line: 4      def RenameFunc2(self): #rename this method -->     def new_name(self): #rename this method\n"
                        + "  ASTEntry<RenameFunc2 (NameTok L=6 C=18)>\n"
                        + "    Line: 5          self.bar.RenameFunc2 -->         self.bar.new_name\n"
                        + "  ASTEntry<RenameFunc2 (NameTok L=8 C=13)>\n"
                        + "    Line: 7  RenameFunc2.RenameFunc2 #and only the 2nd part of the access --> RenameFunc2.new_name #and only the 2nd part of the access\n"
                        + "\n"
                        + "", asStr(references));

        checkProcessors();
    }

    @Override
    protected void checkProcessors() {
    }

    @Override
    protected Class getProcessUnderTest() {
        throw new RuntimeException("Not used in this test!");
    }

}
