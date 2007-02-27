package org.python.pydev.refactoring.ast.visitors.context;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.refactoring.ast.adapters.AbstractNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.adapters.SimpleAdapter;
import org.python.pydev.refactoring.ast.visitors.FastStack;
import org.python.pydev.refactoring.ast.visitors.NodeHelper;

public abstract class AbstractContextVisitor<T> extends VisitorBase {

	protected NodeHelper nodeHelper;

	protected List<T> nodes;

	protected final FastStack<AbstractScopeNode<?>> parents;

	protected final FastStack<SimpleNode> stack;

	protected ModuleAdapter moduleAdapter;

	public AbstractContextVisitor(ModuleAdapter module, AbstractNodeAdapter parent) {
		super();
		assert (module != null);
		this.moduleAdapter = module;

		nodeHelper = new NodeHelper();

		stack = new FastStack<SimpleNode>();
		parents = new FastStack<AbstractScopeNode<?>>();
		parents.push(moduleAdapter);
		stack.push(module.getASTNode());

		nodes = new ArrayList<T>();

	}

	private void add(T node) {
		nodes.add(node);
	}

	protected void after() {
		stack.pop();
	}

	protected AbstractNodeAdapter before(SimpleNode node) {
		AbstractNodeAdapter context = createContext(node);
		stack.push(node);
		return context;
	}

	private AbstractNodeAdapter create(SimpleNode node) {

		AbstractScopeNode<?> parent = parents.peek();

		if (nodeHelper.isClassDef(node)) {
			return new ClassDefAdapter(moduleAdapter, parent, (ClassDef) node);
		} else if (nodeHelper.isFunctionDef(node)) {
			return new FunctionDefAdapter(moduleAdapter, parent, (FunctionDef) node);
		} else
			return new SimpleAdapter(moduleAdapter, parent, node);
	}

	protected abstract T createAdapter(AbstractScopeNode<?> parent, SimpleNode node);

	protected AbstractNodeAdapter createContext(SimpleNode node) {
		if (nodeHelper.isModule(node)) {
			assert (node == moduleAdapter.getASTNode());
			return moduleAdapter;
		}

		return create(node);
	}

	public List<T> getAll() {
		return nodes;
	}

	protected boolean isInClassDef() {
		for (SimpleNode node : stack) {
			if (nodeHelper.isClassDef(node))
				return true;
		}
		return false;
	}

	protected boolean isInFunctionDef() {
		for (SimpleNode node : stack) {
			if (nodeHelper.isFunctionDef(node))
				return true;
		}
		return false;
	}

	protected boolean isParentClassDecl() {
		return nodeHelper.isClassDef(parents.peek().getASTNode());
	}

	public Iterator<T> iterator() {
		return nodes.iterator();
	}

	protected void registerInContext(SimpleNode node) {
		T context = createAdapter(parents.peek(), node);
		add(context);
	}

	protected void trackContext(SimpleNode node) throws Exception {
		AbstractNodeAdapter context = before(node);
		pushParent(context);
		traverse(node);
		parents.pop();
		after();
	}

	protected void updateASTContext(SimpleNode node) throws Exception {
		before(node);
		traverse(node);
		after();
	}

	public void traverse(FunctionDef node) throws Exception {
		visit(node.decs);
		visit(node.name);
		visit(node.args);
		visit(node.body);
	}

	@Override
	public void traverse(SimpleNode node) throws Exception {
		if (nodeHelper.isFunctionDef(node)) {
			traverse((FunctionDef) node);
		} else
			node.traverse(this);
	}

	@Override
	protected Object unhandled_node(SimpleNode node) throws Exception {
		return null;
	}

	public void visit(SimpleNode node) throws Exception {
		if (node == null)
			return;
		node.accept(this);
	}

	protected void visit(SimpleNode[] body) throws Exception {
		if (body == null)
			return;
		for (SimpleNode node : body) {
			visit(node);
		}
	}

	public Object visitClassDef(ClassDef node) throws Exception {
		trackContext(node);
		return null;
	}

	@Override
	public Object visitFunctionDef(FunctionDef node) throws Exception {
		AbstractNodeAdapter context = before(node);
		pushParent(context);
		traverse(node);
		parents.pop();
		after();
		return null;
	}

	protected void pushParent(AbstractNodeAdapter context) {
		if (context instanceof AbstractScopeNode<?>) {
			parents.push((AbstractScopeNode<?>) context);
		}
	}

}
