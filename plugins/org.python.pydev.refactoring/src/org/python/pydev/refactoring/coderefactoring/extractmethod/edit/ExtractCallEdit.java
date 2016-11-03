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

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.coderefactoring.extractmethod.request.ExtractMethodRequest;
import org.python.pydev.refactoring.core.edit.AbstractReplaceEdit;

public class ExtractCallEdit extends AbstractReplaceEdit {

    private String methodName;
    private int offset;
    private IASTNodeAdapter<?> offsetAdapter;
    private int replaceLength;
    private List<String> callParameters;
    private List<String> returnVariables;

    public ExtractCallEdit(ExtractMethodRequest req) {
        super(req);
        this.methodName = req.methodName;
        this.offset = req.selection.getOffset();

        this.replaceLength = req.selection.getLength();
        this.offsetAdapter = req.getOffsetNode();

        this.callParameters = req.parameters;
        this.returnVariables = req.returnVariables;
    }

    @Override
    protected SimpleNode getEditNode() {

        List<exprType> argsList = initCallArguments();
        Call methodCall = new Call(createCallAttribute(), argsList.toArray(new exprType[0]), null, null, null);

        return initSubstituteCall(methodCall);

    }

    private SimpleNode initSubstituteCall(Call methodCall) {
        if (returnVariables.size() == 0) {
            return methodCall;
        } else {
            List<exprType> returnExpr = new ArrayList<exprType>();
            for (String returnVar : returnVariables) {
                returnExpr.add(new Name(returnVar, Name.Store, false));
            }

            exprType[] expr = returnExpr.toArray(new exprType[0]);
            if (expr.length > 1) {
                expr = new exprType[] { new Tuple(expr, Tuple.Load, false) };
            }

            return new Assign(expr, methodCall, null);
        }
    }

    private List<exprType> initCallArguments() {
        List<exprType> argsList = new ArrayList<exprType>();
        for (String parameter : callParameters) {
            argsList.add(new Name(parameter, Name.Load, false));
        }
        return argsList;
    }

    private exprType createCallAttribute() {
        if (this.offsetAdapter instanceof IClassDefAdapter) {
            return new Attribute(new Name("self", Name.Load, false), new NameTok(this.methodName, NameTok.Attrib),
                    Attribute.Load);
        } else {
            return new Name(this.methodName, Name.Load, false);
        }
    }

    @Override
    public int getOffsetStrategy() {
        return 0;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    protected int getReplaceLength() {
        return replaceLength;
    }

}
