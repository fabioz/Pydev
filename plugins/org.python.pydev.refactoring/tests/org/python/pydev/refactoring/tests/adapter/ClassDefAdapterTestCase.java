/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.adapter;

import java.util.List;

import org.eclipse.jface.text.Document;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
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
		ModuleAdapter module = VisitorFactory.createModuleAdapter(null, null, new Document(data.source), new PythonNatureStub());
		List<IClassDefAdapter> classes = module.getClasses();
		assertTrue(classes.size() > 0);

		for (IClassDefAdapter adapter : module.getClasses()) {
			printBaseClass(buffer, adapter);
			for (IClassDefAdapter base : adapter.getBaseClasses()) {
				buffer.append("## " + adapter.getName());
				printBaseDefClass(buffer, base);
			}

		}

		String generated = (buffer.toString().trim());
		assertEquals(data.result, generated);
	}

	private void printBaseDefClass(StringBuffer buffer, IClassDefAdapter base) {
		buffer.append(" Base: " + base.getName());
		buffer.append("\n");

	}

	private void printBaseClass(StringBuffer buffer, IClassDefAdapter adapter) {
		buffer.append("# " + adapter.getName());
		for (String name : adapter.getBaseClassNames()) {
			buffer.append(" " + name);
		}
		buffer.append("\n");
	}
}
