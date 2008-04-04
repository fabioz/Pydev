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
import org.python.pydev.editor.codecompletion.AbstractCompletionProcessorWithCycling;
import org.python.pydev.editor.codecompletion.IPyCodeCompletion;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant2;
import org.python.pydev.editor.codecompletion.templates.PyTemplateCompletionProcessor;
import org.python.pydev.editor.simpleassist.ISimpleAssistParticipant2;

/**
 * Default implementation for the console interpreter. 
 * 
 * Will ask things to the IScriptConsoleCommunication
 */
public class PydevConsoleInterpreter implements IScriptConsoleInterpreter {
    
    private IScriptConsoleCommunication consoleCommunication;

    private List<Runnable> closeRunnables = new ArrayList<Runnable>();

    private List<ISimpleAssistParticipant2> simpleParticipants;

    private List<IPythonNature> naturesUsed;

    @SuppressWarnings("unchecked")
	public PydevConsoleInterpreter() {
        List<Object> p = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_SIMPLE_ASSIST);
        ArrayList<ISimpleAssistParticipant2> list = new ArrayList<ISimpleAssistParticipant2>();
        for(Object o:p){
        	if(o instanceof ISimpleAssistParticipant2){
        		list.add((ISimpleAssistParticipant2) o);
        	}
        }
		this.simpleParticipants = list;
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
            int position, int offset, int whatToShow) throws Exception {

        String text = commandLine.substring(0, position);
        ActivationTokenAndQual tokenAndQual = PySelection.getActivationTokenAndQual(new Document(text), text.length(), true, false);
        
        String actTok = tokenAndQual.activationToken;
        if(tokenAndQual.qualifier != null && tokenAndQual.qualifier.length() > 0){
            if(actTok.length() > 0 && actTok.charAt(actTok.length()-1) != '.'){
                actTok += '.';
            }
            actTok += tokenAndQual.qualifier;
        }

        
        boolean showOnlyTemplates = whatToShow == AbstractCompletionProcessorWithCycling.SHOW_ONLY_TEMPLATES;
        
        //simple completions (clients)
        ArrayList<ICompletionProposal> results = new ArrayList<ICompletionProposal>();
        
        for (ISimpleAssistParticipant2 participant : simpleParticipants) {
            results.addAll(participant.computeConsoleProposals(tokenAndQual.activationToken, 
                    tokenAndQual.qualifier, offset));
        }

        
        ArrayList<ICompletionProposal> results2 = new ArrayList<ICompletionProposal>();
        
        if(!showOnlyTemplates){
            //shell completions 
            ICompletionProposal[] consoleCompletions = consoleCommunication.getCompletions(actTok, offset);
            results2.addAll(Arrays.asList(consoleCompletions));
        }
        
        if(tokenAndQual.activationToken.length() == 0){
            //templates (only if we have no activation token)
            PyTemplateCompletionProcessor pyTemplateCompletionProcessor = new PyTemplateCompletionProcessor();
            pyTemplateCompletionProcessor.addTemplateProposals(viewer, offset, results2);
        
            Collections.sort(results2, IPyCodeCompletion.PROPOSAL_COMPARATOR);
        }
        
        ArrayList<ICompletionProposal> results3 = new ArrayList<ICompletionProposal>();
        if(!showOnlyTemplates){
            //other participants
            List<Object> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_COMPLETION);
            for (Object participant:participants) {
                if(participant instanceof IPyDevCompletionParticipant2){
                    IPyDevCompletionParticipant2 participant2 = (IPyDevCompletionParticipant2) participant;
                    results3.addAll(participant2.computeConsoleCompletions(tokenAndQual, this.naturesUsed, viewer, offset));
                }
            }
            Collections.sort(results3, IPyCodeCompletion.PROPOSAL_COMPARATOR);
        }
        results.addAll(results2);
        results.addAll(results3);
        
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
