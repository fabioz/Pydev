/*
 * Created on Mar 1, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings;


public class RenameLocalVariableRefactoringTest extends RefactoringTestBase {

    public static void main(String[] args) {
        try {
            RenameLocalVariableRefactoringTest test = new RenameLocalVariableRefactoringTest();
            test.setUp();
            test.testRenameInstance2();
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
    
    public void testRenameLocalMethod() throws Exception {
    	String str = "" +
    	"def Foo():\n" +
    	"    def %s():\n" +
    	"        pass\n" +
    	"    if %s(): pass\n" +
    	"\n" +
    	"";
    	int line = 1;
    	int col = 9;
    	checkDefault(str, line, col, "met1", false, true);
    }
    
    public void testRenameLocalMethod2() throws Exception {
    	String str = "" +
    	"def %s():\n" +
    	"    def mm():\n" +
    	"        print %s()\n" +
    	"\n" +
    	"";
    	int line = 0;
    	int col = 5;
    	checkDefault(str, line, col, "met1", false, true);
    }
    
    public void testRenameImportLocally() throws Exception {
    	String str = "" +
    	"from foo import %s\n" +
    	"def mm():\n" +
    	"    print %s\n" +
    	"\n" +
    	"";
    	int line = 0;
    	int col = 17;
    	checkDefault(str, line, col, "bla", false, true);
    }
    
    public void testRenameImportLocally2() throws Exception {
    	String str = "" +
    	"import %s\n" +
    	"def run():\n" +
    	"    print %s.getopt()\n" +
    	"\n" +
    	"";
    	int line = 0;
    	int col = 10;
    	checkDefault(str, line, col, "getopt", false, true);
    }
    
    public void testRenameImportLocally3() throws Exception {
    	String str = "" +
    	"import %s\n" +
    	"def run():\n" +
    	"    print %s\n" +
    	"\n" +
    	"";
    	int line = 0;
    	int col = 10;
    	checkDefault(str, line, col, "sys", false, true);
    }
    
    public void testRenameImportLocally4() throws Exception {
    	String str = "" +
    	"from extendable.constants import %s\n" +
    	"def run():\n" +
    	"    print %s\n" +
    	"\n" +
    	"";
    	int line = 2;
    	int col = 11;
    	checkDefault(str, line, col, "CONSTANT1", false, true);
    }
    
    public void testRenameMethodImportLocally() throws Exception {
    	String str = "" +
    	"from testAssist.assist.ExistingClass import %s\n" +
    	"def run():\n" +
    	"    print %s\n" +
    	"\n" +
    	"";
    	int line = 2;
    	int col = 11;
    	checkDefault(str, line, col, "existingMethod", false, true);
    }
    
    public void testRenameMethodImportLocally2() throws Exception {
    	String str = "" +
    	"from testAssist.assist import ExistingClass\n" +
    	"def run():\n" +
    	"    print ExistingClass.%s\n" +
    	"\n" +
    	"";
    	int line = 2;
    	int col = 26;
    	checkDefault(str, line, col, "existingMethod", false, true);
    }
    
    public void testRenameClassImportLocally() throws Exception {
    	String str = "" +
    	"from testAssist.assist import %s\n" +
    	"def run():\n" +
    	"    print %s\n" +
    	"\n" +
    	"";
    	int line = 2;
    	int col = 11;
    	checkDefault(str, line, col, "ExistingClass", false, true);
    }
    
    public void testRenameInstance() throws Exception {
        String str=getDefaultDocStr();
        int line = 2;
        int col = 10;
        
        checkDefault(str, line, col);
    }


}
