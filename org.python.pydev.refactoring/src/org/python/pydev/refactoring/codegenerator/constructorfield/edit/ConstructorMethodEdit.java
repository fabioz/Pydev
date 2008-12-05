/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.codegenerator.constructorfield.edit;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.SpecialStr;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NameTokType;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.INodeAdapter;
import org.python.pydev.refactoring.ast.visitors.NodeHelper;
import org.python.pydev.refactoring.codegenerator.constructorfield.request.ConstructorFieldRequest;
import org.python.pydev.refactoring.core.edit.AbstractInsertEdit;

public class ConstructorMethodEdit extends AbstractInsertEdit {

    private static final String KW_ARG = "kwArg";

    private static final String VAR_ARG = "varArg";

    private int offsetStrategy;

    private List<INodeAdapter> attributes;

    private IClassDefAdapter classAdapter;

    public ConstructorMethodEdit(ConstructorFieldRequest req) {
        super(req);
        this.classAdapter = req.getClassAdapter();
        this.attributes = req.getAttributeAdapters();
        this.offsetStrategy = req.getOffsetStrategy();
    }

    /**
     * Tricky :)
     */
    @Override
    protected SimpleNode getEditNode() {
        List<IClassDefAdapter> bases = classAdapter.getBaseClasses();

        List<stmtType> body = new ArrayList<stmtType>();

        argumentsType args = extractArguments(bases);

        constructorCalls(bases, body);

        initAttributes(body);

        return new FunctionDef(new NameTok(NodeHelper.KEYWORD_INIT, NameTok.FunctionName), args, body.toArray(new stmtType[0]), null, null);

    }

    private void initAttributes(List<stmtType> body) {
        for (INodeAdapter adapter : attributes) {
            Assign initParam = initAttribute(adapter);
            body.add(initParam);
        }
    }

    private void constructorCalls(List<IClassDefAdapter> bases, List<stmtType> body) {
        for (IClassDefAdapter base : bases) {
            Expr init = extractConstructorInit(base);
            if (init != null)
                body.add(init);
        }
    }

    private Assign initAttribute(INodeAdapter adapter) {
        exprType target = new Attribute(new Name(NodeHelper.KEYWORD_SELF, Name.Load, false), new NameTok(adapter.getName(), NameTok.Attrib),
                Attribute.Store);
        Assign initParam = new Assign(new exprType[] { target }, new Name(nodeHelper.getPublicAttr(adapter.getName()), Name.Load, false));
        return initParam;
    }

    private Expr extractConstructorInit(IClassDefAdapter base) {
        FunctionDefAdapter init = base.getFirstInit();
        if (init != null) {
            if (!init.getArguments().hasOnlySelf()) {
                Attribute classInit = new Attribute(
                        new Name(moduleAdapter.getBaseContextName(this.classAdapter, base.getName()), Name.Load, false), new NameTok(
                                NodeHelper.KEYWORD_INIT, NameTok.Attrib), Attribute.Load);
                List<exprType> constructorParameters = init.getArguments().getSelfFilteredArgs();

                Name selfArg = new Name(NodeHelper.KEYWORD_SELF, Name.Load, false);
                if (constructorParameters.size() > 0 || init.getArguments().hasVarArg() || init.getArguments().hasKwArg()) {
                    selfArg.getSpecialsAfter().add(new SpecialStr(",", 0, 0));
                }
                constructorParameters.add(0, selfArg);

                exprType[] argExp = constructorParameters.toArray(new exprType[0]);
                Name varArg = null;
                Name kwArg = null;

                if (init.getArguments().hasVarArg())
                    varArg = new Name(VAR_ARG, Name.Load, false);

                if (init.getArguments().hasKwArg())
                    kwArg = new Name(KW_ARG, Name.Load, false);

                Call initCall = new Call(classInit, argExp, null, varArg, kwArg);
                return new Expr(initCall);
            }
        }
        return null;
    }

    private argumentsType extractArguments(List<IClassDefAdapter> bases) {
        NameTokType varArg = null;
        NameTokType kwArg = null;

        SortedSet<String> argsNames = new TreeSet<String>();

        for (IClassDefAdapter baseClass : bases) {
            FunctionDefAdapter init = baseClass.getFirstInit();
            if (init != null) {
                if (!init.getArguments().hasOnlySelf()) {
                    argsNames.addAll(init.getArguments().getSelfFilteredArgNames());
                }
                if (varArg == null && init.getArguments().hasVarArg())
                    varArg = new NameTok(VAR_ARG, NameTok.VarArg);

                if (kwArg == null && init.getArguments().hasKwArg())
                    kwArg = new NameTok(KW_ARG, NameTok.KwArg);
            }
        }

        addOwnArguments(argsNames);
        exprType[] argsExpr = generateExprArray(argsNames);

        return new argumentsType(argsExpr, varArg, kwArg, null);
    }

    private exprType[] generateExprArray(SortedSet<String> argsNames) {
        List<exprType> argsExprList = new ArrayList<exprType>();
        Name selfArg = new Name(NodeHelper.KEYWORD_SELF, Name.Param, false);
        argsExprList.add(selfArg);
        for (String parameter : argsNames) {
            argsExprList.add(new Name(parameter.trim(), Name.Param, false));
        }

        exprType[] argsExpr = argsExprList.toArray(new exprType[0]);
        return argsExpr;
    }

    private void addOwnArguments(SortedSet<String> argsNames) {
        for (INodeAdapter adapter : attributes) {
            argsNames.add(nodeHelper.getPublicAttr(adapter.getName()));
        }
    }

    @Override
    public int getOffsetStrategy() {
        return offsetStrategy;
    }

}
