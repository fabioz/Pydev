/******************************************************************************
* Copyright (C) 2006-2013  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>    - initial implementation
*     Jonah Graham <jonah@kichwacoders.com> - ongoing maintenance
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.coderefactoring.extractlocal;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.core.base.AbstractPythonRefactoring;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.base.RefactoringInfo.SelectionComputer;
import org.python.pydev.refactoring.core.base.RefactoringInfo.SelectionComputer.SelectionComputerKind;
import org.python.pydev.refactoring.core.change.IChangeProcessor;
import org.python.pydev.refactoring.core.validator.NameValidator;
import org.python.pydev.refactoring.messages.Messages;
import org.python.pydev.refactoring.utils.ListUtils;
import org.python.pydev.shared_core.string.ICoreTextSelection;

public class ExtractLocalRefactoring extends AbstractPythonRefactoring {
    private ExtractLocalRequestProcessor requestProcessor;

    public ExtractLocalRefactoring(RefactoringInfo info) {
        super(info);

        this.requestProcessor = new ExtractLocalRequestProcessor(info);
    }

    @Override
    protected List<IChangeProcessor> getChangeProcessors() {
        IChangeProcessor changeProcessor = new ExtractLocalChangeProcessor(getName(), this.info, this.requestProcessor);
        return ListUtils.wrap(changeProcessor);
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
        SelectionComputer bestSelection = info.getSelectionComputer(SelectionComputerKind.extractLocal);
        exprType expression = bestSelection.getSingleExpression();
        ICoreTextSelection selection = bestSelection.selection;

        /* No valid selections found, report error */
        if (expression == null) {
            status.addFatalError(Messages.extractLocalNoExpressionSelected);
            return status;
        }

        AbstractScopeNode<?> scopeAdapter = info.getModuleAdapter().getScopeAdapter(selection);
        requestProcessor.setDuplicates(scopeAdapter.getDuplicates(selection, expression));

        requestProcessor.setSelection(selection);
        requestProcessor.setExpression(expression);

        return status;
    }

    @Override
    public String getName() {
        return Messages.extractLocalLabel;
    }

    public ExtractLocalRequestProcessor getRequestProcessor() {
        return requestProcessor;
    }

    /**
     * Checks if a given variable name is valid or not. Using invalid
     * identifiers (e.g. special chars) or already used variable names
     * aren't allowed and would result in a fatal status
     * 
     * @param variableName
     * @return status
     */
    public RefactoringStatus checkVarName(String variableName) {
        RefactoringStatus status = new RefactoringStatus();

        NameValidator nameValidator = new NameValidator(status, info.getScopeAdapter());
        nameValidator.validateVariableName(variableName);
        nameValidator.validateUniqueVariable(variableName);

        return status;
    }

}
