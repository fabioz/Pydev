package org.python.pydev.refactoring.ast.visitors.context;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.refactoring.ast.adapters.AbstractNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;

public class ClassDefVisitor extends AbstractContextVisitor<IClassDefAdapter> {

	public ClassDefVisitor(ModuleAdapter module, AbstractNodeAdapter parent) {
		super(module, parent);
	}

	@Override
	protected IClassDefAdapter createAdapter(AbstractScopeNode<?> parent, SimpleNode node) {
		return new ClassDefAdapter(moduleAdapter, parent, (ClassDef) node);
	}

	@Override
	public Object visitClassDef(ClassDef node) throws Exception {
		registerInContext(node);
		return super.visitClassDef(node);
	}

}
