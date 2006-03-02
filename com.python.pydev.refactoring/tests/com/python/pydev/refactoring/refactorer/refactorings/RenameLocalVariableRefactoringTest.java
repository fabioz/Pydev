/*
 * Created on Mar 1, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.refactoring.RefactoringRequest;

public class RenameLocalVariableRefactoringTest extends CodeCompletionTestsBase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(RenameLocalVariableRefactoringTest.class);
    }


    private Document doc;

    protected void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(false);

    }

    protected void tearDown() throws Exception {
    	CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
    }

    
    public void testRenameInstance() throws Exception {
        //the rename local refactoring, as its name says, can only be applied to locals, and not to globals.
        //the targets are parameters and local instances
        
        String str="" +
        "def method():\n"+
        "    aaa = 2\n"+
        "    print aaa\n"+
        "";
        
        doc = new Document(str);
        String name = "bbb";
        int line = 1;
        int col = 4;
        PySelection ps = new PySelection(doc, line, col);

        RefactoringRequest request = new RefactoringRequest(null, ps, nature);
        request.name = name;
        RenameLocalVariableRefactoring refactoring = new RenameLocalVariableRefactoring(request);
        RefactoryChange refactoringChange = refactoring.getRefactoringChange();
        
    }
}
