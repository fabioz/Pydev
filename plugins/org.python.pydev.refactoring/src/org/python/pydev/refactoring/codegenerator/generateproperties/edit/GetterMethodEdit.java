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
*     Fabio Zadrozny <fabiofz@gmail.com>  - initial implementation
*     Camilo Bernal <cabernal@redhat.com> - ongoing maintenance
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.codegenerator.generateproperties.edit;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.factory.NodeHelper;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.GeneratePropertiesRequest;
import org.python.pydev.refactoring.core.edit.AbstractInsertEdit;

/**
 * Creates the getter function:
 * 
 * <pre>
 *    def get_attribute(self):
 *        return self._attribute
 * </pre>
 */
public class GetterMethodEdit extends AbstractInsertEdit {

    private String attributeName;
    private String accessorName;

    private int offsetStrategy;

    public GetterMethodEdit(GeneratePropertiesRequest req) {
        super(req);
        this.attributeName = req.getAttributeName();
        this.accessorName = GeneratePropertiesRequest.getAccessorName("get", attributeName);

        this.offsetStrategy = req.offsetMethodStrategy;
    }

    @Override
    protected SimpleNode getEditNode() {
        NameTok functionName = new NameTok(accessorName, NameTok.FunctionName);
        argumentsType args = createArguments();
        stmtType[] body = createBody();

        return new FunctionDef(functionName, args, body, null, null, false);
    }

    private argumentsType createArguments() {
        exprType[] params = new exprType[] { new Name(NodeHelper.KEYWORD_SELF, Name.Param, false) };
        return new argumentsType(params, null, null, null, null, null, null, null, null, null);
    }

    private stmtType[] createBody() {
        Name self = new Name(NodeHelper.KEYWORD_SELF, Name.Load, false);
        NameTok name = new NameTok(nodeHelper.getPrivateAttr(attributeName), NameTok.Attrib);
        Attribute returnAttribute = new Attribute(self, name, Attribute.Load);
        Return returnStmt = new Return(returnAttribute);

        return new stmtType[] { returnStmt };
    }

    @Override
    public int getOffsetStrategy() {
        return offsetStrategy;
    }

}
