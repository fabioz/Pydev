/*
 * Created on Feb 2, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import org.python.pydev.plugin.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public class CompletionState {
    public String activationToken; 
    public int line;
    public int col;
    public PythonNature nature;

    public boolean recursing=false;
    
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
        state.recursing = recursing;
        state.nature = nature;
        return state;
    }
    
}
