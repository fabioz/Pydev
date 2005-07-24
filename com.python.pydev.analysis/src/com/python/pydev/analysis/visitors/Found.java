/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.visitors;

import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;

public class Found{
    /**
     * This is the token that is from the current module that created the token (if on some wild import)
     * 
     * May be equal to tok
     */
    public SourceToken generator;
    
    /**
     * This is the token that has been added to the namespace (may have been created on the current module or not).
     */
    public IToken tok;
    
    /**
     * Identifies if the current token has been used or not
     */
    public boolean used = false;
    
    Found(IToken tok, SourceToken generator){
        this.tok = tok;
        this.generator = generator;
    }
}