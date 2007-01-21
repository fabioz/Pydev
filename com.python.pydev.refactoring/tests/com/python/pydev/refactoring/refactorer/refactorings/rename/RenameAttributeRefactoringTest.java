package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.util.List;
import java.util.Map;

import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.refactoring.wizards.rename.PyRenameAttributeProcess;

public class RenameAttributeRefactoringTest extends RefactoringRenameTestBase  {


    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = false;
            RenameAttributeRefactoringTest test = new RenameAttributeRefactoringTest();
            test.setUp();
            test.testRenameAttribute();
            test.tearDown();

            junit.textui.TestRunner.run(RenameAttributeRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class getProcessUnderTest() {
        return PyRenameAttributeProcess.class;
    }
    
    public void testRenameAttribute() throws Exception {
        //Line 1 = "    a.attrInstance = 10"
        //rename attrInstance
        Map<String, List<ASTEntry>> references = getReferencesForRenameSimple("reflib.renameattribute.attr2", 1, 8); 
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); 
        assertTrue(references.containsKey("reflib.renameattribute.attr1")); 
        assertEquals(1, references.get(CURRENT_MODULE_IN_REFERENCES).size());
        assertEquals(1, references.get("reflib.renameattribute.attr1").size());
    }
}
