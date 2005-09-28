/*
 * Created on 21/08/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.ArrayList;
import java.util.List;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.NameTok;
import org.python.parser.ast.aliasType;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
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

    /**
     * @return the name of the module that was resolved by visiting this token (or null if it
     * was not  
     * This information can be handy 
     */
    public String visitImportToken(IToken token, PythonNature nature, String moduleName) {
        //try to find it as a relative import

        if(token instanceof SourceToken){
            AbstractModule module = null;
            String initial = null;
            String foundAs = null;
            
            //representations that will be tested
            List<String> reps = new ArrayList<String>();
            
            String tail = null; //the tail is used for relative imports
            if(moduleName != null){
                tail = FullRepIterable.headAndTail(moduleName)[0]; //discard the head
            }            
            
            SourceToken tok = (SourceToken) token;
            SimpleNode ast = tok.getAst();
            
            
            //try to build the import string --------------------------------------------
            if(ast instanceof Import){
                Import imp = (Import) ast;
                aliasType[] n = imp.names;
                if(n != null){
                    for (int i = 0; i < n.length; i++) {
                        String name = ((NameTok)n[i].name).id;
                        
                        reps.add(name); //add as absolute
                        if(tail != null){
                            reps.add(tail+"."+name); //add as relative
                        }
                    }
                }
                
            }else if(ast instanceof ImportFrom){
                ImportFrom imp = (ImportFrom) ast;
                String fromModule = ((NameTok)imp.module).id;
                
                if(imp.names != null && imp.names.length > 0){
                    for (int i = 0; i < imp.names.length; i++) {
                        String name = ((NameTok)imp.names[i].name).id;
                        
                        reps.add(fromModule+"."+name); //add as absolute
                        if(tail != null){
                            reps.add(tail+"."+fromModule+"."+name); //add as relative
                        }
                    }
                }else{
                    
                    reps.add(fromModule); //add as absolute
                    if(tail != null){
                        reps.add(tail+"."+fromModule); //add as relative
                    }
                }
            }
    
            //try em ----------------------------------------------------------------
            //has absolute and relative all together
            
            for(String rep : reps){
                if(rep == null){
                    continue;
                }
                
                initial = rep;
                //get in reverse (e.g.: will return os.path and then os)
                //this happens because we have to check for the first module that defines it
                //in the most complete possible form.
                for (String part : new FullRepIterable(rep, true)) {
                    module = nature.getAstManager().getModule(part, nature);
                    if(module != null){
                        foundAs = part;
                        break;
                    }
                }
            
                if (module != null){
                    if( isRepAvailable(nature, module, initial, foundAs)){
                        return foundAs;
                    }
                    module = null; //the token was not really found, still can check with the absolute representation though
                }
            }
            
            //if it got here, it was not resolved
            messagesManager.addMessage(IAnalysisPreferences.TYPE_UNRESOLVED_IMPORT, token);
        }
        return null;
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
