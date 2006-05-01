/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings;


public class RenameSelfVariableRefactoringTest extends RefactoringTestBase{
    public static void main(String[] args) {
        try {
            RenameSelfVariableRefactoringTest test = new RenameSelfVariableRefactoringTest();
            test.setUp();
            test.testClassMethodRename2();
            test.tearDown();

            junit.textui.TestRunner.run(RenameSelfVariableRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    
    public void testHierarchyRename1() throws Exception {
        String str ="" +
        "class Foo(Bar):\n" +
        "    def m1(self):\n" +
        "        self.%s = 1\n" +
        "        print self.%s\n" +
        "    def m2(self):\n" +
        "        print self.%s\n" +
        "class Bar:\n" +
        "    def m3(self):\n" +
        "        self.%s = 1\n" +
        "        print self.%s\n" +
        "\n" +
        "";
        int line = 3;
        int col = 20;
        checkDefault(str, line, col);
        
    }
    
    public void testSimpleRename() throws Exception {
        String str ="" +
        "class Foo:\n" +
        "    def m1(self):\n" +
        "        self.%s = 1\n" +
        "        print self.%s\n" +
        "    def m2(self):\n" +
        "        print self.%s\n" +
        "";
        int line = 3;
        int col = 20;
        checkDefault(str, line, col);
    }
    
    public void testClassMethodRename() throws Exception {
        String str ="" +
        "class Foo:\n" +
        "    def %s(self):\n" +
        "        print self.%s()\n" +
        "";
        int line = 2;
        int col = 19;
        checkDefault(str, line, col, "m1", false, true);
    }
    
    public void testClassMethodRename2() throws Exception {
        String str ="" +
        "class Foo:\n" +
        "    def %s(self):\n" +
        "        print self.%s()\n" +
        "";
        int line = 1;
        int col = 9;
        checkDefault(str, line, col, "m1", false, true);
    }



}
