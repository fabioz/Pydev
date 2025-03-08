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

import org.python.pydev.ast.adapters.FunctionDefAdapter;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.factory.PyAstFactory;
import org.python.pydev.parser.prettyprinterv2.MakeAstValidForPrettyPrintingVisitor;
import org.python.pydev.refactoring.codegenerator.overridemethods.request.OverrideMethodsRequest;
import org.python.pydev.refactoring.core.edit.AbstractInsertEdit;

public class MethodEdit extends AbstractInsertEdit {

    private int offsetStrategy;

    private FunctionDefAdapter method;

    public MethodEdit(OverrideMethodsRequest req) {
        super(req);
        this.method = req.method;
        this.offsetStrategy = req.offsetStrategy;
    }

    @Override
    protected SimpleNode getEditNode() {
        FunctionDef origin = method.getASTNode();

        PyAstFactory factory = new PyAstFactory(adapterPrefs);
        stmtType overrideBody = factory.createOverrideBody(origin); //Note that the copy won't have a parent.

        FunctionDef functionDef = origin.createCopy(false);
        functionDef.body = new stmtType[] { overrideBody != null ? overrideBody : new Pass() };

        try {
            MakeAstValidForPrettyPrintingVisitor.makeValid(functionDef);
        } catch (Exception e) {
            Log.log(e);
        }
        return functionDef;
    }

    @Override
    public int getOffsetStrategy() {
        return offsetStrategy;
    }

}
