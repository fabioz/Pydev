package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.util.HashSet;
import java.util.Map;

import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.refactoring.wizards.rename.PyRenameSelfAttributeProcess;

public class RenameSelfRefactoringTest extends RefactoringRenameTestBase  {


    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = false;
            RenameSelfRefactoringTest test = new RenameSelfRefactoringTest();
            test.setUp();
            test.testRenameSelf();
            test.tearDown();

            junit.textui.TestRunner.run(RenameSelfRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class getProcessUnderTest() {
        return PyRenameSelfAttributeProcess.class;
    }
    
    public void testRenameSelf() throws Exception {
        //Line 0 = "def Method1(param1, param2=None):"
        //rename param1
        Map<String, HashSet<ASTEntry>> references = getReferencesForRenameSimple("reflib.renameself.renameselfclass", 2, 14); 
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); 
        assertTrue(references.containsKey("reflib.renameself.renameselfclass2")); 
        assertEquals(3, references.get(CURRENT_MODULE_IN_REFERENCES).size());
        assertEquals(4, references.get("reflib.renameself.renameselfclass2").size());
    }
}
