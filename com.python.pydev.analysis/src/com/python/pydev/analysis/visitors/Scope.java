/*
 * Created on 27/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;

public class Scope {
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
     * Adds many tokens at once (created by the same token -- wild import)
     */
    public void addTokens(List list, IToken generator) {
        ScopeItems m = scope.peek();
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            IToken o = (IToken) iter.next();
            addToken(generator, m, o, o.getRepresentation());
        }
    }
    
    /**
     * @param generator
     * @param m
     * @param o
     */
    public void addToken(IToken generator, IToken o) {
        addToken(generator, o, o.getRepresentation());
        
    }
    
    public void addToken(IToken generator, IToken o, String rep) {
        ScopeItems m = scope.peek();
        addToken(generator, m, o, rep);
    }
    /**
     * @param generator
     * @param m
     * @param o
     */
    public void addToken(IToken generator, ScopeItems m, IToken o, String rep) {
        Found found = m.get(rep);
        if(found != null && !found.isUsed()){ //it will be removed from the scope
            if(m.getIfSubScope() == 0){
                messagesManager.addUnusedMessage(found);
            }else{
                found.addGeneratorToFound(generator,o, getCurrScopeId());
                //ok, it was added, so, let's call this over because we've appended it to another found,
                //no reason to re-add it again.
                return;
            }
        }
        if (generator == null){
            m.put(rep, new Found(o,(SourceToken) o, getCurrScopeId())); //the generator and the token are the same
        }else{
            m.put(rep, new Found(o,(SourceToken) generator, getCurrScopeId()));
        }
    }
    
    /**
     * initializes a new scope
     */
    public void startScope() {
        int newId = getNewId();
        scope.push(new ScopeItems(newId));
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
     * @param found
     * @param name
     * @return
     */
    public boolean find(String name) {
        boolean found = false;
        for (ScopeItems m : scope) {
            
            Found f = m.get(name);
            if(f != null){
                f.setUsed(true);
                found = true;
            }
        }
        return found;
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


}
