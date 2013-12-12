/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 15, 2004
 *
 * @author Fabio Zadrozny
 */
package com.python.pydev.refactoring.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.PyRefactoringRequest;

/**
 * @author Fabio Zadrozny
 */
public class PyRename extends PyRefactorAction {

    /**
     * we need:
     * 
     *     renameByCoordinates(filename, line, column, newname)
     */
    @Override
    protected String perform(IAction action, IProgressMonitor monitor) throws Exception {
        if (!canModifyEditor()) {
            return "";
        }

        String res = "";
        res = AbstractPyRefactoring.getPyRefactoring().rename(
                new PyRefactoringRequest(getRefactoringRequest(monitor)));
        return res;
    }

}
