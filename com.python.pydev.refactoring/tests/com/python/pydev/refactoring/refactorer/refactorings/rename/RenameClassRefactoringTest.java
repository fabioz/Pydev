/*
 * Created on Apr 30, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings.rename;

import org.eclipse.core.runtime.CoreException;

public class RenameClassRefactoringTest extends RefactoringTestBase {


    public static void main(String[] args) {
        try {
            RenameClassRefactoringTest test = new RenameClassRefactoringTest();
            test.setUp();
            test.testRenameClassVar();
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
    	"\n" +
    	"\n" +
    	"\n";
    	checkRename(str, 1, 5, "bla", false, true);
    }

}
