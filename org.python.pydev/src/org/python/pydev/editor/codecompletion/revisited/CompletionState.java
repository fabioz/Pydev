/*
 * Created on Feb 2, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.util.HashMap;
import java.util.Map;

import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;

/**
 * @author Fabio Zadrozny
 */
public class CompletionState implements ICompletionState {
    public String activationToken; 
    public int line;
    public int col;
    public IPythonNature nature;
    
    public Memo<String> memory = new Memo<String>();
    public Memo<Definition> definitionMemory = new Memo<Definition>();
    public Memo<IModule> wildImportMemory = new Memo<IModule>();
    public Memo<String> importedModsCalled = new Memo<String>();
    public Memo<String> findMemory = new Memo<String>();
    
    public boolean builtinsGotten=false;
    public boolean localImportsGotten=false;
    public boolean isInCalltip=false;

    public CompletionState getCopy(){
        CompletionState state = new CompletionState();
        state.activationToken = activationToken;
        state.line = line;
        state.col = col;
        state.importedModsCalled = importedModsCalled;
        state.nature = nature;
        
        state.memory = memory;
        state.wildImportMemory = wildImportMemory;
        state.definitionMemory = definitionMemory;
        state.findMemory = findMemory;

        state.builtinsGotten = builtinsGotten;
        state.localImportsGotten = localImportsGotten;
        state.isInCalltip = isInCalltip;
        
        return state;
    }
    
    /**
     * this is a class that can act as a memo and check if something is defined more than 'n' times
     * 
     * @author Fabio Zadrozny
     */
    static class Memo<E>{
        
    	private int max;

		public Memo(){
    		this.max = MAX_NUMBER_OF_OCURRENCES;
    	}
    	
		public Memo(int max){
    		this.max = max;
    	}
    	
        /**
         * if more than this number of ocurrences is found, we are in a recursion
         */
        private static final int MAX_NUMBER_OF_OCURRENCES = 5;
        
        public Map<IModule, Map<E, Integer>> memo = new HashMap<IModule, Map<E, Integer>>();

        public boolean isInRecursion(IModule caller, E def){
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
                    if(numberOfOccurences > max){
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
    
    /**
     * @param line2
     * @param col2
     * @param token
     * @param qual
     * @param nature2
     */
    public CompletionState(int line2, int col2, String token, IPythonNature nature2) {
        this.line = line2;
        this.col = col2;
        this.activationToken = token;
        this.nature = nature2;
    }
    
    public CompletionState(){
        
    }


    /**
     * @param module
     * @param base
     */
    public void checkWildImportInMemory(IModule caller, IModule wild) {
        if(this.wildImportMemory.isInRecursion(caller, wild)){
            throw new CompletionRecursionException("Possible recursion found (caller: "+caller.getName()+", import: "+wild.getName()+" ) - stopping analysis.");
        }
        
    }
    
    /**
     * @param module
     * @param definition
     */
    public void checkDefinitionMemory(IModule module, IDefinition definition) {
        if(this.definitionMemory.isInRecursion(module, (Definition) definition)){
            throw new CompletionRecursionException("Possible recursion found (token: "+definition+") - stopping analysis.");
        }

    }

    /**
     * @param module
     */
    public void checkFindMemory(IModule module, String value) {
        if(this.findMemory.isInRecursion(module, value)){
            throw new CompletionRecursionException("Possible recursion found (value: "+value+") - stopping analysis.");
        }
        
    }
    /**
     * @param module
     * @param base
     */
    public void checkMemory(IModule module, String base) {
        if(this.memory.isInRecursion(module, base)){
            throw new CompletionRecursionException("Possible recursion found (token: "+base+") - stopping analysis.");
        }
    }

    /**
     * @return a default completion state for globals (empty act. token)
     */
    public static ICompletionState getEmptyCompletionState(IPythonNature nature) {
        return new CompletionState(0,0,"", nature);
    }
    
    /**
     * @return a default completion state for globals (act token defined)
     */
    public static ICompletionState getEmptyCompletionState(String token, IPythonNature nature) {
        return new CompletionState(0,0,token, nature);
    }

    public String getActivationToken() {
        return activationToken;
    }

    public IPythonNature getNature() {
        return nature;
    }

    public void setActivationToken(String string) {
        activationToken = string;
    }

    public void setBuiltinsGotten(boolean b) {
        builtinsGotten = b;
    }

    public void setCol(int i) {
        col = i;
    }

    public void setLine(int i) {
        line = i;
    }

    public void setLocalImportsGotten(boolean b) {
        localImportsGotten = b;
    }

    public boolean getLocalImportsGotten() {
        return localImportsGotten;
    }

    public int getLine() {
        return line;
    }

    public int getCol() {
        return col;
    }

    public boolean getBuiltinsGotten() {
        return builtinsGotten;
    }

	public void raiseNFindTokensOnImportedModsCalled(IModule mod, String tok) {
		if(this.importedModsCalled.isInRecursion(mod, tok)){
			throw new CompletionRecursionException("Possible recursion found (mod: "+mod.getName()+", tok: "+ tok +" ) - stopping analysis.");
		}
	}

    public boolean getIsInCalltip() {
        return isInCalltip;
    }
    
}
