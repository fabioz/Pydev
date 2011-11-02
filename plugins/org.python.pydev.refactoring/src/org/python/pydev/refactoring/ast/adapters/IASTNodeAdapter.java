/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.adapters;

import org.python.pydev.parser.jython.SimpleNode;

public interface IASTNodeAdapter<T extends SimpleNode> extends INodeAdapter {
    T getASTNode();

    SimpleNode getASTParent();

    String getNodeBodyIndent();

    int getNodeFirstLine();

    int getNodeIndent();

    int getNodeLastLine();

    AbstractNodeAdapter<? extends SimpleNode> getParent();

    SimpleNode getParentNode();

    boolean isModule();

    ModuleAdapter getModule();
}
