/*
 * Created on Mar 1, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings;


public class RenameLocalVariableRefactoringTest extends RefactoringTestBase {

    public static void main(String[] args) {
        try {
            RenameLocalVariableRefactoringTest test = new RenameLocalVariableRefactoringTest();
            test.setUp();
            test.testRenameParameter();
            test.tearDown();

            junit.textui.TestRunner.run(RenameLocalVariableRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private String getDefaultDocStr() {
        return "" +
        "def method():\n"+
        "    %s = 2\n"+
        "    print %s\n"+
        "";
    }
    
    public void testRenameErr() throws Exception {
        int line = 2;
        int col = 10;
        checkDefault(getDefaultDocStr(), line, col, "bb", true, false, "aaa bb");
    }

    public void testRenameInstance2() throws Exception {
        String str = "" +
                "class Foo:\n" +
                "    def m1(self,%s):\n" +//we want to target only the bb in this method and not in the next
                "        print %s\n" +
                "    def m2(self,bb):\n" +
                "        return bb\n" +
                "\n";
        int line = 2;
        int col = 16;
        checkDefault(str, line, col);
    }
    
    public void testRenameParameter() throws Exception {
    	String str = "" +
    	"class Foo:\n" +
    	"    def m1(self,%s):\n" +//we want to target only the parameter foo in this method and not the attribute
    	"        print %s\n" +
    	"        print self.foo" +
    	"\n";
    	int line = 1;
    	int col = 16;
    	checkDefault(str, line, col, "foo", false);
    }
    
    public void testRenameParameter2() throws Exception {
    	String str = "" +
    	"class Foo:\n" +
    	"    def m1(self,%s):\n" +//we want to target only the parameter foo in this method and not the attribute
    	"        print %s\n" +
    	"        print %s.bla" +
    	"\n";
    	int line = 1;
    	int col = 16;
    	checkDefault(str, line, col, "foo", false);
    }
    
    public void testRenameInstance() throws Exception {
        String str=getDefaultDocStr();
        int line = 2;
        int col = 10;
        
        checkDefault(str, line, col);
    }


}
