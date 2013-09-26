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

package org.python.pydev.refactoring.codegenerator.generateproperties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.core.base.AbstractPythonRefactoring;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.change.IChangeProcessor;
import org.python.pydev.refactoring.messages.Messages;

public class GeneratePropertiesRefactoring extends AbstractPythonRefactoring {

    private GeneratePropertiesRequestProcessor requestProcessor;

    private IChangeProcessor changeProcessor;

    public GeneratePropertiesRefactoring(RefactoringInfo req) {
        super(req);
        try {
            initWizard();
        } catch (Throwable e) {
            status.addInfo(Messages.infoFixCode);
        }
    }

    private void initWizard() throws Throwable {
        this.requestProcessor = new GeneratePropertiesRequestProcessor(this.info.getAdapterPrefs());
        this.changeProcessor = new GeneratePropertiesChangeProcessor(getName(), this.info, this.requestProcessor);
    }

    @Override
    protected List<IChangeProcessor> getChangeProcessors() {
        List<IChangeProcessor> processors = new ArrayList<IChangeProcessor>();
        processors.add(changeProcessor);
        return processors;
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
        List<IClassDefAdapter> classes = this.info.getClasses();

        for (IClassDefAdapter adapter : classes) {
            if (!adapter.getAttributes().isEmpty()) {
                return super.checkInitialConditions(pm);
            }
        }
        status.addFatalError(Messages.generatePropertiesUnavailable);

        return status;
    }

    @Override
    public String getName() {
        return Messages.generatePropertiesLabel;
    }

    public GeneratePropertiesRequestProcessor getRequestProcessor() {
        return requestProcessor;
    }
}
