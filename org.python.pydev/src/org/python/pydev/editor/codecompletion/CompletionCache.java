/*
 * Created on Aug 11, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.contentassist.CompletionProposal;

/**
 * @author Fabio Zadrozny
 */
public class CompletionCache {
    
    private Map cache = new HashMap();

    private List cacheEntries = new ArrayList();
    

    public List getAllProposals(String theDoc, String activationToken,
            int documentOffset, int qlen,
            PyCodeCompletion codeCompletion) {
        
        List allProposals = null;

        if (cache.containsKey(theDoc)) {
            //if it is in the cache, we can just get the proposals, 
            //the only thing here, is that we have to change its size depending
            //on the new qlen.
            
            
            allProposals = new ArrayList();
            List proposals = (List) cache.get(theDoc);
            
            for (Iterator iter = proposals.iterator(); iter.hasNext();) {
                CompletionProposal prop = (CompletionProposal) iter.next();
                String displayString = prop.getDisplayString();
                allProposals.add(new CompletionProposal(displayString,
                        documentOffset - qlen, qlen, displayString.length()));
            }
        
            
            
        } else {
            List theList = codeCompletion.autoComplete(theDoc,
                    activationToken);

            allProposals = new ArrayList();

            for (Iterator iter = theList.iterator(); iter.hasNext();) {
                String element = (String) iter.next();
                CompletionProposal proposal = new CompletionProposal(element,
                        documentOffset - qlen, qlen, element.length());
                allProposals.add(proposal);
            }
            cacheEntries.add(theDoc);
            cache.put(theDoc, allProposals);
            //we don't want this the get huge...
            if (cacheEntries.size() > 20) {
                Object entry = cacheEntries.remove(0);
                cache.remove(entry);
            }
        }
        return allProposals;

    }
}