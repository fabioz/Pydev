/*
 * Created on Dec 10, 2006
 * @author Fabio
 */
package com.python.pydev.refactoring.refactorer.refactorings.rename;

import java.util.List;
import java.util.Map;

import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.refactoring.wizards.rename.PyRenameFunctionProcess;


/**
 * Class that should test the renaming of classes within a number of modules in
 * the workspace.
 * 
 * @author Fabio
 */
public class RenameFunctionRefactoringTest extends RefactoringRenameTestBase {


    public static void main(String[] args) {
        try {
            DEBUG_REFERENCES = false;
//            RenameFunctionRefactoringTest test = new RenameFunctionRefactoringTest();
//            test.setUp();
//            test.testRename1();
//            test.tearDown();

            junit.textui.TestRunner.run(RenameFunctionRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class getProcessUnderTest() {
        return PyRenameFunctionProcess.class;
    }

    
    public void testRename1() throws Exception {
        Map<String, List<ASTEntry>> references = getReferencesForRenameSimple("reflib.renamefunction.renfoo", 0, 8);
        assertTrue(references.containsKey("reflib.renamefunction.renfoo") == false); //the current module does not have a separated key here
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); //the current module must also be there
        
        assertTrue(references.containsKey("reflib.renamefunction.__init__") == false);
        
        //the modules with a duplicate definition here should not be in the results.
        assertTrue(references.containsKey("reflib.renamefunction.accessdup") == false);
        assertTrue(references.containsKey("reflib.renamefunction.duprenfoo") == false);
        checkProcessors();
    }

    
    public void testRename2() throws Exception {
        Map<String, List<ASTEntry>> references = getReferencesForRenameSimple("reflib.renamefunction.accessfoo", 0, 22);
        assertTrue(references.containsKey("reflib.renamefunction.accessfoo") == false); //the current module does not have a separated key here
        assertTrue(references.containsKey(CURRENT_MODULE_IN_REFERENCES)); //the current module must also be there
        assertTrue(references.containsKey("reflib.renamefunction.renfoo")); //the module where it is actually defined
        checkProcessors();
    }
    

}
