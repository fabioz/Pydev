/*
 * Created on 21/08/2005
 */
package com.python.pydev.analysis.visitors;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;

/**
 * The import checker not only generates information on errors for unresolved modules, but also gathers
 * dependency information so that we can do incremental building of dependent modules.
 * 
 * @author Fabio
 */
public class ImportChecker {

    /**
     * used to manage the messages
     */
    private MessagesManager messagesManager;
    
    /**
     * Information that will be used for generating dependency info (when this object is constructed,
     * all the dependency information regarding the module that we will analyze will be removed). 
     */
    private AbstractAdditionalDependencyInfo infoForProject;

    /**
     * this is the nature we are analyzing
     */
    private PythonNature nature;

    /**
     * this is the name of the module that we are analyzing
     */
    private String moduleName;

    /**
     * constructor - will remove all dependency info on the project that we will start to analyze
     */
    public ImportChecker(MessagesManager messagesManager, PythonNature nature, String moduleName) {
        this.messagesManager = messagesManager;
        
        this.nature = nature;
        this.moduleName = moduleName;
    }

    /**
     * @param token MUST be an import token
     * 
     * @return the module where the token was found and a String representing the way it was found 
     * in the module.
     * 
     * Note: it may return information even if the token was not found in the representation required. This is useful
     * to get dependency info, because it is actually dependent on the module, event though it does not have the
     * token we were looking for.
     */
    public Tuple<AbstractModule, String> visitImportToken(IToken token) {
        //try to find it as a relative import
    	Tuple<AbstractModule, String> modTok = null;
        boolean wasResolved = false;
    	
        if(token instanceof SourceToken){
        	
        	modTok = nature.getAstManager().findOnImportedMods(new IToken[]{token}, nature, token.getRepresentation(), moduleName);
        	if(modTok != null && modTok.o1 != null){

        		if(modTok.o2.length() == 0){
        		    wasResolved = true;
                    
        		} else if( isRepAvailable(nature, modTok.o1, modTok.o2)){
        		    wasResolved = true;
                }
        	}
        	
            
            //if it got here, it was not resolved
        	if(!wasResolved && messagesManager != null){
        		messagesManager.addMessage(IAnalysisPreferences.TYPE_UNRESOLVED_IMPORT, token);
        	}
        }
        
        if(infoForProject != null && moduleName != null){
            //add the dependency info
            if(modTok != null){
                infoForProject.addDependency(moduleName, modTok.o1.getName());
                System.out.printf("Add normal dependency: %s - %s\n", moduleName, modTok.o1.getName());
            }else{
                // still undefined, altough, if it is created, we want this to be reanalyzed (we add it as absolute and relative)
                System.out.printf("Add undefined dependency: %s - %s\n", moduleName, token.getOriginalRep());
                System.out.printf("Add undefined dependency: %s - %s\n", moduleName, token.getAsRelativeImport(moduleName));
                infoForProject.addDependency(moduleName, token.getOriginalRep()); 
                infoForProject.addDependency(moduleName, token.getAsRelativeImport(moduleName)); 
            }
        }
        
        //might still return a modTok, even if the token we were looking for was not found.
        return modTok;
    }
    

    private boolean isRepAvailable(PythonNature nature, AbstractModule module, String qualifier) {
        boolean found = false;
        if(module != null){
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
        return found;
    }

}
