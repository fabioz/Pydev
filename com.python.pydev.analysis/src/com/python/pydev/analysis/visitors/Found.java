/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.editor.codecompletion.revisited.IToken;

class GenAndTok{
    
    /**
     * This is the token that is from the current module that created the token (if on some wild import)
     * 
     * May be equal to tok
     */
    public IToken generator;

    /**
     * This is the token that has been added to the namespace (may have been created on the current module or not).
     */
    public IToken tok;
    
    /**
     * the scope id of the definition
     */
    public int scopeId;
    
    public GenAndTok(IToken generator, IToken tok, int scopeId) {
        this.generator = generator;
        this.tok = tok;
        this.scopeId = scopeId;
    }
}
public class Found implements Iterable<GenAndTok>{
    
    private List<GenAndTok> found = new ArrayList<GenAndTok>();
    
    /**
     * Identifies if the current token has been used or not
     */
    private boolean used = false;
    
    Found(IToken tok, IToken generator, int scopeId){
        this.found.add(new GenAndTok(generator, tok, scopeId));
    }

    /**
     * @param used The used to set.
     */
    public void setUsed(boolean used) {
        this.used = used;
    }

    /**
     * @return Returns the used.
     */
    public boolean isUsed() {
        return used;
    }

    public Iterator<GenAndTok> iterator() {
        return this.found.iterator();
    }

    public void addGeneratorToFound(IToken generator2, IToken tok2, int scopeId) {
        this.found.add(new GenAndTok(generator2, tok2, scopeId));
    }

    public GenAndTok getSingle() {
        return found.get(0);
    }
}