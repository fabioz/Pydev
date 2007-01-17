/*
 * Created on Mar 1, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings.renamelocal;

import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;


public class RenameLocalVariableRefactoringTest extends RefactoringLocalTestBase {

    public static void main(String[] args) {
        try {
            RenameLocalVariableRefactoringTest test = new RenameLocalVariableRefactoringTest();
            test.setUp();
            test.testRenameParamFromCall();
            test.tearDown();

            junit.textui.TestRunner.run(RenameLocalVariableRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = true;
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
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
        checkRename(getDefaultDocStr(), line, col, "bb", true, false, "aaa bb");
    }

    public void testRenameImportFromReference() throws Exception {
        int line = 1;
        int col = 11;
        String str = 
            "from testrec2.core import %s\n" +
            "class Foo(%s.Leaf):\n" + 
            "    def setUp(self):\n" +
            "        %s.Leaf.setUp(self)";

        checkRename(str, line, col, "noleaf", false, true);
    }

    public void testRenameImportFromReference2() throws Exception {
        //the difference is that in this one we find the leaf module and in the first we don't
        int line = 1;
        int col = 11;
        String str = 
            "from testrec2.core import %s\n" +
            "class Foo(%s.Leaf):\n" + 
            "    def setUp(self):\n" +
            "        %s.Leaf.setUp(self)";

        checkRename(str, line, col, "leaf", false, true);
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
        checkRename(str, line, col);
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
    	checkRename(str, line, col, "foo", false);
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
    	checkRename(str, line, col, "foo", false);
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
    	checkRename(str, line, col, "met1", false, true);
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
    	checkRename(str, line, col, "met1", false, true);
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
    	checkRename(str, line, col, "bla", false, true);
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
    	checkRename(str, line, col, "getopt", false, true);
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
    	checkRename(str, line, col, "sys", false, true);
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
    	checkRename(str, line, col, "CONSTANT1", false, true);
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
    	checkRename(str, line, col, "existingMethod", false, true);
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
    	checkRename(str, line, col, "existingMethod", false, true);
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
    	checkRename(str, line, col, "ExistingClass", false, true);
    }
    
    public void testRenameMethodLocally() throws Exception {
    	String str = "" +
    	"class Foo:\n" +
    	"    def %s(self):\n" +
    	"        tup = 10\n" +
    	"        tup[1].append(1)\n" +
    	"";
    	int line = 1;
    	int col = 9;
    	checkRename(str, line, col, "foo", false, true);
    }
    
    
    public void testRenameLocalAttr() throws Exception {
    	String str = "" +
    	"class Foo:\n" +
    	"    def foo(self):\n" +
    	"        %s = [[],[]]\n" +
    	"        %s[1].append(1)\n" +
    	"";
    	int line = 2;
    	int col = 9;
    	checkRename(str, line, col, "tup", false, true);
    }
    
    public void testRenameAttribute() throws Exception {
    	String str = "" +
    	"class Foo(object):\n" +
    	"    def %s(self):\n" +
    	"        pass\n" +
    	"    \n" +
    	"foo = Foo()\n" +
    	"foo.%s()\n" +
    	"";
    	int line = 1;
    	int col = 9;
    	checkRename(str, line, col, "met1", false, true);
    }
    
    public void testRenameUndefined() throws Exception {
    	String str = "" +
    	"from a import %s\n" +
    	"print %s\n" +
    	"";
    	int line = 0;
    	int col = 15;
    	checkRename(str, line, col, "foo", false, true);
    }

    public void testNotFoundAttr() throws Exception {
        //TODO: the problem here is that when the attribute is not found,
        //we need to change the definition too.
    	String str = "" +
    	"class Foo:\n" +
    	"    def %s(self, foo):\n" +
    	"        print foo.%s\n" +
    	"";
    	int line = 2;
    	int col = 19;
    	checkRename(str, line, col, "met1", false, true);
    }
    
    
    public void testMultiStr() throws Exception {
        String str = "" +
        "%s = str(1)+ 'foo2'\n" +
        "print %s\n" +
        "";
        int line = 0;
        int col = 1;
        checkRename(str, line, col, "ss", false, true);
    }
    
    
    public void testDict() throws Exception {
    	String str = "" +
    	"%s = {}\n" +
    	"print %s[1]\n" +
    	"";
    	int line = 0;
    	int col = 1;
    	checkRename(str, line, col, "ddd", false, true);
    }
    
    
    public void testRenameInstance() throws Exception {
        String str=getDefaultDocStr();
        int line = 2;
        int col = 10;
        
        checkRename(str, line, col);
    }

    
    public void testImport() throws Exception {
        String str = "" +
        "import os.%s\n" +
        "print os.%s\n" +
        "";
        int line = 1;
        int col = 10;
        checkRename(str, line, col, "path", false, true);
    }
    
    public void testLocalNotGotten() throws Exception {
    	String str = "" +
    	"def m1():\n" +
    	"    foo.%s = 10\n" + //accessing this should not affect the locals (not taking methods into account)
    	"    print foo.%s\n" + 
    	"    bla = 20\n" +
    	"    print bla\n" +
    	"";
    	
    	int line = 1;
    	int col = 9;
    	checkRename(str, line, col, "bla", false, true);
    }
    
    public void testLocalNotGotten2() throws Exception {
    	String str = "" +
    	"def m1():\n" +
    	"    print foo.%s\n" + //accessing this should not affect the locals (not taking methods into account) 
    	"    print bla\n" +
    	"";
    	
    	int line = 1;
    	int col = 15;
    	checkRename(str, line, col, "bla", false, true);
    }
    
    public void testLocalGotten() throws Exception {
    	String str = "" +
    	"def m1():\n" +
    	"    print foo.bla\n" +  
    	"    print %s\n" + //should only affect the locals
    	"";
    	
    	int line = 2;
    	int col = 11;
    	checkRename(str, line, col, "bla", false, true);
    }
    
    public void testRenameSelf() throws Exception {
    	String str = "" +
		"def checkProps(%s):\n" +
		"    getattr(%s).value\n" +
		"\n";
    	
    	int line = 0;
    	int col = 17;
    	checkRename(str, line, col, "fff", false, true);
    }
    
    public void testRenameClassVar() throws Exception {
    	String str = "" +
		"class Foo:\n" +
		"    %s = ''\n" +
		"    def toSimulator(self):\n" +
		"        print self.%s\n" +
		"";
    	
    	int line = 3;
    	int col = 21;
    	checkRename(str, line, col, "vlMolecularWeigth", false, true);
    }
    
    public void testRenameNonLocal() throws Exception {
        String str = "" +
        "import b    \n" +
        "print b.%s\n" +
        "class C2:\n" +
        "    def m1(self):\n" +
        "        barr = 10\n" + //should not rename the local
        "\n" +
        "";
        
        int line = 1;
        int col = 9;
        checkRename(str, line, col, "barr", false, true);
    }

    public void testRenameNonLocal2() throws Exception {
        String str = "" +
        "def m1():\n" +
        "    bar = 10\n" +
        "    bar.%s\n" + //selected (Foo)
        "    foop = 20\n" +
        "";
        
        int line = 2;
        int col = 9;
        checkRename(str, line, col, "foop", false, true);
    }
    
    public void testRename1() throws Exception {
    	String str = "" +
    	"import pprint\n" +
    	"def myHook():\n" +
    	"    pprint.%s()\n" +
    	"";
    	
    	int line = 2;
    	int col = 12;
    	checkRename(str, line, col, "PrettyPrinter", false, true);
    }
    
    public void testRename2() throws Exception {
    	String str = "" +
    	"import bla as %s\n" +
    	"raise %s.ffff(msg)\n" +
    	"\n" +
    	"";
    	
    	int line = 0;
    	int col = 15;
    	checkRename(str, line, col, "fooo", false, true);
    }
    
    public void testRename3() throws Exception {
    	String str = "" +
    	"def m1(a):\n" +
    	"    a.data.%s\n" +
    	"";
    	
    	int line = 1;
    	int col = 12;
    	checkRename(str, line, col, "fooo", false, true);
    }
    
    public void testRenameParamFromCall() throws Exception {
    	String str = "" +
    	"def m1():\n" +
    	"    def m2(%s):\n" +
    	"        m1(%s=%s-1)\n" +
    	"";
    	
    	int line = 2;
    	int col = 12;
    	checkRename(str, line, col, "fooo", false, true);
    }
    
    public void testRenameParamFromCall2() throws Exception {
    	String str = "" +
    	"def m1():\n" +
    	"    def m2(%s):\n" +
    	"        m1(%s=%s-1)\n" +
    	"";
    	
    	int line = 1;
    	int col = 12;
    	checkRename(str, line, col, "fooo", false, true);
    }
    

}
