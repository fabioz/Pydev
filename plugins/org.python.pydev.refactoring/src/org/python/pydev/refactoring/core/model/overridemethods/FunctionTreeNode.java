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
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.model.overridemethods;

import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.core.model.tree.ITreeNode;
import org.python.pydev.refactoring.core.model.tree.TreeNodeSimple;

public class FunctionTreeNode extends TreeNodeSimple<FunctionDefAdapter> {

    private static final String CLOSEBRACKET = ")";

    private static final String OPENBRACKET = "(";

    public FunctionTreeNode(ITreeNode parent, FunctionDefAdapter adapter) {
        super(parent, adapter);
    }

    @Override
    public String getImageName() {
        return ITreeNode.NODE_METHOD;
    }

    @Override
    public String getLabel() {
        return adapter.getName() + OPENBRACKET + adapter.getSignature() + CLOSEBRACKET;
    }

}
