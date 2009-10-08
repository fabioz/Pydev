/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.tests.core;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.REF;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.refactoring.ast.PythonModuleManager;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.tests.adapter.PythonNatureStub;

public abstract class AbstractIOTestCase extends TestCase implements IInputOutputTestCase {
	private String generated;
	protected TestData data;
	protected CodeCompletionTestsBase codeCompletionTestsBase = new CodeCompletionTestsBase();

    protected ModuleAdapter createModuleAdapterFromDataSource() throws Exception {
        codeCompletionTestsBase.restorePythonPath(
                REF.getFileAbsolutePath(data.file.getParentFile()), 
                true);
        ModuleAdapter module = VisitorFactory.createModuleAdapter(
                new PythonModuleManager(CodeCompletionTestsBase.nature),
                data.file, new Document(data.source), CodeCompletionTestsBase.nature);
        return module;
    }
    


	public AbstractIOTestCase(String name) {
		this(name, false);
	}

	public AbstractIOTestCase(String name, boolean ignoreEmptyLines) {
		super(name);
	}
	

    protected void assertContentsEqual(String expected, String generated) {
        assertEquals(StringUtils.replaceNewLines(expected, "\n"), StringUtils.replaceNewLines(generated, "\n"));
    }
	
	@Override
	protected void setUp() throws Exception {
		PythonModuleManager.setTesting(true);
        codeCompletionTestsBase.setUp();
	}
	
	@Override
	protected void tearDown() throws Exception {
		PythonModuleManager.setTesting(false);
		codeCompletionTestsBase.tearDown();
	}
	
	protected String getGenerated() {
		return generated.trim();
	}

	public void setTestGenerated(String source) {
		this.generated = source;
	}
	
	public void setData(TestData data) {
		this.data = data;
	}
	
	public String getExpected() {
		return data.result;
	}
}
