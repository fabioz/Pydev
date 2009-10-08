/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.visitors.context;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;

public class GlobalFunctionDefVisitor extends AbstractContextVisitor<FunctionDefAdapter> {

    public GlobalFunctionDefVisitor(ModuleAdapter module, AbstractScopeNode<?> parent) {
        super(module, parent);
    }

    @Override
    protected FunctionDefAdapter createAdapter(AbstractScopeNode<?> parent, SimpleNode node) {
        return new FunctionDefAdapter(moduleAdapter, parent, (FunctionDef) node, moduleAdapter.getAdapterPrefs());
    }

    @Override
    public void visit(SimpleNode node) throws Exception {
        super.visit(node);
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        registerInContext(node);
        return super.visitFunctionDef(node);
    }

}
