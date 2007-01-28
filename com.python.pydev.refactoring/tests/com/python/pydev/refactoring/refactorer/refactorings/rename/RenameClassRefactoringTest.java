/*
 * Created on Dec 10, 2006
 * @author Fabio
 */
package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.util.List;
import java.util.Map;

import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.refactoring.wizards.rename.PyRenameClassProcess;


/**
 * Class that should test the renaming of classes within a number of modules in
 * the workspace.
 * 
 * @author Fabio
 */
public class RenameClassRefactoringTest extends RefactoringRenameTestBase {


    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = true;
            RenameClassRefactoringTest test = new RenameClassRefactoringTest();
            test.setUp();
            test.testRename1();
            test.tearDown();

            junit.textui.TestRunner.run(RenameClassRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    
    protected Class getProcessUnderTest() {
        return PyRenameClassProcess.class;
    }

    
    public void testRename1() throws Exception {
        Map<String, List<ASTEntry>> references = getReferencesForRenameSimple("reflib.renameclass.renfoo", 0, 8);
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); //the current module must also be there
        
        assertFalse(references.containsKey("reflib.renameclass.renfoo")); //the current module does not have a separated key here
        assertFalse(references.containsKey("reflib.renameclass.__init__"));
        
        //the modules with a duplicate definition here should not be in the results.
        assertFalse(references.containsKey("reflib.renameclass.accessdup"));
        assertFalse(references.containsKey("reflib.renameclass.duprenfoo"));
        
        assertEquals(4, references.get(CURRENT_MODULE_IN_REFERENCES).size());
        assertContains(1, 7, references.get(CURRENT_MODULE_IN_REFERENCES));
        assertContains(4, 7, references.get(CURRENT_MODULE_IN_REFERENCES));
        assertContains(6, 11, references.get(CURRENT_MODULE_IN_REFERENCES));
        assertContains(7, 10, references.get(CURRENT_MODULE_IN_REFERENCES));
        
        assertEquals(4, references.get("reflib.renameclass.accessfoo").size());
        assertContains(1, 20, references.get("reflib.renameclass.accessfoo"));
        assertContains(4, 7 , references.get("reflib.renameclass.accessfoo"));
        assertContains(5, 11, references.get("reflib.renameclass.accessfoo"));
        assertContains(6, 9, references.get("reflib.renameclass.accessfoo"));
        
        assertEquals(2, references.size());

    }

    
    public void testRename2() throws Exception {
        Map<String, List<ASTEntry>> references = getReferencesForRenameSimple("reflib.renameclass.accessfoo", 0, 22);
        assertTrue(references.containsKey("reflib.renameclass.accessfoo") == false); //the current module does not have a separated key here
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); //the current module must also be there
        assertTrue(references.containsKey("reflib.renameclass.renfoo")); //the module where it is actually defined
    }
    
    public void testRenameLocalClass() throws Exception {
        Map<String, List<ASTEntry>> references = getReferencesForRenameSimple("reflib.renamelocaltoken.__init__", 1, 12);
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); 
        assertEquals(1, references.size());
        List<ASTEntry> entries = references.get(CURRENT_MODULE_IN_REFERENCES);
        assertEquals(2, entries.size());
    }
    

}
