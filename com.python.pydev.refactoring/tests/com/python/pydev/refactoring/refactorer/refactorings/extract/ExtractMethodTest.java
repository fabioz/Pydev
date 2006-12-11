/*
 * Created on May 31, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings.extract;

import com.python.pydev.refactoring.refactorer.refactorings.renamelocal.RefactoringLocalTestBase;

public class ExtractMethodTest extends RefactoringLocalTestBase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ExtractMethodTest.class);
    }

    public void test1() throws Exception {
        String initial = ""+
            "a=1\n" +
            "";
        
        String expected = ""+
    		"m1()\n" +
    		"def m1():\n" +
    		"    a=1\n" +
    		"";
        
        checkExtract(initial, expected, 0,0,3, false, "m1");
    }
}
