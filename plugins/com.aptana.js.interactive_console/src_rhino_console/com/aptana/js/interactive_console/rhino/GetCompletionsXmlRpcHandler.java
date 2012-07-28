package com.aptana.js.interactive_console.rhino;

import java.util.ArrayList;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;

public class GetCompletionsXmlRpcHandler extends AbstractRhinoXmlRpcHandler {

    public GetCompletionsXmlRpcHandler(RhinoConsoleMain rhinoConsoleMain) {
        super(rhinoConsoleMain);
    }

    public Object execute(XmlRpcRequest arg0) throws XmlRpcException {
        return new ArrayList<Object>();
    }

}
