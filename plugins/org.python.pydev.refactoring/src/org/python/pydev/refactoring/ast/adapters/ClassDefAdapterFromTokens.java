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

package org.python.pydev.refactoring.ast.adapters;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.python.pydev.core.IToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.SpecialStr;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.parser.jython.ast.factory.PyAstFactory;

public class ClassDefAdapterFromTokens implements IClassDefAdapter {

    private List<IToken> tokens;
    private String parentName;
    private AdapterPrefs adapterPrefs;
    private ModuleAdapter module;
    private List<FunctionDefAdapter> cache;

    public ClassDefAdapterFromTokens(ModuleAdapter module, String parentName, List<IToken> tokens,
            AdapterPrefs adapterPrefs) {
        this.module = module;
        this.parentName = parentName;
        this.tokens = tokens;
        this.adapterPrefs = adapterPrefs;
    }

    @Override
    public List<SimpleAdapter> getAssignedVariables() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public List<SimpleAdapter> getAttributes() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public List<String> getBaseClassNames() {
        return new ArrayList<String>();
    }

    @Override
    public List<IClassDefAdapter> getBaseClasses() {
        return new ArrayList<IClassDefAdapter>();
    }

    @Override
    public FunctionDefAdapter getFirstInit() {
        return null;
    }

    @Override
    public List<FunctionDefAdapter> getFunctions() {
        return getFunctionsInitFiltered();
    }

    @Override
    public synchronized List<FunctionDefAdapter> getFunctionsInitFiltered() {
        if (cache == null) {
            cache = new ArrayList<FunctionDefAdapter>();
            for (IToken tok : this.tokens) {
                if (tok.getType() == IToken.TYPE_FUNCTION || tok.getType() == IToken.TYPE_BUILTIN
                        || tok.getType() == IToken.TYPE_UNKNOWN) {
                    String args = tok.getArgs();

                    List<exprType> arguments = new ArrayList<exprType>();
                    boolean useAnyArgs = false;
                    if (args.length() > 0) {
                        StringTokenizer strTok = new StringTokenizer(args, "( ,)");
                        if (!strTok.hasMoreTokens()) {
                            useAnyArgs = true;
                        } else {
                            while (strTok.hasMoreTokens()) {
                                String nextArg = strTok.nextToken();
                                arguments.add(new Name(nextArg, Name.Load, false));
                            }
                        }
                    } else {
                        useAnyArgs = true;
                    }

                    argumentsType functionArguments = new argumentsType(arguments.toArray(new exprType[0]), null, null,
                            null, null, null, null, null, null, null);
                    if (useAnyArgs) {
                        Name name = new Name("self", Name.Store, false);
                        name.addSpecial(new SpecialStr(",", -1, -1), true);
                        functionArguments.args = new exprType[] { name };
                        functionArguments.vararg = new NameTok("args", NameTok.VarArg);
                        functionArguments.kwarg = new NameTok("kwargs", NameTok.KwArg);
                    }
                    //                System.out.println(tok.getRepresentation()+tok.getArgs());
                    FunctionDef functionDef = PyAstFactory.createFunctionDefFull(
                            new NameTok(tok.getRepresentation(), NameTok.FunctionName), functionArguments, null, null,
                            null, false);
                    cache.add(new FunctionDefAdapter(this.getModule(), null, functionDef, adapterPrefs));
                }
            }
        }
        return cache;
    }

    @Override
    public String getNodeBodyIndent() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public List<PropertyAdapter> getProperties() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean hasAttributes() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean hasBaseClass() {
        return false;
    }

    @Override
    public boolean hasFunctions() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean hasFunctionsInitFiltered() {
        return this.tokens.size() > 0;
    }

    @Override
    public boolean hasInit() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isNested() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isNewStyleClass() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public String getName() {
        return parentName;
    }

    @Override
    public String getParentName() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public ClassDef getASTNode() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public SimpleNode getASTParent() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public ModuleAdapter getModule() {
        return this.module;
    }

    @Override
    public int getNodeFirstLine(boolean considerDecorators) {
        return 0;
    }

    @Override
    public int getNodeIndent() {
        return 0;
    }

    @Override
    public int getNodeLastLine() {
        return 0;
    }

    @Override
    public AbstractNodeAdapter<? extends SimpleNode> getParent() {
        return null;
    }

    @Override
    public SimpleNode getParentNode() {
        return null;
    }

    @Override
    public boolean isModule() {
        return false;
    }

}
