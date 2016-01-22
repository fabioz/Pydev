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
import org.python.pydev.core.concurrency.ConditionEvent;
import org.python.pydev.core.concurrency.ConditionEventWithValue;
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
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.io.ThreadStreamReader;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.string.StringUtils;
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
    private volatile IXmlRpcClient client;

    /**
     * This is the server responsible for giving input to a raw_input() requested
     * and for opening editors (as a result of %edit in IPython)
     */
    private WebServer webServer;

    private final String[] commandArray;

    private final String[] envp;

    private StdStreamsThread stdStreamsThread;

    private class StdStreamsThread extends Thread {

        /**
         * Responsible for getting the stdout of the process.
         */
        private final ThreadStreamReader stdOutReader;

        /**
         * Responsible for getting the stderr of the process.
         */
        private final ThreadStreamReader stdErrReader;

        private volatile boolean stopped = false;

        private final Object lock = new Object();

        public StdStreamsThread(Process process, String encoding) {
            this.setName("StdStreamsThread: " + process);
            this.setDaemon(true);
            stdOutReader = new ThreadStreamReader(process.getInputStream(), true, encoding);
            stdErrReader = new ThreadStreamReader(process.getErrorStream(), true, encoding);
            stdOutReader.start();
            stdErrReader.start();
        }

        @Override
        public void run() {
            while (!stopped) {
                synchronized (lock) {
                    if (onContentsReceived != null) {
                        String stderrContents = stdErrReader.getAndClearContents();
                        String stdOutContents = stdOutReader.getAndClearContents();
                        if (stdOutContents.length() > 0 || stderrContents.length() > 0) {
                            onContentsReceived.call(new Tuple<String, String>(stdOutContents, stderrContents));
                        }
                    }
                    try {
                        lock.wait(50);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        public void stopLoop() {
            stopped = true;
            synchronized (lock) {
                lock.notifyAll();
            }
            stdOutReader.stopGettingOutput();
            stdErrReader.stopGettingOutput();
        }
    }

    /**
     * Initializes the xml-rpc communication.
     *
     * @param port the port where the communication should happen.
     * @param process this is the process that was spawned (server for the XML-RPC)
     *
     * @throws MalformedURLException
     */
    public PydevConsoleCommunication(int port, final Process process, int clientPort, String[] commandArray,
            String[] envp, String encoding)
                    throws Exception {
        this.commandArray = commandArray;
        this.envp = envp;

        finishedExecution = new ConditionEvent(new ICallback0<Boolean>() {

            @Override
            public Boolean call() {
                try {
                    process.exitValue();
                    return true; // already exited
                } catch (Exception e) {
                    return false;
                }
            }
        }, 200);

        //start the server that'll handle input requests
        this.webServer = new WebServer(clientPort);
        XmlRpcServer serverToHandleRawInput = this.webServer.getXmlRpcServer();
        serverToHandleRawInput.setHandlerMapping(new XmlRpcHandlerMapping() {

            public XmlRpcHandler getHandler(String handlerName) throws XmlRpcNoSuchHandlerException, XmlRpcException {
                return PydevConsoleCommunication.this;
            }
        });

        this.webServer.start();
        this.stdStreamsThread = new StdStreamsThread(process, encoding);
        this.stdStreamsThread.start();

        IXmlRpcClient client = new ScriptXmlRpcClient(process);
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
        if (this.stdStreamsThread != null) {
            this.stdStreamsThread.stopLoop();
            this.stdStreamsThread = null;
        }

        if (this.webServer != null) {
            this.webServer.shutdown();
            this.webServer = null;
        }
    }

    @Override
    public boolean isConnected() {
        return this.client != null;
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
    private volatile ConditionEventWithValue<InterpreterResponse> nextResponse = new ConditionEventWithValue<>(null,
            20);

    /**
     * Helper to keep on busy loop.
     */
    private volatile Object lock = new Object();

    /**
     * Keeps a flag indicating that we were able to communicate successfully with the shell at least once
     * (if we haven't we may retry more than once the first time, as jython can take a while to initialize
     * the communication)
     * This is set to true on successful {@link #hello(IProgressMonitor)}
     */
    private volatile boolean firstCommWorked = false;

    private final ConditionEvent finishedExecution;

    /**
     * When non-null, the Debug Target to notify when the underlying process is suspended or running.
     */
    private IPydevConsoleDebugTarget debugTarget = null;

    private ICallback<Object, Tuple<String, String>> onContentsReceived;

    /**
     * Called when the server is requesting some input from this class.
     */
    public Object execute(XmlRpcRequest request) throws XmlRpcException {
        String methodName = request.getMethodName();
        if ("RequestInput".equals(methodName)) {
            return requestInput();
        } else if ("IPythonEditor".equals(methodName)) {
            return openEditor(request);
        } else if ("NotifyAboutMagic".equals(methodName)) {
            return "";
        } else if ("NotifyFinished".equals(methodName)) {
            finishedExecution.set();
            return "";
        }
        Log.log("Unexpected call to execute for method name: " + methodName);
        return "";
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

        //let the busy loop from execInterpreter free and enter a busy loop
        //in this function until execInterpreter gives us an input
        setNextResponse(new InterpreterResponse(false, needInput));

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

    @Override
    public void setOnContentsReceivedCallback(ICallback<Object, Tuple<String, String>> onContentsReceived) {
        this.onContentsReceived = onContentsReceived;
    }

    /**
     * Holding the last response (if the last response needed more input, we'll buffer contents internally until
     * we do have a suitable line and will pass it in a batch to the interpreter).
     */
    private volatile InterpreterResponse lastResponse = null;

    /**
     * List with the strings to be passed to the interpreter once we have a line that's suitable for evaluation.
     */
    private final List<String> moreBuffer = new ArrayList<>();

    /**
     * Instructs the client to raise KeyboardInterrupt and return to a clean command prompt.  This can be
     * called to terminate:
     * - infinite or excessively long processing loops (CPU bound)
     * - I/O wait (e.g. urlopen, time.sleep)
     * - asking for input from the console i.e. input(); this is a special case of the above because PyDev
     *   is involved
     * - command prompt continuation processing, so that the user doesn't have to work out the exact
     *   sequence of close brackets required to get the prompt back
     * This requires the cooperation of the client (the call to interrupt must be processed by the XMLRPC
     * server) but in most cases is better than just terminating the process.
     */
    public void interrupt() {
        Job job = new Job("Interrupt console process") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    lastResponse = null;
                    setNextResponse(new InterpreterResponse(false, false));
                    moreBuffer.clear();

                    PydevConsoleCommunication.this.client.execute("interrupt", new Object[0]);
                    if (PydevConsoleCommunication.this.waitingForInput) {
                        PydevConsoleCommunication.this.inputReceived = "";
                        PydevConsoleCommunication.this.waitingForInput = false;
                    }
                } catch (Exception e) {
                    Log.log(IStatus.ERROR, "Problem interrupting python process", e);
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    /**
     * Executes a given line in the interpreter.
     *
     * @param command the command to be executed in the client
     */
    public void execInterpreter(String command, final ICallback<Object, InterpreterResponse> onResponseReceived) {
        setNextResponse(null);
        if (waitingForInput) {
            inputReceived = command;
            waitingForInput = false;
            //the thread that we started in the last exec is still alive if we were waiting for an input.
        } else {

            if (lastResponse != null && lastResponse.need_input == false && lastResponse.more) {
                if (command.trim().length() > 0 && Character.isWhitespace(command.charAt(0))) {
                    moreBuffer.add(command);
                    //Pass same response back again (we still need more input to try to do some evaluation).
                    onResponseReceived.call(lastResponse);
                    return;
                }
            }
            final String executeCommand;
            if (moreBuffer.size() > 0) {
                executeCommand = StringUtils.join("\n", moreBuffer) + "\n" + command;
                moreBuffer.clear();
            } else {
                executeCommand = command;
            }

            //create a thread that'll keep locked until an answer is received from the server.
            Job job = new Job("PyDev Console Communication") {

                /**
                 * Executes the needed command
                 *
                 * @return a tuple with (null, more) or (error, false)
                 *
                 * @throws XmlRpcException
                 */
                private boolean exec() throws XmlRpcException {

                    if (client == null) {
                        return false;
                    }

                    Object ret = client.execute(executeCommand.contains("\n") ? "execMultipleLines" : "execLine",
                            new Object[] { executeCommand });
                    if (!(ret instanceof Boolean)) {
                        if (ret instanceof Object[]) {
                            Object[] objects = (Object[]) ret;
                            ret = StringUtils.join(" ", objects);
                        } else {
                            ret = "" + ret;
                        }
                        if (onContentsReceived != null) {
                            onContentsReceived.call(new Tuple<String, String>("", ret.toString()));
                        }
                        return false;
                    }
                    boolean more = (Boolean) ret;
                    return more;
                }

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    final boolean needInput = false;
                    try {
                        if (!firstCommWorked) {
                            throw new Exception(
                                    "hello must be called successfully before execInterpreter can be used.");
                        }

                        finishedExecution.unset();
                        boolean more = exec();
                        if (!more) {
                            finishedExecution.waitForSet();
                        }

                        setNextResponse(new InterpreterResponse(more, needInput));

                    } catch (Exception e) {
                        Log.log(e);
                        setNextResponse(new InterpreterResponse(false, needInput));
                    }
                    return Status.OK_STATUS;
                }
            };

            job.schedule();

        }

        //busy loop until we have a response
        InterpreterResponse waitForSet = nextResponse.waitForSet();
        lastResponse = waitForSet;
        onResponseReceived.call(waitForSet);
    }

    /**
     * @return completions from the client
     */
    public ICompletionProposal[] getCompletions(String text, String actTok, int offset, boolean showForTabCompletion)
            throws Exception {
        if (waitingForInput) {
            return new ICompletionProposal[0];
        }
        Object fromServer = client.execute("getCompletions", new Object[] { text, actTok });
        List<ICompletionProposal> ret = new ArrayList<ICompletionProposal>();

        convertConsoleCompletionsToICompletions(text, actTok, offset, fromServer, ret, showForTabCompletion);
        ICompletionProposal[] proposals = ret.toArray(new ICompletionProposal[ret.size()]);
        return proposals;
    }

    public static void convertConsoleCompletionsToICompletions(final String text, String actTok, int offset,
            Object fromServer,
            List<ICompletionProposal> ret, boolean showForTabCompletion) {
        IFilterCompletion filter = null;
        if (actTok != null && actTok.indexOf("].") != -1) {
            // Fix issue: when we request a code-completion on a list position i.e.: "lst[0]." IPython is giving us completions from the
            // filesystem, so, this is a workaround for that where we remove such completions.
            filter = new IFilterCompletion() {

                @Override
                public boolean acceptCompletion(int type, PyLinkedModeCompletionProposal completion) {
                    if (type == IToken.TYPE_IPYTHON) {
                        if (completion.getDisplayString().startsWith(".")) {
                            return false;
                        }
                    }
                    return true;
                }

            };
        }

        convertToICompletions(text, actTok, offset, fromServer, ret, showForTabCompletion, filter);
    }

    public static interface IFilterCompletion {
        boolean acceptCompletion(int type, PyLinkedModeCompletionProposal completion);
    }

    private static void convertToICompletions(final String text, String actTok, int offset, Object fromServer,
            List<ICompletionProposal> ret, boolean showForTabCompletion, IFilterCompletion filter) {
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
            final String trimmedText = text.trim();

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
                            // Decrement the replacement offset _only_ if the token begins with %
                            // as ipthon completes a<tab> to %alias etc.
                            if (name.charAt(0) == '%' && text.length() > 0 && text.charAt(0) == '%') {
                                replacementOffset -= 1;

                                // handle cd -- we handle this by returning the full path from ipython
                                // TODO: perhaps we could do this for all completions
                            } else if (trimmedText.equals("cd") || trimmedText.startsWith("cd ")
                                    || trimmedText.equals("%cd") || trimmedText.startsWith("%cd ")) {

                                // text == the full search e.g. "cd works"   ; "cd workspaces/foo"
                                // actTok == the last segment of the path e.g. "foo"  ;
                                // nameAndArgs == full completion e.g. "workspaces/foo/"

                                if (showForTabCompletion) {
                                    replacementOffset = 0;
                                    length = text.length();

                                } else {
                                    if (name.charAt(0) == '/') {
                                        //Should be something as cd c:/temp/foo (and name is /temp/foo)
                                        char[] chars = text.toCharArray();
                                        for (int i = 0; i < chars.length; i++) {
                                            char c = chars[i];
                                            if (c == name.charAt(0)) {
                                                String sub = text.substring(i, text.length());
                                                if (name.startsWith(sub)) {
                                                    replacementOffset -= (sub.length() - FullRepIterable
                                                            .getLastPart(actTok)
                                                            .length());
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    PyLinkedModeCompletionProposal completion = new PyLinkedModeCompletionProposal(nameAndArgs,
                            replacementOffset, length, cursorPos,
                            PyCodeCompletionImages.getImageForType(type), nameAndArgs, pyContextInformation, docStr,
                            priority, PyCompletionProposal.ON_APPLY_DEFAULT, args, false);
                    if (filter == null || filter.acceptCompletion(type, completion)) {
                        ret.add(completion);
                    }
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
        this.nextResponse.set(nextResponse);
        updateDebugTarget(nextResponse);
    }

    /**
     * Update the debug target (if non-null) of suspended state of console.
     * @param nextResponse2
     */
    private void updateDebugTarget(InterpreterResponse nextResponse) {
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
                    Object resulta = client.execute("hello", new Object[] { "Hello pydevconsole" });
                    if (resulta instanceof String) {
                        result = (String) resulta;
                    } else {
                        result = StringUtils.join("", (Object[]) resulta);
                    }
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
                String commandLine = this.commandArray != null ? ProcessUtils.getArgumentsAsStr(this.commandArray)
                        : "(unable to determine command line)";
                String environment = this.envp != null ? ProcessUtils.getEnvironmentAsStr(this.envp) : "null";
                throw new Exception("Failed to recive suitable Hello response from pydevconsole. Last msg received: "
                        + result + "\nCommand Line used: " + commandLine + "\n\nEnvironment:\n" + environment);
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
