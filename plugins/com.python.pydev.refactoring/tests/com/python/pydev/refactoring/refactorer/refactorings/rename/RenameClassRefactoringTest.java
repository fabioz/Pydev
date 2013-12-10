/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Dec 10, 2006
 * @author Fabio
 */
package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.io.File;
import java.util.HashSet;
import java.util.Map;

import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.structure.Tuple;

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

    @Override
    protected Class<PyRenameClassProcess> getProcessUnderTest() {
        return PyRenameClassProcess.class;
    }

    public void testRename1() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameclass.renfoo", 0, 8);
        assertEquals(""
                + "reflib.renameclass.accessdup\n"
                + "  ASTEntry<RenFoo (Name L=3 C=7)>\n"
                + "    Line: 2  print RenFoo --> print new_name\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=23)>\n"
                + "    Line: 0  from duprenfoo import RenFoo --> from duprenfoo import new_name\n"
                + "\n"
                + "reflib.renameclass.accessfoo\n"
                + "  ASTEntry<RenFoo (Name L=4 C=7)>\n"
                + "    Line: 3  print RenFoo --> print new_name\n"
                + "  ASTEntry<RenFoo (Name L=5 C=11)>\n"
                + "    Line: 4  #Comment: RenFoo --> #Comment: new_name\n"
                + "  ASTEntry<RenFoo (Name L=6 C=9)>\n"
                + "    Line: 5  'String:RenFoo' --> 'String:new_name'\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=20)>\n"
                + "    Line: 0  from renfoo import RenFoo --> from renfoo import new_name\n"
                + "\n"
                + "reflib.renameclass.duprenfoo\n"
                + "  ASTEntry<RenFoo (Name L=6 C=7)>\n"
                + "    Line: 5  print RenFoo --> print new_name\n"
                + "  ASTEntry<RenFoo (NameTok L=3 C=7)>\n"
                + "    Line: 2  class RenFoo(object): --> class new_name(object):\n"
                + "\n"
                + "reflib.renameclass.renfoo\n"
                + "  ASTEntry<RenFoo (ClassDef L=1 C=1)>\n"
                + "    Line: 0  class RenFoo(object): --> class new_name(object):\n"
                + "  ASTEntry<RenFoo (Name L=4 C=7)>\n"
                + "    Line: 3  print RenFoo --> print new_name\n"
                + "  ASTEntry<RenFoo (Name L=6 C=11)>\n"
                + "    Line: 5  #comment: RenFoo must be renamed --> #comment: new_name must be renamed\n"
                + "  ASTEntry<RenFoo (Name L=7 C=10)>\n"
                + "    Line: 6  'string: RenFoo must be renamed' --> 'string: new_name must be renamed'\n"
                + "\n"
                + "reflib.renamefunction.accessdup\n"
                + "  ASTEntry<RenFoo (Name L=3 C=7)>\n"
                + "    Line: 2  print RenFoo --> print new_name\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=23)>\n"
                + "    Line: 0  from duprenfoo import RenFoo --> from duprenfoo import new_name\n"
                + "\n"
                + "reflib.renamefunction.accessfoo\n"
                + "  ASTEntry<RenFoo (Name L=4 C=7)>\n"
                + "    Line: 3  print RenFoo --> print new_name\n"
                + "  ASTEntry<RenFoo (Name L=5 C=17)>\n"
                + "    Line: 4  #comment access RenFoo --> #comment access new_name\n"
                + "  ASTEntry<RenFoo (Name L=7 C=5)>\n"
                + "    Line: 6      RenFoo access -->     new_name access\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=20)>\n"
                + "    Line: 0  from renfoo import RenFoo --> from renfoo import new_name\n"
                + "\n"
                + "reflib.renamefunction.duprenfoo\n"
                + "  ASTEntry<RenFoo (Name L=6 C=7)>\n"
                + "    Line: 5  print RenFoo --> print new_name\n"
                + "  ASTEntry<RenFoo (NameTok L=3 C=5)>\n"
                + "    Line: 2  def RenFoo(a): --> def new_name(a):\n"
                + "\n"
                + "reflib.renamefunction.renfoo\n"
                + "  ASTEntry<RenFoo (Name L=4 C=7)>\n"
                + "    Line: 3  print RenFoo --> print new_name\n"
                + "  ASTEntry<RenFoo (Name L=5 C=14)>\n"
                + "    Line: 4  'String with RenFoo' --> 'String with new_name'\n"
                + "  ASTEntry<RenFoo (Name L=6 C=15)>\n"
                + "    Line: 5  #comment with RenFoo --> #comment with new_name\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=5)>\n"
                + "    Line: 0  def RenFoo(): --> def new_name():\n"
                + "\n"
                + "", asStr(references));

    }

    public void testRename2() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameclass.accessfoo", 0, 22);
        assertEquals(""
                + "reflib.renameclass.accessdup\n"
                + "  ASTEntry<RenFoo (Name L=3 C=7)>\n"
                + "    Line: 2  print RenFoo --> print new_name\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=23)>\n"
                + "    Line: 0  from duprenfoo import RenFoo --> from duprenfoo import new_name\n"
                + "\n"
                + "reflib.renameclass.accessfoo\n"
                + "  ASTEntry<RenFoo (Name L=4 C=7)>\n"
                + "    Line: 3  print RenFoo --> print new_name\n"
                + "  ASTEntry<RenFoo (Name L=5 C=11)>\n"
                + "    Line: 4  #Comment: RenFoo --> #Comment: new_name\n"
                + "  ASTEntry<RenFoo (Name L=6 C=9)>\n"
                + "    Line: 5  'String:RenFoo' --> 'String:new_name'\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=20)>\n"
                + "    Line: 0  from renfoo import RenFoo --> from renfoo import new_name\n"
                + "\n"
                + "reflib.renameclass.duprenfoo\n"
                + "  ASTEntry<RenFoo (Name L=6 C=7)>\n"
                + "    Line: 5  print RenFoo --> print new_name\n"
                + "  ASTEntry<RenFoo (NameTok L=3 C=7)>\n"
                + "    Line: 2  class RenFoo(object): --> class new_name(object):\n"
                + "\n"
                + "reflib.renameclass.renfoo\n"
                + "  ASTEntry<RenFoo (Name L=4 C=7)>\n"
                + "    Line: 3  print RenFoo --> print new_name\n"
                + "  ASTEntry<RenFoo (Name L=6 C=11)>\n"
                + "    Line: 5  #comment: RenFoo must be renamed --> #comment: new_name must be renamed\n"
                + "  ASTEntry<RenFoo (Name L=7 C=10)>\n"
                + "    Line: 6  'string: RenFoo must be renamed' --> 'string: new_name must be renamed'\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=7)>\n"
                + "    Line: 0  class RenFoo(object): --> class new_name(object):\n"
                + "\n"
                + "reflib.renamefunction.accessdup\n"
                + "  ASTEntry<RenFoo (Name L=3 C=7)>\n"
                + "    Line: 2  print RenFoo --> print new_name\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=23)>\n"
                + "    Line: 0  from duprenfoo import RenFoo --> from duprenfoo import new_name\n"
                + "\n"
                + "reflib.renamefunction.accessfoo\n"
                + "  ASTEntry<RenFoo (Name L=4 C=7)>\n"
                + "    Line: 3  print RenFoo --> print new_name\n"
                + "  ASTEntry<RenFoo (Name L=5 C=17)>\n"
                + "    Line: 4  #comment access RenFoo --> #comment access new_name\n"
                + "  ASTEntry<RenFoo (Name L=7 C=5)>\n"
                + "    Line: 6      RenFoo access -->     new_name access\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=20)>\n"
                + "    Line: 0  from renfoo import RenFoo --> from renfoo import new_name\n"
                + "\n"
                + "reflib.renamefunction.duprenfoo\n"
                + "  ASTEntry<RenFoo (Name L=6 C=7)>\n"
                + "    Line: 5  print RenFoo --> print new_name\n"
                + "  ASTEntry<RenFoo (NameTok L=3 C=5)>\n"
                + "    Line: 2  def RenFoo(a): --> def new_name(a):\n"
                + "\n"
                + "reflib.renamefunction.renfoo\n"
                + "  ASTEntry<RenFoo (Name L=4 C=7)>\n"
                + "    Line: 3  print RenFoo --> print new_name\n"
                + "  ASTEntry<RenFoo (Name L=5 C=14)>\n"
                + "    Line: 4  'String with RenFoo' --> 'String with new_name'\n"
                + "  ASTEntry<RenFoo (Name L=6 C=15)>\n"
                + "    Line: 5  #comment with RenFoo --> #comment with new_name\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=5)>\n"
                + "    Line: 0  def RenFoo(): --> def new_name():\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRenameLocalClass() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renamelocaltoken.__init__", 1,
                12);
        assertEquals(""
                + "reflib.renamelocaltoken.__init__\n"
                + "  ASTEntry<LocalFoo (ClassDef L=2 C=5)>\n"
                + "    Line: 1      class LocalFoo: -->     class new_name:\n"
                + "  ASTEntry<LocalFoo (Name L=4 C=11)>\n"
                + "    Line: 3      print LocalFoo -->     print new_name\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRename3() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameclass2.defuser", 2, 8);
        assertEquals(""
                + "reflib.renameclass2.defuser\n"
                + "  ASTEntry<Definition (Name L=3 C=7)>\n"
                + "    Line: 2  print Definition --> print new_name\n"
                + "  ASTEntry<Definition (NameTok L=1 C=18)>\n"
                + "    Line: 0  from sub1 import Definition --> from sub1 import new_name\n"
                + "\n"
                + "reflib.renameclass2.defuser2\n"
                + "  ASTEntry<Definition (Name L=3 C=7)>\n"
                + "    Line: 2  print Definition --> print new_name\n"
                + "  ASTEntry<Definition (NameTok L=1 C=21)>\n"
                + "    Line: 0  from defuser import Definition --> from defuser import new_name\n"
                + "\n"
                + "reflib.renameclass2.sub1.__init__\n"
                + "  ASTEntry<Definition (NameTok L=1 C=20)>\n"
                + "    Line: 0  from defmod import Definition --> from defmod import new_name\n"
                + "\n"
                + "reflib.renameclass2.sub1.defmod\n"
                + "  ASTEntry<Definition (NameTok L=1 C=7)>\n"
                + "    Line: 0  class Definition(object): --> class new_name(object):\n"
                + "\n"
                + "", asStr(references));
    }

    public void testRename4() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameclass.renkkk", 0, 8);
        assertEquals(
                ""
                        + "reflib.renameclass.renkkk\n"
                        + "  ASTEntry<ActionProvider (ClassDef L=1 C=1)>\n"
                        + "    Line: 0  class ActionProvider(object): --> class new_name(object):\n"
                        + "  ASTEntry<ActionProvider (Name L=4 C=9)>\n"
                        + "    Line: 3          ActionProvider()._DoSlotImportSimulation() -->         new_name()._DoSlotImportSimulation()\n"
                        + "\n"
                        + "", asStr(references));
        //        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); //the current module must also be there
        //        assertEquals(1, references.size());
        //
        //        Collection<ASTEntry> refs = references.get(CURRENT_MODULE_IN_REFERENCES);
        //        for (ASTEntry entry : refs) {
        //            assertTrue((entry.node.beginColumn == 1 && entry.node.beginLine == 1)
        //                    || (entry.node.beginColumn == 9 && entry.node.beginLine == 4));
        //            assertEquals("ActionProvider", entry.getName());
        //        }
    }

}
