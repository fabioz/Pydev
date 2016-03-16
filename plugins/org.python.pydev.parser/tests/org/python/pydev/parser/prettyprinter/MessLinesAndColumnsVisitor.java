/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinter;

import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.prettyprinterv2.MakeAstValidForPrettyPrintingVisitor;

public class MessLinesAndColumnsVisitor extends MakeAstValidForPrettyPrintingVisitor {

    @Override
    protected void fixNode(SimpleNode node) {
        node.beginLine = -1;
        node.beginColumn = -1;
        handleSpecials(node.specialsBefore);
        handleSpecials(node.specialsAfter);

    }

    private void handleSpecials(List<Object> specials) {
        if (specials != null) {
            for (Object o : specials) {
                if (o instanceof commentType) {
                    fixNode((SimpleNode) o);
                }
            }
        }
    }

}
