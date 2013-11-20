/******************************************************************************
* Copyright (C) 2006-2009  IFS Institute for Software and others
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
