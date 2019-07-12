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

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.str_typeType;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.GeneratePropertiesRequest;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.SelectionState;
import org.python.pydev.refactoring.core.edit.AbstractInsertEdit;

/**
 * Creates the property:
 * 
 * <pre>
 *    attribute = property(get_attribute, set_attribute, del_attribute, "attribute's docstring")
 * </pre>
 */
public class PropertyEdit extends AbstractInsertEdit {

    private static final Name NONE = new Name("None", Name.Load, true);
    private static final Name PROPERTY = new Name("property", Name.Load, false);

    private GeneratePropertiesRequest request;

    private String attributeName;
    private String propertyName;

    private SelectionState state;

    private int offsetStrategy;

    public PropertyEdit(GeneratePropertiesRequest req) {
        super(req);
        this.request = req;
        this.attributeName = req.getAttributeName();
        this.propertyName = req.getPropertyName();
        this.state = req.getSelectionState();
        this.offsetStrategy = req.offsetPropertyStrategy;
    }

    @Override
    protected SimpleNode getEditNode() {
        exprType[] target = new exprType[] { new Name(propertyName, Name.Store, false) };
        Call property = createPropertyCall();

        return new Assign(target, property, null);
    }

    private Call createPropertyCall() {
        exprType[] args = createPropertyArguments();
        Call property = new Call(PROPERTY, args, null, null, null);
        return property;
    }

    private exprType[] createPropertyArguments() {
        List<exprType> args = new ArrayList<exprType>();

        addArgument(args, state.isGetter(), "get");
        addArgument(args, state.isSetter(), "set");
        addArgument(args, state.isDelete(), "del");
        if (state.isDocstring()) {
            args.add(new Str(propertyName + "'s docstring", str_typeType.SingleDouble, false, false, false, false));
        } else {
            args.add(NONE);
        }

        return args.toArray(new exprType[args.size()]);
    }

    private void addArgument(List<exprType> args, boolean isAvailable, String accessType) {
        if (isAvailable) {
            args.add(new Name(GeneratePropertiesRequest.getAccessorName(accessType, attributeName), Name.Load, false));
        } else {
            args.add(NONE);
        }
    }

    @Override
    public int getOffsetStrategy() {
        return offsetStrategy;
    }

}
