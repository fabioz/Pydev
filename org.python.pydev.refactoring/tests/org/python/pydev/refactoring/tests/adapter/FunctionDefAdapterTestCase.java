package org.python.pydev.refactoring.tests.adapter;

import java.util.List;

import org.eclipse.jface.text.Document;
import org.python.pydev.refactoring.ast.adapters.ClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.tests.core.AbstractIOTestCase;

public class FunctionDefAdapterTestCase extends AbstractIOTestCase {

	public FunctionDefAdapterTestCase(String name) {
		super(name);
	}

	@Override
	public void runTest() throws Throwable {
		StringBuffer buffer = new StringBuffer();
		ModuleAdapter module = VisitorFactory.createModuleAdapter(null, null, new Document(
				getSource()));
		List<ClassDefAdapter> classes = module.getClasses();
		assertTrue(classes.size() > 0);

		buffer
				.append("# ClassName FunctionName hasArg hasVarArg hasKwArg ArgumentsOnly\n");
		for (ClassDefAdapter adapter : module.getClasses()) {
			printFunction(buffer, adapter);
		}

		this.setTestGenerated(buffer.toString().trim());
		assertEquals(getExpected(), getGenerated());
	}

	private void printFunction(StringBuffer buffer, ClassDefAdapter adapter) {
		final String DOUBLETAB = " ";
		buffer.append("# " + adapter.getName() + "\n");
		for (FunctionDefAdapter function : adapter.getFunctions()) {
			buffer.append("# ");
			buffer.append(function.getName() + DOUBLETAB
					+ function.getArguments().hasArg() + DOUBLETAB
					+ function.getArguments().hasVarArg() + DOUBLETAB
					+ function.getArguments().hasKwArg() + DOUBLETAB
					+ function.getArguments().getArgOnly());
			buffer.append("\n");
		}
		buffer.append("\n");
	}

	@Override
	public String getExpected() {
		return getResult();
	}

}
