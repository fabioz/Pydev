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
            test.testRenameModuleInWorkspace2();
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
    
    public void testRenameModuleInWorkspace() throws Exception {
        //importer.py and importer2.py are the same:
        //
        //import mod1
        //from mod1 import submod1
        
        Map<String, List<ASTEntry>> references = getReferencesForRenameSimple("reflib.renamemodule.importer", 0, 8); 
        assertEquals(3, references.size());
        
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); 
        assertEquals(2, references.get(CURRENT_MODULE_IN_REFERENCES).size());
        
    	assertTrue(references.containsKey("reflib.renamemodule.mod1.__init__")); //module renamed 
        assertEquals(1, references.get("reflib.renamemodule.mod1.__init__").size());
        
        assertTrue(references.containsKey("reflib.renamemodule.importer2")); 
        assertEquals(2, references.get("reflib.renamemodule.importer2").size());
    }
    
    public void testRenameModuleInWorkspace2() throws Exception {
        //importer.py and importer2.py are the same:
        //
        //import mod1
        //from mod1 import submod1
        
        Map<String, List<ASTEntry>> references = getReferencesForRenameSimple("reflib.renamemodule.importer", 1, 18); 
        assertEquals(5, references.size());
        
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); 
        assertEquals(1, references.get(CURRENT_MODULE_IN_REFERENCES).size());
        
        assertTrue(references.containsKey("reflib.renamemodule.mod1.submod1")); //module renamed 
        assertEquals(1, references.get("reflib.renamemodule.mod1.submod1").size());
        
        assertTrue(references.containsKey("reflib.renamemodule.importer2")); 
        assertEquals(1, references.get("reflib.renamemodule.importer2").size());
        
        assertTrue(references.containsKey("reflib.renamemodule.importer3")); 
        assertEquals(1, references.get("reflib.renamemodule.importer3").size());
        
        assertTrue(references.containsKey("reflib.renamemodule.importer4")); 
        assertEquals(1, references.get("reflib.renamemodule.importer4").size());
	}
    

}
