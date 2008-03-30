package org.python.pydev.debug.newconsole;

import java.net.MalformedURLException;

import org.apache.xmlrpc.XmlRpcException;

/**
 * Interface that determines what's needed from the xml-rpc server.
 *
 * @author Fabio
 */
public interface IPydevXmlRpcClient {

    /**
     * Sets the port which the server is expecting to communicate. 
     * @param port port where the server was started.
     * 
     * @throws MalformedURLException
     */
    void setPort(int port) throws MalformedURLException;

    /**
     * @param command the command to be executed in the server
     * @param args the arguments passed to the command
     * @return the result from executing the command
     * 
     * @throws XmlRpcException
     */
    Object execute(String command, Object[] args) throws XmlRpcException;

}
