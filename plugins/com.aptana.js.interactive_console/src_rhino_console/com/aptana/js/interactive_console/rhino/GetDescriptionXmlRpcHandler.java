package com.aptana.js.interactive_console.rhino;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;

public class GetDescriptionXmlRpcHandler extends AbstractRhinoXmlRpcHandler {

    public GetDescriptionXmlRpcHandler(RhinoConsoleMain rhinoConsoleMain) {
        super(rhinoConsoleMain);
    }

    public Object execute(XmlRpcRequest request) throws XmlRpcException {
        RhinoInterpreter interpreter = rhinoConsoleMain.getInterpreter();
        if (request.getParameterCount() != 1) {
            throw new XmlRpcException("Expected 1 parameter.");
        }
        Object parameter = request.getParameter(0);
        String evalStr = parameter.toString();
        try {
            return interpreter.getDescription(evalStr);
        } catch (Exception e) {
        }
        return "";
    }

}
