package org.python.pydev.editor.codecompletion;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
        Set<ICompletionProposal> returnProposals = new HashSet<ICompletionProposal>();
        String lowerCaseQualifier = qualifier.toLowerCase();
        
        for (Iterator iter = pythonAndTemplateProposals.iterator(); iter.hasNext();) {
            Object o = iter.next();
            if (o instanceof ICompletionProposal) {
                ICompletionProposal proposal = (ICompletionProposal) o;
            
                String displayString = proposal.getDisplayString();
                if(onlyForCalltips){
                    if (displayString.equals(qualifier)){
                        returnProposals.add(proposal);
                        
                    }else if (displayString.length() > qualifier.length() && displayString.startsWith(qualifier)){
                        if(displayString.charAt(qualifier.length()) == '('){
                            returnProposals.add(proposal);
                            
                        }
                    }
                }else if (displayString.toLowerCase().startsWith(lowerCaseQualifier)) {
                    returnProposals.add(proposal);
                }
            }else{
                throw new RuntimeException("Error: expected instanceof ICompletionProposal and received: "+o.getClass().getName());
            }
        }
    
    
        // and fill with list elements
        ICompletionProposal[] proposals = returnProposals.toArray(new ICompletionProposal[returnProposals.size()]);
    
        Arrays.sort(proposals, IPyCodeCompletion.PROPOSAL_COMPARATOR);
        return proposals;
    }

}
