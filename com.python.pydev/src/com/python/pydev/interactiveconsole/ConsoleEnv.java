/*
 * Created on Mar 7, 2006
 */
package com.python.pydev.interactiveconsole;

import java.io.OutputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.python.pydev.debug.ui.launching.AbstractLaunchShortcut;
import org.python.pydev.plugin.PydevPlugin;

/**
 * This class represents a console environment, so that we can evaluate some expression.
 * 
 * @author Fabio
 */
public class ConsoleEnv {

    protected IProject project;
    protected OutputStream consoleOutput;
    protected IProcess process;

    public ConsoleEnv(IProject project, final OutputStream consoleOutput) {
        this.project = project;
        this.consoleOutput = consoleOutput;
        try {
            this.consoleOutput.write("Starting PythonExecutionEnvironment".getBytes());
            
            ILaunchConfiguration configuration = 
                AbstractLaunchShortcut.createDefaultLaunchConfiguration(project, "org.python.pydev.debug.regularLaunchConfigurationType", 
                    AbstractLaunchShortcut.getDefaultLocation(project), PydevPlugin.getPythonInterpreterManager(), project.getName());
            
            ILaunch launch = configuration.launch("interactive", new NullProgressMonitor());
            IProcess[] processes = launch.getProcesses();
            process = processes[0];
            process.getStreamsProxy().getOutputStreamMonitor().addListener(new IStreamListener(){

                public void streamAppended(String text, IStreamMonitor monitor) {
                    try {
                        consoleOutput.write(text.getBytes());
                    } catch (Exception e) {
                        PydevPlugin.log(e);
                    }
                }
                
            });
            process.getStreamsProxy().getErrorStreamMonitor().addListener(new IStreamListener(){
                
                public void streamAppended(String text, IStreamMonitor monitor) {
                    try {
                        consoleOutput.write(text.getBytes());
                    } catch (Exception e) {
                        PydevPlugin.log(e);
                    }
                }
                
            });
        } catch (Exception e) {
            PydevPlugin.log(e);
            throw new RuntimeException(e);
        }
        
    }

    public String execute(String code) {
        System.out.println("executing:"+code);
        try {
            process.getStreamsProxy().write(code);
            process.getStreamsProxy().write("\r\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "";
    }

}
