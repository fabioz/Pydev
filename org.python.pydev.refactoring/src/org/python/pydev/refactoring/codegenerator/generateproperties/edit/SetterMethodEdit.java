/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.codegenerator.generateproperties.edit;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.refactoring.ast.visitors.NodeHelper;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.GeneratePropertiesRequest;
import org.python.pydev.refactoring.core.edit.AbstractInsertEdit;

public class SetterMethodEdit extends AbstractInsertEdit {

    private static final String SET = "set";

    private static final String VALUE = "value";

    private String attributeName;

    private int offsetStrategy;

    public SetterMethodEdit(GeneratePropertiesRequest req) {
        super(req);
        this.attributeName = nodeHelper.getPublicAttr(req.getAttributeName());
        this.offsetStrategy = req.getMethodOffsetStrategy();
    }

    @Override
    protected SimpleNode getEditNode() {
        argumentsType args = initArguments();
        Assign assign = initSetAssignment();
        List<stmtType> body = initBody(assign);

        return new FunctionDef(new NameTok(SET + getCapitalString(attributeName), NameTok.FunctionName), args, body
                .toArray(new stmtType[0]), null, null);
    }

    private List<stmtType> initBody(Assign assign) {
        List<stmtType> body = new ArrayList<stmtType>();
        body.add(assign);
        return body;
    }

    private Assign initSetAssignment() {
        exprType[] targets = new exprType[1];
        targets[0] = new Attribute(new Name(NodeHelper.KEYWORD_SELF, Name.Load, false), new NameTok(nodeHelper.getPrivateAttr(attributeName),
                NameTok.Attrib), Attribute.Store);

        Assign assign = new Assign(targets, new Name(VALUE, Name.Load, false));
        return assign;
    }

    private argumentsType initArguments() {
        exprType[] params = new exprType[2];
        params[0] = (new Name(NodeHelper.KEYWORD_SELF, Name.Param, false));
        params[1] = (new Name(VALUE, Name.Param, false));
        argumentsType args = new argumentsType(params, null, null, null);
        return args;
    }

    @Override
    public int getOffsetStrategy() {
        return offsetStrategy;
    }

}
