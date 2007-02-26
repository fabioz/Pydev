package org.python.pydev.refactoring.tests.visitors;

import java.util.Iterator;

import org.eclipse.jface.text.Document;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.adapters.PropertyAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.ast.visitors.context.ClassDefVisitor;
import org.python.pydev.refactoring.ast.visitors.context.PropertyVisitor;
import org.python.pydev.refactoring.tests.core.AbstractIOTestCase;

public class PropertyVisitorTestCase extends AbstractIOTestCase {
	public PropertyVisitorTestCase(String name) {
		super(name);
	}

	@Override
	public void runTest() throws Throwable {
		StringBuffer buffer = new StringBuffer();
		ModuleAdapter module = VisitorFactory.createModuleAdapter(null, null,
				new Document(getSource()));
		ClassDefVisitor classVisitor = VisitorFactory.createContextVisitor(
				ClassDefVisitor.class, module.getASTNode(), module, module);

		assertTrue(classVisitor.getAll().size() > 0);
		PropertyVisitor propertyVisitor = VisitorFactory.createContextVisitor(
				PropertyVisitor.class, classVisitor.getAll().get(0)
						.getASTNode(), module, classVisitor.getAll().get(0));

		printProperties(buffer, propertyVisitor);

		assertEquals(getExpected(), getGenerated());
	}

	private void printProperties(StringBuffer buffer,
			PropertyVisitor propertyVisitor) {
		Iterator<PropertyAdapter> iter = propertyVisitor.iterator();
		buffer.append("# " + propertyVisitor.getAll().size() + "\n");
		while (iter.hasNext()) {
			PropertyAdapter propertyAdapter = iter.next();
			buffer.append("# " + propertyAdapter.getParentName() + " "
					+ propertyAdapter.getName() + " "
					+ propertyAdapter.isComplete() + " "
					+ propertyAdapter.hasGetter() + " "
					+ propertyAdapter.hasSetter() + " "
					+ propertyAdapter.hasDelete() + " "
					+ propertyAdapter.hasDocString() + "\n");
		}
		this.setTestGenerated(buffer.toString().trim());
	}

	@Override
	public String getExpected() {
		return getResult();
	}
}
