/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ui.model.tree;

import org.python.pydev.refactoring.ast.adapters.INodeAdapter;
import org.python.pydev.refactoring.messages.Messages;

public interface ITreeNode {

    public static String NODE_CLASS = Messages.imgClass;

    public static String NODE_METHOD = Messages.imgMethod;

    public static String NODE_ATTRIBUTE = Messages.imgAttribute;

    public String getImageName();

    public ITreeNode getParent();

    public String getLabel();

    public boolean hasChildren();

    public Object[] getChildren();

    public INodeAdapter getAdapter();
}
