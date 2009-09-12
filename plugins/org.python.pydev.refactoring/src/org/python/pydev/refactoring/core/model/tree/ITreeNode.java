/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.model.tree;

import org.python.pydev.refactoring.ast.adapters.INodeAdapter;
import org.python.pydev.refactoring.messages.Messages;

public interface ITreeNode {
    String NODE_CLASS = Messages.imgClass;
    String NODE_METHOD = Messages.imgMethod;
    String NODE_ATTRIBUTE = Messages.imgAttribute;

    String getImageName();

    ITreeNode getParent();

    String getLabel();

    boolean hasChildren();

    Object[] getChildren();

    INodeAdapter getAdapter();
}
