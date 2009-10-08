/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.adapters;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NameTokType;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;

public class ObjectAdapter extends ClassDefAdapter {

    public ObjectAdapter(ModuleAdapter module, AbstractScopeNode<?> parent, AdapterPrefs adapterPrefs) {
        super(module, parent, null, adapterPrefs);
    }

    @Override
    public ClassDef getASTNode() {

        NameTokType name = new NameTok("object", NameTok.ClassName);

        List<stmtType> body = initMethods();

        ClassDef object = new ClassDef(name, null, body.toArray(new stmtType[0]), null, null, null, null);

        return object;
    }

    private List<stmtType> initMethods() {
        List<stmtType> body = new ArrayList<stmtType>();
        body.add(createClass());
        body.add(createDelattr());
        body.add(createGetattribute());
        body.add(createHash());
        body.add(createInit());
        body.add(createReduce());
        body.add(createReduceEx());
        body.add(createRepr());
        body.add(createSetattr());
        body.add(createStr());
        return body;
    }

    private FunctionDef createClass() {
        NameTokType name = nameToken("__class__");

        exprType[] args = new exprType[1];
        args[0] = new Name("cls", Name.Param, false);

        NameTokType vararg = new NameTok("args", NameTok.VarArg);

        argumentsType arguments = new argumentsType(args, vararg, null, null, null, null, null, null, null, null);

        return new FunctionDef(name, arguments, null, null, null);
    }

    private FunctionDef createDelattr() {
        NameTokType name = nameToken("__delattr__");
        argumentsType arguments = arguments("name");

        return new FunctionDef(name, arguments, null, null, null);
    }

    private FunctionDef createGetattribute() {
        NameTokType name = nameToken("__getattribute__");
        argumentsType arguments = arguments("name");

        return new FunctionDef(name, arguments, null, null, null);
    }

    private FunctionDef createHash() {
        NameTokType name = nameToken("__hash__");
        argumentsType arguments = arguments();

        return new FunctionDef(name, arguments, null, null, null);
    }

    private FunctionDef createInit() {
        NameTokType name = nameToken("__init__");

        exprType[] args = new exprType[1];
        args[0] = new Name("self", Name.Param, false);

        NameTokType vararg = new NameTok("args", NameTok.VarArg);

        argumentsType arguments = new argumentsType(args, vararg, null, null, null, null, null, null, null, null);

        return new FunctionDef(name, arguments, null, null, null);
    }

    private FunctionDef createReduce() {
        NameTokType name = nameToken("__reduce__");
        argumentsType arguments = arguments();

        return new FunctionDef(name, arguments, null, null, null);
    }

    private FunctionDef createReduceEx() {
        NameTokType name = nameToken("__reduce_ex__");
        argumentsType arguments = arguments();

        return new FunctionDef(name, arguments, null, null, null);
    }

    private FunctionDef createRepr() {
        NameTokType name = nameToken("__repr__");
        argumentsType arguments = arguments();

        return new FunctionDef(name, arguments, null, null, null);
    }

    private FunctionDef createSetattr() {
        NameTokType name = nameToken("__setattr__");
        argumentsType arguments = arguments("name", "value");

        return new FunctionDef(name, arguments, null, null, null);
    }

    private NameTok nameToken(String string) {
        return new NameTok(string, NameTok.FunctionName);
    }

    private FunctionDef createStr() {
        NameTokType name = new NameTok("__str__", NameTok.FunctionName);
        argumentsType arguments = arguments();

        return new FunctionDef(name, arguments, null, null, null);
    }

    private argumentsType arguments(String... args) {
        LinkedList<exprType> argsExpr = new LinkedList<exprType>();
        argsExpr.add(new Name("self", Name.Param, false));

        for(String argumetName:args){
            argsExpr.add(new Name(argumetName, Name.Param, false));
        }

        exprType[] xArgs = argsExpr.toArray(new exprType[argsExpr.size()]);
        argumentsType arguments = new argumentsType(xArgs, null, null, null, null, null, null, null, null, null);
        return arguments;
    }
}
