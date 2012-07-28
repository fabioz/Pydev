package com.aptana.js.interactive_console.rhino;

import java.io.IOException;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;

import com.aptana.interactive_console.console.InterpreterResponse;
import com.aptana.js.interactive_console.console.JSConsoleCommunication;
import com.aptana.js.interactive_console.console.env.RhinoEclipseProcess;
import com.aptana.shared_core.callbacks.ICallback;
import com.aptana.shared_core.net.SocketUtil;
import com.aptana.shared_core.utils.Log;
import com.aptana.shared_core.utils.Tuple;

public class RhinoConsoleMainTest extends TestCase {

    public void testRhinoConsole() throws Exception {
        final RhinoConsoleMain console = new RhinoConsoleMain();
        final Integer[] unusedPorts = SocketUtil.findUnusedLocalPorts(2);
        new Thread() {
            public void run() {
                try {
                    console.startXmlRpcServer(unusedPorts[0]);
                } catch (IOException e) {
                    Log.log(e);
                }
            };
        }.start();
        RhinoEclipseProcess process = new RhinoEclipseProcess(0, 0);
        JSConsoleCommunication comm = new JSConsoleCommunication(unusedPorts[0], process, unusedPorts[1]);
        ICallback<Object, Tuple<String, String>> onContentsReceived = new ICallback<Object, Tuple<String, String>>() {

            public Object call(Tuple<String, String> arg) {
                return null;
            }
        };
        ICallback<Object, InterpreterResponse> onResponseReceived = new ICallback<Object, InterpreterResponse>() {

            public Object call(InterpreterResponse arg) {
                return null;
            }
        };
        comm.hello(new NullProgressMonitor());
        comm.execInterpreter("var a = 10;", onResponseReceived, onContentsReceived);
        comm.getCompletions("var a = 10;", "a", 5);
        comm.getDescription("a");
        comm.close();

    }
}
