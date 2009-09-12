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
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.refactoring.coderefactoring.extractlocal.request.ExtractLocalRequest;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.refactoring.utils.ListUtils;

public class ExtractLocalRequestProcessor implements IRequestProcessor<ExtractLocalRequest> {

    private RefactoringInfo info;
    private ITextSelection selection;
    private exprType expression;
    private String variableName;

    public ExtractLocalRequestProcessor(RefactoringInfo info) {
        this.info = info;
    }

    public List<ExtractLocalRequest> getRefactoringRequests() {
        return ListUtils.wrap(new ExtractLocalRequest(info, selection, expression, variableName));
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

}
