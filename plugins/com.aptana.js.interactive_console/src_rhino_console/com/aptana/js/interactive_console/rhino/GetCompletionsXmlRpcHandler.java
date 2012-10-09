package com.aptana.js.interactive_console.rhino;

import java.util.ArrayList;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;

public class GetCompletionsXmlRpcHandler extends AbstractRhinoXmlRpcHandler {

    public GetCompletionsXmlRpcHandler(RhinoConsoleMain rhinoConsoleMain) {
        super(rhinoConsoleMain);
    }

    public Object execute(XmlRpcRequest request) throws XmlRpcException {
        RhinoInterpreter interpreter = rhinoConsoleMain.getInterpreter();
        if (request.getParameterCount() != 2) {
            throw new XmlRpcException("Expected 2 parameters.");
        }
        String text = request.getParameter(0).toString();
        String actTok = request.getParameter(1).toString();
        try {
            return interpreter.getCompletions(text, actTok);
        } catch (Exception e) {
        }
        return new ArrayList<Object>();
    }

}
