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

package org.python.pydev.refactoring.core.model.generateproperties;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.refactoring.ast.adapters.INodeAdapter;
import org.python.pydev.refactoring.ast.adapters.PropertyTextAdapter;
import org.python.pydev.refactoring.core.model.tree.ITreeNode;
import org.python.pydev.refactoring.core.model.tree.TreeNodeSimple;
import org.python.pydev.refactoring.messages.Messages;

public class TreeAttributeNode extends TreeNodeSimple<INodeAdapter> {

    public TreeAttributeNode(ITreeNode parent, INodeAdapter adapter) {
        super(parent, adapter);
    }

    @Override
    public Object[] getChildren() {
        List<ITreeNode> children = new ArrayList<ITreeNode>();
        children.add(new TreeNodeSimple<PropertyTextAdapter>(this, new PropertyTextAdapter(PropertyTextAdapter.GETTER,
                Messages.generatePropertiesGetter)));
        children.add(new TreeNodeSimple<PropertyTextAdapter>(this, new PropertyTextAdapter(PropertyTextAdapter.SETTER,
                Messages.generatePropertiesSetter)));
        children.add(new TreeNodeSimple<PropertyTextAdapter>(this, new PropertyTextAdapter(PropertyTextAdapter.DELETE,
                Messages.generatePropertiesDelete)));
        children.add(new TreeNodeSimple<PropertyTextAdapter>(this, new PropertyTextAdapter(
                PropertyTextAdapter.DOCSTRING, Messages.generatePropertiesDocString)));
        return children.toArray();
    }

    @Override
    public String getImageName() {
        return ITreeNode.NODE_ATTRIBUTE;
    }

}
