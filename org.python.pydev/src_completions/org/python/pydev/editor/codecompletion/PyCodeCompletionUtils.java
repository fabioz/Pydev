package org.python.pydev.editor.codecompletion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.docutils.DocUtils;

public class PyCodeCompletionUtils {

    /**
     * Return a document to parse, using some heuristics to make it parseable.
     * 
     * @param doc
     * @param documentOffset
     * @return
     */
    public static String getDocToParse(IDocument doc, int documentOffset) {
        int lineOfOffset = -1;
        try {
            lineOfOffset = doc.getLineOfOffset(documentOffset);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        if (lineOfOffset != -1) {
            String docToParseFromLine = DocUtils.getDocToParseFromLine(doc, lineOfOffset);
            if (docToParseFromLine != null)
                return docToParseFromLine;
            // return "\n"+docToParseFromLine;
            else
                return "";
        } else {
            return "";
        }
    }

    /**
     * Filters the python completions so that only the completions we care about are shown (given the qualifier) 
     * @param pythonAndTemplateProposals the completions to sort / filter
     * @param qualifier the qualifier we care about
     * @param onlyForCalltips if we should filter having in mind that we're going to show it for a calltip
     * @return the completions to show to the user
     */
    @SuppressWarnings("unchecked")
    public static ICompletionProposal[] onlyValidSorted(List pythonAndTemplateProposals, String qualifier, boolean onlyForCalltips) {
        //FOURTH: Now, we have all the proposals, only thing is deciding wich ones are valid (depending on
        //qualifier) and sorting them correctly.
        Map<String, List<ICompletionProposal>> returnProposals = new HashMap<String, List<ICompletionProposal>>();
        String lowerCaseQualifier = qualifier.toLowerCase();
        
        for (Iterator iter = pythonAndTemplateProposals.iterator(); iter.hasNext();) {
            Object o = iter.next();
            if (o instanceof ICompletionProposal) {
                ICompletionProposal proposal = (ICompletionProposal) o;
            
                String displayString;
                if(proposal instanceof PyCompletionProposal){
                    PyCompletionProposal pyCompletionProposal = (PyCompletionProposal) proposal;
                    displayString = pyCompletionProposal.getInternalDisplayStringRepresentation();
                    
                }else{
                    displayString = proposal.getDisplayString();
                }
                
                if(onlyForCalltips){
                    if (displayString.equals(qualifier)){
                        addProposal(returnProposals, proposal, displayString);
                        
                    }else if (displayString.length() > qualifier.length() && displayString.startsWith(qualifier)){
                        if(displayString.charAt(qualifier.length()) == '('){
                            addProposal(returnProposals, proposal, displayString);
                        }
                    }
                }else if (displayString.toLowerCase().startsWith(lowerCaseQualifier)) {
                    List<ICompletionProposal> existing = returnProposals.get(displayString);
                    if(existing != null){
                        //a proposal with the same string is already there...
                        boolean addIt = true;
                        if(proposal instanceof PyCompletionProposal){
                            PyCompletionProposal propP = (PyCompletionProposal) proposal;
                            
                            OUT:
                            for(Iterator<ICompletionProposal> it = existing.iterator(); it.hasNext();){
                                ICompletionProposal curr = it.next();
                                int overrideBehavior = propP.getOverrideBehavior(curr);
                                
                                switch (overrideBehavior) {
                                    case PyCompletionProposal.BEHAVIOR_COEXISTS:
                                        //just go on (it will be added later)
                                        break;
                                    case PyCompletionProposal.BEHAVIOR_OVERRIDES:
                                        it.remove();
                                        break;
                                        
                                    case PyCompletionProposal.BEHAVIOR_IS_OVERRIDEN:
                                        addIt=false;
                                        break OUT;

                                }
                            }
                        }
                        if(addIt){
                            existing.add(proposal);
                        }
                    }else{
                        //it's null, so, 1st insertion...
                        List<ICompletionProposal> lst = new ArrayList<ICompletionProposal>();
                        lst.add(proposal);
                        returnProposals.put(displayString, lst);
                    }
                }
            }else{
                throw new RuntimeException("Error: expected instanceof ICompletionProposal and received: "+o.getClass().getName());
            }
        }
    
    
        // and fill with list elements
        Collection<List<ICompletionProposal>> values = returnProposals.values();
        ArrayList<ICompletionProposal> tproposals = new ArrayList<ICompletionProposal>();
        for(List<ICompletionProposal> value:values){
            tproposals.addAll(value);
        }
        ICompletionProposal[] proposals = tproposals.toArray(new ICompletionProposal[returnProposals.size()]);
    
        Arrays.sort(proposals, IPyCodeCompletion.PROPOSAL_COMPARATOR);
        return proposals;
    }

    private static void addProposal(Map<String, List<ICompletionProposal>> returnProposals, ICompletionProposal proposal, String displayString) {
        List<ICompletionProposal> lst = returnProposals.get(displayString);
        if(lst == null){
            lst = new ArrayList<ICompletionProposal>();
            returnProposals.put(displayString, lst);
        }
        lst.add(proposal);
    }

}
