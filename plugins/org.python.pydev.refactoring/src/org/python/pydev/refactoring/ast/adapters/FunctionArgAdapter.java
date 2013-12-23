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

package org.python.pydev.refactoring.ast.adapters;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.visitors.rewriter.Rewriter;
import org.python.pydev.shared_core.string.StringUtils;

public class FunctionArgAdapter extends AbstractNodeAdapter<argumentsType> {

    public FunctionArgAdapter(ModuleAdapter module, FunctionDefAdapter parent, argumentsType node,
            AdapterPrefs adapterPrefs) {
        super(module, parent, node, adapterPrefs);
        Assert.isNotNull(module);
    }

    public boolean hasKwArg() {
        return getASTNode().kwarg != null;
    }

    public boolean hasVarArg() {
        return (getASTNode().vararg != null);
    }

    public boolean hasArg() {
        return (getASTNode().args != null) && (getASTNode().args.length > 0);
    }

    public List<String> getArgOnly() {
        List<String> args = new ArrayList<String>();
        for (exprType arg : getASTNode().args) {
            args.add(nodeHelper.getName(arg));
        }
        return args;
    }

    public List<String> getSelfFilteredArgNames() {
        List<String> args = new ArrayList<String>();
        for (exprType arg : getSelfFilteredArgs()) {
            args.add(nodeHelper.getName(arg));
        }
        return args;
    }

    public List<exprType> getSelfFilteredArgs() {
        List<exprType> args = new ArrayList<exprType>();
        if (getASTNode().args == null) {
            return args;
        }

        for (exprType arg : getASTNode().args) {
            String argument = nodeHelper.getName(arg);
            if (!nodeHelper.isSelf(argument)) {
                args.add((exprType) arg.createCopy()); //We have to create a copy because we don't want specials.
            }
        }
        return args;
    }

    public boolean isEmptyArgument() {
        return (!hasArg()) && (!(hasVarArg())) && (!(hasKwArg()));
    }

    public boolean hasOnlySelf() {
        return getSelfFilteredArgs().size() == 0 && (!(hasVarArg())) && (!(hasKwArg()));
    }

    public String getSignature() {
        argumentsType astNode = this.getASTNode().createCopy();
        AdapterPrefs adapterPrefs = new AdapterPrefs(getModule().getEndLineDelimiter(), this.getModule().nature);
        String ret = StringUtils.replaceNewLines(Rewriter.createSourceFromAST(astNode, true, adapterPrefs), "");
        return ret;
    }
}
