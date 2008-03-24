package org.python.pydev.debug.newconsole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.ActivationTokenAndQual;
import org.python.pydev.dltk.console.IScriptConsoleCommunication;
import org.python.pydev.dltk.console.IScriptConsoleInterpreter;
import org.python.pydev.dltk.console.InterpreterResponse;
import org.python.pydev.dltk.console.ui.IScriptConsoleViewer;
import org.python.pydev.editor.codecompletion.IPyCodeCompletion;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant2;
import org.python.pydev.editor.simpleassist.ISimpleAssistParticipant;

/**
 * Default implementation for the console interpreter. 
 * 
 * Will ask things to the IScriptConsoleCommunication
 */
public class PydevConsoleInterpreter implements IScriptConsoleInterpreter {
    
    private IScriptConsoleCommunication consoleCommunication;

    private List<Runnable> closeRunnables = new ArrayList<Runnable>();

    private List<ISimpleAssistParticipant> participants;

    private List<IPythonNature> naturesUsed;

    @SuppressWarnings("unchecked")
    public PydevConsoleInterpreter() {
        this.participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_SIMPLE_ASSIST);
    }
    
    /*
     * (non-Javadoc)
     * @see org.python.pydev.dltk.console.IScriptConsoleInterpreter#exec(java.lang.String)
     */
    public InterpreterResponse exec(String command) throws Exception {
        return consoleCommunication.execInterpreter(command);
    }

    @SuppressWarnings("unchecked")
    public ICompletionProposal[] getCompletions(IScriptConsoleViewer viewer, String commandLine, 
            int position, int offset) throws Exception {

        String text = commandLine.substring(0, position);
        ActivationTokenAndQual tokenAndQual = PySelection.getActivationTokenAndQual(new Document(text), text.length(), true, false);
        
        String actTok = tokenAndQual.activationToken;
        if(tokenAndQual.qualifier != null && tokenAndQual.qualifier.length() > 0){
            if(actTok.length() > 0 && actTok.charAt(actTok.length()-1) != '.'){
                actTok += '.';
            }
            actTok += tokenAndQual.qualifier;
        }

        //simple completions (clients)
        ArrayList<ICompletionProposal> results = new ArrayList<ICompletionProposal>();
        for (ISimpleAssistParticipant participant : participants) {
            results.addAll(participant.computeCompletionProposals(tokenAndQual.activationToken, 
                    tokenAndQual.qualifier, null, null, offset));
        }


        
        ArrayList<ICompletionProposal> results2 = new ArrayList<ICompletionProposal>();
        
        //shell completions 
        ICompletionProposal[] consoleCompletions = consoleCommunication.getCompletions(actTok, offset);
        results2.addAll(Arrays.asList(consoleCompletions));
        
        
        //other participants
        List<IPyDevCompletionParticipant> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_COMPLETION);
        for (IPyDevCompletionParticipant participant:participants) {
            if(participant instanceof IPyDevCompletionParticipant2){
                IPyDevCompletionParticipant2 participant2 = (IPyDevCompletionParticipant2) participant;
                results2.addAll(participant2.getConsoleCompletions(tokenAndQual, this.naturesUsed, viewer, offset));
            }
        }
        
        Collections.sort(results2, IPyCodeCompletion.PROPOSAL_COMPARATOR);
        results.addAll(results2);
        
        return (ICompletionProposal[]) results.toArray(new ICompletionProposal[0]);
    }

    
    /*
     * (non-Javadoc)
     * @see org.python.pydev.dltk.console.IScriptConsoleShell#getDescription(org.eclipse.jface.text.IDocument, int)
     */
    public String getDescription(IDocument doc, int position) throws Exception {
        ActivationTokenAndQual tokenAndQual = PySelection.getActivationTokenAndQual(doc, position, true, false);
        String actTok = tokenAndQual.activationToken;
        if(tokenAndQual.qualifier != null && tokenAndQual.qualifier.length() > 0){
            if(actTok.length() > 0 && actTok.charAt(actTok.length()-1) != '.'){
                actTok += '.';
            }
            actTok += tokenAndQual.qualifier;
        }
        return consoleCommunication.getDescription(actTok);
    }

    /*
     * (non-Javadoc)
     * @see org.python.pydev.dltk.console.IScriptConsoleShell#close()
     */
    public void close() {
        if (consoleCommunication != null) {
            try{
                consoleCommunication.close();
            }catch(Exception e){
                //ignore
            }
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

    public void setNaturesUsed(List<IPythonNature> naturesUsed) {
        this.naturesUsed = naturesUsed;
    }


}
