/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.filters;

import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.navigator.elements.PythonNode;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntryWithChildren;

public class CommentsFilter extends AbstractFilter {

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (element instanceof PythonNode) {
            PythonNode node = (PythonNode) element;
            ASTEntryWithChildren astThis = node.entry.getAstThis();
            if (astThis == null) {
                return true;
            }
            SimpleNode n = astThis.node;
            if (NodeUtils.isComment(n)) {
                return false;
            }
        }
        return true;
    }

}
