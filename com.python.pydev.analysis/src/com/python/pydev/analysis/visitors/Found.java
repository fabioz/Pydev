/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;

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

    /**
     * this is the scope where it was found
     */
    public ScopeItems scopeFound;
    
    public GenAndTok(IToken generator, IToken tok, int scopeId, ScopeItems scopeFound) {
        this.generator = generator;
        this.tok = tok;
        this.scopeId = scopeId;
        this.scopeFound = scopeFound;
    }
    
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("GenAndTok [ ");
        buffer.append(generator.getRepresentation());
        buffer.append(" - ");
        buffer.append(tok.getRepresentation());
        buffer.append(" (scopeId:");
        buffer.append(scopeId);
        buffer.append(") ]");
        return buffer.toString();
    }
}
public class Found implements Iterable<GenAndTok>{
    
    private List<GenAndTok> found = new ArrayList<GenAndTok>();
    
    /**
     * Identifies if the current token has been used or not
     */
    private boolean used = false;

    /**
     * If this is an import, it may be resolved to some module and some token within that module...
     */
	public Tuple<AbstractModule, String> modAndTokResolved;
    
    Found(IToken tok, IToken generator, int scopeId, ScopeItems scopeFound){
        this.found.add(new GenAndTok(generator, tok, scopeId, scopeFound));
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

    public void addGeneratorToFound(IToken generator2, IToken tok2, int scopeId, ScopeItems scopeFound) {
        this.found.add(new GenAndTok(generator2, tok2, scopeId, scopeFound));
    }

    public GenAndTok getSingle() {
        return found.get(0);
    }

    public boolean isImport() {
        return getSingle().generator.isImport();
    }
    
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Found { (used:");
        buffer.append(used);
        buffer.append(") [");
        
        for (GenAndTok g : found) {
            buffer.append(g);
            buffer.append("  ");
        }
        buffer.append(" ]}");
        return buffer.toString();
    }
}