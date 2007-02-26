package org.python.pydev.refactoring.tests.jython;

import java.io.StringWriter;

import org.python.pydev.refactoring.tests.core.AbstractRewriterTestCase;
import org.python.util.PythonInterpreter;

/**
 * @author Dennis Hunziker, Ueli Kistler
 */
public class JythonTestCase extends AbstractRewriterTestCase {

	public JythonTestCase(String name) {
		super(name);
	}

	private String interpretedGenerated;

	private String interpretedExcepted;

	@Override
	public void runTest() throws Throwable {
		super.runRewriter();
		setInterpretedExcepted(execPythonCode(getExpected()));
		setInterpretedGenerated(execPythonCode(getGenerated()));

		assertEquals(interpretedExcepted, interpretedGenerated);
	}

	private PythonInterpreter initInterpreter() {
		PythonInterpreter pi = new PythonInterpreter();
		pi.set("False", 0);
		pi.set("True", 1);
		return pi;
	}

	private String execPythonCode(String source) {
		PythonInterpreter pi = initInterpreter();

		StringWriter out = new StringWriter();
		pi.setOut(out);

		pi.exec(source);

		return out.getBuffer().toString();
	}

	public void setInterpretedExcepted(String interpretedExcepted) {
		this.interpretedExcepted = interpretedExcepted;
	}

	public void setInterpretedGenerated(String interpretedGenerated) {
		this.interpretedGenerated = interpretedGenerated;
	}
}
