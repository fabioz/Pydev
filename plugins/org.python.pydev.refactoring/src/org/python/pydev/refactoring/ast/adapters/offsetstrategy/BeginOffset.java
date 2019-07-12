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
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.ISpecialStr;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.parser.visitors.FindLastLineVisitor;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;

public class BeginOffset extends AbstractOffsetStrategy {

    public BeginOffset(IASTNodeAdapter<? extends SimpleNode> adapter, IDocument doc, AdapterPrefs adapterPrefs) {
        super(adapter, doc, adapterPrefs);
    }

    @Override
    protected int getLine() {
        SimpleNode node = adapter.getASTNode();
        if (nodeHelper.isClassDef(node)) {
            ClassDef classNode = (ClassDef) node;
            Str strNode = NodeUtils.getNodeDocStringNode(node);

            if (strNode != null) {
                return NodeUtils.getLineEnd(strNode);
            }

            FindLastLineVisitor findLastLineVisitor = new FindLastLineVisitor();
            try {
                classNode.name.accept(findLastLineVisitor);
                if (classNode.bases != null) {
                    for (SimpleNode n : classNode.bases) {
                        n.accept(findLastLineVisitor);
                    }
                }
                SimpleNode lastNode = findLastLineVisitor.getLastNode();
                ISpecialStr lastSpecialStr = findLastLineVisitor.getLastSpecialStr();
                if (lastSpecialStr != null
                        && (lastSpecialStr.toString().equals(":") || lastSpecialStr.toString().equals(")"))) {
                    // it was an from xxx import (euheon, utehon)
                    return lastSpecialStr.getBeginLine();
                } else {
                    return lastNode.beginLine;
                }
            } catch (Exception e) {
                Log.log(e);
            }

        }

        int startLine = adapter.getNodeFirstLine(true) - 1;
        if (startLine < 0) {
            startLine = 0;
        }
        return startLine;
    }
}
