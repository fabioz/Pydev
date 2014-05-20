package org.python.pydev.debug.processfactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IProcessFactory;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.jvnet.process_factory.AbstractProcess;
import org.jvnet.process_factory.ProcessFactory;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.ui.DebugPrefsPage;

public class PyProcessFactory implements IProcessFactory {

    public static final String PROCESS_FACTORY_ID = "org.python.pydev.debug.processfactory.PyProcessFactory";

    @Override
    public IProcess newProcess(ILaunch launch, Process process, String label, Map attributes) {
        return new RuntimeProcess(launch, new ProcessWrapper(process), label, attributes);
    }

    static class ProcessWrapper extends Process {

        private Process process;

        public ProcessWrapper(Process process) {
            this.process = process;
        }

        @Override
        public OutputStream getOutputStream() {
            return process.getOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return process.getInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return process.getErrorStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            return process.waitFor();
        }

        @Override
        public int exitValue() {
            return process.exitValue();
        }

        @Override
        public void destroy() {
            if (DebugPrefsPage.getKillSubprocessesWhenTerminatingProcess()) {
                try {
                    AbstractProcess p = ProcessFactory.CreateProcess(process);
                    //I.e.: this is the real change in this wrapper: when killing a process, we'll kill the children 
                    //processes too, not only the main process (i.e.: so that we don't have zombie processes alive for 
                    //Django, etc).
                    p.killRecursively();
                } catch (Exception e) {
                    Log.log(e);
                }
            }
            try {
                process.destroy();
            } catch (Exception e) {
                Log.log(e);
            }
        }

    }

}
