/*
 * Created on 21/08/2005
 */
package com.python.pydev.analysis.visitors;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.IAnalysisPreferences;

public class ImportChecker {

    /**
     * used to manage the messages
     */
    private MessagesManager messagesManager;

    /**
     * constructor
     */
    public ImportChecker(MessagesManager messagesManager) {
        this.messagesManager = messagesManager;
    }

    public void visitImportToken(IToken token, PythonNature nature) {
        String repComplete = token.getCompletePath();
        
        
        AbstractModule module = null;
        String initial = repComplete;
        String foundAs = null;
        //first check for relative imports
        for (String part : new FullRepIterable(repComplete, true)) {
            module = nature.getAstManager().getModule(part, nature);
            if(module != null){
                //may have found a module (if it is the same module were the token was defined
                //we will have to discard it later).
                foundAs = part;
                break;
            }
        }
        
        boolean found = false;
        if (module != null){
            found = isRepAvailable(nature, module, initial, foundAs);
            if(found){
                return;
            }
            module = null; //the token was not really found, still can check with the absolute representation though
        }
        
        if(module == null){
            //still not found
            String rep = token.getRepresentation();
            initial = rep;
            for (String part : new FullRepIterable(rep, true)) {
                module = nature.getAstManager().getModule(part, nature);
                if(module != null){
                    foundAs = part;
                    break;
                }
            }
        }
        
        found = isRepAvailable(nature, module, initial, foundAs);

        if(! found){
            //if it got here, it was not resolved
            messagesManager.addMessage(IAnalysisPreferences.TYPE_UNRESOLVED_IMPORT, token);
        }
    }

    private boolean isRepAvailable(PythonNature nature, AbstractModule module, String initial, String foundAs) {
        boolean found = false;
        if(module != null){
            //ok, the module was found, but if we were getting some token defined in it, we still have to check it 
            //(e.g.: os defines path as a token and not a module)
            if(initial.equals(foundAs)){
                found = true;
                
            } else{
                
                String qualifier = initial.substring(foundAs.length());
                if(qualifier.startsWith(".")){
                    qualifier = qualifier.substring(1);
                }

                //ok, we are getting some token from the module... let's see if it is really available.
                String[] headAndTail = FullRepIterable.headAndTail(qualifier);
                String actToken = headAndTail[0];  //tail (if os.path, it is os) 
                String hasToBeFound = headAndTail[1]; //head (it is path)
                
                //if it was os.path:
                //initial would be os.path
                //foundAs would be os
                //actToken would be path
                
                //now, what we will do is try to do a code completion in os and see if path is found
                CompletionState comp = CompletionState.getEmptyCompletionState(actToken, nature);
                IToken[] completionsForModule = nature.getAstManager().getCompletionsForModule(module, comp);
                for (IToken foundTok : completionsForModule) {
                    if(foundTok.getRepresentation().equals(hasToBeFound)){
                        found = true;
                    }
                }
            }
        }
        return found;
    }

}
