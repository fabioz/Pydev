package org.python.pydev.debug.newconsole;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.AbstractDebugTargetWithTransmission;
import org.python.pydev.debug.model.IVariableLocator;
import org.python.pydev.debug.model.PyVariable;
import org.python.pydev.debug.model.PyVariableCollection;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.GetFrameCommand;
import org.python.pydev.debug.model.remote.VersionCommand;
import org.python.pydev.dltk.console.InterpreterResponse;
import org.python.pydev.plugin.SocketUtil;
import org.python.pydev.runners.SimpleRunner;

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

    @Override
    protected void setUp() throws Exception {
        String consoleFile = REF.createFileFromParts(TestDependent.TEST_PYDEV_PLUGIN_LOC, "pysrc", "pydevconsole.py")
                .getAbsolutePath();
        String pydevdDir = new File(TestDependent.TEST_PYDEV_DEBUG_PLUGIN_LOC, "pysrc").getAbsolutePath();
        Integer[] ports = SocketUtil.findUnusedLocalPorts(2);
        int port = ports[0];
        int clientPort = ports[1];

        homeDir = REF.getTempFileAt(new File("."), "fake_homedir");
        if (homeDir.exists()) {
            REF.deleteDirectoryTree(homeDir);
        }
        homeDir = homeDir.getAbsoluteFile();
        homeDir.mkdir();
        String[] cmdarray = new String[] { TestDependent.PYTHON_EXE, consoleFile, String.valueOf(port),
                String.valueOf(clientPort) };

        Map env = new TreeMap();
        env.put("HOME", homeDir.toString());
        env.put("PYTHONPATH", pydevdDir);
        String sysRoot = System.getenv("SystemRoot");
        if (sysRoot != null) {
            env.put("SystemRoot", sysRoot); //Needed on windows boxes (random/socket. module needs it to work).
        }

        String[] envp = new String[env.size()];
        int i = 0;
        for (Object entry : env.entrySet()) {
            Map.Entry e = (Entry) entry;
            Object key = e.getKey();
            envp[i] = key + "=" + e.getValue();
            i += 1;
        }

        process = SimpleRunner.createProcess(cmdarray, envp, null);
        pydevConsoleCommunication = new PydevConsoleCommunication(port, process, clientPort);
        pydevConsoleCommunication.hello(new NullProgressMonitor());

        ServerSocket socket = new ServerSocket(0);
        pydevConsoleCommunication.connectToDebugger(socket.getLocalPort());
        socket.setSoTimeout(5000);
        Socket accept = socket.accept();

        debugTarget = new DummyDebugTarget();
        debugTarget.startTransmission(accept);
    }

    @Override
    protected void tearDown() throws Exception {
        if (homeDir.exists()) {
            REF.deleteDirectoryTree(homeDir);
        }
        process.destroy();
        pydevConsoleCommunication.close();
        debugTarget.terminate();

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

        @Override
        public void processCommand(String sCmdCode, String sSeqCode, String payload) {
            System.out.println(sCmdCode + ":" + sSeqCode + ":" + payload);
        }

        public IProcess getProcess() {
            return null;
        }

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
            if (object[0] != null)
                return;
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

        final Boolean passed[] = new Boolean[1];
        pydevConsoleCommunication.postCommand(new VersionCommand(debugTarget) {
            @Override
            public void processOKResponse(int cmdCode, String payload) {
                if (cmdCode == AbstractDebuggerCommand.CMD_VERSION && "1.1".equals(payload))
                    passed[0] = true;
                else
                    passed[0] = false;
            }

            @Override
            public void processErrorResponse(int cmdCode, String payload) {
                passed[0] = false;
            }
        });

        waitUntilNonNull(passed);
        Assert.assertTrue(passed[0]);

    }

    private void execInterpreter(String command) {
        final Boolean done[] = new Boolean[1];
        ICallback<Object, InterpreterResponse> onResponseReceived = new ICallback<Object, InterpreterResponse>() {

            public Object call(InterpreterResponse arg) {
                done[0] = true;
                return null;
            }
        };
        ICallback<Object, Tuple<String, String>> onContentsReceived = new ICallback<Object, Tuple<String, String>>() {

            public Object call(Tuple<String, String> arg) {
                return null;
            }

        };
        pydevConsoleCommunication.execInterpreter(command, onResponseReceived, onContentsReceived);
        waitUntilNonNull(done);
    }

    /**
     * Test that variables can be seen
     */
    public void testVariable() throws Exception {

        execInterpreter("my_var=1");

        IVariableLocator frameLocator = new IVariableLocator() {
            public String getPyDBLocation() {
                return "console_main\t0\tFRAME";
            }
        };

        final Boolean passed[] = new Boolean[1];
        CustomGetFrameCommand cmd = new CustomGetFrameCommand(passed, debugTarget, frameLocator.getPyDBLocation());
        pydevConsoleCommunication.postCommand(cmd);
        waitUntilNonNull(passed);
        Assert.assertTrue(passed[0]);

        PyVariable[] variables = PyVariableCollection.getCommandVariables(cmd, debugTarget, frameLocator);
        Map<String, PyVariable> variableMap = new HashMap<String, PyVariable>();
        for (PyVariable variable : variables) {
            variableMap.put(variable.getName(), variable);
        }
        Assert.assertTrue(variableMap.containsKey("my_var"));
        Assert.assertEquals("int: 1", variableMap.get("my_var").getValueString());
    }

}