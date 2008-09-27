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

    public abstract T getASTNode();

    public abstract SimpleNode getASTParent();

    public abstract int getNodeBodyIndent();

    public abstract int getNodeFirstLine();

    public abstract int getNodeIndent();

    public abstract int getNodeLastLine();

    public abstract AbstractNodeAdapter<? extends SimpleNode> getParent();

    public abstract SimpleNode getParentNode();

    public abstract boolean isModule();

    public ModuleAdapter getModule();

}
