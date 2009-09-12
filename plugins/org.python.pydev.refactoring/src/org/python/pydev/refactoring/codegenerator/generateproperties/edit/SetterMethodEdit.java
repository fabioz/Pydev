/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.codegenerator.generateproperties.edit;

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

/**
 * Creates the setter function:
 * 
 * <pre>
 *    def set_attribute(self, value):
 *        self._attribute = value
 * </pre>
 */
public class SetterMethodEdit extends AbstractInsertEdit {

    private static final String VALUE = "value";

    private String attributeName;
    private String accessorName;

    private int offsetStrategy;

    public SetterMethodEdit(GeneratePropertiesRequest req) {
        super(req);
        this.attributeName = req.getAttributeName();
        this.accessorName = req.getAccessorName("set", attributeName);

        this.offsetStrategy = req.offsetMethodStrategy;
    }

    @Override
    protected SimpleNode getEditNode() {
        NameTok functionName = new NameTok(accessorName, NameTok.FunctionName);
        argumentsType args = createArguments();
        stmtType[] body = createBody();

        return new FunctionDef(functionName, args, body, null, null);
    }

    private argumentsType createArguments() {
        Name self = new Name(NodeHelper.KEYWORD_SELF, Name.Param, false);
        Name value = new Name(VALUE, Name.Param, false);
        exprType[] params = new exprType[] { self, value };

        return new argumentsType(params, null, null, null, null, null, null, null, null, null);
    }

    private stmtType[] createBody() {
        Name self = new Name(NodeHelper.KEYWORD_SELF, Name.Load, false);
        NameTok name = new NameTok(nodeHelper.getPrivateAttr(attributeName), NameTok.Attrib);
        Attribute attribute = new Attribute(self, name, Attribute.Store);

        Name value = new Name(VALUE, Name.Load, false);
        Assign assign = new Assign(new exprType[] { attribute }, value);

        return new stmtType[] { assign };
    }

    @Override
    public int getOffsetStrategy() {
        return offsetStrategy;
    }

}
