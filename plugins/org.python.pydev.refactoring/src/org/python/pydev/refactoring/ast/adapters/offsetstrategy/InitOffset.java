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

package org.python.pydev.refactoring.ast.adapters.offsetstrategy;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.ast.visitors.position.LastLineVisitor;

public class InitOffset extends BeginOffset {

    public InitOffset(IASTNodeAdapter<? extends SimpleNode> adapter, IDocument doc, AdapterPrefs adapterPrefs) {
        super(adapter, doc, adapterPrefs);
    }

    @Override
    protected int getLine() {
        SimpleNode node = adapter.getASTNode();
        if (nodeHelper.isClassDef(node)) {

            ClassDef classNode = (ClassDef) node;
            for (stmtType statement : classNode.body) {
                if (nodeHelper.isInit(statement)) {
                    FunctionDef func = (FunctionDef) statement;
                    stmtType lastStmt = func.body[func.body.length - 1];
                    LastLineVisitor visitor = VisitorFactory.createVisitor(LastLineVisitor.class, lastStmt);
                    return visitor.getLastLine();
                }
            }
        }
        return super.getLine();
    }

}
