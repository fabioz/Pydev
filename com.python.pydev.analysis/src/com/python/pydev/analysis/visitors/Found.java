/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.editor.codecompletion.revisited.IToken;

class GenAndTok{
    public IToken generator;
    public IToken tok;
    
    public GenAndTok(IToken generator, IToken tok) {
        this.generator = generator;
        this.tok = tok;
    }
}
public class Found implements Iterable<GenAndTok>{
    
    /**
     * This is the token that is from the current module that created the token (if on some wild import)
     * 
     * May be equal to tok
     */
    private List<IToken> generator = new ArrayList<IToken>();
    
    /**
     * This is the token that has been added to the namespace (may have been created on the current module or not).
     */
    private List<IToken> tok = new ArrayList<IToken>();
    
    /**
     * Identifies if the current token has been used or not
     */
    private boolean used = false;
    
    Found(IToken tok, IToken generator){
        this.tok.add(tok);
        this.generator.add(generator);
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
        final Iterator<IToken> iGen = generator.iterator();
        final Iterator<IToken> iTok = tok.iterator();
        return new Iterator<GenAndTok>(){

            public boolean hasNext() {
                return iGen.hasNext();
            }

            public GenAndTok next() {
                return new GenAndTok(iGen.next(), iTok.next());
            }

            public void remove() {
                throw new RuntimeException("not supported");
            }
            
        };
    }

    public void addGeneratorToFound(IToken generator2, IToken tok2) {
        this.generator.add(generator2);
        this.tok.add(tok2);
    }
}