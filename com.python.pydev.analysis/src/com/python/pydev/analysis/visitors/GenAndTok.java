/*
 * Created on May 16, 2006
 */
package com.python.pydev.analysis.visitors;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.IToken;

public class GenAndTok{
    
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
     * These are the tokens that refer this generator
     */
    public List<IToken> references = new ArrayList<IToken>();
    
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
        buffer.append(") ");
        
        if(references.size() > 0){
	        buffer.append(" (references:");
	        for (IToken ref : references) {
				buffer.append(ref.getRepresentation());
				buffer.append(",");
			}
	        buffer.deleteCharAt(buffer.length()-1); //remove the last comma
	        buffer.append(") ");
        }
        
        buffer.append("]");
        return buffer.toString();
    }
}
