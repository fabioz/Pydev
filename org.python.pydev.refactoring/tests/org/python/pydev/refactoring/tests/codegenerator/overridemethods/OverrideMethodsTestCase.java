/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.codegenerator.overridemethods;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.codegenerator.overridemethods.edit.MethodEdit;
import org.python.pydev.refactoring.tests.adapter.PythonNatureStub;
import org.python.pydev.refactoring.tests.core.AbstractIOTestCase;

import com.thoughtworks.xstream.XStream;

public class OverrideMethodsTestCase extends AbstractIOTestCase {

	public OverrideMethodsTestCase(String name) {
		super(name);
	}

	@Override
	public void runTest() throws Throwable {
		MockupOverrideMethodsConfig config = initConfig();

		MockupOverrideMethodsRequestProcessor requestProcessor = setupRequestProcessor(config);

		IDocument refactoringDoc = applyOverrideMethod(requestProcessor);

		this.setTestGenerated(refactoringDoc.get());
		assertEquals(getExpected(), getGenerated());
	}

	private IDocument applyOverrideMethod(MockupOverrideMethodsRequestProcessor requestProcessor) throws BadLocationException {
		MethodEdit methodEdit = new MethodEdit(requestProcessor.getRefactoringRequests().get(0));

		IDocument refactoringDoc = new Document(getSource());
		methodEdit.getEdit().apply(refactoringDoc);
		return refactoringDoc;
	}

	private MockupOverrideMethodsRequestProcessor setupRequestProcessor(MockupOverrideMethodsConfig config) throws Throwable {
		ModuleAdapter module = VisitorFactory.createModuleAdapter(null, null, new Document(getSource()), new PythonNatureStub());
		List<IClassDefAdapter> classes = module.getClasses();
		assertTrue(classes.size() > 0);

		MockupOverrideMethodsRequestProcessor requestProcessor = new MockupOverrideMethodsRequestProcessor(module, config);
		return requestProcessor;
	}

	private MockupOverrideMethodsConfig initConfig() {
		MockupOverrideMethodsConfig config = null;
		XStream xstream = new XStream();
		xstream.alias("config", MockupOverrideMethodsConfig.class);

		if (getConfig().length() > 0) {
			config = (MockupOverrideMethodsConfig) xstream.fromXML(getConfig());
		} else {
			fail("Could not unserialize configuration");
		}
		return config;
	}

	@Override
	public String getExpected() {
		return getResult();
	}

}
