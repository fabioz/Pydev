/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.codecoverage;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import junit.framework.TestCase;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcNoSuchHandlerException;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.webserver.WebServer;
import org.python.pydev.core.TestDependent;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.io.ThreadStreamReader;
import org.python.pydev.shared_core.net.SocketUtil;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_interactive_console.console.IXmlRpcClient;
import org.python.pydev.shared_interactive_console.console.ScriptXmlRpcClient;

public class XmlRpcTest extends TestCase {

    String[] EXPECTED = new String[] { "false", "false", "10", "false", "false", "false", "false", "true", "false",
            "true", "false", "true", "false", "20", "30", "false", "false", "false", "false",

            "false", "false", "start get completions", "Foo", "1", "foo", "3|4", "end get completions",
            "start raw_input", "'input_request'", "false", "false", "finish raw_input", "'foo'", "false", "false",
            "Console already exited with value: 0 while waiting for an answer.|exceptions.SystemExit:0", };

    private int next = -1;

    private ThreadStreamReader err;

    private ThreadStreamReader out;

    private WebServer webServer;

    public static void main(String[] args) throws MalformedURLException, XmlRpcException {
        try {
            XmlRpcTest xmlRpcTest = new XmlRpcTest();
            xmlRpcTest.setUp();
            xmlRpcTest.testXmlRpcServerJython();
            xmlRpcTest.tearDown();
            junit.textui.TestRunner.run(XmlRpcTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (this.webServer != null) {
            this.webServer.shutdown();
        }
    }

    private Process startServer(int client_port, int port, boolean python) throws IOException {
        File f = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC + "pysrc/pydevconsole.py");

        String[] cmdLine;
        if (python) {
            cmdLine = new String[] { TestDependent.PYTHON_EXE, "-u", FileUtils.getFileAbsolutePath(f), "" + port,
                    "" + client_port };
        } else {
            cmdLine = new String[] { TestDependent.JAVA_LOCATION, "-classpath", TestDependent.JYTHON_JAR_LOCATION,
                    "org.python.util.jython", FileUtils.getFileAbsolutePath(f), "" + port, "" + client_port };
        }

        Process process = Runtime.getRuntime().exec(cmdLine);
        err = new ThreadStreamReader(process.getErrorStream());
        out = new ThreadStreamReader(process.getInputStream());
        err.start();
        out.start();

        this.webServer = new WebServer(client_port);
        XmlRpcServer serverToHandleRawInput = this.webServer.getXmlRpcServer();
        serverToHandleRawInput.setHandlerMapping(new XmlRpcHandlerMapping() {

            public XmlRpcHandler getHandler(String handlerName) throws XmlRpcNoSuchHandlerException, XmlRpcException {
                return new XmlRpcHandler() {

                    public Object execute(XmlRpcRequest request) throws XmlRpcException {
                        return "input_request";
                    }
                };
            }
        });

        this.webServer.start();
        return process;
    }

    public void testXmlRpcServerPython() throws XmlRpcException, IOException, InterruptedException {
        // XmlRpcTest fails because of "PyDev console: using default backend (IPython not available)."
        // being printed out. I (Jonah Graham) have some further plans related to this code so
        // plan on deferring a fix for this for now.
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        checkServer(true);
    }

    public void testXmlRpcServerJython() throws XmlRpcException, IOException, InterruptedException {
        // XmlRpcTest fails because of "PyDev console: using default backend (IPython not available)."
        // being printed out. I (Jonah Graham) have some further plans related to this code so
        // plan on deferring a fix for this for now.
        // In addition, the start-up delay for Jython is sometimes insufficient (i.e. when on a VM
        // like on travis) leading to a transient failure. It would be better to do something
        // like the "hello" in PydevConsoleCommunication with a long worst-case timeout
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        checkServer(false);
    }

    public void checkServer(boolean python) throws XmlRpcException, IOException, InterruptedException {
        Integer[] ports = SocketUtil.findUnusedLocalPorts(2);
        int port = ports[0];
        int clientPort = ports[1];

        Process process = startServer(clientPort, port, python);

        //        int port = 8000;
        //        Process process = null;

        //give some time for the process to start
        if (!python) {
            synchronized (this) {
                this.wait(2500);
            }
        } else {
            synchronized (this) {
                this.wait(500);
            }
        }

        try {
            int exitValue = process.exitValue();
            fail("Already exited with val: " + exitValue);
        } catch (IllegalThreadStateException e) {
            //that's ok
        }

        try {
            IXmlRpcClient client = new ScriptXmlRpcClient(process);
            client.setPort(port);

            printArr(client.execute("execLine", new Object[] { "abc = 10" }));
            printArr(client.execute("execLine", new Object[] { "abc" }));
            printArr(client.execute("execLine", new Object[] { "import sys" }));
            printArr(client.execute("execLine", new Object[] { "class Foo:" }));
            printArr(client.execute("execLine", new Object[] { "    print 20" }));
            printArr(client.execute("execLine", new Object[] { "    print >> sys.stderr, 30" }));
            printArr(client.execute("execLine", new Object[] { "" }));
            printArr(client.execute("execLine", new Object[] { "foo=Foo()" }));
            printArr(client.execute("execLine", new Object[] { "foo.__doc__=None" }));
            printArr("start get completions");
            Object[] completions = (Object[]) client.execute("getCompletions", new Object[] { "fo" });
            //the completions may come in any order, we must sort it for the test and remove things we don't expect.
            Arrays.sort(completions, new Comparator<Object>() {
                public int compare(Object o1, Object o2) {
                    String s1 = (String) ((Object[]) o1)[0];
                    String s2 = (String) ((Object[]) o2)[0];
                    return s1.compareTo(s2);
                }
            });
            ArrayList<Object> arrayList = new ArrayList<Object>();
            for (Object o : completions) {
                Object[] found = (Object[]) o;
                if (found[0].equals("foo") || found[0].equals("Foo")) {
                    arrayList.add(found);
                }
            }

            printArr(arrayList.toArray());
            printArr("end get completions");

            printArr("start raw_input");
            printArr(client.execute("execLine", new Object[] { "raw_input()" }));
            printArr("finish raw_input");
            printArr(client.execute("execLine", new Object[] { "'foo'" }));
            //            System.out.println("Ask exit");
            printArr(client.execute("execLine", new Object[] { "sys.exit(0)" }));
            //            System.out.println("End Ask exit");
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        assertEquals(next, EXPECTED.length - 1);
    }

    private void printArr(Object... execute) {
        if (this.out != null) {
            print(this.out.getAndClearContents());
            print(this.err.getAndClearContents());
        }

        for (Object o : execute) {
            print(o);
        }
    }

    private void print(Object execute) {
        if (execute instanceof Object[]) {
            Object[] objects = (Object[]) execute;
            for (Object o : objects) {
                print(o);
            }
        } else {
            String s = "" + execute;
            if (s.length() > 0) {
                String expected = EXPECTED[nextExpected()].trim();
                String found = s.trim();
                if (!expected.equals(found)) {
                    if (expected.equals("false")) {
                        expected = "0";
                    }
                    if (expected.equals("true")) {
                        expected = "1";
                    }
                    if (expected.equals("3|4")) {
                        if (found.equals("3") || found.equals("4")) {
                            return;
                        }
                    }
                    if (expected
                            .equals("Console already exited with value: 0 while waiting for an answer.|exceptions.SystemExit:0")) {
                        if ((found.indexOf("Console already exited with value: 0 while waiting for an answer.") != -1)
                                || (found.indexOf("exceptions.SystemExit:0") != -1)
                                || (found.indexOf("Failed to create input stream: Connection refused") != -1)) {
                            return;
                        }
                    }
                    String errorMessage = StringUtils.format(
                            "Expected: >>%s<< and not: >>%s<< (position:%s)",
                            expected, found, next);
                    assertEquals(errorMessage, expected, found);
                }
            }
        }
    }

    private int nextExpected() {
        next += 1;
        return next;
    }

}
