/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jul 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codefolding;

import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.python.pydev.parser.visitors.scope.ASTEntry;

/**
 * This class was created so that we can check if some annotation reappears
 * (if this happens, we don't want to delete it).
 * 
 * @author Fabio Zadrozny
 */
public class PyProjectionAnnotation extends ProjectionAnnotation {

    public ASTEntry node;

    public PyProjectionAnnotation(ASTEntry node, boolean isCollapsed) {
        super(isCollapsed);
        this.node = node;
    }

    /**
     * @param node2
     * @param model 
     * @return
     */
    public boolean appearsSame(ASTEntry node2) {

        if (node2.getClass().equals(node.getClass()) == false)
            return false;

        if (getCompleteName(node2).equals(getCompleteName(node)) == false)
            return false;

        return true;
    }

    /**
     * @param node2
     */
    private String getCompleteName(ASTEntry node2) {

        String ret = node2.getName();

        while (node2.parent != null) {
            ret = node2.parent.getName() + "." + ret;
            node2 = node2.parent;
        }

        return ret;
    }

}
