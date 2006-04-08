/*
 * Created on Mar 1, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;

import com.python.pydev.refactoring.refactorer.Refactorer;
import com.python.pydev.refactoring.wizards.PyRenameLocalVariableProcessor;

public class RenameLocalVariableRefactoringTest extends CodeCompletionTestsBase {

    public static void main(String[] args) {
        try {
            RenameLocalVariableRefactoringTest test = new RenameLocalVariableRefactoringTest();
            test.setUp();
            test.testRenameInstance2();
            test.tearDown();

            junit.textui.TestRunner.run(RenameLocalVariableRefactoringTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    private Document doc;

    protected void setUp() throws Exception {
        super.setUp();
        createDefaultDoc();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(false);
        AbstractPyRefactoring.setPyRefactoring(new Refactorer());

    }

    private void createDefaultDoc() {
        String str="" +
        "def method():\n"+
        "    aaa = 2\n"+
        "    print aaa\n"+
        "";
        
        doc = new Document(str);
    }

    protected void tearDown() throws Exception {
    	CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
    }

    
    public void testRenameErr() throws Exception {
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

    private void applyRefactoring(RefactoringRequest request) throws CoreException {
        applyRefactoring(request, false);
    }

    /**
     * @param request
     * @throws CoreException
     */
    private void applyRefactoring(RefactoringRequest request, boolean expectError) throws CoreException {
        PyRenameLocalVariableProcessor processor = new PyRenameLocalVariableProcessor(request);
        checkStatus(processor.checkInitialConditions(new NullProgressMonitor()), expectError);
        checkStatus(processor.checkFinalConditions(new NullProgressMonitor(), null), expectError);
        Change change = processor.createChange(new NullProgressMonitor());
        change.perform(new NullProgressMonitor());
    }

    private void checkStatus(RefactoringStatus status, boolean expectError) {
        if(!expectError){
            assertNull(status.getEntryMatchingSeverity(RefactoringStatus.ERROR));
            assertNull(status.getEntryMatchingSeverity(RefactoringStatus.FATAL));
        }else{
            assertNotNull(status.getEntryMatchingSeverity(RefactoringStatus.ERROR));
            assertNotNull(status.getEntryMatchingSeverity(RefactoringStatus.FATAL));
        }
    }
}
