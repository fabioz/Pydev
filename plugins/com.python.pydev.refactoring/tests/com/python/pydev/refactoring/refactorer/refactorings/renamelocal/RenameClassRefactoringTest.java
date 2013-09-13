/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 30, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings.renamelocal;

import org.eclipse.core.runtime.CoreException;

public class RenameClassRefactoringTest extends RefactoringLocalTestBase {

    public static void main(String[] args) {
        try {
            DEBUG = true;
            RenameClassRefactoringTest test = new RenameClassRefactoringTest();
            test.setUp();
            test.testRenameClassCall();
            test.tearDown();

            junit.textui.TestRunner.run(RenameClassRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testRenameClass() throws CoreException {
        String str = "" +
                "class %s:\n" +
                "   pass\n" +
                "print %s\n" +
                "\n";
        int line = 2;
        int col = 8;
        checkRename(str, line, col, "Foo", false, true);
    }

    public void testRenameClassVar() throws CoreException {
        String str = "" +
                "class Foo:\n" +
                "    %s = 10\n" +
                "    def m1(self):\n" +
                "        print self.%s\n" +
                "\n"
                +
                "\n" +
                "\n";
        checkRename(str, 1, 5, "bla", false, true);
    }

    public void testRenameClassCall() throws CoreException {
        String str = "" +
                "class Foo:\n" +
                "    def DoBar(self):\n" +
                "        %s(1,2)\n" +
                "class %s(object):\n"
                +
                "    pass\n" +
                "\n";
        checkRename(str, 2, 9, "Bar", false, true);
    }

    public void testRenameClassFromComments() throws CoreException {
        String str = ""
                +
                "#===================================================================================================\n"
                +
                "# Cache\n"
                +
                "#===================================================================================================\n"
                +
                "class Cache(object):\n"
                +
                "    def ClearCaches(self):\n"
                +
                "        self.calc_cache.clear()\n"
                +
                "#===================================================================================================\n"
                +
                "# %s\n"
                +
                "#===================================================================================================\n"
                +
                "class %s(object):\n" +
                "    def __init__(self, info):\n" +
                "        self.info = info\n" +
                "\n";
        checkRename(str, 7, 2, "ExportMethodCalcBase", false, true);
    }

    public void testRenameClassComments() throws CoreException {
        String str = ""
                +
                "#===================================================================================================\n"
                +
                "# Cache\n"
                +
                "#===================================================================================================\n"
                +
                "class Cache(object):\n"
                +
                "    def ClearCaches(self):\n"
                +
                "        self.calc_cache.clear()\n"
                +
                "#===================================================================================================\n"
                +
                "# %s\n"
                +
                "#===================================================================================================\n"
                +
                "class %s(object):\n" +
                "    def __init__(self, info):\n" +
                "        self.info = info\n" +
                "\n";
        checkRename(str, 9, 7, "ExportMethodCalcBase", false, true);
    }

}
