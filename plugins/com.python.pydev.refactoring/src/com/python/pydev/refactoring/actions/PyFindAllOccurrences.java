package com.python.pydev.refactoring.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;

import com.python.pydev.refactoring.IPyRefactoring2;
import com.python.pydev.refactoring.search.FindOccurrencesSearchQuery;

public class PyFindAllOccurrences extends PyRefactorAction{
    
    public static final boolean DEBUG_FIND_REFERENCES = false;
    
    IPyRefactoring pyRefactoring;
    
    @Override
    protected IPyRefactoring getPyRefactoring() {
        if(pyRefactoring == null){
            pyRefactoring = AbstractPyRefactoring.getPyRefactoring(); //the one provided by the com part (no need to check for it).
        }
        return pyRefactoring;
    }

    @Override
    protected String getInputMessage() {
        return null;
    }

    @Override
    protected String perform(IAction action, String name, IProgressMonitor monitor) throws Exception {
        IPyRefactoring2 r = (IPyRefactoring2) getPyRefactoring();
        RefactoringRequest req = getRefactoringRequest(new NullProgressMonitor()); //as we're doing it in the backgroup
        req.fillInitialNameAndOffset();
        if(req.initialName != null && req.initialName.trim().length() > 0){
            NewSearchUI.runQueryInBackground(newQuery(r, req));
        }
        return null;
    }

    private ISearchQuery newQuery(final IPyRefactoring2 r, final RefactoringRequest req) {
        return new FindOccurrencesSearchQuery(r, req);
    }

}
