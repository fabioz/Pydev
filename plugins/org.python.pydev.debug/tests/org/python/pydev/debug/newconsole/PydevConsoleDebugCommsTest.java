/******************************************************************************
* Copyright (C) 2012-2013  Jonah Graham and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com>    - initial API and implementation
*     Andrew Ferrazzutti <aferrazz@redhat.com> - ongoing maintenance
*     Fabio Zadrozny <fabiofz@gmail.com>       - ongoing maintenance
******************************************************************************/
package org.python.pydev.debug.newconsole;

import java.io.File;
import java.lang.ProcessBuilder.Redirect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.junit.Assert;
import org.python.pydev.core.TestDependent;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.AbstractDebugTargetWithTransmission;
import org.python.pydev.debug.model.IVariableLocator;
import org.python.pydev.debug.model.PyStackFrameConsole;
import org.python.pydev.debug.model.PyThreadConsole;
import org.python.pydev.debug.model.PyVariable;
import org.python.pydev.debug.model.PyVariableCollection;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.GetFrameCommand;
import org.python.pydev.debug.model.remote.VersionCommand;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.net.SocketUtil;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_interactive_console.console.InterpreterResponse;

import junit.framework.TestCase;

/**
 * The purpose of this test is to verify the pydevconsole + pydevd works. This
 * test does not try to test the console or debugger itself, just the
 * combination and new code paths that the feature introduces.
 *
 * TODO: Iterate over Jython/Python with and without IPython available.
 *
 */
public class PydevConsoleDebugCommsTest extends TestCase {

    private PydevConsoleCommunication pydevConsoleCommunication;
    private Process process;
    private DummyDebugTarget debugTarget;
    /** Fake $HOME for IPython */
    private File homeDir;
    private ICallback<Object, Tuple<String, String>> onContentsReceived;
    private FastStringBuffer stdout = new FastStringBuffer();
    private FastStringBuffer stderr = new FastStringBuffer();

    @Override
    protected void setUp() throws Exception {
        String consoleFile = FileUtils
                .createFileFromParts(TestDependent.PYSRC_LOC, "pydevconsole.py")
                .getAbsolutePath();
        String pydevdDir = new File(TestDependent.PYSRC_LOC).getAbsolutePath();
        Integer[] ports = SocketUtil.findUnusedLocalPorts(2);
        int port = ports[0];
        int clientPort = ports[1];
        onContentsReceived = (arg) -> {
            stdout.append(arg.o1);
            stderr.append(arg.o2);
            return null;
        };

        homeDir = FileUtils.getTempFileAt(new File("."), "fake_homedir");
        if (homeDir.exists()) {
            FileUtils.deleteDirectoryTree(homeDir);
        }
        homeDir = homeDir.getAbsoluteFile();
        homeDir.mkdir();
        String[] cmdarray = new String[] { TestDependent.PYTHON_EXE, consoleFile, String.valueOf(port),
                String.valueOf(clientPort) };

        ProcessBuilder builder = new ProcessBuilder(cmdarray);
        Map<String, String> env = builder.environment();
        env.put("HOME", homeDir.toString());
        env.put("PYTHONPATH", pydevdDir);
        env.put("PYTHONIOENCODING", "utf-8");
        env.put("PYTHONUNBUFFERED", "1");
        assertTrue(builder.redirectInput().type() == Type.PIPE);
        assertTrue(builder.redirectError().type() == Type.PIPE);

        String[] envp = ProcessUtils.getMapEnvAsArray(env);
        process = builder.start();

        pydevConsoleCommunication = new PydevConsoleCommunication(port, process, clientPort, cmdarray, envp, "utf-8");
        pydevConsoleCommunication.setOnContentsReceivedCallback(onContentsReceived);
        pydevConsoleCommunication.hello(new NullProgressMonitor());

        ServerSocket socket = SocketUtil.createLocalServerSocket();
        pydevConsoleCommunication.connectToDebugger(socket.getLocalPort());
        socket.setSoTimeout(5000);
        Socket accept = socket.accept();

        debugTarget = new DummyDebugTarget();
        debugTarget.startTransmission(accept);
        debugTarget.initialize(false);
    }

    @Override
    protected void tearDown() throws Exception {
        process.destroy();
        pydevConsoleCommunication.close();
        debugTarget.terminate();

        if (homeDir.exists()) {
            //This must be the last thing: the process will lock this directory.
            FileUtils.deleteDirectoryTree(homeDir);
        }
    }

    private final class CustomGetFrameCommand extends GetFrameCommand {
        private final Boolean[] passed;

        private CustomGetFrameCommand(Boolean[] passed, AbstractDebugTarget debugger, String locator) {
            super(debugger, locator);
            this.passed = passed;
        }

        @Override
        public void processErrorResponse(int cmdCode, String payload) {
            passed[0] = false;
        }

        @Override
        public void processOKResponse(int cmdCode, String payload) {
            super.processOKResponse(cmdCode, payload);
            passed[0] = true;
        }
    }

    private class DummyDebugTarget extends AbstractDebugTarget {

        public DummyDebugTarget() {
            super(null);
        }

        @Override
        public void processCommand(String sCmdCode, String sSeqCode, String payload) {
            int cmdCode = Integer.parseInt(sCmdCode);
            if (cmdCode == AbstractDebuggerCommand.CMD_SET_PROTOCOL) {
            } else if (cmdCode == AbstractDebuggerCommand.CMD_THREAD_CREATED) {
            } else {
                System.out.println(
                        "DummyDebugTarget processing unexpected command: " + sCmdCode + ":" + sSeqCode + ":" + payload);
            }
        }

        @Override
        public IProcess getProcess() {
            return null;
        }

        @Override
        public void launchRemoved(ILaunch launch) {
        }

        @Override
        public boolean canTerminate() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

    }

    private void waitUntilNonNull(Object[] object) {
        for (int i = 0; i < 50; i++) {
            if (object[0] != null) {
                return;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                // Retry now
            }
        }
        Assert.fail("Timed out waiting for non-null");
    }

    /**
     * This test is the basic comms working, send the command down via XML-RPC
     * based new method postCommand and receive response back via
     * {@link AbstractDebugTargetWithTransmission}
     */
    public void testVersion() throws Exception {
        try {
            final Boolean passed[] = new Boolean[1];
            debugTarget.postCommand(new VersionCommand(debugTarget) {
                @Override
                public void processOKResponse(int cmdCode, String payload) {
                    if (cmdCode == AbstractDebuggerCommand.CMD_VERSION && "@@BUILD_NUMBER@@".equals(payload)) {
                        passed[0] = true;
                    } else {
                        passed[0] = false;
                    }
                }

                @Override
                public void processErrorResponse(int cmdCode, String payload) {
                    passed[0] = false;
                }
            });

            waitUntilNonNull(passed);
            Assert.assertTrue(passed[0]);
        } catch (Throwable e) {
            onError(e);
        }

    }

    private InterpreterResponse execInterpreter(String command) {
        final InterpreterResponse response[] = new InterpreterResponse[1];
        ICallback<Object, InterpreterResponse> onResponseReceived = new ICallback<Object, InterpreterResponse>() {

            @Override
            public Object call(InterpreterResponse arg) {
                response[0] = arg;
                return null;
            }
        };
        pydevConsoleCommunication.execInterpreter(command, onResponseReceived);
        waitUntilNonNull(response);
        return response[0];
    }

    /**
     * #PyDev-502: PyDev 3.9 F2 doesn't support backslash continuations
     */
    public void testContinuation() throws Exception {
        try {
            InterpreterResponse response = execInterpreter("from os import \\");
            assertTrue(response.more);
            response = execInterpreter("  path,\\");
            assertTrue(response.more);
            response = execInterpreter("  remove");
            assertTrue(response.more);
            response = execInterpreter("\n");
        } catch (Throwable e) {
            onError(e);
        }
    }

    private void onError(Throwable e) throws Exception {
        String msg = "Found:\n stdout: " + stdout + "\n stderr: " + stderr + "\n";
        throw new Exception(msg, e);
    }

    public void testSimple() throws Exception {
        try {
            InterpreterResponse response = execInterpreter("import sys");
            assertFalse(response.more);
            response = execInterpreter("print('Python version:', sys.version_info[0])");
            assertFalse(response.more);
        } catch (Throwable e) {
            onError(e);
        }
    }

    /**
     * Test that variables can be seen
     */
    public void testVariable() throws Exception {
        try {
            execInterpreter("my_var=1");

            IVariableLocator frameLocator = new IVariableLocator() {
                @Override
                public String getPyDBLocation() {
                    // Make a reference to the virtual frame representing the interactive console
                    return PyThreadConsole.VIRTUAL_CONSOLE_ID + "\t" + PyStackFrameConsole.VIRTUAL_FRAME_ID + "\tFRAME";
                }

                @Override
                public String getThreadId() {
                    return PyThreadConsole.VIRTUAL_CONSOLE_ID;
                }
            };

            final Boolean passed[] = new Boolean[1];
            CustomGetFrameCommand cmd = new CustomGetFrameCommand(passed, debugTarget, frameLocator.getPyDBLocation());
            debugTarget.postCommand(cmd);
            waitUntilNonNull(passed);
            Assert.assertTrue(passed[0]);

            PyVariable[] variables = PyVariableCollection.getCommandVariables(cmd, debugTarget, frameLocator);
            Map<String, PyVariable> variableMap = new HashMap<String, PyVariable>();
            for (PyVariable variable : variables) {
                variableMap.put(variable.getName(), variable);
            }
            Assert.assertTrue(variableMap.containsKey("my_var"));
            Assert.assertEquals("int: 1", variableMap.get("my_var").getValueString());
        } catch (Throwable e) {
            onError(e);
        }
    }

}