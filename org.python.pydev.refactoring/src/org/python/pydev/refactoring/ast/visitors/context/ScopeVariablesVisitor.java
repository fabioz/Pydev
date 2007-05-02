package org.python.pydev.refactoring.ast.visitors.context;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.adapters.SimpleAdapter;
import org.python.pydev.refactoring.ast.visitors.NodeHelper;

public class ScopeVariablesVisitor extends AbstractContextVisitor<SimpleAdapter> {

	public ScopeVariablesVisitor(ModuleAdapter module, AbstractScopeNode<?> parent) {
		super(module, parent);
	}

	@Override
	public void visit(SimpleNode node) throws Exception {
		if (nodeHelper.isClassDef(node))
			return;
		if (nodeHelper.isFunctionDef(node))
			return;

		super.visit(node);
	}

	@Override
	public void traverse(SimpleNode node) throws Exception {
		if (nodeHelper.isClassDef(node))
			return;
		if (nodeHelper.isFunctionDef(node))
			return;

		super.traverse(node);
	}

	@Override
	public Object visitImport(Import node) throws Exception {
		return null;
	}

	@Override
	public Object visitImportFrom(ImportFrom node) throws Exception {
		return null;
	}

	@Override
	protected SimpleAdapter createAdapter(AbstractScopeNode<?> parent, SimpleNode node) {
		return new SimpleAdapter(this.moduleAdapter, parent, node, moduleAdapter.getEndLineDelimiter());
	}

	@Override
	public Object visitAttribute(Attribute node) throws Exception {
		visit(node.value); // could be a local variable if not self
		return null;
	}

	@Override
	public Object visitClassDef(ClassDef node) throws Exception {
		visit(node.body);
		return null;
	}

	@Override
	public Object visitModule(Module node) throws Exception {
		visit(node.body);
		return null;
	}

	@Override
	public Object visitName(Name node) throws Exception {
		if (node.id.compareTo(NodeHelper.KEYWORD_SELF) == 0)
			return null;

		registerInContext(node);
		return null;
	}

}
