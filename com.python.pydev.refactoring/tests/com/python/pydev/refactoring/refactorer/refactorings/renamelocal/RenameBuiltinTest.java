package com.python.pydev.refactoring.refactorer.refactorings.renamelocal;


public class RenameBuiltinTest extends RefactoringLocalTestBase {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(RenameBuiltinTest.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
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
