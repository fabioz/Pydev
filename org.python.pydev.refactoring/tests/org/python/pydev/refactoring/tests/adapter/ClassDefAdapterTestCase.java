package org.python.pydev.refactoring.tests.adapter;

import java.util.List;

import org.eclipse.jface.text.Document;
import org.python.pydev.refactoring.ast.adapters.ClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.tests.core.AbstractIOTestCase;

public class ClassDefAdapterTestCase extends AbstractIOTestCase {

	public ClassDefAdapterTestCase(String name) {
		super(name);
	}

	@Override
	public void runTest() throws Throwable {
		StringBuffer buffer = new StringBuffer();
		ModuleAdapter module = VisitorFactory.createModuleAdapter(null, null,
				new Document(getSource()));
		List<ClassDefAdapter> classes = module.getClasses();
		assertTrue(classes.size() > 0);

		for (ClassDefAdapter adapter : module.getClasses()) {
			printBaseClass(buffer, adapter);
			for (ClassDefAdapter base : adapter.getBaseClasses()) {
				buffer.append("## " + adapter.getName());
				printBaseDefClass(buffer, base);
			}

		}

		this.setTestGenerated(buffer.toString().trim());
		assertEquals(getExpected(), getGenerated());
	}

	private void printBaseDefClass(StringBuffer buffer, ClassDefAdapter base) {
		buffer.append(" Base: " + base.getName());
		buffer.append("\n");

	}

	private void printBaseClass(StringBuffer buffer, ClassDefAdapter adapter) {
		buffer.append("# " + adapter.getName());
		for (String name : adapter.getBaseClassNames()) {
			buffer.append(" " + name);
		}
		buffer.append("\n");
	}

	@Override
	public String getExpected() {
		return getResult();
	}

}
