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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.python.pydev.editor.PyEdit;

/**
 * @author Fabio Zadrozny
 */
public class CompletionCache {

    private Map cache = new HashMap();

    private List cacheEntries = new ArrayList();

    /**
     * Returns all the completions
     * 
     * @param edit
     * @param doc
     * @param partialDoc
     * @param activationToken
     * @param documentOffset
     * @param qlen
     * @param codeCompletion
     * @return
     * @throws CoreException
     */
    public List getAllProposals(PyEdit edit, IDocument doc,
            String activationToken, int documentOffset, int qlen,
            PyCodeCompletion codeCompletion) throws CoreException {

        String importsTipperStr = codeCompletion.getImportsTipperStr(activationToken, edit, doc, documentOffset);
        
        String partialDoc = "";
        if (importsTipperStr.length() != 0){
            partialDoc = importsTipperStr;
            System.out.println(importsTipperStr);
        }else{
            partialDoc = PyCodeCompletion.getDocToParse(doc, documentOffset);
            partialDoc += activationToken;
        }
        
        List allProposals = getCacheProposals(partialDoc, documentOffset, qlen);

        if(allProposals == null){ //no cache proposals
            List theList = codeCompletion.autoComplete(edit, doc, documentOffset, activationToken);

            allProposals = new ArrayList();

            for (Iterator iter = theList.iterator(); iter.hasNext();) {
                String element[] = (String[]) iter.next();
                
                CompletionProposal proposal = new CompletionProposal(element[0],
                        documentOffset - qlen, qlen, element[0].length(),null, null, null, element[1]);
                allProposals.add(proposal);
            }
            addProposalsToCache(partialDoc, allProposals);
        }
        return allProposals;

    }

    /**
     * @param partialDoc
     * @param allProposals
     */
    private void addProposalsToCache(String partialDoc, List allProposals) {
        for (Iterator iter = allProposals.iterator(); iter.hasNext();) {
            CompletionProposal element = (CompletionProposal) iter.next();
            
            String displayString = element.getDisplayString();
            //we don't add to cache if there is an error here...
            if(displayString.startsWith("ERROR") || displayString.startsWith("SERVER_ERROR")){
                return;
            }
        }
        
        cacheEntries.add(partialDoc);
        cache.put(partialDoc, allProposals);
        //we don't want this to get huge...
        if (cacheEntries.size() > 4) {
            Object entry = cacheEntries.remove(0);
            cache.remove(entry);
        }
    }

    /**
     * @param partialDoc
     * @param documentOffset
     * @param qlen
     * @return
     */
    private List getCacheProposals(String partialDoc, int documentOffset, int qlen) {
        List allProposals = null;
        if (cache.containsKey(partialDoc)) {
            //if it is in the cache, we can just get the proposals,
            //the only thing here, is that we have to change its size depending
            //on the new qlen.

            allProposals = new ArrayList();
            List proposals = (List) cache.get(partialDoc);

            for (Iterator iter = proposals.iterator(); iter.hasNext();) {
                CompletionProposal prop = (CompletionProposal) iter.next();
                String displayString = prop.getDisplayString();
                allProposals.add(new CompletionProposal(
                        displayString,                     //   
                        documentOffset - qlen,             //         
                        qlen,                              //         
                        displayString.length(),            //        
                        prop.getImage(),                   //      
                        prop.getDisplayString(),           //     
                        prop.getContextInformation(),      //     
                        prop.getAdditionalProposalInfo()));//  
            }
        }
        return allProposals;
    }
}