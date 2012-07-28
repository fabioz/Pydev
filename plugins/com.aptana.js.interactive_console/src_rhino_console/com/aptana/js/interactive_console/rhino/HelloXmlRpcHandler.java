package com.aptana.js.interactive_console.rhino;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.XmlRpcRequest;

/**
 * Used to check if the server is alive.
 * 
 * @author Fabio Zadrozny
 */
public class HelloXmlRpcHandler implements XmlRpcHandler {

    public Object execute(XmlRpcRequest arg0) throws XmlRpcException {
        return "Hello eclipse";
    }

}
