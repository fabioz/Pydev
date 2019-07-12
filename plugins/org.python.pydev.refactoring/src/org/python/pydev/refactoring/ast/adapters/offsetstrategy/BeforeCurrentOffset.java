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

package org.python.pydev.refactoring.ast.adapters.offsetstrategy;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;

public class BeforeCurrentOffset extends BeginOffset {

    private AbstractScopeNode<?> scopeAdapter;

    public BeforeCurrentOffset(IASTNodeAdapter<? extends SimpleNode> adapter, IDocument doc, AdapterPrefs adapterPrefs,
            AbstractScopeNode<?> scopeAdapter) {
        super(adapter, doc, adapterPrefs);
        this.scopeAdapter = scopeAdapter;
    }

    @Override
    protected int getLine() {
        if (scopeAdapter != null) {
            return scopeAdapter.getNodeFirstLine(true) - 1;
        }
        return super.getLine();
    }

}
