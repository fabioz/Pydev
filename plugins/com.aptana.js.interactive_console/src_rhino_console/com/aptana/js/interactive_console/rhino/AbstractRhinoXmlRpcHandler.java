package com.aptana.js.interactive_console.rhino;

import org.apache.xmlrpc.XmlRpcHandler;

public abstract class AbstractRhinoXmlRpcHandler implements XmlRpcHandler {

    protected RhinoConsoleMain rhinoConsoleMain;

    public AbstractRhinoXmlRpcHandler(RhinoConsoleMain rhinoConsoleMain) {
        this.rhinoConsoleMain = rhinoConsoleMain;
    }

}
