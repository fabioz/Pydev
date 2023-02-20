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

package org.python.pydev.refactoring.codegenerator.constructorfield.edit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NameTokType;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.factory.NodeHelper;
import org.python.pydev.parser.jython.ast.factory.PyAstFactory;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.INodeAdapter;
import org.python.pydev.refactoring.codegenerator.constructorfield.request.ConstructorFieldRequest;
import org.python.pydev.refactoring.core.edit.AbstractInsertEdit;

public class ConstructorMethodEdit extends AbstractInsertEdit {

    private static final String KWARGS = "kwargs";
    private static final String ARGS = "args";
    private int offsetStrategy;
    private List<INodeAdapter> attributes;
    private IClassDefAdapter classAdapter;

    public ConstructorMethodEdit(ConstructorFieldRequest req) {
        super(req);
        this.classAdapter = req.classAdapter;
        this.attributes = req.attributeAdapters;
        this.offsetStrategy = req.offsetStrategy;
    }

    /**
     * Tricky :)
     * @throws MisconfigurationException
     */
    @Override
    protected SimpleNode getEditNode() throws MisconfigurationException {
        List<IClassDefAdapter> bases = classAdapter.getBaseClasses();

        //extractArguments
        NameTokType varArg = null;
        NameTokType kwArg = null;

        Set<String> argsNames = new LinkedHashSet<String>();

        for (IClassDefAdapter baseClass : bases) {
            FunctionDefAdapter init = baseClass.getFirstInit();
            if (init != null) {
                if (!init.getArguments().hasOnlySelf()) {
                    argsNames.addAll(init.getArguments().getSelfFilteredArgNames());
                }
                if (varArg == null && init.getArguments().hasVarArg()) {
                    varArg = new NameTok(ARGS, NameTok.VarArg);
                }

                if (kwArg == null && init.getArguments().hasKwArg()) {
                    kwArg = new NameTok(KWARGS, NameTok.KwArg);
                }
            }
        }

        //addOwnArguments
        for (INodeAdapter adapter1 : attributes) {
            argsNames.add(nodeHelper.getPublicAttr(adapter1.getName()));
        }
        List<exprType> argsExprList = new ArrayList<exprType>();
        Name selfArg = new Name(NodeHelper.KEYWORD_SELF, Name.Param, false);
        argsExprList.add(selfArg);
        for (String parameter : argsNames) {
            argsExprList.add(new Name(parameter.trim(), Name.Param, false));
        }

        exprType[] argsExpr = argsExprList.toArray(new exprType[0]);
        argumentsType args = new argumentsType(argsExpr, varArg, kwArg, null, null, null, null, null, null, null);

        //constructorCalls
        List<stmtType> body = new ArrayList<stmtType>();
        for (IClassDefAdapter base : bases) {
            Expr init = extractConstructorInit(base);
            if (init != null) {
                body.add(init);
            }
        }

        //initAttributes
        for (INodeAdapter adapter : attributes) {
            exprType target = new Attribute(new Name(NodeHelper.KEYWORD_SELF, Name.Load, false), new NameTok(
                    adapter.getName(), NameTok.Attrib), Attribute.Store);
            Assign initParam1 = new Assign(new exprType[] { target }, new Name(nodeHelper.getPublicAttr(adapter
                    .getName()), Name.Load, false), null);
            Assign initParam = initParam1;
            body.add(initParam);
        }

        //create function def
        return PyAstFactory.createFunctionDefFull(new NameTok(NodeHelper.KEYWORD_INIT, NameTok.FunctionName), args,
                body.toArray(new stmtType[0]), null, null, false);
    }

    private Expr extractConstructorInit(IClassDefAdapter base) {
        FunctionDefAdapter init = base.getFirstInit();
        if (init != null) {
            if (!init.getArguments().hasOnlySelf()) {
                Attribute classInit = new Attribute(new Name(moduleAdapter.getBaseContextName(this.classAdapter,
                        base.getName()), Name.Load, false), new NameTok(NodeHelper.KEYWORD_INIT, NameTok.Attrib),
                        Attribute.Load);
                List<exprType> constructorParameters = init.getArguments().getSelfFilteredArgs();

                Name selfArg = new Name(NodeHelper.KEYWORD_SELF, Name.Load, false);
                constructorParameters.add(0, selfArg);

                exprType[] argExp = constructorParameters.toArray(new exprType[0]);
                Name varArg = null;
                Name kwArg = null;

                if (init.getArguments().hasVarArg()) {
                    varArg = new Name(ARGS, Name.Load, false);
                }

                if (init.getArguments().hasKwArg()) {
                    kwArg = new Name(KWARGS, Name.Load, false);
                }

                Call initCall = new Call(classInit, argExp, null, varArg, kwArg);
                return new Expr(initCall);
            }
        }
        return null;
    }

    @Override
    public int getOffsetStrategy() {
        return offsetStrategy;
    }

}
