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

package org.python.pydev.refactoring.coderefactoring.extractmethod.edit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.renamer.LocalVarRenameVisitor;
import org.python.pydev.refactoring.coderefactoring.extractmethod.request.ExtractMethodRequest;
import org.python.pydev.refactoring.core.edit.AbstractInsertEdit;

public class ExtractMethodEdit extends AbstractInsertEdit {

    private String methodName;

    private ModuleAdapter parsedSelection;

    private int offsetStrategy;

    private AbstractScopeNode<?> scopeAdapter;

    private List<String> parameters;

    private List<String> returnVariables;

    private Map<String, String> renamedVariables;

    private int selectionOffset;

    private int selectionLen;

    public ExtractMethodEdit(ExtractMethodRequest req) {
        super(req);
        this.methodName = req.methodName;
        this.scopeAdapter = req.scopeAdapter;
        this.parsedSelection = req.parsedSelection;
        this.offsetStrategy = req.offsetStrategy;

        this.parameters = req.parameters;
        this.returnVariables = req.returnVariables;
        this.renamedVariables = req.renamedVariables;
        this.selectionOffset = req.selection.getOffset();
        this.selectionLen = req.selection.getLength();
    }

    @Override
    protected SimpleNode getEditNode() {
        List<stmtType> body = initExtractedBody();
        List<exprType> argsList = initExtractedMethodArguments();
        addReturnValue(body);
        FunctionDef extractedMethod = initExtractedMethod(body, argsList);
        applyRenamedVariables(extractedMethod);

        return extractedMethod;
    }

    private FunctionDef initExtractedMethod(List<stmtType> body, List<exprType> argsList) {
        argumentsType args = new argumentsType(argsList.toArray(new exprType[0]), null, null, null, null, null, null,
                null, null, null);

        FunctionDef extractedMethod = new FunctionDef(new NameTok(methodName, NameTok.FunctionName), args,
                body.toArray(new stmtType[0]), null, null, false);
        return extractedMethod;
    }

    private List<exprType> initExtractedMethodArguments() {
        List<exprType> argsList = new ArrayList<exprType>();
        if (this.scopeAdapter instanceof FunctionDefAdapter) {
            IASTNodeAdapter<? extends SimpleNode> parentScopeAdapter = scopeAdapter.getParent();
            while (parentScopeAdapter instanceof FunctionDefAdapter) {
                parentScopeAdapter = parentScopeAdapter.getParent();
            }
            if (parentScopeAdapter instanceof IClassDefAdapter) {
                argsList.add(new Name("self", Name.Load, false));
            }
        }
        for (String variable : this.parameters) {
            argsList.add(new Name(variable, Name.Param, false));
        }
        return argsList;
    }

    private List<stmtType> initExtractedBody() {
        stmtType[] extractBody = parsedSelection.getASTNode().body;
        List<stmtType> body = new ArrayList<stmtType>();
        for (stmtType stmt : extractBody) {
            body.add((stmtType) stmt.createCopy());
        }
        return body;
    }

    private void applyRenamedVariables(FunctionDef extractedMethod) {

        if (renamedVariables.size() > 0) {
            LocalVarRenameVisitor renameVisitor = new LocalVarRenameVisitor(this.adapterPrefs);
            renameVisitor.setRenameMap(renamedVariables);
            try {
                extractedMethod.accept(renameVisitor);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void addReturnValue(List<stmtType> body) {
        List<exprType> returnList = new ArrayList<exprType>();
        for (String variable : this.returnVariables) {
            returnList.add(new Name(variable, Name.Load, false));
        }

        exprType returnValue = null;
        if (returnList.size() == 1) {
            returnValue = returnList.get(0);

        } else if (returnList.size() > 1) {
            returnValue = new Tuple(returnList.toArray(new exprType[0]), Tuple.Load, false);

        } else if (body.size() == 1) {
            // return expression as-is (note: body must be cleared)
            if (body.get(0) instanceof Expr) {
                Expr expression = (Expr) body.get(0);
                returnValue = expression.value;
                body.clear();
            }
        }

        if (returnValue != null) {
            body.add(new Return(returnValue));
        }
    }

    @Override
    public int getOffsetStrategy() {
        return offsetStrategy;
    }

    @Override
    public int getOffset() {
        int superOffset = super.getOffset();
        if (superOffset > this.selectionOffset && superOffset < this.selectionOffset + this.selectionLen) {
            try {
                return this.moduleAdapter.getStartLineBefore(this.selectionOffset);
            } catch (Exception e) {
                return this.selectionOffset;
            }
        }
        return superOffset;
    }
}
