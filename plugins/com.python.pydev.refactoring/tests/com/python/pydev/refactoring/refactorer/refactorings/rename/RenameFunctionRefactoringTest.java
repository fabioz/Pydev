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

import com.python.pydev.analysis.refactoring.wizards.rename.PyRenameFunctionProcess;

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
    protected Class<PyRenameFunctionProcess> getProcessUnderTest() {
        return PyRenameFunctionProcess.class;
    }

    public void testRename1() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renamefunction.renfoo", 0, 8);
        assertEquals(""
                + "reflib.renameclass.accessdup\n"
                + "  ASTEntry<RenFoo (Name L=3 C=7)>\n"
                + "    Line: 2  print(RenFoo) --> print(new_name)\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=24)>\n"
                + "    Line: 0  from .duprenfoo import RenFoo --> from .duprenfoo import new_name\n"
                + "\n"
                + "reflib.renameclass.accessfoo\n"
                + "  ASTEntry<RenFoo (Name L=4 C=7)>\n"
                + "    Line: 3  print(RenFoo) --> print(new_name)\n"
                + "  ASTEntry<RenFoo (Name L=5 C=11)>\n"
                + "    Line: 4  #Comment: RenFoo --> #Comment: new_name\n"
                + "  ASTEntry<RenFoo (Name L=6 C=9)>\n"
                + "    Line: 5  'String:RenFoo' --> 'String:new_name'\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=21)>\n"
                + "    Line: 0  from .renfoo import RenFoo --> from .renfoo import new_name\n"
                + "\n"
                + "reflib.renameclass.duprenfoo\n"
                + "  ASTEntry<RenFoo (Name L=6 C=7)>\n"
                + "    Line: 5  print(RenFoo) --> print(new_name)\n"
                + "  ASTEntry<RenFoo (NameTok L=3 C=7)>\n"
                + "    Line: 2  class RenFoo(object): --> class new_name(object):\n"
                + "\n"
                + "reflib.renameclass.renfoo\n"
                + "  ASTEntry<RenFoo (Name L=4 C=7)>\n"
                + "    Line: 3  print(RenFoo) --> print(new_name)\n"
                + "  ASTEntry<RenFoo (Name L=6 C=11)>\n"
                + "    Line: 5  #comment: RenFoo must be renamed --> #comment: new_name must be renamed\n"
                + "  ASTEntry<RenFoo (Name L=7 C=10)>\n"
                + "    Line: 6  'string: RenFoo must be renamed' --> 'string: new_name must be renamed'\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=7)>\n"
                + "    Line: 0  class RenFoo(object): --> class new_name(object):\n"
                + "\n"
                + "reflib.renamefunction.accessdup\n"
                + "  ASTEntry<RenFoo (Name L=3 C=7)>\n"
                + "    Line: 2  print(RenFoo) --> print(new_name)\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=24)>\n"
                + "    Line: 0  from .duprenfoo import RenFoo --> from .duprenfoo import new_name\n"
                + "\n"
                + "reflib.renamefunction.accessfoo\n"
                + "  ASTEntry<RenFoo (Name L=4 C=7)>\n"
                + "    Line: 3  print(RenFoo) --> print(new_name)\n"
                + "  ASTEntry<RenFoo (Name L=5 C=17)>\n"
                + "    Line: 4  #comment access RenFoo --> #comment access new_name\n"
                + "  ASTEntry<RenFoo (Name L=7 C=5)>\n"
                + "    Line: 6      RenFoo access -->     new_name access\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=21)>\n"
                + "    Line: 0  from .renfoo import RenFoo --> from .renfoo import new_name\n"
                + "\n"
                + "reflib.renamefunction.duprenfoo\n"
                + "  ASTEntry<RenFoo (Name L=6 C=7)>\n"
                + "    Line: 5  print(RenFoo) --> print(new_name)\n"
                + "  ASTEntry<RenFoo (NameTok L=3 C=5)>\n"
                + "    Line: 2  def RenFoo(a): --> def new_name(a):\n"
                + "\n"
                + "reflib.renamefunction.renfoo\n"
                + "  ASTEntry<RenFoo (Name L=4 C=7)>\n"
                + "    Line: 3  print(RenFoo) --> print(new_name)\n"
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
                "reflib.renamefunction.accessfoo", 0,
                22);
        assertEquals(""
                + "reflib.renameclass.accessdup\n"
                + "  ASTEntry<RenFoo (Name L=3 C=7)>\n"
                + "    Line: 2  print(RenFoo) --> print(new_name)\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=24)>\n"
                + "    Line: 0  from .duprenfoo import RenFoo --> from .duprenfoo import new_name\n"
                + "\n"
                + "reflib.renameclass.accessfoo\n"
                + "  ASTEntry<RenFoo (Name L=4 C=7)>\n"
                + "    Line: 3  print(RenFoo) --> print(new_name)\n"
                + "  ASTEntry<RenFoo (Name L=5 C=11)>\n"
                + "    Line: 4  #Comment: RenFoo --> #Comment: new_name\n"
                + "  ASTEntry<RenFoo (Name L=6 C=9)>\n"
                + "    Line: 5  'String:RenFoo' --> 'String:new_name'\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=21)>\n"
                + "    Line: 0  from .renfoo import RenFoo --> from .renfoo import new_name\n"
                + "\n"
                + "reflib.renameclass.duprenfoo\n"
                + "  ASTEntry<RenFoo (Name L=6 C=7)>\n"
                + "    Line: 5  print(RenFoo) --> print(new_name)\n"
                + "  ASTEntry<RenFoo (NameTok L=3 C=7)>\n"
                + "    Line: 2  class RenFoo(object): --> class new_name(object):\n"
                + "\n"
                + "reflib.renameclass.renfoo\n"
                + "  ASTEntry<RenFoo (Name L=4 C=7)>\n"
                + "    Line: 3  print(RenFoo) --> print(new_name)\n"
                + "  ASTEntry<RenFoo (Name L=6 C=11)>\n"
                + "    Line: 5  #comment: RenFoo must be renamed --> #comment: new_name must be renamed\n"
                + "  ASTEntry<RenFoo (Name L=7 C=10)>\n"
                + "    Line: 6  'string: RenFoo must be renamed' --> 'string: new_name must be renamed'\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=7)>\n"
                + "    Line: 0  class RenFoo(object): --> class new_name(object):\n"
                + "\n"
                + "reflib.renamefunction.accessdup\n"
                + "  ASTEntry<RenFoo (Name L=3 C=7)>\n"
                + "    Line: 2  print(RenFoo) --> print(new_name)\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=24)>\n"
                + "    Line: 0  from .duprenfoo import RenFoo --> from .duprenfoo import new_name\n"
                + "\n"
                + "reflib.renamefunction.accessfoo\n"
                + "  ASTEntry<RenFoo (Name L=4 C=7)>\n"
                + "    Line: 3  print(RenFoo) --> print(new_name)\n"
                + "  ASTEntry<RenFoo (Name L=5 C=17)>\n"
                + "    Line: 4  #comment access RenFoo --> #comment access new_name\n"
                + "  ASTEntry<RenFoo (Name L=7 C=5)>\n"
                + "    Line: 6      RenFoo access -->     new_name access\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=21)>\n"
                + "    Line: 0  from .renfoo import RenFoo --> from .renfoo import new_name\n"
                + "\n"
                + "reflib.renamefunction.duprenfoo\n"
                + "  ASTEntry<RenFoo (Name L=6 C=7)>\n"
                + "    Line: 5  print(RenFoo) --> print(new_name)\n"
                + "  ASTEntry<RenFoo (NameTok L=3 C=5)>\n"
                + "    Line: 2  def RenFoo(a): --> def new_name(a):\n"
                + "\n"
                + "reflib.renamefunction.renfoo\n"
                + "  ASTEntry<RenFoo (Name L=4 C=7)>\n"
                + "    Line: 3  print(RenFoo) --> print(new_name)\n"
                + "  ASTEntry<RenFoo (Name L=5 C=14)>\n"
                + "    Line: 4  'String with RenFoo' --> 'String with new_name'\n"
                + "  ASTEntry<RenFoo (Name L=6 C=15)>\n"
                + "    Line: 5  #comment with RenFoo --> #comment with new_name\n"
                + "  ASTEntry<RenFoo (NameTok L=1 C=5)>\n"
                + "    Line: 0  def RenFoo(): --> def new_name():\n"
                + "\n"
                + "", asStr(references));

        checkProcessors();
    }

    public void testRename3() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renameparameter.methoddef", 1,
                6);
        assertEquals(
                ""
                        + "reflib.renameparameter.methodaccess\n"
                        + "  ASTEntry<Method1 (Name L=2 C=1)>\n"
                        + "    Line: 1  Method1(10, param2=20) --> new_name(10, param2=20)\n"
                        + "  ASTEntry<Method1 (Name L=3 C=1)>\n"
                        + "    Line: 2  Method1(param1=10, param2=20) --> new_name(param1=10, param2=20)\n"
                        + "  ASTEntry<Method1 (Name L=5 C=1)>\n"
                        + "    Line: 4  Method1(param1=param1, param2=20) --> new_name(param1=param1, param2=20)\n"
                        + "  ASTEntry<Method1 (NameTok L=1 C=24)>\n"
                        + "    Line: 0  from .methoddef import Method1 --> from .methoddef import new_name\n"
                        + "\n"
                        + "reflib.renameparameter.methoddef\n"
                        + "  ASTEntry<Method1 (NameTok L=2 C=5)>\n"
                        + "    Line: 1  def Method1(param1=param1, param2=None): --> def new_name(param1=param1, param2=None):\n"
                        + "\n"
                        + "",
                asStr(references));

        checkProcessors();
    }

    public void testRename4() throws Exception {
        Map<Tuple<String, File>, HashSet<ASTEntry>> references = getReferencesForRenameSimple(
                "reflib.renamefunction.classfunc", 1,
                8);
        assertEquals(""
                + "reflib.renamefunction.classfunc\n"
                + "  ASTEntry<mmm (NameTok L=2 C=9)>\n"
                + "    Line: 1      def mmm(self): -->     def new_name(self):\n"
                + "  ASTEntry<mmm (NameTok L=5 C=3)>\n"
                + "    Line: 4  f.mmm() --> f.new_name()\n"
                + "\n"
                + "", asStr(references));
        checkProcessors();
    }

}
