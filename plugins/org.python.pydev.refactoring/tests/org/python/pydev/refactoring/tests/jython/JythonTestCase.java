/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.jython;

import java.io.StringWriter;

import org.python.pydev.jython.IPythonInterpreter;
import org.python.pydev.jython.JythonPlugin;
import org.python.pydev.refactoring.tests.core.AbstractRewriterTestCase;

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

    private String execPythonCode(String source) {
        IPythonInterpreter pi = JythonPlugin.newPythonInterpreter(false);

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
