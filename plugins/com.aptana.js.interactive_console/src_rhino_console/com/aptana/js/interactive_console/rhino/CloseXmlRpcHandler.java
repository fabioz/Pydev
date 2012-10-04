package com.aptana.js.interactive_console.rhino;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.webserver.WebServer;

import com.aptana.shared_core.log.Log;

/**
 * Stops the xml-rpc server.
 * 
 * @author Fabio Zadrozny
 */
public class CloseXmlRpcHandler extends AbstractRhinoXmlRpcHandler {

    private WebServer webServer;

    public CloseXmlRpcHandler(WebServer webServer, RhinoConsoleMain rhinoConsoleMain) {
        super(rhinoConsoleMain);
        this.webServer = webServer;
    }

    public Object execute(XmlRpcRequest arg0) throws XmlRpcException {
        try {
            this.webServer.shutdown();
        } catch (Exception e) {
            Log.log(e);
        }
        try {
            this.rhinoConsoleMain.getInterpreter().dispose();
        } catch (Exception e) {
            Log.log(e);
        }
        return "Closed";
    }

}
