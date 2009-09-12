/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.tests.core;

import junit.framework.TestCase;

import org.python.pydev.refactoring.ast.PythonModuleManager;

public abstract class AbstractIOTestCase extends TestCase implements IInputOutputTestCase {
	private String generated;
	protected TestData data;

	public AbstractIOTestCase(String name) {
		this(name, false);
	}

	public AbstractIOTestCase(String name, boolean ignoreEmptyLines) {
		super(name);
	}
	
	@Override
	protected void setUp() throws Exception {
		PythonModuleManager.setTesting(true);
	}
	
	@Override
	protected void tearDown() throws Exception {
		PythonModuleManager.setTesting(false);
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
