package org.python.pydev.debug.newconsole;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.ActivationTokenAndQual;
import org.python.pydev.dltk.console.IScriptConsoleCommunication;
import org.python.pydev.dltk.console.IScriptConsoleInterpreter;
import org.python.pydev.dltk.console.InterpreterResponse;

/**
 * Default implementation for the console interpreter. 
 * 
 * Will ask things to the IScriptConsoleCommunication
 */
public class PydevConsoleInterpreter implements IScriptConsoleInterpreter {
    
    private IScriptConsoleCommunication consoleCommunication;

    private List<Runnable> closeRunnables = new ArrayList<Runnable>();

    /*
     * (non-Javadoc)
     * @see org.python.pydev.dltk.console.IScriptConsoleInterpreter#exec(java.lang.String)
     */
    public InterpreterResponse exec(String command) throws Exception {
        return consoleCommunication.execInterpreter(command);
    }

    /*
     * (non-Javadoc)
     * @see org.python.pydev.dltk.console.IScriptConsoleShell#getCompletions(java.lang.String, int, int)
     */
    public ICompletionProposal[] getCompletions(String commandLine, int position, int offset) throws Exception {

        String text = commandLine.substring(0, position);
        ActivationTokenAndQual tokenAndQual = PySelection.getActivationTokenAndQual(new Document(text), text.length(), true, false);
        
        String actTok = tokenAndQual.activationToken;
        if(tokenAndQual.qualifier != null && tokenAndQual.qualifier.length() > 0){
            if(actTok.length() > 0 && actTok.charAt(actTok.length()-1) != '.'){
                actTok += '.';
            }
            actTok += tokenAndQual.qualifier;
        }
        return consoleCommunication.getCompletions(actTok, offset);
    }

    /*
     * (non-Javadoc)
     * @see org.python.pydev.dltk.console.IScriptConsoleShell#getDescription(java.lang.String, int)
     */
    public String getDescription(String commandLine, int position) throws Exception {
        return consoleCommunication.getDescription(commandLine);
    }

    /*
     * (non-Javadoc)
     * @see org.python.pydev.dltk.console.IScriptConsoleShell#close()
     */
    public void close() throws IOException {
        if (consoleCommunication != null) {
            consoleCommunication.close();
            consoleCommunication = null;
        }
        // run all close runnables.
        for (Runnable r:this.closeRunnables) {
            r.run();
        }
        
        //we can close just once!
        this.closeRunnables = null; 
    }

    
    /*
     * (non-Javadoc)
     * @see org.python.pydev.dltk.console.IConsoleRequest#setConsoleCommunication(org.python.pydev.dltk.console.IScriptConsoleCommunication)
     */
    public void setConsoleCommunication(IScriptConsoleCommunication protocol) {
        this.consoleCommunication = protocol;
    }

    
    public void addCloseOperation(Runnable runnable) {
        this.closeRunnables.add(runnable);
    }


}
