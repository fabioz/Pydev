package com.aptana.js.interactive_console.rhino;

import java.util.List;

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
        try {
            interpreter.eval("var a = function(){");
            fail("Expected error on evaluation (function not finished).");
        } catch (EvaluatorException e) {
        }

        interpreter.eval("var a = [];");
        interpreter.eval("a.push(10);");
        List<Object[]> completions = interpreter.getCompletions("a.", "a.");
        checkFound("length", completions);

        completions = interpreter.getCompletions("something_not_there.", "something_not_there.");
        assertEquals(0, completions.size());

        completions = interpreter.getCompletions("", "");
        checkFound("Array", completions);
    }

    private void checkFound(String expected, List<Object[]> completions) {
        for (Object[] objects : completions) {
            if (objects[0].equals(expected)) {
                return;
            }
        }
        fail("Did not find: " + expected);
    }
}
