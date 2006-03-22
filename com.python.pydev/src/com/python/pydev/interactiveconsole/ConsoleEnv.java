/*
 * Created on Mar 7, 2006
 */
package com.python.pydev.interactiveconsole;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.console.IOConsole;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.PydevPlugin;

/**
 * This class represents a console environment, so that we can evaluate some expression.
 * 
 * @author Fabio
 */
public class ConsoleEnv {

    protected IProject project;
    protected IProcess process;
    protected IOConsole processConsole;
    protected boolean showInputInPrompt;
    protected IResource resource;

    /**
     * Creates a console environment for a given project and a given resource.
     * @param manager the interpreter manager to be used
     * @throws UserCanceledException 
     * @throws IOException 
     * @throws BadLocationException 
     */
    public ConsoleEnv(IProject project, IResource resource, boolean showInputInPrompt2, IInterpreterManager manager, PyEdit edit) throws UserCanceledException {
        this.project = project;
        this.resource = resource;
        try {
            startIt(project, resource, showInputInPrompt2, manager, edit);
        } catch (CoreException e) {
            PydevPlugin.log(e);
            throw new RuntimeException(e);
        
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        
        } catch (IOException e) {
            throw new RuntimeException(e);

        }        
    }

    /**
     * This funcion makes the launch of the interpreter in interactive mode and writes the initial commands 
     * (without showing them at the prompt).
     * @param manager the interpreter manager to be used
     * @param edit 
     * @throws UserCanceledException 
     */
    private void startIt(IProject project, IResource resource, boolean showInputInPrompt2, IInterpreterManager manager, PyEdit edit) throws CoreException, BadLocationException, IOException, UserCanceledException {
        IProcessFactory processFactory = new IProcessFactory();
        process = processFactory.createIProcess(project, resource, manager, edit);
        processConsole = processFactory.getIOConsole(process);
        
        //we don't want to show this...
        this.showInputInPrompt = false;
        write(null, InteractiveConsolePreferencesPage.getInitialInterpreterCmds());
        
        this.showInputInPrompt = showInputInPrompt2;
    }

    /**
     * This method executes the passed code.
     * 
     * @param code the string of code to execute
     */
    public void execute(String code) {
        try {
            boolean addFinalNewLine = false;

            //we will only add an additional new line when we are not evaluating on a per-line basis
            if(!InteractiveConsolePreferencesPage.evalOnNewLine()){
                String[] strings = code.split("\r\n");
                if(strings.length > 1){
                    for (String string : strings) {
                        if(string.length() > 0 && Character.isWhitespace(string.charAt(0))){
                            //and only if we have some whitespace in the beginning of some text
                            addFinalNewLine = true;
                            break;
                        }
                    }
                }
            }
            
            IDocument doc = processConsole.getDocument();
            write(doc, code);
            if(addFinalNewLine){
                write(doc, "\r\n");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes something to the process (throught the document or throught the console).
     */
    private void write(IDocument doc, String string) throws BadLocationException, IOException {
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

    public void terminate() {
        try {
            process.terminate();
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
    }

}
