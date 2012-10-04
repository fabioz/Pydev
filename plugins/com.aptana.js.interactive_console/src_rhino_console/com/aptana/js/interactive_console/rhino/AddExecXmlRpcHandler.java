package com.aptana.js.interactive_console.rhino;

import java.io.PrintStream;
import java.util.ArrayList;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Undefined;

import com.aptana.shared_core.string.StringUtils;

public class AddExecXmlRpcHandler extends AbstractRhinoXmlRpcHandler {

    private ArrayList<String> history;

    public AddExecXmlRpcHandler(RhinoConsoleMain rhinoConsoleMain) {
        super(rhinoConsoleMain);
        history = new ArrayList<String>();
    }

    /**
     * @return if more input is needed to complete the statement.
     */
    public Object execute(XmlRpcRequest request) throws XmlRpcException {
        boolean more = false;
        RhinoInterpreter interpreter = rhinoConsoleMain.getInterpreter();
        if (request.getParameterCount() != 1) {
            throw new XmlRpcException("Expected 1 parameter.");
        }
        Object parameter = request.getParameter(0);
        String evalStr = parameter.toString();
        history.add(evalStr);
        try {
            Object eval = interpreter.eval(StringUtils.join("\n", history));
            if (!(eval instanceof Undefined)) {
                PrintStream out = rhinoConsoleMain.getInterpreter().getOut();
                out.println(eval);
            }
            history.clear();
            more = false;
        } catch (EvaluatorException e) {
            more = true;
        } catch (Exception e) {
            history.clear();
            more = false;
            PrintStream err = rhinoConsoleMain.getInterpreter().getErr();
            err.println(e.getMessage());
        }
        return more;
    }

}
