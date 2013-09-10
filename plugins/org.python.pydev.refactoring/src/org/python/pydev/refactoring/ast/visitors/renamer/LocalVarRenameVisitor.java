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

package org.python.pydev.refactoring.ast.visitors.renamer;

import java.util.HashMap;
import java.util.Map;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.parser.jython.ast.factory.NodeHelper;

public class LocalVarRenameVisitor extends VisitorBase {

    private Map<String, String> renameMap;

    private NodeHelper nodeHelper;

    public LocalVarRenameVisitor(AdapterPrefs adapterPrefs) {
        this.renameMap = new HashMap<String, String>();
        this.nodeHelper = new NodeHelper(adapterPrefs);
    }

    public void visit(SimpleNode node) throws Exception {
        if (node == null) {
            return;
        }

        if (nodeHelper.isFunctionOrClassDef(node)) {
            return;
        }

        node.accept(this);
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        if (node != null) {
            node.traverse(this);
        }
    }

    @Override
    public Object visitName(Name node) throws Exception {
        if (renameMap.containsKey(node.id)) {
            node.id = renameMap.get(node.id);
        }
        return null;
    }

    @Override
    public Object visitNameTok(NameTok node) throws Exception {
        if (renameMap.containsKey(node.id)) {
            node.id = renameMap.get(node.id);
        }
        return null;
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        // ignore function name
        visit(node.args);
        visit(node.body);
        return null;
    }

    private void visit(argumentsType args) throws Exception {
        visit(args.args);
        visit(args.vararg);
        visit(args.kwarg);
    }

    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        visit(node.value);
        return null;
    }

    private void visit(SimpleNode[] nodes) throws Exception {
        for (SimpleNode node : nodes) {
            visit(node);
        }
    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    /**
     * 
     * @param renameMap
     *            Contains association oldName => newName
     */
    public void setRenameMap(Map<String, String> renameMap) {
        this.renameMap = renameMap;
    }

}
