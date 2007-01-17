package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.util.List;
import java.util.Map;

import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.refactoring.wizards.rename.PyRenameParameterProcess;

public class RenameParamRefactoringTest extends RefactoringRenameTestBase  {
	

    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = false;
//            RenameParamRefactoringTest test = new RenameParamRefactoringTest();
//            test.setUp();
//            test.testRename1();
//            test.tearDown();

            junit.textui.TestRunner.run(RenameParamRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class getProcessUnderTest() {
        return PyRenameParameterProcess.class;
    }
    
    public void testRenameParameter() throws Exception {
    	//Line 0 = "def Method1(param1, param2=None):"
    	//rename param1
        Map<String, List<ASTEntry>> references = getReferencesForRenameSimple("reflib.renameparameter.methoddef", 0, 14); 
    	assertTrue(references.containsKey("reflib.renameparameter.methodaccess")); 
    	assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); 
		
	}

}
