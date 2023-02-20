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

package org.python.pydev.refactoring.codegenerator.overridemethods.edit;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.factory.PyAstFactory;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.codegenerator.overridemethods.request.OverrideMethodsRequest;
import org.python.pydev.refactoring.core.edit.AbstractInsertEdit;

public class MethodEdit extends AbstractInsertEdit {

    private int offsetStrategy;

    private FunctionDefAdapter method;

    private String baseClassName;

    public MethodEdit(OverrideMethodsRequest req) {
        super(req);
        this.method = req.method;
        this.baseClassName = req.getBaseClassName();
        this.offsetStrategy = req.offsetStrategy;
    }

    @Override
    protected SimpleNode getEditNode() {
        FunctionDef origin = method.getASTNode();
        stmtType[] body = initBody(origin);

        return PyAstFactory.createFunctionDefFull(origin.name, origin.args, body, null, null, false);
    }

    private stmtType[] initBody(FunctionDef origin) {
        stmtType[] body = new stmtType[1];
        body[0] = new Return(createBaseClassCall(origin));
        return body;
    }

    private Call createBaseClassCall(FunctionDef origin) {

        exprType[] args = null;
        exprType starargs = null;
        exprType kwargs = null;

        if (origin.args != null) {
            args = extractArgs(origin.args);
            starargs = extractStarargs(origin.args);
            kwargs = extractKwargs(origin.args);
        }

        Call funCall = new Call(createAttribute(), args, null, starargs, kwargs);

        return funCall;
    }

    private exprType extractKwargs(argumentsType argType) {
        NameTok kwarg = (NameTok) argType.kwarg;
        if (kwarg != null) {
            return new Name(kwarg.id, Name.Load, false);
        } else {
            return null;
        }
    }

    private exprType extractStarargs(argumentsType argType) {
        NameTok vararg = (NameTok) argType.vararg;
        if (vararg != null) {
            return new Name(vararg.id, Name.Load, false);
        } else {
            return null;
        }
    }

    private exprType[] extractArgs(argumentsType argType) {
        exprType[] ret = new exprType[argType.args.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = (exprType) argType.args[i].createCopy();
        }
        return ret;
    }

    private Attribute createAttribute() {
        return new Attribute(new Name(baseClassName, Name.Load, false), new NameTok(method.getName(), NameTok.Attrib),
                Attribute.Load);
    }

    @Override
    public int getOffsetStrategy() {
        return offsetStrategy;
    }

}
