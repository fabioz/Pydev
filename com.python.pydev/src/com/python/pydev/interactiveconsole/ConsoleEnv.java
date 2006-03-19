/*
 * Created on Mar 7, 2006
 */
package com.python.pydev.interactiveconsole;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.console.IConsole;
import org.python.pydev.core.IInterpreterManager;
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
    protected ProcessConsole processConsole;
    protected boolean showInputInPrompt;
    protected IResource resource;

    /**
     * Creates a console environment for a given project and a given resource.
     * @param manager the interpreter manager to be used
     */
    public ConsoleEnv(IProject project, IResource resource, boolean showInputInPrompt2, IInterpreterManager manager) {
        this.project = project;
        this.resource = resource;
        try {
            startIt(project, resource, showInputInPrompt2, manager);
        } catch (Exception e) {
            PydevPlugin.log(e);
            throw new RuntimeException(e);
        }
        
    }

    /**
     * This funcion makes the launch of the interpreter in interactive mode and writes the initial commands 
     * (without showing them at the prompt).
     * @param manager the interpreter manager to be used
     */
    private void startIt(IProject project, IResource resource, boolean showInputInPrompt2, IInterpreterManager manager) throws CoreException, BadLocationException, IOException {
        String type = null;
        if(manager.isPython()){
            type = "org.python.pydev.debug.regularLaunchConfigurationType";
        }else if (manager.isJython()){
            type = "org.python.pydev.debug.jythonLaunchConfigurationType";
        }else{
            throw new RuntimeException("Unable to determine its launch type.");
        }
        ILaunchConfiguration configuration = 
            AbstractLaunchShortcut.createDefaultLaunchConfiguration(resource, type, 
                AbstractLaunchShortcut.getDefaultLocation(resource), manager, project.getName());
        
        ILaunch launch = configuration.launch("interactive", new NullProgressMonitor());
        IProcess[] processes = launch.getProcesses();
        process = processes[0];

        IConsole console = DebugUITools.getConsole(process);
        processConsole = (ProcessConsole) console;
        
        //we don't want to show this...
        this.showInputInPrompt = false;
        write(null, process, InteractiveConsolePreferencesPage.getInitialInterpreterCmds());
        
        this.showInputInPrompt = showInputInPrompt2;
    }

    /**
     * This method executes the passed code.
     * 
     * @param code the string of code to execute
     */
    public void execute(String code) {
        try {
            IDocument doc = processConsole.getDocument();
            boolean addFinalNewLine = false;

            //we will only add an additional new line when we are not evaluating on a per-line basis
            if(!InteractiveConsolePreferencesPage.evalOnNewLine()){
                String[] strings = code.split("\r\n");
                for (String string : strings) {
                    if(string.length() > 0 && Character.isWhitespace(string.charAt(0))){
                        //and only if we have some whitespace in the beginning of some text
                        addFinalNewLine = true;
                        break;
                    }
                }
            }
            
            write(doc, process, code);
            if(addFinalNewLine){
                write(doc, process, "\r\n");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes something to the process (throught the document or throught the console).
     */
    private void write(IDocument doc, IProcess process2, String string) throws BadLocationException, IOException {
        if(showInputInPrompt){
            doc.replace(doc.getLength(), 0, string);
        }else{
            process.getStreamsProxy().write(string);
        }
    }

    /**
     * @return whether this process is already terminated or not
     */
    public boolean isTerminated() {
        return process.isTerminated();
    }

}
