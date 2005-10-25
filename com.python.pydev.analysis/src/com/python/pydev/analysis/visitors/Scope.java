/*
 * Created on 27/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.plugin.nature.PythonNature;

public class Scope implements Iterable<ScopeItems>{
    
    /**
     * the scope type is a method
     */
    public static final int SCOPE_TYPE_GLOBAL = 0;

    /**
     * the scope type is a method
     */
    public static final int SCOPE_TYPE_METHOD = 1;

    /**
     * the scope type is a class
     */
    public static final int SCOPE_TYPE_CLASS = 2;
    
    /**
     * when we are at method definition, not always is as expected...
     */
    public boolean isInMethodDefinition = false;
    
    
    /**
     * used to check for invalid imports
     */
    private ImportChecker importChecker;


    /**
     * @param scopeType
     * @return a string representing the scope type
     */
    public static String getScopeTypeStr(int scopeType){
        switch(scopeType){
        case Scope.SCOPE_TYPE_GLOBAL:
            return "Global Scope";
        case Scope.SCOPE_TYPE_CLASS:
            return "Class Scope";
        case Scope.SCOPE_TYPE_METHOD:
            return "Method Scope";
        }
        return null;
    }
    
    /**
     * this stack is used to hold the scope. when we enter a scope, an item is added, and when we
     * exit, it is removed (and the analysis of unused tokens should happen at this time).
     */
    private Stack<ScopeItems> scope = new Stack<ScopeItems>();
    
    private MessagesManager messagesManager;
    
    private Stack<Integer> scopeId = new Stack<Integer>();

    
    
    private int scopeUnique = 0;

    private int getNewId() {
        scopeUnique++;
        return scopeUnique;
    }
    
    public Scope(MessagesManager messagesManager, PythonNature nature, String moduleName) {
        this.messagesManager = messagesManager;
        this.importChecker = new ImportChecker(this.messagesManager, nature, moduleName);
    }

    /**
     * Adds many tokens at once (created by the same token -- 
     * this ONLY happens for:
     * - wild imports (kind of obvious)
     * - imports such as import os.path (one token is created for os and one for os.path) 
     */
    public void addTokens(List list, IToken generator) {
    	boolean requireTokensToBeImports = false;
    	if(generator != null ){
    		//it will only enter here if it is a wild import (for other imports, the generator is equal to the
    		//import)
    		if(!generator.isImport() ){
    			throw new RuntimeException("Only imports should generate multiple tokens " +
    			"(it may be null for imports in the form import foo.bar, but then all its tokens must be imports).");
    		}
            importChecker.visitImportToken(generator);

    	}else{
    		requireTokensToBeImports = true;
    	}
    	
        ScopeItems m = scope.peek();
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            IToken o = (IToken) iter.next();
            //the token that we find here is either an import (in the case of some from xxx import yyy or import aa.bb)
            //or a Name, ClassDef, MethodDef, etc. (in the case of wild imports)
            if(requireTokensToBeImports){
            	if(!o.isImport()){
            		throw new RuntimeException("Expecting import token");
            	}
            	importChecker.visitImportToken(o);
            }
            Found found = addToken(generator, m, o, o.getRepresentation());
        	//check each import generated to see if we are able to resolve it.
        }
    }
    
    public Found addToken(IToken generator, IToken o) {
        return addToken(generator, o, o.getRepresentation());
        
    }
    
    public Found addToken(IToken generator, IToken o, String rep) {
        ScopeItems m = scope.peek();
        return addToken(generator, m, o, rep);
    }

    /**
     * Adds a token to the global scope
     */
    public Found addTokenToGlobalScope(IToken generator) {
        ScopeItems globalScope = getGlobalScope();
        return addToken(generator, globalScope, generator, generator.getRepresentation());
    }

    /**
     * when adding a token, we also have to check if there is not a token with the same representation
     * added, because if there is, the previous token might not be used at all...
     * 
     * @param generator that's the token that generated this representation
     * @param m the current scope items
     * @param o the generator token
     * @param rep the representation of the token (o) 
     * @return 
     */
    public Found addToken(IToken generator, ScopeItems m, IToken o, String rep) {
        if(generator == null){
            generator = o;
        }
        
        Found found = findFirst(rep, false);
        
        
        boolean isReimport = false;
        if(!isInMethodDefinition && found != null){ //it will be removed from the scope
            if(found.isImport() && generator.isImport()){
                isReimport = true;
                //keep on going, as it still might be used or unused
                
            }else{
            
                if(!found.isUsed() && m.getIfSubScope() == 0){ // it was not used, and we're not in an if scope...
                    
                    //this kind of unused message should only happen if we are at the same scope...
                    if(found.getSingle().scopeFound.getScopeId() == getCurrScopeId()){
                        
                        //we don't get unused at the global scope or class definition scope unless it's an import
                        if(found.getSingle().scopeFound.getScopeType() == Scope.SCOPE_TYPE_METHOD || found.isImport()){ 
                            messagesManager.addUnusedMessage(found);
                        }
                    }
                    
                } else{ 
                    //found... may have been or not used, anyway, we're in an if scope, so, that does not matter...
                    found.addGeneratorToFound(generator,o, getCurrScopeId(), getCurrScopeItems());
                    
                    //ok, it was added, so, let's call this over because we've appended it to another found,
                    //no reason to re-add it again.
                    return found;
                }
            }
        }
        
        Found newFound = new Found(o,(SourceToken) generator, m.getScopeId(), m);
        m.put(rep, newFound);

        if(isReimport){
            messagesManager.addReimportMessage(newFound);
        }
        return newFound;
    }
    
    public ScopeItems getCurrScopeItems() {
        return scope.peek();
    }


    /**
     * initializes a new scope
     */
    public void startScope(int scopeType) {
        int newId = getNewId();
        scope.push(new ScopeItems(newId, scopeType));
        scopeId.push(newId);
        
    }
    
    public int getCurrScopeId(){
        return scopeId.peek();
    }

    public ScopeItems endScope() {
        scopeId.pop();
        return scope.pop();
    }

    public int size() {
        return scope.size();
    }
    
    /**
     * 
     * @param name the name to search for
     * @param setUsed indicates if the found tokens should be marked used
     * @return true if a given name was found in any of the scopes we have so far
     */
    public boolean find(String name, boolean setUsed) {
        return findInScopes(name, setUsed).size() > 0;
    }

    public List<Found> findInScopes(String name, boolean setUsed) {
        List<Found> ret = new ArrayList<Found>();
        for (ScopeItems m : scope) {
            
            Found f = m.get(name);
            if(f != null){
                if(setUsed){
                    f.setUsed(true);
                }
                ret.add(f);
            }
        }
        return ret;
    }
    
    public Found findFirst(String name, boolean setUsed) {
        TopDownStackIteratable<ScopeItems> topDown = new TopDownStackIteratable<ScopeItems>(scope);
        for (ScopeItems m : topDown) {
            
            Found f = m.get(name);
            if(f != null){
                if(setUsed){
                    f.setUsed(true);
                }
                return f;
            }
        }
        return null;
    }
    
    public void addIfSubScope() {
        scope.peek().addIfSubScope();
    }

    public void removeIfSubScope() {
        scope.peek().removeIfSubScope();
    }

    public ScopeItems currentScope() {
        if(scope.size() == 0){
            return null;
        }
        return scope.peek();
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Scope: ");
        for (ScopeItems item : scope) {
            buffer.append("\n");
            buffer.append(item);
            
        }
        return buffer.toString();
    }

    public ScopeItems getGlobalScope() {
        return scope.get(0);
    }

	public Iterator<ScopeItems> iterator() {
		return this.scope.iterator();
	}

	
    /**
     * find out if an item is in the names to ignore given its full representation
     */
    public boolean findInNamesToIgnore(String fullRep, Map<String, IToken> lastInStack) {
        
        int i = fullRep.indexOf('.', 0);

        while(i >= 0){
            String sub = fullRep.substring(0,i);
            i = fullRep.indexOf('.', i+1);
            if(lastInStack.containsKey(sub)){
                return true;
            }
        }

        return lastInStack.containsKey(fullRep);
    }

    /**
     * checks if there is some token in the names that are defined (but should be ignored)
     */
    public boolean isInNamesToIgnore(String rep) {
    	int currScopeType = getCurrScopeItems().getScopeType();
    	
        for(ScopeItems s : this.scope){
        	//ok, if we are in a scope method, we may not get things that were defined in a class scope.
        	if(currScopeType == SCOPE_TYPE_METHOD && s.getScopeType() == SCOPE_TYPE_CLASS){
    			continue;
        	}
        	
        	Map<String,IToken> m = s.namesToIgnore;
            if(findInNamesToIgnore(rep, m)){
                return true;
            }
        }
        return false;
    }

}
