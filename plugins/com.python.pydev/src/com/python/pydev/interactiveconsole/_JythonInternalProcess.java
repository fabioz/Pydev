package com.python.pydev.interactiveconsole;
///*
// * Created on Mar 21, 2006
// */
//package com.python.pydev.interactiveconsole;
//
//import java.io.IOException;
//
//import org.eclipse.core.runtime.IProgressMonitor;
//import org.eclipse.core.runtime.IStatus;
//import org.eclipse.core.runtime.Status;
//import org.eclipse.core.runtime.jobs.Job;
//import org.eclipse.debug.core.DebugException;
//import org.eclipse.debug.core.ILaunch;
//import org.eclipse.debug.core.model.IProcess;
//import org.eclipse.debug.core.model.IStreamMonitor;
//import org.eclipse.debug.core.model.IStreamsProxy;
//import org.eclipse.ui.console.ConsolePlugin;
//import org.eclipse.ui.console.IConsole;
//import org.eclipse.ui.console.IOConsole;
//import org.eclipse.ui.console.IOConsoleInputStream;
//import org.python.pydev.core.docutils.StringUtils;
//import org.python.pydev.core.log.Log;
//import org.python.pydev.editor.PyEdit;
//import org.python.pydev.jython.IInteractiveConsole;
//import org.python.pydev.jython.JythonPlugin;
//import org.python.pydev.jython.ScriptOutput;
//
///**
// * This class defines a Jython process that runs inside of Eclipse.
// */
//public class JythonInternalProcess implements IProcess {
//
//    private boolean terminated;
//    private IInteractiveConsole interactiveConsole;
//    private IOConsole fConsole;
//    private IStreamsProxy proxy;
//    private InputReadJob job;
//    
//    public JythonInternalProcess(PyEdit editor){
//        try {
//            interactiveConsole = JythonPlugin.newInteractiveConsole();
//            fConsole = new IOConsole(getLabel(), JythonPlugin.getBundleInfo().getImageCache().getDescriptor("icons/python.gif"));
//            ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { fConsole });
//            
//            interactiveConsole.setOut(new ScriptOutput(JythonPlugin.getBlack(), fConsole, true));
//            interactiveConsole.setErr(new ScriptOutput(JythonPlugin.getRed(), fConsole, true));
//            interactiveConsole.set("False", 0);
//            interactiveConsole.set("True", 1);
//            interactiveConsole.set("editor", editor);
//
//            IOConsoleInputStream inputStream = fConsole.getInputStream();
//            inputStream.setColor(JythonPlugin.getGreen());
//            job = new InputReadJob(inputStream, getStreamsProxy());
//            job.setSystem(true);
//            job.schedule();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    /**
//     * @return the console associated with the run
//     */
//    public IOConsole getIOConsole() {
//        return fConsole;
//    }
//    
//    public String getLabel() {
//        return "Jython Internal Process";
//    }
//
//    public ILaunch getLaunch() {
//        throw new RuntimeException("not impl");
//    }
//
//    public IStreamsProxy getStreamsProxy() {
//        if(proxy == null){
//            proxy = new IStreamsProxy(){
//    
//                public IStreamMonitor getErrorStreamMonitor() {
//                    throw new RuntimeException("not impl");
//                }
//    
//                public IStreamMonitor getOutputStreamMonitor() {
//                    throw new RuntimeException("not impl");
//                }
//    
//                public void write(String input) throws IOException {
//                    input = StringUtils.rightTrim(input);
//                    interactiveConsole.push(input);
//                }
//            };
//        }
//        return proxy;
//    }
//
//    public void setAttribute(String key, String value) {
//        throw new RuntimeException("not impl");
//    }
//
//    public String getAttribute(String key) {
//        throw new RuntimeException("not impl");
//    }
//
//    public int getExitValue() throws DebugException {
//        throw new RuntimeException("not impl");
//    }
//
//    public Object getAdapter(Class adapter) {
//        throw new RuntimeException("not impl");
//    }
//
//    public boolean canTerminate() {
//        return true;
//    }
//
//    public boolean isTerminated() {
//        return terminated;
//    }
//
//    public void terminate() throws DebugException {
//        ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[]{fConsole});
//        terminated = true;
//    }
//    
//    /**
//     * Class that reads the input and writes it to the streams proxy.
//     */
//    private class InputReadJob extends Job {
//
//        private IStreamsProxy streamsProxy;
//        private IOConsoleInputStream fInput;
//        InputReadJob(IOConsoleInputStream fInput, IStreamsProxy streamsProxy) {
//            super("Jython Internal Process Console Input Job"); 
//            this.streamsProxy = streamsProxy;
//            this.fInput = fInput;
//        }
//
//        protected IStatus run(IProgressMonitor monitor) {
//            try {
//                byte[] b = new byte[1024];
//                int read = 0;
//                while (read >= 0 && !terminated) {
//                    read = fInput.read(b);
//                    if (read > 0) {
//                        String s = new String(b, 0, read);
//                        streamsProxy.write(s);
//                    }
//                }
//            } catch (IOException e) {
//                Log.log(e);
//            }
//            return Status.OK_STATUS;
//        }
//    }
//
//
//}
