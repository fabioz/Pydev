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

import com.python.pydev.refactoring.wizards.rename.PyRenameAnyLocalProcess;
import com.python.pydev.refactoring.wizards.rename.PyRenameAttributeProcess;

@SuppressWarnings("rawtypes")
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

    private Class expectedProcessClass;

    @Override
    protected Class getProcessUnderTest() {
        return expectedProcessClass;
    }

    public void testRenameAttribute() throws Exception {
        expectedProcessClass = PyRenameAttributeProcess.class;
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
                + "  ASTEntry<attrInstance (Name L=3 C=6)>\n"
                + "    Line: 2      #attrInstance comment -->     #new_name comment\n"
                + "  ASTEntry<attrInstance (Name L=4 C=6)>\n"
                + "    Line: 3      'attrInstance comment' -->     'new_name comment'\n"
                + "  ASTEntry<attrInstance (NameTok L=2 C=7)>\n"
                + "    Line: 1      a.attrInstance = 10 -->     a.new_name = 10\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRenameAttribute2() throws Exception {
        expectedProcessClass = PyRenameAnyLocalProcess.class;
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameattribute2.mod1", 3, 18);
        assertEquals(""
                + "reflib.renameattribute2.mod1\n"
                + "  ASTEntry<attribute_to_be_found (NameTok L=4 C=18)>\n"
                + "    Line: 3          if param.attribute_to_be_found: -->         if param.new_name:\n"
                + "\n"
                + "reflib.renameattribute2.mod2\n"
                + "  ASTEntry<attribute_to_be_found (NameTok L=4 C=14)>\n"
                + "    Line: 3          self.attribute_to_be_found = True -->         self.new_name = True\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRenameClassAttribute() throws Exception {
        expectedProcessClass = PyRenameAttributeProcess.class;
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameclassattribute.mod2", 5, 24);
        assertEquals(
                ""
                        + "reflib.renameclassattribute.mod1\n"
                        + "  ASTEntry<class_attribute_to_be_found (NameTok L=4 C=18)>\n"
                        + "    Line: 3          if param.class_attribute_to_be_found: -->         if param.new_name:\n"
                        + "\n"
                        + "reflib.renameclassattribute.mod2\n"
                        + "  ASTEntry<class_attribute_to_be_found (Name L=3 C=5)>\n"
                        + "    Line: 2      class_attribute_to_be_found = True -->     new_name = True\n"
                        + "  ASTEntry<class_attribute_to_be_found (NameTok L=6 C=23)>\n"
                        + "    Line: 5          ClassWithAttr.class_attribute_to_be_found = True -->         ClassWithAttr.new_name = True\n"
                        + "\n"
                        + "", asStr(references));
    }
}
