/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
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
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.coderefactoring.extractmethod;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.core.log.Log;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.ast.visitors.selection.SelectionException;
import org.python.pydev.refactoring.core.base.AbstractPythonRefactoring;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.base.RefactoringInfo.SelectionComputer;
import org.python.pydev.refactoring.core.base.RefactoringInfo.SelectionComputer.SelectionComputerKind;
import org.python.pydev.refactoring.core.change.IChangeProcessor;
import org.python.pydev.refactoring.messages.Messages;

public class ExtractMethodRefactoring extends AbstractPythonRefactoring {
    private ExtractMethodRequestProcessor requestProcessor;
    private IChangeProcessor changeProcessor;
    private final ModuleAdapter module;

    public ExtractMethodRefactoring(RefactoringInfo req) {
        super(req);
        this.module = req.getModuleAdapter();
    }

    @Override
    protected List<IChangeProcessor> getChangeProcessors() {
        List<IChangeProcessor> processors = new ArrayList<IChangeProcessor>();
        this.changeProcessor = new ExtractMethodChangeProcessor(getName(), this.info, this.requestProcessor);
        processors.add(changeProcessor);
        return processors;
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
        SelectionComputer selectionComputer = this.info.getSelectionComputer(SelectionComputerKind.extractMethod);

        if (selectionComputer.selection == null) {
            status.addFatalError(Messages.extractMethodIncompleteSelection);
            return status;
        }

        try {
            VisitorFactory.validateSelection(selectionComputer.selectionModuleAdapter);
        } catch (SelectionException e) {
            status.addFatalError(e.getMessage());
            return status;
        }

        try {
            this.requestProcessor = new ExtractMethodRequestProcessor(
                    info.getScopeAdapter(),
                    selectionComputer.selectionModuleAdapter,
                    this.module,
                    selectionComputer.selection);
        } catch (Throwable e) {
            Log.log(e);
            status.addFatalError("Unexpected exception: " + e.getMessage());
        }

        if (this.requestProcessor.getScopeAdapter() == null
                || this.requestProcessor.getScopeAdapter() instanceof IClassDefAdapter) {
            status.addFatalError(Messages.extractMethodScopeInvalid);
            return status;
        }

        return status;
    }

    @Override
    public String getName() {
        return Messages.extractMethodLabel;
    }

    public ExtractMethodRequestProcessor getRequestProcessor() {
        return requestProcessor;
    }
}
