package org.python.pydev.refactoring.tests.core;

import junit.framework.TestCase;

/**
 * @author Dennis Hunziker, Ueli Kistler
 */
public abstract class AbstractIOTestCase extends TestCase implements IInputOutputTestCase {

	private static final String EMPTY = "";

	private StringBuffer sourceLines = null;

	private StringBuffer resultLines = null;

	private StringBuffer configLines;

	private String generated;

	public AbstractIOTestCase(String name) {
		this(name, false);
	}

	public AbstractIOTestCase(String name, boolean ignoreEmptyLines) {
		super(name);
		sourceLines = new StringBuffer();
		resultLines = new StringBuffer();
		configLines = new StringBuffer();
	}

	public String getExpected() {
		return EMPTY.equals(getResult()) ? getSource() : getResult();
	}

	public String getResult() {
		return getResultLines().toString().trim();
	}

	public String getSource() {
		return getSourceLines().toString().trim();
	}

	public void setSource(String line) {
		sourceLines.append(line);
	}

	public void setResult(String line) {
		resultLines.append(line);
	}

	public void setConfig(String line) {
		configLines.append(line);
	}

	private StringBuffer getResultLines() {
		return resultLines;
	}

	protected String getGenerated() {
		return generated.trim();
	}

	protected String getConfig() {
		return configLines.toString().trim();
	}

	private StringBuffer getSourceLines() {
		return sourceLines;
	}

	public void setTestGenerated(String source) {
		this.generated = source;
	}
}
