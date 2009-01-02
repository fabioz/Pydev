package com.python.pydev.refactoring.refactorer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.editor.refactoring.TooManyMatchesException;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.refactoring.IPyRefactoring2;
import com.python.pydev.refactoring.wizards.rename.PyRenameEntryPoint;
import com.python.pydev.refactoring.wizards.rename.PyRenameRefactoringWizard;
import com.python.pydev.ui.hierarchy.HierarchyNodeModel;

/**
 * This is the entry point for any refactoring that we implement.
 * 
 * @author Fabio
 */
public class Refactorer extends AbstractPyRefactoring implements IPyRefactoring2{
    
    public String getName() {
        return "Pydev Extensions Refactorer";
    }
    
    //-------------------------------------------- things we currently cannot do
    public boolean canExtract() {
        return false;
    }
    public boolean canRename() {
        return true;
    }
    public boolean canInlineLocalVariable() {
        return false;
    }
    public boolean canExtractLocalVariable() {
        return false;
    }
    
    //--------------------------------------------------------- things we can do
    public boolean canFindDefinition() {
        return true;
    }
    //----------------------------------------------------- end things we can do

    
    public String extract(RefactoringRequest request) {
        return null;
    }

    
    /**
     * Renames something... 
     * 
     * Basically passes things to the rename processor (it will choose the kind of rename that will happen). 
     * 
     * @see org.python.pydev.editor.refactoring.IPyRefactoring#rename(org.python.pydev.editor.refactoring.RefactoringRequest)
     */
    public String rename(RefactoringRequest request) {
        try {
            RenameRefactoring renameRefactoring = new RenameRefactoring(new PyRenameEntryPoint(request));
            request.fillInitialNameAndOffset();
            final PyRenameRefactoringWizard wizard = new PyRenameRefactoringWizard(renameRefactoring, "Rename", "inputPageDescription", request, request.initialName);
            try {
                RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
                op.run(PyAction.getShell(), "Rename Refactor Action");
            } catch (InterruptedException e) {
                // do nothing. User action got cancelled
            }
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
        return null;
    }
    
    public ItemPointer[] findDefinition(RefactoringRequest request) throws TooManyMatchesException {
        return new RefactorerFindDefinition().findDefinition(request);
    }
    
    public String inlineLocalVariable(RefactoringRequest request) {
        return null;
    }
    
    public String extractLocalVariable(RefactoringRequest request) {
        return null;
    }

    public void restartShell(RefactoringRequest request) {
        //no shell
    }

    public void killShell(RefactoringRequest request) {
        //no shell
    }

    public void checkAvailableForRefactoring(RefactoringRequest request) {
        //can always do it (does not depend upon the project)
    }
    
    /**
     * We always return false here because we use the refactoring cycle provided
     * by the actions in the refactoring toolkit from Eclipse.
     * 
     * @see org.python.pydev.editor.refactoring.IPyRefactoring#useDefaultRefactoringActionCycle()
     */
    public boolean useDefaultRefactoringActionCycle() {
        return false;
    }
    
    /**
     * 
     * do nothing (should throw exception if not available) -- in this refactorer,
     * we're able to refactor any python natures (such as jython).
     * @see org.python.pydev.editor.refactoring.IPyRefactoring#canRefactorNature(org.python.pydev.core.IPythonNature)
     */
    public void canRefactorNature(IPythonNature pythonNature) throws RuntimeException {
        //empty
    }
    
    
    
    // --------------------------------------------------------- IPyRefactoring2
    public boolean areAllInSameClassHierarchy(List<AssignDefinition> defs) {
        return new RefactorerFinds(this).areAllInSameClassHierarchy(defs);
    }
    
    public HierarchyNodeModel findClassHierarchy(RefactoringRequest request) {
        return new RefactorerFinds(this).findClassHierarchy(request);
    }

    public Map<Tuple<String, IFile>, HashSet<ASTEntry>> findAllOccurrences(RefactoringRequest req) throws OperationCanceledException, CoreException{
        PyRenameEntryPoint processor = new PyRenameEntryPoint(req);
        //to see if a new request was not created in the meantime (in which case this one will be cancelled)
        req.checkCancelled();
        
        RefactoringStatus status = processor.checkInitialConditions(req.getMonitor());
        if(status.getSeverity() == RefactoringStatus.FATAL){
            return null;
        }
        req.checkCancelled();
        
        status = processor.checkFinalConditions(req.getMonitor(), null, false);
        if(status.getSeverity() == RefactoringStatus.FATAL){
            return null;
        }
        req.checkCancelled();
        
        Map<Tuple<String, IFile>, HashSet<ASTEntry>> occurrencesInOtherFiles = processor.getOccurrencesInOtherFiles();
        
        HashSet<ASTEntry> occurrences = processor.getOccurrences();
        occurrencesInOtherFiles.put(new Tuple<String, IFile>(req.moduleName, req.pyEdit.getIFile()), occurrences);
        return occurrencesInOtherFiles;
    }
    


}
