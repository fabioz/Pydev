/*
 * Created on Feb 2, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.util.HashMap;
import java.util.Map;

import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public class CompletionState {
    public String activationToken; 
    public int line;
    public int col;
    public PythonNature nature;
    
    /**
     * this is a class that can act as a memo and check if something is defined more than 'n' times
     * 
     * @author Fabio Zadrozny
     */
    static class Memo<E>{
        
        /**
         * if more than this number of ocurrences is found, we are in a recursion
         */
        private static final int MAX_NUMBER_OF_OCURRENCES = 5;
        
        public Map<AbstractModule, Map<E, Integer>> memo = new HashMap<AbstractModule, Map<E, Integer>>();

        public boolean isInRecursion(AbstractModule caller, E def){
            Map<E, Integer> val;
            
            boolean occuredMoreThanMax = false;
            if(!memo.containsKey(caller)){
                
                //still does not exist, let's create the structure...
                val = new HashMap<E, Integer>();
                memo.put(caller, val);
                
            }else{
                val = memo.get(caller);
                
                if(val.containsKey(def)){ //may be a recursion
                    Integer numberOfOccurences = val.get(def);
                    
                    //should never be null...
                    if(numberOfOccurences > MAX_NUMBER_OF_OCURRENCES){
                        occuredMoreThanMax = true; //ok, we are recursing...
                    }
                }
            }
            
            //let's raise the number of ocurrences anyway
            Integer numberOfOccurences = val.get(def);
            if(numberOfOccurences == null){
                val.put(def, 1); //this is the first ocurrence
            }else{
                val.put(def, numberOfOccurences+1);
            }
            
            return occuredMoreThanMax;
        }
    }
    
    public Memo<String> memory = new Memo<String>();
    public Memo<Definition> definitionMemory = new Memo<Definition>();
    public Memo<AbstractModule> wildImportMemory = new Memo<AbstractModule>();
    
    public boolean builtinsGotten=false;
    
    /**
     * @param line2
     * @param col2
     * @param token
     * @param qual
     * @param nature2
     */
    public CompletionState(int line2, int col2, String token, PythonNature nature2) {
        this.line = line2;
        this.col = col2;
        this.activationToken = token;
        this.nature = nature2;
    }
    
    public CompletionState(){
        
    }

    public CompletionState getCopy(){
        CompletionState state = new CompletionState();
        state.activationToken = activationToken;
        state.line = line;
        state.col = col;
        state.builtinsGotten = builtinsGotten;
        state.nature = nature;
        state.memory = memory;
        state.wildImportMemory = wildImportMemory;
        state.definitionMemory = definitionMemory;
        return state;
    }

    /**
     * @param module
     * @param base
     */
    public void checkWildImportInMemory(AbstractModule caller, AbstractModule wild) {
        if(this.wildImportMemory.isInRecursion(caller, wild)){
            throw new CompletionRecursionException("Possible recursion found (caller: "+caller.getName()+", import: "+wild.getName()+" ) - stopping analysis.");
        }
        
    }
    
    /**
     * @param module
     * @param definition
     */
    public void checkDefinitionMemory(AbstractModule module, Definition definition) {
        if(this.definitionMemory.isInRecursion(module, definition)){
            throw new CompletionRecursionException("Possible recursion found (token: "+definition+") - stopping analysis.");
        }

    }
    /**
     * @param module
     * @param base
     */
    public void checkMemory(SourceModule module, String base) {
        if(this.memory.isInRecursion(module, base)){
            throw new CompletionRecursionException("Possible recursion found (token: "+base+") - stopping analysis.");
        }
    }

    /**
     * @return a default completion state for globals (empty act. token)
     */
    public static CompletionState getEmptyCompletionState(PythonNature nature) {
        return new CompletionState(0,0,"", nature);
    }
    
    /**
     * @return a default completion state for globals (act token defined)
     */
    public static CompletionState getEmptyCompletionState(String token, PythonNature nature) {
        return new CompletionState(0,0,token, nature);
    }
    
}
