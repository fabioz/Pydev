package com.aptana.js.interactive_console.rhino;

import java.io.PrintStream;

import junit.framework.TestCase;

import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Undefined;

import com.aptana.shared_core.io.ExtendedByteArrayOutputStream;

public class RhinoInterpreterTest extends TestCase {

    public void testRhinoInterpreter() throws Exception {
        RhinoInterpreter interpreter = new RhinoInterpreter();
        Object o;
        o = interpreter.eval("var a = 10;");
        assertEquals(Undefined.instance, o);
        o = interpreter.eval("a");
        assertEquals(10, o);

        ExtendedByteArrayOutputStream out = new ExtendedByteArrayOutputStream();
        interpreter.setOut(out);
        interpreter.setErr(out);
        o = interpreter.eval("print(a)");
        assertEquals("10", out.readAndDelete().trim());
        PrintStream initialOut = System.out;
        try {
            System.setOut(new PrintStream(out));
            o = interpreter.eval("java.lang.System.out.print(a)");
            assertEquals("10.0", out.readAndDelete().trim());
        } catch (Exception e) {
            System.setOut(initialOut);
        }
        try {
            interpreter.eval("var a = function(){");
            fail("Expected error on evaluation (function not finished).");
        } catch (EvaluatorException e) {
        }
    }
}
