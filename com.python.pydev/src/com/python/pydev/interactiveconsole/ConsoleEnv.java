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

    
    public ConsoleEnv(IProject project, IResource resource, boolean showInputInPrompt2) {
        this.project = project;
        this.resource = resource;
        try {
            startIt(project, resource, showInputInPrompt2);
        } catch (Exception e) {
            PydevPlugin.log(e);
            throw new RuntimeException(e);
        }
        
    }

    private void startIt(IProject project, IResource resource, boolean showInputInPrompt2) throws CoreException, BadLocationException, IOException {
        ILaunchConfiguration configuration = 
            AbstractLaunchShortcut.createDefaultLaunchConfiguration(resource, "org.python.pydev.debug.regularLaunchConfigurationType", 
                AbstractLaunchShortcut.getDefaultLocation(resource), PydevPlugin.getPythonInterpreterManager(), project.getName());
        
        ILaunch launch = configuration.launch("interactive", new NullProgressMonitor());
        IProcess[] processes = launch.getProcesses();
        process = processes[0];

        IConsole console = DebugUITools.getConsole(process);
        processConsole = (ProcessConsole) console;
        
        //we don't want to show this...
        this.showInputInPrompt = false;
        write(null, process, "import sys; sys.ps1=''; sys.ps2=''\r\n");
        write(null, process, "'PYTHONPATH:',sys.path\r\n");
        
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
