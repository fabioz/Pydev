/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 13/08/2005
 */
package org.python.pydev.editor.codecompletion.shell;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Display;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.concurrency.Semaphore;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.PyCodeCompletionPreferencesPage;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledToken;
import org.python.pydev.logging.DebugSettings;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.net.SocketUtil;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.Timer;

/**
 * This is the shell that 'talks' to the python / jython process (it is intended to be subclassed so that
 * we know how to deal with each).
 *
 * Its methods are synched to prevent concurrent access.
 *
 * @author fabioz
 *
 */
public abstract class AbstractShell {

    public static final int BUFFER_SIZE = 1024 * 20; //When it was just 1024 it was 8 times slower for numpy completions!

    private static final int MAIN_THREAD_SHELL = 1;

    private static final int OTHER_THREADS_SHELL = 2;

    public static int[] getAllShellIds() {
        return new int[] { MAIN_THREAD_SHELL, OTHER_THREADS_SHELL };
    }

    public static final int getShellId() {
        return Display.getCurrent() != null ? MAIN_THREAD_SHELL : OTHER_THREADS_SHELL;
    }

    protected static final int DEFAULT_SLEEP_BETWEEN_ATTEMPTS = 1000; //1sec, so we can make the number of attempts be shown as elapsed in secs

    protected static final int DEBUG_SHELL = -1;

    /**
     * Determines if we are already in a method that starts the shell
     */
    private volatile boolean inStart = false;

    /**
     * Determines if we are (theoretically) already connected (meaning that trying to start the shell
     * again will not do anything)
     *
     * Ending the shell sets this to false and starting it sets it to true (if successful)
     */
    private volatile boolean isConnected = false;

    private volatile boolean isInRead = false;

    private volatile boolean isInWrite = false;

    private volatile boolean isInRestart = false;

    private IInterpreterInfo shellInterpreter;

    /**
     * Lock to know if there is someone already using this shell for some operation
     */
    private final Semaphore semaphore = new Semaphore(1);

    private final Object ioLock = new Object();

    private static void dbg(String string, int priority) {
        if (priority <= DEBUG_SHELL) {
            System.out.println(string);
        }
        if (DebugSettings.DEBUG_CODE_COMPLETION) {
            Log.toLogFile(string, AbstractShell.class);
        }
    }

    /**
     * the encoding used to encode messages
     */
    /*default*/static final String ENCODING_UTF_8 = "UTF-8";

    /**
     * if we are already finished for good, we may not start new shells (this is a static, because this
     * should be set only at shutdown).
     */
    /*default*/static volatile boolean finishedForGood = false;

    protected ProcessCreationInfo process;

    /**
     * We should read this socket.
     */
    private Socket socket;

    /**
     * Python file that works as the server.
     */
    protected File serverFile;

    /**
     * Server socket (accept connections).
     */
    private ServerSocket serverSocket;

    private ServerSocketChannel serverSocketChannel;

    /**
     * Initialize given the file that points to the python server (execute it
     * with python).
     *
     * @param f file pointing to the python server
     *
     * @throws IOException
     * @throws CoreException
     */
    protected AbstractShell(File f) throws IOException, CoreException {
        if (finishedForGood) {
            throw new RuntimeException(
                    "Shells are already finished for good, so, it is an invalid state to try to create a new shell.");
        }

        serverFile = f;
        if (!serverFile.exists()) {
            throw new RuntimeException("Can't find python server file");
        }
    }

    private final Object waitLock = new Object();

    /**
     * Just wait a little...
     */
    private void sleepALittle(int t) {
        try {
            synchronized (waitLock) {
                waitLock.wait(t); //millis
            }
        } catch (InterruptedException e) {
        }
    }

    // Methods forwarded to the ShellsContainer (keeping existing API).
    // Methods forwarded to the ShellsContainer (keeping existing API).
    // Methods forwarded to the ShellsContainer (keeping existing API).

    /**
     * simple stop of a shell (it may be later restarted)
     */
    public static void stopServerShell(IInterpreterInfo interpreter, int id) {
        ShellsContainer.stopServerShell(interpreter, id);
    }

    /**
     * Stops all registered shells (should only be called at plugin shutdown). 
     */
    public static void shutdownAllShells() {
        ShellsContainer.shutdownAllShells();
    }

    /**
     * Restarts all the shells and clears any related cache.
     *
     * @return an error message if some exception happens in this process (an empty string means all went smoothly).
     */
    public static String restartAllShells() {
        return ShellsContainer.restartAllShells();
    }

    /**
     * register a shell and give it an id
     *
     * @param nature the nature (which has the information on the interpreter we want to used)
     * @param id the shell id
     * @see #MAIN_THREAD_SHELL
     * @see #OTHER_THREADS_SHELL
     *
     * @param shell the shell to register
     */
    public static void putServerShell(IPythonNature nature, int id, AbstractShell shell) {
        ShellsContainer.putServerShell(nature, id, shell);
    }

    public static AbstractShell getServerShell(IPythonNature nature, int id) throws IOException,
            JDTNotAvailableException, CoreException, MisconfigurationException, PythonNatureWithoutProjectException {
        return ShellsContainer.getServerShell(nature, id);
    }

    /**
     * This method creates the python server process and starts the sockets, so that we
     * can talk with the server.
     * @throws IOException
     * @throws CoreException
     * @throws MisconfigurationException
     * @throws PythonNatureWithoutProjectException
     */
    /*package*/void startIt(IPythonNature nature) throws IOException, JDTNotAvailableException,
            CoreException, MisconfigurationException, PythonNatureWithoutProjectException {
        this.startIt(nature.getProjectInterpreter());
    }

    /**
     * This method creates the python server process and starts the sockets, so that we
     * can talk with the server.
     *
     * @param milisSleep: time to wait after creating the process.
     * @throws IOException is some error happens creating the sockets - the process is terminated.
     * @throws JDTNotAvailableException
     * @throws CoreException
     * @throws CoreException
     * @throws MisconfigurationException
     */
    /*package*/void startIt(IInterpreterInfo interpreter) throws IOException,
            JDTNotAvailableException, CoreException, MisconfigurationException {

        int milisSleep = AbstractShell.DEFAULT_SLEEP_BETWEEN_ATTEMPTS;
        synchronized (ioLock) {

            this.shellInterpreter = interpreter;
            if (inStart || isConnected) {
                //it is already in the process of starting, so, if we are in another thread, just forget about it.
                return;
            }
            inStart = true;
            try {
                if (finishedForGood) {
                    throw new RuntimeException(
                            "Shells are already finished for good, so, it is an invalid state to try to restart it.");
                }

                try {

                    serverSocketChannel = ServerSocketChannel.open();
                    serverSocketChannel.configureBlocking(false);
                    serverSocketChannel.bind(new InetSocketAddress(0));

                    serverSocket = serverSocketChannel.socket();
                    int port = serverSocket.getLocalPort();
                    SocketUtil.checkValidPort(port);

                    if (process != null) {
                        endIt(); //end the current process
                    }

                    process = createServerProcess(interpreter, port);
                    dbg("executed: " + process.getProcessLog(), 1);

                    sleepALittle(200); //Give it some time to warmup.
                    try {
                        int exitVal = process.exitValue(); //should throw exception saying that it still is not terminated...
                        String msg = "Error creating python process - exited before creating sockets - exitValue = ("
                                + exitVal + ").\n" + process.getProcessLog();
                        dbg(msg, 1);
                        Log.log(msg);
                        throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR, msg, new Exception(msg)));
                    } catch (IllegalThreadStateException e2) { //this is ok
                    }

                    dbg("afterCreateProcess ", 1);
                    //ok, process validated, so, let's get its output and store it for further use.

                    boolean connected = false;
                    int attempt = 0;

                    dbg("connecting... ", 1);
                    sleepALittle(milisSleep);
                    int maxAttempts = PyCodeCompletionPreferencesPage.getNumberOfConnectionAttempts();

                    dbg("maxAttempts: " + maxAttempts, 1);
                    dbg("finishedForGood: " + finishedForGood, 1);

                    while (!connected && attempt < maxAttempts && !finishedForGood) {
                        attempt += 1;
                        dbg("connecting attept..." + attempt, 1);
                        try {

                            try {
                                dbg("serverSocket.accept()! ", 1);
                                long initial = System.currentTimeMillis();
                                SocketChannel accept = null;
                                while (accept == null && System.currentTimeMillis() - initial < 5000) { //Each attempt is 5 seconds...
                                    dbg("serverSocketChannel.accept(): waiting for python client to connect back to the eclipse java vm",
                                            1);
                                    accept = serverSocketChannel.accept();
                                    if (accept == null) {
                                        sleepALittle(500);
                                    }
                                }
                                if (accept != null) {
                                    socket = accept.socket();
                                    dbg("socketToRead.setSoTimeout(8000) ", 1);
                                    socket.setSoTimeout(8 * 1000); //let's give it a higher timeout
                                    connected = true;
                                    dbg("connected! ", 1);
                                } else {
                                    String msg = "The python client still hasn't connected back to the eclipse java vm (will retry...)";
                                    dbg(msg, 1);
                                    Log.log(msg);
                                }
                            } catch (SocketTimeoutException e) {
                                //that's ok, timeout for waiting connection expired, let's check it again in the next loop
                                dbg("SocketTimeoutException! ", 1);
                            }
                        } catch (IOException e1) {
                            dbg("IOException! ", 1);
                        }

                        //if not connected, let's sleep a little for another attempt
                        if (!connected) {
                            if (attempt > 1) {
                                //Don't log first failed attempt.
                                String msg = "Attempt: " + attempt + " of " + maxAttempts
                                        + " failed, trying again...(socket connected: "
                                        + (socket == null ? "still null" : socket.isConnected()) + ")";

                                dbg(msg, 1);
                                Log.log(msg);
                                sleepALittle(milisSleep);
                            }
                        }
                    }

                    if (!connected && !finishedForGood) {
                        dbg("NOT connected ", 1);

                        //what, after all this trouble we are still not connected????!?!?!?!
                        //let's communicate this to the user...
                        String isAlive;
                        try {
                            int exitVal = process.exitValue(); //should throw exception saying that it still is not terminated...
                            isAlive = " - the process in NOT ALIVE anymore (output=" + exitVal + ") - ";
                        } catch (IllegalThreadStateException e2) { //this is ok
                            isAlive = " - the process in still alive (killing it now)- ";
                            process.destroy();
                        }
                        closeConn(); //make sure all connections are closed as we're not connected

                        String msg = "Error connecting to python process (most likely cause for failure is a firewall blocking communication or a misconfigured network).\n"
                                + isAlive + "\n" + process.getProcessLog();

                        RuntimeException exception = new RuntimeException(msg);
                        dbg(msg, 1);
                        Log.log(exception);
                        throw exception;
                    }

                } catch (IOException e) {

                    if (process != null) {
                        process.destroy();
                        process = null;
                    }
                    throw e;
                }
            } finally {
                this.inStart = false;
            }

            //if it got here, everything went ok (otherwise we would have gotten an exception).
            isConnected = true;
        }
        synchronized (lockLastPythonPath) {
            lastPythonPath = null;
        }

    }

    /**
     * @param port the port to be used to connect the socket.
     * @return a tuple with:
     *  - command line used to execute process
     *  - environment used to execute process
     *
     * @throws IOException
     * @throws JDTNotAvailableException
     * @throws MisconfigurationException
     */
    protected abstract ProcessCreationInfo createServerProcess(IInterpreterInfo interpreter, int port)
            throws IOException, JDTNotAvailableException, MisconfigurationException;

    /**
     * @param operation
     * @return
     * @throws IOException
     */
    private FastStringBuffer read(IProgressMonitor monitor) throws IOException {
        synchronized (ioLock) {

            if (finishedForGood) {
                throw new RuntimeException(
                        "Shells are already finished for good, so, it is an invalid state to try to read from it.");
            }
            if (inStart) {
                throw new RuntimeException(
                        "The shell is still not completely started, so, it is an invalid state to try to read from it.");
            }
            if (!isConnected) {
                throw new RuntimeException(
                        "The shell is still not connected, so, it is an invalid state to try to read from it.");
            }
            if (isInRead) {
                throw new RuntimeException(
                        "The shell is already in read mode, so, it is an invalid state to try to read from it.");
            }
            if (isInWrite) {
                throw new RuntimeException(
                        "The shell is already in write mode, so, it is an invalid state to try to read from it.");
            }

            isInRead = true;

            try {
                FastStringBuffer strBuf = new FastStringBuffer(AbstractShell.BUFFER_SIZE);
                byte[] b = new byte[AbstractShell.BUFFER_SIZE];
                int searchFrom = 0;
                while (true) {

                    int len = this.socket.getInputStream().read(b);
                    if (len == 0) {
                        break;
                    }

                    String s = new String(b, 0, len);
                    searchFrom = strBuf.length() - 5; //-5 because that's the len of END@@
                    if (searchFrom < 0) {
                        searchFrom = 0;
                    }
                    strBuf.append(s);

                    if (strBuf.indexOf("END@@", searchFrom) != -1) {
                        break;
                    } else {
                        sleepALittle(10);
                    }
                }

                strBuf.replaceFirst("@@COMPLETIONS", "");
                searchFrom -= "@@COMPLETIONS".length();
                if (searchFrom < 0) {
                    searchFrom = 0;
                }

                //remove END@@
                try {
                    int endIndex = strBuf.indexOf("END@@", searchFrom);
                    if (endIndex != -1) {
                        strBuf.setCount(endIndex);
                        return strBuf;
                    } else {
                        throw new RuntimeException("Couldn't find END@@ on received string.");
                    }
                } catch (RuntimeException e) {
                    if (strBuf.length() > 500) {
                        strBuf.setCount(499).append("...(continued)...");//if the string gets too big, it can crash Eclipse...
                    }
                    Log.log(IStatus.ERROR, ("ERROR WITH STRING:" + strBuf), e);
                    return new FastStringBuffer();
                }
            } finally {
                isInRead = false;
            }
        }
    }

    /**
     * @return s string with the contents read.
     * @throws IOException
     */
    private FastStringBuffer read() throws IOException {
        FastStringBuffer r = read(null);
        //System.out.println("RETURNING:"+URLDecoder.decode(URLDecoder.decode(r,ENCODING_UTF_8),ENCODING_UTF_8));
        return r;
    }

    /**
     * @param str
     * @throws IOException
     */
    private void write(String str) throws IOException {
        synchronized (ioLock) {

            if (finishedForGood) {
                throw new RuntimeException(
                        "Shells are already finished for good, so, it is an invalid state to try to write to it.");
            }
            if (inStart) {
                throw new RuntimeException(
                        "The shell is still not completely started, so, it is an invalid state to try to write to it.");
            }
            if (!isConnected) {
                throw new RuntimeException(
                        "The shell is still not connected, so, it is an invalid state to try to write to it.");
            }
            if (isInRead) {
                throw new RuntimeException(
                        "The shell is already in read mode, so, it is an invalid state to try to write to it.");
            }
            if (isInWrite) {
                throw new RuntimeException(
                        "The shell is already in write mode, so, it is an invalid state to try to write to it.");
            }

            isInWrite = true;

            //dbg("WRITING:"+str);
            try {
                OutputStream outputStream = this.socket.getOutputStream();
                outputStream.write(str.getBytes());
                outputStream.flush();
            } finally {
                isInWrite = false;
            }
        }
    }

    /**
     * @throws IOException
     */
    private void closeConn() throws IOException {
        //let's not send a message... just close the sockets and kill it
        //        try {
        //            write("@@KILL_SERVER_END@@");
        //        } catch (Exception e) {
        //        }
        synchronized (ioLock) {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
            }
            socket = null;

            try {
                if (serverSocketChannel != null) {
                    serverSocketChannel.close();
                }
            } catch (Exception e) {
            }
            serverSocketChannel = null;

            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (Exception e) {
            }
            serverSocket = null;
        }
        synchronized (lockLastPythonPath) {
            lastPythonPath = null;
        }
    }

    /**
     * this function should be used with care... it only destroys our processes without closing the
     * connections correctly (intended for shutdowns)
     */
    /*default*/void shutdown() {
        synchronized (ioLock) {
            socket = null;
            serverSocket = null;
            serverSocketChannel = null;
            if (process != null) {
                process.destroy();
                process = null;
            }
        }
        synchronized (lockLastPythonPath) {
            lastPythonPath = null;
        }
    }

    /**
     * Kill our sub-process.
     * @throws IOException
     */
    /*default*/void endIt() {
        synchronized (ioLock) {
            try {
                closeConn();
            } catch (Exception e) {
                //that's ok...
            }

            //set that we are still not connected
            isConnected = false;

            if (process != null) {
                process.destroy();
                process = null;
            }
        }
        synchronized (lockLastPythonPath) {
            lastPythonPath = null;
        }
    }

    /**
     * @throws CoreException
     *
     */
    private void restartShell() throws CoreException {
        synchronized (ioLock) {
            if (!isInRestart) {// we don't want to end up in a loop here...
                isInRestart = true;
                try {
                    if (finishedForGood) {
                        throw new RuntimeException(
                                "Shells are already finished for good, so, it is an invalid state to try to restart a new shell.");
                    }

                    try {
                        this.endIt();
                    } catch (Exception e) {
                    }
                    try {
                        this.startIt(shellInterpreter);
                    } catch (Exception e) {
                        Log.log(IStatus.ERROR, "ERROR restarting shell.", e);
                    }
                } finally {
                    isInRestart = false;
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private AutoCloseable acquire(String msg) {
        final Timer timer = new Timer();
        semaphore.acquire();
        if (DEBUG_SHELL >= 1) {
            String name = Thread.currentThread().getName();
            msg += " (" + name + ")";
            timer.printDiff("Time to aqcuire: " + msg);
        }
        final String s = msg;
        return new AutoCloseable() {

            @Override
            public void close() throws Exception {
                if (DEBUG_SHELL >= 1) {
                    timer.printDiff("-- Time to execute: " + s);
                }
                semaphore.release();
            }
        };
    }

    private FastStringBuffer writeAndGetResults(String... str) throws CoreException {

        try {
            synchronized (ioLock) {
                this.write(StringUtils.join("", str));
                FastStringBuffer read = this.read();
                return read;
            }

        } catch (Exception e) {
            String message = "ERROR reading shell.";
            if (process != null) {
                message += "\n" + process.getProcessLog();
            }
            Log.log(IStatus.ERROR, message, e);

            restartShell();
            return null;
        } finally {
            if (process != null) {
                //Clear the contents from the output from time to time
                //Note: it's important having a thread reading the stdout and stderr, otherwise the
                //python client could become halted and would need to be restarted.
                process.clearOutput();
            }
        }
    }

    private final Object lockLastPythonPath = new Object();
    private String lastPythonPath = null;

    /**
     * @param pythonpath
     */
    private void internalChangePythonPath(List<String> pythonpath) throws Exception {
        if (finishedForGood) {
            throw new RuntimeException(
                    "Shells are already finished for good, so, it is an invalid state to try to change its dir.");
        }
        String pythonpathStr;

        synchronized (lockLastPythonPath) {
            pythonpathStr = StringUtils.join("|", pythonpath.toArray(new String[pythonpath.size()]));

            if (lastPythonPath != null && lastPythonPath.equals(pythonpathStr)) {
                return;
            }
            lastPythonPath = pythonpathStr;
        }
        try {
            writeAndGetResults("@@CHANGE_PYTHONPATH:", URLEncoder.encode(pythonpathStr, ENCODING_UTF_8), "\nEND@@");
        } catch (Exception e) {
            Log.log("Error changing the pythonpath to: " + StringUtils.join("\n", pythonpath), e);
            throw e;
        }
    }

    /**
     * @return list with tuples: new String[]{token, description}
     * @throws CoreException
     */
    public Tuple<String, List<String[]>> getImportCompletions(String str, List<String> pythonpath)
            throws Exception {
        FastStringBuffer read = null;

        str = URLEncoder.encode(str, ENCODING_UTF_8);

        try (AutoCloseable permit = acquire(StringUtils.join("", "getImportCompletions: ", str))) {
            internalChangePythonPath(pythonpath);
            read = this.writeAndGetResults("@@IMPORTS:", str, "\nEND@@");
        }
        return ShellConvert.convertStringToCompletions(read);
    }

    /**
     * @param moduleName the name of the module where the token is defined
     * @param token the token we are looking for
     * @return the file where the token was defined, its line and its column (or null if it was not found)
     * @throws Exception 
     */
    public Tuple<String[], int[]> getLineCol(String moduleName, String token, List<String> pythonpath)
            throws Exception {
        FastStringBuffer read = null;

        String str = moduleName + "." + token;
        str = URLEncoder.encode(str, ENCODING_UTF_8);

        try (AutoCloseable permit = acquire("getLineCol")) {
            internalChangePythonPath(pythonpath);
            read = this.writeAndGetResults("@@SEARCH", str, "\nEND@@");
        }

        Tuple<String, List<String[]>> theCompletions = ShellConvert.convertStringToCompletions(read);

        List<String[]> def = theCompletions.o2;
        if (def.size() == 0) {
            return null;
        }

        String[] comps = def.get(0);
        if (comps.length == 0) {
            return null;
        }

        int line = Integer.parseInt(comps[0]);
        int col = Integer.parseInt(comps[1]);

        String foundAs = comps[2];
        return new Tuple<String[], int[]>(new String[] { theCompletions.o1, foundAs }, new int[] { line, col });
    }

    /**
     * Gets completions for jedi library (https://github.com/davidhalter/jedi)
     */
    public List<CompiledToken> getJediCompletions(File editorFile, PySelection ps, String charset,
            List<String> pythonpath) throws Exception {

        FastStringBuffer read = null;
        String str = StringUtils.join(
                "|",
                new String[] { String.valueOf(ps.getCursorLine()), String.valueOf(ps.getCursorColumn()),
                        charset, FileUtils.getFileAbsolutePath(editorFile),
                        StringUtils.replaceNewLines(ps.getDoc().get(), "\n") });

        str = URLEncoder.encode(str, ENCODING_UTF_8);

        try (AutoCloseable permit = acquire("getJediCompletions")) {
            internalChangePythonPath(pythonpath);
            read = this.writeAndGetResults("@@MSG_JEDI:", str, "\nEND@@");
        }

        Tuple<String, List<String[]>> theCompletions = ShellConvert.convertStringToCompletions(read);
        ArrayList<CompiledToken> lst = new ArrayList<>(theCompletions.o2.size());
        for (String[] s : theCompletions.o2) {
            //new CompiledToken(rep, doc, args, parentPackage, type);
            lst.add(new CompiledToken(s[0], s[1], "", "", Integer.parseInt(s[3])));
        }
        return lst;
    }

}
