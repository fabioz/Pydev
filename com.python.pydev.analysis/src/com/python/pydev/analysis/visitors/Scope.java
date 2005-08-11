/*
 * Created on 27/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;

public class Scope {
    
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
    
    public Scope(MessagesManager messagesManager) {
        this.messagesManager = messagesManager;
    }

    /**
     * Adds many tokens at once (created by the same token -- e.g.: wild import)
     */
    public void addTokens(List list, IToken generator) {
        ScopeItems m = scope.peek();
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            IToken o = (IToken) iter.next();
            addToken(generator, m, o, o.getRepresentation());
        }
    }
    
    public void addToken(IToken generator, IToken o) {
        addToken(generator, o, o.getRepresentation());
        
    }
    
    public void addToken(IToken generator, IToken o, String rep) {
        ScopeItems m = scope.peek();
        addToken(generator, m, o, rep);
    }

    /**
     * Adds a token to the global scope
     */
    public void addTokenToGlobalScope(IToken generator) {
        ScopeItems globalScope = getGlobalScope();
        addToken(generator, globalScope, generator, generator.getRepresentation());
    }

    /**
     * when adding a token, we also have to check if there is not a token with the same representation
     * added, because if there is, the previous token might not be used at all...
     * 
     * @param generator that's the token that generated this representation
     * @param m the current scope items
     * @param o the generator token
     * @param rep the representation of the token (o) 
     */
    public void addToken(IToken generator, ScopeItems m, IToken o, String rep) {
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
                    return;
                }
            }
        }
        
        Found newFound = new Found(o,(SourceToken) generator, m.getScopeId(), m);
        m.put(rep, newFound);

        if(isReimport){
            messagesManager.addReimportMessage(newFound);
        }
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

}
