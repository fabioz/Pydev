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
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Delete;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.refactoring.ast.visitors.NodeHelper;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.GeneratePropertiesRequest;
import org.python.pydev.refactoring.core.edit.AbstractInsertEdit;

public class DeleteMethodEdit extends AbstractInsertEdit {

    private static final String DEL = "del";

    private String attributeName;

    private int offsetStrategy;

    public DeleteMethodEdit(GeneratePropertiesRequest req) {
        super(req);
        this.offsetStrategy = req.getMethodOffsetStrategy();
        this.attributeName = nodeHelper.getPublicAttr(req.getAttributeName());
    }

    @Override
    protected SimpleNode getEditNode() {
        argumentsType args = initArguments();
        exprType[] targets = initDeleteTarget();
        List<stmtType> body = initBody(targets);

        return new FunctionDef(new NameTok(DEL + getCapitalString(attributeName), NameTok.FunctionName), args, body.toArray(new stmtType[0]), null, null);
    }

    private List<stmtType> initBody(exprType[] targets) {
        List<stmtType> body = new ArrayList<stmtType>();
        body.add(new Delete(targets));
        return body;
    }

    private exprType[] initDeleteTarget() {
        exprType[] targets = new exprType[1];
        targets[0] = new Attribute(new Name(NodeHelper.KEYWORD_SELF, Name.Load, false), new NameTok(nodeHelper.getPrivateAttr(attributeName), NameTok.Attrib), Attribute.Del);
        return targets;
    }

    private argumentsType initArguments() {
        exprType[] params = new exprType[1];
        params[0] = (new Name(NodeHelper.KEYWORD_SELF, Name.Param, false));
        argumentsType args = new argumentsType(params, null, null, null, null, null, null, null, null, null);
        return args;
    }

    @Override
    public int getOffsetStrategy() {
        return offsetStrategy;
    }

}
