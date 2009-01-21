/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.coderefactoring.extractlocal;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.coderefactoring.extractlocal.request.ExtractLocalRequest;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.core.request.IRequestProcessor;

public class ExtractLocalRequestProcessor implements IRequestProcessor<ExtractLocalRequest> {

    private String variableName;
    
    private RefactoringInfo info;

    private exprType expression;

    private AbstractScopeNode<?> scopeAdapter;

    public ExtractLocalRequestProcessor(RefactoringInfo info) {
        this.info = info;
        this.scopeAdapter = info.getScopeAdapter();
    }

    public List<ExtractLocalRequest> getRefactoringRequests() {
        List<ExtractLocalRequest> requests = new ArrayList<ExtractLocalRequest>();
        requests.add(new ExtractLocalRequest(info, expression, variableName));
        return requests;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void setExpression(exprType expression) {
        this.expression = expression;
    }

    public AbstractScopeNode<?> getScopeAdapter() {
        return scopeAdapter;
    }
}
