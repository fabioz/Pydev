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
import org.eclipse.jface.text.BadLocationException;

/**
 * @author Fabio Zadrozny
 */
public class CompletionCache {

    /**
     * This is the cache size.
     */
    public static final int CACHE_SIZE = 4;
    
    /**
     * This is where we retrieve / store the completions (key is doc and value is list of completions).
     */
    private Map cache = new HashMap();

    /**
     * This is a list where we add the entries. It is used so that we can keep an ordered list of document
     * (keys in the cache), so that we know which one to remove when it starts growing.
     */
    private List cacheEntries = new ArrayList();
    
    /**
     * This constant can be used for debbuging without cache.
     */
    public static final boolean USE_CACHE = false;

    
    /**
     * Constructor (creates the image cache).
     */
    public CompletionCache(){
    }
    
    
    /**
     * Returns all the completions in a list in position 0 of tuple and boolean in position 1, indication
     * whether templates should be shown.
     * @throws CoreException
     * @throws BadLocationException
     */
    public Object[] getProposals(CompletionRequest request) throws CoreException, BadLocationException {

        //this indicates whether the templates are going to be shown (if in imports section they are not).
        boolean showTemplates = true;
        
        //if non empty string, we're in imports section.
        String importsTipperStr = request.codeCompletion.getImportsTipperStr(request);
        
        String partialDoc = ""; //the document used as the key for the cache.
        if (importsTipperStr.length() != 0){
            partialDoc = importsTipperStr;
            showTemplates = false; //don't show templates if we are in the imports section.
        }else{
            partialDoc = PyCodeCompletion.getDocToParse(request.doc, request.documentOffset);
            partialDoc += request.activationToken;
        }
        
        
        //Get the cache proposals (if wanted - see the constant)
        List allProposals = null;
        if(USE_CACHE){
            allProposals = getCacheProposals(partialDoc, request.documentOffset, request.qlen);
        }

        
        
        if(allProposals == null){ //no cache proposals
            
            //get the code completion proposals.
            allProposals = request.codeCompletion.getCodeCompletionProposals(request);
            
            
            //add the newly gotten proposals to the cache.
            if(USE_CACHE){
                addProposalsToCache(partialDoc, allProposals);
            }
        }
        return new Object[]{allProposals, new Boolean(showTemplates)};

    }


    /**
     * This method adds the proposals to a cache, so that they can be retrieved later with little effort.
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
        if (cacheEntries.size() > CACHE_SIZE) {
            Object entry = cacheEntries.remove(0);
            cache.remove(entry);
        }
    }

    /**
     * Checks if we have proposals with the partial doc provided.
     * 
     * @param partialDoc string with the document (used in cache)
     * @param documentOffset used to get the insertion point.
     * @param qlen qualifier lenght used to know the insertion point.
     * @return list with the cache proposals or null if it is not found in the cache.
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
                        documentOffset - qlen,             //this is what changes         
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