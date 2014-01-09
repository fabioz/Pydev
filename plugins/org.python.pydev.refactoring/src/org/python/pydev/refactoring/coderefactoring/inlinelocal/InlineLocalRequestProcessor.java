/******************************************************************************
* Copyright (C) 2006-2009  IFS Institute for Software and others
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
 */

package org.python.pydev.refactoring.coderefactoring.inlinelocal;

import java.util.List;

import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.refactoring.coderefactoring.inlinelocal.request.InlineLocalRequest;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.refactoring.utils.ListUtils;

public class InlineLocalRequestProcessor implements IRequestProcessor<InlineLocalRequest> {
    private RefactoringInfo info;
    private Assign assignment;
    private List<Name> variables;

    public InlineLocalRequestProcessor(RefactoringInfo info) {
        this.info = info;
    }

    public List<InlineLocalRequest> getRefactoringRequests() {
        return ListUtils.wrap(new InlineLocalRequest(info, assignment, variables));
    }

    public void setAssign(Assign assignment) {
        this.assignment = assignment;
    }

    public void setVariables(List<Name> relatedVariables) {
        this.variables = relatedVariables;

    }

    public String getVariableName() {
        return ((Name) assignment.targets[0]).id;
    }

    public int getOccurences() {
        return variables.size() - 1;
    }

}
