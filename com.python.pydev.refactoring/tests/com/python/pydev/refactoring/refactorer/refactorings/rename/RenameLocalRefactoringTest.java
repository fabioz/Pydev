package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.util.List;
import java.util.Map;

import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.refactoring.wizards.rename.PyRenameLocalProcess;

public class RenameLocalRefactoringTest extends RefactoringRenameTestBase  {


    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = false;
            RenameLocalRefactoringTest test = new RenameLocalRefactoringTest();
            test.setUp();
            test.testRenameLocal();
            test.tearDown();

            junit.textui.TestRunner.run(RenameLocalRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class getProcessUnderTest() {
        return PyRenameLocalProcess.class;
    }
    
    public void testRenameLocal() throws Exception {
        //Line 1 = "    aa = 10"
        Map<String, List<ASTEntry>> references = getReferencesForRenameSimple("reflib.renamelocal.local1", 1, 5); 
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); 
        assertEquals(1, references.size()); 
        assertEquals(2, references.get(CURRENT_MODULE_IN_REFERENCES).size());
    }
}