package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.util.HashSet;
import java.util.Map;

import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.refactoring.wizards.rename.PyRenameLocalProcess;

public class RenameGlobalRefactoringTest extends RefactoringRenameTestBase {
    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = false;
//            RenameGlobalRefactoringTest test = new RenameGlobalRefactoringTest();
//            test.setUp();
//            test.testRename1();
//            test.tearDown();

            junit.textui.TestRunner.run(RenameGlobalRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    
    protected Class getProcessUnderTest() {
        return PyRenameLocalProcess.class;
    }

    
    public void testRename1() throws Exception {
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renameglobal.renglobal", 0, 8);
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); //the current module must also be there
        
    }

    
    

}
