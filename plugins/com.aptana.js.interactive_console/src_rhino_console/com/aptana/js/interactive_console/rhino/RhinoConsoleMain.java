package com.aptana.js.interactive_console.rhino;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcNoSuchHandlerException;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.webserver.WebServer;

public class RhinoConsoleMain {

    /**
     * Should receive 2 parameters: 
     * 
     * port: the port in which the xml-rpc server should be created.
     * 
     * client_port: used if some request needs input from eclipse (where it has its own xml-rpc server awaiting).
     * 
     * Creates a xml-rpc server that listens for the following calls:
     * 
     * addExec
     * getCompletions
     * getDescription
     * close
     * hello
     */
    public static void main(String[] args) throws Exception {
        RhinoConsoleMain console = new RhinoConsoleMain();
        console.startXmlRpcServer(Integer.parseInt(args[0]));
    }

    private RhinoInterpreter rhinoInterpreter;

    public RhinoConsoleMain() {
        this.rhinoInterpreter = new RhinoInterpreter();
    }

    public void startXmlRpcServer(int port) throws IOException {
        WebServer webServer = new WebServer(port);
        XmlRpcServer serverToHandleRawInput = webServer.getXmlRpcServer();

        final Map<String, XmlRpcHandler> nameToHandler = new HashMap<String, XmlRpcHandler>();
        nameToHandler.put("addExec", new AddExecXmlRpcHandler(this));
        nameToHandler.put("getCompletions", new GetCompletionsXmlRpcHandler(this));
        nameToHandler.put("getDescription", new GetDescriptionXmlRpcHandler(this));
        nameToHandler.put("close", new CloseXmlRpcHandler(webServer, this));
        nameToHandler.put("hello", new HelloXmlRpcHandler());

        serverToHandleRawInput.setHandlerMapping(new XmlRpcHandlerMapping() {

            public XmlRpcHandler getHandler(String handlerName) throws XmlRpcNoSuchHandlerException, XmlRpcException {
                XmlRpcHandler handler = nameToHandler.get(handlerName);
                if (handler != null) {
                    return handler;
                }
                throw new XmlRpcNoSuchHandlerException("No handler for method " + handlerName);
            }
        });

        webServer.start();
    }

    public void setErr(OutputStream stream) {
        rhinoInterpreter.setErr(stream);
    }

    public void setOut(OutputStream stream) {
        rhinoInterpreter.setOut(stream);
    }

    public RhinoInterpreter getInterpreter() {
        return this.rhinoInterpreter;
    }
}
