/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.refactorings.renamelocal;

public class RenameBuiltinTest extends RefactoringLocalTestBase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(RenameBuiltinTest.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    protected boolean getCompiledModulesEnabled() {
        return true;
    }

    @Override
    protected boolean getForceRestorePythonPath() {
        return true;
    }

    public void testRename3() throws Exception {
        String str = "" +
                "from qt import *\n" +
                "print %s\n" +
                "\n" +
                "\n" +
                "";

        int line = 1;
        int col = 7;
        checkRename(str, line, col, "QDialog", false, true);
    }

}
