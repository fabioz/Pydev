package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.util.List;
import java.util.Map;

import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.refactoring.wizards.rename.PyRenameImportProcess;

public class RenameModuleRefactoringTest extends RefactoringRenameTestBase  {
	

    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = false;
            RenameModuleRefactoringTest test = new RenameModuleRefactoringTest();
            test.setUp();
            test.testRenameModule();
            test.tearDown();

            junit.textui.TestRunner.run(RenameModuleRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class getProcessUnderTest() {
        return PyRenameImportProcess.class;
    }
    
    public void testRenameModule() throws Exception {
        //importer.py:
        //import mod1
        //from mod1 import submod1
        Map<String, List<ASTEntry>> references = getReferencesForRenameSimple("reflib.renamemodule.importer", 0, 8); 
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); 
        assertEquals(2, references.get(CURRENT_MODULE_IN_REFERENCES).size());
    	assertTrue(references.containsKey("reflib.renamemodule.mod1.__init__")); //module renamed 
    	assertEquals(2, references.size());
        assertEquals(1, references.get("reflib.renamemodule.mod1.__init__").size());
	}

}
