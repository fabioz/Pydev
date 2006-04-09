/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;

import com.python.pydev.refactoring.refactorer.Refactorer;
import com.python.pydev.refactoring.wizards.PyRenameProcessor;

public class RefactoringTestBase extends CodeCompletionTestsBase {
    
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

}
