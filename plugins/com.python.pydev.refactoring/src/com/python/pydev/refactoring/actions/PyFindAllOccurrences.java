/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.python.pydev.ast.refactoring.AbstractPyRefactoring;
import org.python.pydev.ast.refactoring.IPyRefactoring2;
import org.python.pydev.ast.refactoring.RefactoringRequest;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;

import com.python.pydev.refactoring.search.FindOccurrencesSearchQuery;

public class PyFindAllOccurrences extends PyRefactorAction {

    @Override
    protected String perform(IAction action, IProgressMonitor monitor) throws Exception {
        IPyRefactoring2 r = (IPyRefactoring2) AbstractPyRefactoring.getPyRefactoring();
        RefactoringRequest req = getRefactoringRequest(new NullProgressMonitor()); //as we're doing it in the background
        req.fillInitialNameAndOffset();
        if (req.initialName != null && req.initialName.trim().length() > 0) {
            NewSearchUI.runQueryInBackground(newQuery(r, req));
        }
        return null;
    }

    private ISearchQuery newQuery(final IPyRefactoring2 r, final RefactoringRequest req) {
        return new FindOccurrencesSearchQuery(r, req);
    }

}
