/*
 * Created on Mar 1, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.refactoring.RefactoringRequest;

public class RenameLocalVariableRefactoringTest extends RefactoringTestBase {

    public static void main(String[] args) {
        try {
            RenameLocalVariableRefactoringTest test = new RenameLocalVariableRefactoringTest();
            test.setUp();
            test.testRenameErr();
            test.tearDown();

            junit.textui.TestRunner.run(RenameLocalVariableRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    private Document doc;

    private void createDefaultDoc() {
        String str=getDefaultDocStr();
        
        doc = new Document(str);
    }

    /**
     * @return
     */
    private String getDefaultDocStr() {
        return "" +
        "def method():\n"+
        "    aaa = 2\n"+
        "    print aaa\n"+
        "";
    }


    
    public void testRenameErr() throws Exception {
        createDefaultDoc();
        int line = 2;
        int col = 10;
        PySelection ps = new PySelection(doc, line, col);
        
        RefactoringRequest request = new RefactoringRequest(null, ps, nature);
        request.moduleName = "foo";
        request.duringProcessInfo.initialName = "aaa bb"; //the initial name is not valid
        request.duringProcessInfo.initialOffset = ps.getAbsoluteCursorOffset();
        request.duringProcessInfo.name = "bbb";
        
        applyRefactoring(request, true);
        
    }
    public void testRenameInstance2() throws Exception {
        String str = "" +
                "class Foo:\n" +
                "    def m1(self,%s):\n" +//we want to target only the bb in this method and not in the next
                "        print %s\n" +
                "    def m2(self,bb):\n" +
                "        return bb\n" +
                "\n";
        doc = new Document(StringUtils.format(str, new Object[]{"bb", "bb"}));
        int line = 2;
        int col = 16;
        PySelection ps = new PySelection(doc, line, col);
        
        RefactoringRequest request = new RefactoringRequest(null, ps, nature);
        request.moduleName = "foo";
        request.duringProcessInfo.initialName = "bb";
        request.duringProcessInfo.initialOffset = ps.getAbsoluteCursorOffset();
        request.duringProcessInfo.name = "aaa";
        
        applyRefactoring(request);
        
        String refactored = doc.get();
        //System.out.println(refactored);
        assertEquals(StringUtils.format(str, new Object[]{"aaa", "aaa"}),  refactored);
        
    }
    public void testRenameInstance() throws Exception {
        createDefaultDoc();
        //the rename local refactoring, as its name says, can only be applied to locals, and not to globals.
        //the targets are parameters and local instances
        int line = 2;
        int col = 10;
        PySelection ps = new PySelection(doc, line, col);

        RefactoringRequest request = new RefactoringRequest(null, ps, nature);
        request.moduleName = "foo";
        request.duringProcessInfo.initialName = "aaa";
        request.duringProcessInfo.initialOffset = ps.getAbsoluteCursorOffset();
        request.duringProcessInfo.name = "bbb";
        
        applyRefactoring(request);
        
        String refactored = doc.get();
        assertEquals(  
                "def method():\n"+
                "    bbb = 2\n"+
                "    print bbb\n"+
                "", 
                refactored);
        
    }


}
