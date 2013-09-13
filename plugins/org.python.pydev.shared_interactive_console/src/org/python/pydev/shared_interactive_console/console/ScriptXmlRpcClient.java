/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_interactive_console.console;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.AsyncCallback;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.python.pydev.shared_core.io.ThreadStreamReader;
import org.python.pydev.shared_core.net.LocalHost;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * Subclass of XmlRpcClient that will monitor the process so that if the process is destroyed, we stop waiting 
 * for messages from it.
 *
 * @author Fabio
 */
public class ScriptXmlRpcClient implements IXmlRpcClient {

    /**
     * Internal xml-rpc client (responsible for the actual communication with the server)
     */
    private XmlRpcClient impl;

    /**
     * The process where the server is being executed.
     */
    private Process process;

    /**
     * This is the thread that's reading the error stream from the process.
     */
    private ThreadStreamReader stdErrReader;

    /**
     * This is the thread that's reading the output stream from the process.
     */
    private ThreadStreamReader stdOutReader;

    /**
     * Constructor (see fields description)
     */
    public ScriptXmlRpcClient(Process process, ThreadStreamReader stdErrReader, ThreadStreamReader stdOutReader) {
        this.impl = new XmlRpcClient();
        this.process = process;
        this.stdErrReader = stdErrReader;
        this.stdOutReader = stdOutReader;
    }

    /**
     * Sets the port where the server is started.
     * @throws MalformedURLException 
     */
    public void setPort(int port) throws MalformedURLException {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL("http://" + LocalHost.getLocalHost() + ":" + port));

        this.impl.setConfig(config);
    }

    /**
     * Executes a command in the server. 
     * 
     * Within this method, we should be careful about being able to return if the server dies.
     * If we wanted to have a timeout, this would be the place to add it.
     * 
     * @return the result from executing the given command in the server.
     */
    public Object execute(String command, Object[] args) throws XmlRpcException {
        final Object[] result = new Object[] { null };

        //make an async call so that we can keep track of not actually having an answer.
        this.impl.executeAsync(command, args, new AsyncCallback() {

            public void handleError(XmlRpcRequest request, Throwable error) {
                result[0] = new Object[] { error.getMessage() };
            }

            public void handleResult(XmlRpcRequest request, Object receivedResult) {
                result[0] = receivedResult;
            }
        });

        //busy loop waiting for the answer (or having the console die).
        while (result[0] == null) {
            try {
                if (process != null) {
                    final String errStream = stdErrReader.getContents();
                    if (errStream.indexOf("sys.exit called. Interactive console finishing.") != -1) {
                        result[0] = new Object[] { errStream };
                        break;
                    }

                    int exitValue = process.exitValue();
                    result[0] = new Object[] { StringUtils.format(
                            "Console already exited with value: %s while waiting for an answer.\n" + "Error stream: "
                                    + errStream + "\n" + "Output stream: " + stdOutReader.getContents(), exitValue) };

                    //ok, we have an exit value!
                    break;
                }
            } catch (IllegalThreadStateException e) {
                //that's ok... let's sleep a bit
                synchronized (this) {
                    try {
                        wait(10);
                    } catch (InterruptedException e1) {
                        //                        Log.log(e1);
                    }
                }
            }
        }
        return result[0];
    }

}
