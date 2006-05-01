/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;

import com.python.pydev.refactoring.refactorer.Refactorer;
import com.python.pydev.refactoring.wizards.PyRenameProcessor;

public class RefactoringTestBase extends CodeCompletionTestsBase {
    
    private static final boolean DEBUG = false;


    protected void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(false);
        AbstractPyRefactoring.setPyRefactoring(new Refactorer());
        
    }
    
    protected void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
    }

    protected void applyRefactoring(RefactoringRequest request) throws CoreException {
        applyRefactoring(request, false);
    }

    /**
     * @param request
     * @throws CoreException
     */
    protected void applyRefactoring(RefactoringRequest request, boolean expectError) throws CoreException {
        PyRenameProcessor processor = new PyRenameProcessor(request);
        checkStatus(processor.checkInitialConditions(new NullProgressMonitor()), expectError);
        checkStatus(processor.checkFinalConditions(new NullProgressMonitor(), null), expectError);
        Change change = processor.createChange(new NullProgressMonitor());
        change.perform(new NullProgressMonitor());
    }

    protected void checkStatus(RefactoringStatus status, boolean expectError) {
        RefactoringStatusEntry err = status.getEntryMatchingSeverity(RefactoringStatus.ERROR);
        RefactoringStatusEntry fatal = status.getEntryMatchingSeverity(RefactoringStatus.FATAL);
        
        if(!expectError){
            assertNull(err != null? err.getMessage() : "", err);
            assertNull(fatal != null? fatal.getMessage() : "", fatal);
        }else{
            assertNotNull(err);
            assertNotNull(fatal);
        }
    }
    
    protected void checkDefault(String strDoc, int line, int col, String initialName, boolean expectError) throws CoreException {
        checkDefault(strDoc, line, col, initialName, expectError, false);
    }
    
    protected void checkDefault(String strDoc, int line, int col, String initialName, boolean expectError, boolean onlyOnLocalScope) throws CoreException {
        Document doc = new Document(StringUtils.format(strDoc, getSame(initialName)));
        PySelection ps = new PySelection(doc, line, col);
        
        RefactoringRequest request = new RefactoringRequest(null, ps, nature);
        request.findReferencesOnlyOnLocalScope = onlyOnLocalScope;
        request.moduleName = "foo";
        request.duringProcessInfo.initialName = initialName; 
        request.duringProcessInfo.initialOffset = ps.getAbsoluteCursorOffset();
        request.duringProcessInfo.name = "bb";
        
        applyRefactoring(request, expectError);
        String refactored = doc.get();
        if(DEBUG){
            System.out.println(refactored);
        }
        if(!expectError){
            assertEquals(StringUtils.format(strDoc, getSame("bb")),  refactored);
        }else{
            //cannot have changed
            assertEquals(StringUtils.format(strDoc, getSame(initialName)),  refactored);
        }
    }

    /**
     * Always checks for the 'default' test, which is:
     * - get the document and change all the ocurrences of %s to 'aa'
     * - apply the refactor process
     * - check if all the %s ocurrences are now 'bb'
     */
    protected void checkDefault(String strDoc, int line, int col) throws CoreException {
        checkDefault(strDoc, line, col, "aa", false);
    }


    protected Object[] getSame(String string) {
        ArrayList<Object> list = new ArrayList<Object>();
        for (int i = 0; i < 10; i++) {
            list.add(string);
        }
        return list.toArray();
    }



}
