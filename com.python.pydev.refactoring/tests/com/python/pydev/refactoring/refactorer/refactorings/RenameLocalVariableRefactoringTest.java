/*
 * Created on Mar 1, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.PyParserTestBase;

public class RenameLocalVariableRefactoringTest extends PyParserTestBase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(RenameLocalVariableRefactoringTest.class);
    }


    private Document doc;

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
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
        RefactoringRequest request = new RefactoringRequest();
        request.doc = doc;
        request.name = "bbb";
        int line = 1;
        int col = 4;
        request.ps = new PySelection(doc, line, col);
        RenameLocalVariableRefactoring refactoring = new RenameLocalVariableRefactoring(request);
        RefactoryChange refactoringChange = refactoring.getRefactoringChange();
        
    }
}
