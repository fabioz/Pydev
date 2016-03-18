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
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
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

import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.refactoring.coderefactoring.extractlocal.request.ExtractLocalRequest;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.refactoring.utils.ListUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class ExtractLocalRequestProcessor implements IRequestProcessor<ExtractLocalRequest> {

    private RefactoringInfo info;
    private ITextSelection selection;
    private exprType expression;
    private String variableName;
    private List<Tuple<ITextSelection, SimpleNode>> duplicates;
    private boolean replaceDuplicates;

    public ExtractLocalRequestProcessor(RefactoringInfo info) {
        this.info = info;
    }

    @Override
    public List<ExtractLocalRequest> getRefactoringRequests() {
        return ListUtils.wrap(new ExtractLocalRequest(info, selection, expression, variableName, duplicates,
                replaceDuplicates));
    }

    public void setSelection(ITextSelection selection) {
        this.selection = selection;
    }

    public void setExpression(exprType expression) {
        this.expression = expression;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public int getDuplicatesSize() {
        if (duplicates == null) {
            return 0;
        }
        return duplicates.size();
    }

    /**
     * @param duplicates
     */
    public void setDuplicates(List<Tuple<ITextSelection, SimpleNode>> duplicates) {
        this.duplicates = duplicates;
    }

    /**
     * @param replace
     */
    public void setReplaceDuplicates(boolean replace) {
        this.replaceDuplicates = replace;
    }

}
