package org.python.pydev.refactoring.ast.adapters;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.ast.visitors.context.LocalAttributeVisitor;
import org.python.pydev.refactoring.ast.visitors.context.PropertyVisitor;
import org.python.pydev.refactoring.ast.visitors.context.ScopeAssignedVisitor;
import org.python.pydev.refactoring.ast.visitors.position.IndentVisitor;

public class ClassDefAdapter extends AbstractScopeNode<ClassDef> {

	private static final String OBJECT = "object";

	private List<SimpleAdapter> attributes;

	private List<PropertyAdapter> properties;

	public ClassDefAdapter(ModuleAdapter module, AbstractScopeNode<?> parent,
			ClassDef node) {
		super(module, parent, node);
		this.attributes = null;
		this.properties = null;
	}

	public List<String> getBaseClassNames() {
		return nodeHelper.getBaseClassName(getASTNode());
	}

	public List<ClassDefAdapter> getBaseClasses() {
		return getModule().getBaseClasses(this);
	}

	public boolean hasBaseClass() {
		return getBaseClassNames().size() > 0;
	}

	public List<SimpleAdapter> getAttributes() {
		if (attributes == null) {
			LocalAttributeVisitor visitor = VisitorFactory
					.createContextVisitor(LocalAttributeVisitor.class,
							getASTNode(), getModule(), this);
			attributes = visitor.getAll();
		}
		return attributes;
	}

	public List<PropertyAdapter> getProperties() {
		if (properties == null) {
			PropertyVisitor visitor = VisitorFactory.createContextVisitor(
					PropertyVisitor.class, getASTNode(), getModule(), this);
			properties = visitor.getAll();
		}
		return properties;
	}

	public List<FunctionDefAdapter> getFunctionsInitFiltered() {
		List<FunctionDefAdapter> functionsFiltered = new ArrayList<FunctionDefAdapter>();
		for (FunctionDefAdapter adapter : getFunctions()) {
			if (!(adapter.isInit())) {
				functionsFiltered.add(adapter);
			}
		}

		return functionsFiltered;
	}

	public boolean hasFunctions() {
		return getFunctions().size() > 0;
	}

	public boolean hasFunctionsInitFiltered() {
		return getFunctionsInitFiltered().size() > 0;
	}

	public boolean isNested() {
		return nodeHelper.isFunctionOrClassDef(getParent().getASTNode());
	}

	public boolean hasAttributes() {
		return getAttributes().size() > 0;
	}

	public int getNodeBodyIndent() {
		ClassDef classNode = getASTNode();
		IndentVisitor visitor = VisitorFactory.createVisitor(
				IndentVisitor.class, classNode.body[0]);

		return visitor.getIndent();
	}

	public boolean hasInit() {
		return (getFirstInit() != null);
	}

	public FunctionDefAdapter getFirstInit() {
		for (FunctionDefAdapter func : getFunctions()) {
			if (func.isInit()) {
				return func;
			}
		}
		return null;
	}

	public List<SimpleAdapter> getAssignedVariables() {
		ScopeAssignedVisitor visitor = VisitorFactory.createContextVisitor(
				ScopeAssignedVisitor.class, getASTNode(), this.getModule(),
				this);
		return visitor.getAll();
	}
	
	public boolean isNewStyleClass() {
		for(String base : getBaseClassNames())
		{
			if (base.compareTo(OBJECT) == 0)
			{
				return true;
			}
		}
		return false;
	}
}
