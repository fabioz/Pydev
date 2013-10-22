/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcNoSuchHandlerException;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.webserver.WebServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IToken;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.env.UserCanceledException;
import org.python.pydev.debug.newconsole.prefs.InteractiveConsolePrefs;
import org.python.pydev.editor.codecompletion.AbstractPyCodeCompletion;
import org.python.pydev.editor.codecompletion.PyCalltipsContextInformation;
import org.python.pydev.editor.codecompletion.PyCodeCompletionImages;
import org.python.pydev.editor.codecompletion.PyLinkedModeCompletionProposal;
import org.python.pydev.editorinput.PyOpenEditor;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.ThreadStreamReader;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_interactive_console.console.IScriptConsoleCommunication;
import org.python.pydev.shared_interactive_console.console.IXmlRpcClient;
import org.python.pydev.shared_interactive_console.console.InterpreterResponse;
import org.python.pydev.shared_interactive_console.console.ScriptXmlRpcClient;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal;
import org.python.pydev.shared_ui.proposals.PyCompletionProposal;
import org.python.pydev.shared_ui.utils.RunInUiThread;

/**
 * Communication with Xml-rpc with the client.
 *
 * After creating the comms, a successful {@link #hello(IProgressMonitor)} message must be sent before using other methods.
 *
 * @author Fabio
 */
public class PydevConsoleCommunication implements IScriptConsoleCommunication, XmlRpcHandler {

    /**
     * XML-RPC client for sending messages to the server.
     */
    private IXmlRpcClient client;

    /**
     * Responsible for getting the stdout of the process.
     */
    private final ThreadStreamReader stdOutReader;

    /**
     * Responsible for getting the stderr of the process.
     */
    private final ThreadStreamReader stdErrReader;

    /**
     * This is the server responsible for giving input to a raw_input() requested
     * and for opening editors (as a result of %edit in IPython)
     */
    private WebServer webServer;

    /**
     * Initializes the xml-rpc communication.
     *
     * @param port the port where the communication should happen.
     * @param process this is the process that was spawned (server for the XML-RPC)
     *
     * @throws MalformedURLException
     */
    public PydevConsoleCommunication(int port, Process process, int clientPort) throws Exception {
        stdOutReader = new ThreadStreamReader(process.getInputStream());
        stdErrReader = new ThreadStreamReader(process.getErrorStream());
        stdOutReader.start();
        stdErrReader.start();

        //start the server that'll handle input requests
        this.webServer = new WebServer(clientPort);
        XmlRpcServer serverToHandleRawInput = this.webServer.getXmlRpcServer();
        serverToHandleRawInput.setHandlerMapping(new XmlRpcHandlerMapping() {

            public XmlRpcHandler getHandler(String handlerName) throws XmlRpcNoSuchHandlerException, XmlRpcException {
                return PydevConsoleCommunication.this;
            }
        });

        this.webServer.start();

        IXmlRpcClient client = new ScriptXmlRpcClient(process, stdErrReader, stdOutReader);
        client.setPort(port);

        this.client = client;
    }

    /**
     * Stops the communication with the client (passes message for it to quit).
     */
    public void close() throws Exception {
        if (this.client != null) {
            Job job = new Job("Close console communication") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        PydevConsoleCommunication.this.client.execute("close", new Object[0]);
                    } catch (Exception e) {
                        //Ok, we can ignore this one on close.
                    }
                    PydevConsoleCommunication.this.client = null;
                    return Status.OK_STATUS;
                }
            };
            job.schedule(); //finish it
        }

        if (this.webServer != null) {
            this.webServer.shutdown();
            this.webServer = null;
        }
    }

    /**
     * Variables that control when we're expecting to give some input to the server or when we're
     * adding some line to be executed
     */

    /**
     * Signals that the next command added should be sent as an input to the server.
     */
    private volatile boolean waitingForInput;

    /**
     * Input that should be sent to the server (waiting for raw_input)
     */
    private volatile String inputReceived;

    /**
     * Response that should be sent back to the shell.
     */
    private volatile InterpreterResponse nextResponse;

    /**
     * Helper to keep on busy loop.
     */
    private volatile Object lock = new Object();

    /**
     * Helper to keep on busy loop.
     */
    private volatile Object lock2 = new Object();

    /**
     * Keeps a flag indicating that we were able to communicate successfully with the shell at least once
     * (if we haven't we may retry more than once the first time, as jython can take a while to initialize
     * the communication)
     * This is set to true on successful {@link #hello(IProgressMonitor)}
     */
    private volatile boolean firstCommWorked = false;

    /**
     * When non-null, the Debug Target to notify when the underlying process is suspended or running.
     */
    private IPydevConsoleDebugTarget debugTarget = null;

    /**
     * Called when the server is requesting some input from this class.
     */
    public Object execute(XmlRpcRequest request) throws XmlRpcException {
        String methodName = request.getMethodName();
        if ("RequestInput".equals(methodName)) {
            return requestInput();
        } else if ("OpenEditor".equals(methodName)) {
            return openEditor(request);
        }
        Log.log("Unexpected call to execute for method name: " + methodName);
        return null;
    }

    private Object openEditor(XmlRpcRequest request) {
        try {

            String filename = request.getParameter(0).toString();
            final int lineNumber = Integer.parseInt(request.getParameter(1).toString());
            final File fileToOpen = new File(filename);

            if (!fileToOpen.exists()) {
                final OutputStream out = new FileOutputStream(fileToOpen);
                try {
                    out.close();
                } catch (final IOException ioe) {
                    // ignore
                }
            }

            RunInUiThread.async(new Runnable() {

                public void run() {
                    IEditorPart editor = PyOpenEditor.doOpenEditorOnFileStore(fileToOpen);
                    if (editor instanceof ITextEditor && lineNumber >= 0) {
                        EditorUtils.showInEditor((ITextEditor) editor, lineNumber);
                    }
                }
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Object requestInput() {
        waitingForInput = true;
        inputReceived = null;
        boolean needInput = true;

        String stdOutContents = stdOutReader.getAndClearContents();
        String stderrContents = stdErrReader.getAndClearContents();
        //let the busy loop from execInterpreter free and enter a busy loop
        //in this function until execInterpreter gives us an input
        setNextResponse(new InterpreterResponse(stdOutContents, stderrContents, false, needInput));

        //busy loop until we have an input
        while (inputReceived == null) {
            synchronized (lock) {
                try {
                    lock.wait(10);
                } catch (InterruptedException e) {
                    Log.log(e);
                }
            }
        }
        return inputReceived;
    }

    /**
     * Executes a given line in the interpreter.
     *
     * @param command the command to be executed in the client
     */
    public void execInterpreter(final String command, final ICallback<Object, InterpreterResponse> onResponseReceived,
            final ICallback<Object, Tuple<String, String>> onContentsReceived) {
        setNextResponse(null);
        if (waitingForInput) {
            inputReceived = command;
            waitingForInput = false;
            //the thread that we started in the last exec is still alive if we were waiting for an input.
        } else {
            //create a thread that'll keep locked until an answer is received from the server.
            Job job = new Job("PyDev Console Communication") {

                /**
                 * Executes the needed command
                 *
                 * @return a tuple with (null, more) or (error, false)
                 *
                 * @throws XmlRpcException
                 */
                private Tuple<String, Boolean> exec() throws XmlRpcException {

                    if (client == null) {
                        return new Tuple<String, Boolean>(
                                "PydevConsoleCommunication.client is null (cannot communicate with server).", false);
                    }

                    Object[] execute = (Object[]) client.execute("addExec", new Object[] { command });

                    Object object = execute[0];
                    boolean more;

                    String errorContents = null;
                    if (object instanceof Boolean) {
                        more = (Boolean) object;

                    } else {
                        String str = object.toString();

                        String lower = str.toLowerCase();
                        if (lower.equals("true") || lower.equals("1")) {
                            more = true;
                        } else if (lower.equals("false") || lower.equals("0")) {
                            more = false;
                        } else {
                            more = false;
                            errorContents = str;
                        }
                    }
                    return new Tuple<String, Boolean>(errorContents, more);
                }

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    final boolean needInput = false;
                    try {
                        if (!firstCommWorked) {
                            throw new Exception("hello must be called successfully before execInterpreter can be used.");
                        }

                        Tuple<String, Boolean> executed = exec();
                        String errorContents = executed.o1;
                        boolean more = executed.o2;

                        String stdOutContents;
                        if (errorContents == null) {
                            errorContents = stdErrReader.getAndClearContents();
                        } else {
                            errorContents += "\n" + stdErrReader.getAndClearContents();
                        }
                        stdOutContents = stdOutReader.getAndClearContents();
                        setNextResponse(new InterpreterResponse(stdOutContents, errorContents, more, needInput));

                    } catch (Exception e) {
                        Log.log(e);
                        setNextResponse(new InterpreterResponse("", "Exception while pushing line to console:"
                                + e.getMessage(), false, needInput));
                    }
                    return Status.OK_STATUS;
                }
            };

            job.schedule();

        }

        int i = 500; //only get contents each 500 millis...

        //busy loop until we have a response
        while (nextResponse == null) {
            synchronized (lock2) {
                try {
                    lock2.wait(20);
                } catch (InterruptedException e) {
                    //                    Log.log(e);
                }
            }

            i -= 20;

            if (i <= 0 && nextResponse == null) {
                i = 250; //after the first, get it each 250 millis
                String stderrContents = stdErrReader.getAndClearContents();
                String stdOutContents = stdOutReader.getAndClearContents();
                if (stdOutContents.length() > 0 || stderrContents.length() > 0) {
                    onContentsReceived.call(new Tuple<String, String>(stdOutContents, stderrContents));
                }
            }
        }
        onResponseReceived.call(nextResponse);
    }

    /**
     * @return completions from the client
     */
    public ICompletionProposal[] getCompletions(String text, String actTok, int offset) throws Exception {
        if (waitingForInput) {
            return new ICompletionProposal[0];
        }
        Object fromServer = client.execute("getCompletions", new Object[] { text, actTok });
        List<ICompletionProposal> ret = new ArrayList<ICompletionProposal>();

        convertToICompletions(text, actTok, offset, fromServer, ret);
        ICompletionProposal[] proposals = ret.toArray(new ICompletionProposal[ret.size()]);
        return proposals;
    }

    public static void convertToICompletions(String text, String actTok, int offset, Object fromServer,
            List<ICompletionProposal> ret) {
        if (fromServer instanceof Object[]) {
            Object[] objects = (Object[]) fromServer;
            fromServer = Arrays.asList(objects);
        }
        if (fromServer instanceof List) {
            int length = actTok.lastIndexOf('.');
            if (length == -1) {
                length = actTok.length();
            } else {
                length = actTok.length() - length - 1;
            }

            List comps = (List) fromServer;
            for (Object o : comps) {
                if (o instanceof Object[]) {
                    //name, doc, args, type
                    Object[] comp = (Object[]) o;

                    String name = (String) comp[0];
                    String docStr = (String) comp[1];
                    int type = extractInt(comp[3]);
                    String args = AbstractPyCodeCompletion.getArgs((String) comp[2], type,
                            ICompletionState.LOOKING_FOR_INSTANCED_VARIABLE);
                    String nameAndArgs = name + args;

                    int priority = IPyCompletionProposal.PRIORITY_DEFAULT;

                    if (type == IToken.TYPE_LOCAL) {
                        priority = IPyCompletionProposal.PRIORITY_LOCALS;

                    } else if (type == IToken.TYPE_PARAM) {
                        priority = IPyCompletionProposal.PRIORITY_LOCALS_1;
                    } else if (type == IToken.TYPE_IPYTHON_MAGIC) {
                        priority = IPyCompletionProposal.PRIORTTY_IPYTHON_MAGIC;
                    }

                    //                    ret.add(new PyCompletionProposal(name,
                    //                            offset-length, length, name.length(),
                    //                            PyCodeCompletionImages.getImageForType(type), name, null, docStr, priority));

                    int cursorPos = name.length();
                    if (args.length() > 1) {
                        cursorPos += 1;
                    }

                    int replacementOffset = offset - length;
                    PyCalltipsContextInformation pyContextInformation = null;
                    if (args.length() > 2) {
                        pyContextInformation = new PyCalltipsContextInformation(args, replacementOffset + name.length()
                                + 1); //just after the parenthesis
                    } else {

                        //Support for IPython completions (non standard names)

                        //i.e.: %completions, cd ...
                        if (name.length() > 0) {

                            //magic ipython stuff (starting with %)
                            if (name.charAt(0) == '%') {
                                replacementOffset -= 1;

                            } else if (name.charAt(0) == '/') {
                                //Should be something as cd c:/temp/foo (and name is /temp/foo)
                                char[] chars = text.toCharArray();
                                for (int i = 0; i < chars.length; i++) {
                                    char c = chars[i];
                                    if (c == name.charAt(0)) {
                                        String sub = text.substring(i, text.length());
                                        if (name.startsWith(sub)) {
                                            replacementOffset -= (sub.length() - FullRepIterable.getLastPart(actTok)
                                                    .length());
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ret.add(new PyLinkedModeCompletionProposal(nameAndArgs, replacementOffset, length, cursorPos,
                            PyCodeCompletionImages.getImageForType(type), nameAndArgs, pyContextInformation, docStr,
                            priority, PyCompletionProposal.ON_APPLY_DEFAULT, args, false));

                }
            }
        }
    }

    /**
     * Extracts an int from an object
     *
     * @param objToGetInt the object that should be gotten as an int
     * @return int with the int the object represents
     */
    private static int extractInt(Object objToGetInt) {
        if (objToGetInt instanceof Integer) {
            return (Integer) objToGetInt;
        }
        return Integer.parseInt(objToGetInt.toString());
    }

    /**
     * @return the description of the given attribute in the shell
     */
    public String getDescription(String text) throws Exception {
        if (waitingForInput) {
            return "Unable to get description: waiting for input.";
        }
        return client.execute("getDescription", new Object[] { text }).toString();
    }

    /**
     * The Debug Target to notify when the underlying process is suspended or
     * running.
     *
     * @param debugTarget
     */
    public void setDebugTarget(IPydevConsoleDebugTarget debugTarget) {
        this.debugTarget = debugTarget;
    }

    /**
     * The Debug Target to notify when the underlying process is suspended or
     * running.
     */
    public IPydevConsoleDebugTarget getDebugTarget() {
        return debugTarget;
    }

    /**
     * Common code to handle all cases of setting nextResponse so that the
     * attached debug target can be notified of effective state.
     *
     * @param nextResponse new next response
     */
    private void setNextResponse(InterpreterResponse nextResponse) {
        this.nextResponse = nextResponse;
        updateDebugTarget();
    }

    /**
     * Update the debug target (if non-null) of suspended state of console.
     */
    private void updateDebugTarget() {
        if (debugTarget != null) {
            if (nextResponse == null || nextResponse.need_input == true) {
                debugTarget.setSuspended(false);
            } else {
                debugTarget.setSuspended(true);
            }
        }
    }

    /**
     * Request that pydevconsole connect (with pydevd) to the specified port
     *
     * @param localPort
     *            port for pydevd to connect to.
     * @throws Exception if connection fails
     */
    public void connectToDebugger(int localPort) throws Exception {
        if (waitingForInput) {
            throw new Exception("Can't connect debugger now, waiting for input");
        }
        Object result = client.execute("connectToDebugger", new Object[] { localPort });
        Exception exception = null;
        if (result instanceof Object[]) {
            Object[] resultarray = (Object[]) result;
            if (resultarray.length == 1) {
                if ("connect complete".equals(resultarray[0])) {
                    return;
                }
                if (resultarray[0] instanceof String) {
                    exception = new Exception((String) resultarray[0]);
                }
                if (resultarray[0] instanceof Exception) {
                    exception = (Exception) resultarray[0];
                }
            }
        }
        throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR,
                "pydevconsole failed to execute connectToDebugger", exception));
    }

    /**
     * Wait for an established connection.
     * @param monitor
     * @throws Exception if no suitable response is received before the timeout
     * @throws UserCanceledException if user cancelled with monitor
     */
    public void hello(IProgressMonitor monitor) throws Exception, UserCanceledException {
        int maximumAttempts = InteractiveConsolePrefs.getMaximumAttempts();
        monitor.beginTask("Establishing Connection To Console Process", maximumAttempts);
        try {
            if (firstCommWorked) {
                return;
            }

            // We'll do a connection attempt, we can try to
            // connect n times (until the 1st time the connection
            // is accepted) -- that's mostly because the server may take
            // a while to get started.

            String result = null;
            for (int commAttempts = 0; commAttempts < maximumAttempts; commAttempts++) {
                if (monitor.isCanceled()) {
                    throw new UserCanceledException("Canceled before hello was successful");
                }
                try {
                    Object[] resulta;
                    resulta = (Object[]) client.execute("hello", new Object[] { "Hello pydevconsole" });
                    result = resulta[0].toString();
                } catch (XmlRpcException e) {
                    // We'll retry in a moment
                }

                if ("Hello eclipse".equals(result)) {
                    firstCommWorked = true;
                    break;
                }

                if (result.startsWith("Console already exited with value")) {
                    // Failed, probably some error starting the process
                    break;
                }

                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    // Retry now
                }
                monitor.worked(1);
            }

            if (!firstCommWorked) {
                throw new Exception("Failed to recive suitable Hello response from pydevconsole. Last msg received: "
                        + result);
            }
        } finally {
            monitor.done();
        }
    }

    /**
     * Not required for normal pydev console
     */
    public void linkWithDebugSelection(boolean isLinkedWithDebug) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Enable GUI Loop integration in PyDev Console
     * @param enableGuiName The name of the GUI to enable, see inputhook.py:enable_gui for list of legal names
     * @throws Exception on connection issues
     */
    public void enableGui(String enableGuiName) throws Exception {
        if (waitingForInput) {
            throw new Exception("Can't connect debugger now, waiting for input");
        }
        client.execute("enableGui", new Object[] { enableGuiName });
    }
}
