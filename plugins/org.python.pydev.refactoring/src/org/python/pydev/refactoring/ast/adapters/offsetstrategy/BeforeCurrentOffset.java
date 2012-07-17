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

    protected int getLine() {
        if (scopeAdapter != null) {
            return scopeAdapter.getNodeFirstLine() - 1;
        }
        return super.getLine();
    }

}
