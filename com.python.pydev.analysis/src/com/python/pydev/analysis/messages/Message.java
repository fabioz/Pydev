/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis.messages;

import org.python.pydev.core.IToken;

import com.python.pydev.analysis.IAnalysisPreferences;


public class Message extends AbstractMessage {


    private Object message;

    /**
     * @param startLine starts at 1
     * @param endLine starts at 1
     * @param startCol starts at 1
     * @param endCol starts at 1
     */
    public Message(int type, Object message, int startLine, int endLine, int startCol, int endCol, IAnalysisPreferences prefs) {
        super(type, startLine, endLine, startCol, endCol, prefs);
        this.message = message;
    }
    
    public Message(int type, Object message, IToken generator, IAnalysisPreferences prefs) {
        super(type, generator, prefs);
        this.message = message;
    }

    public Object getShortMessage() {
        return message;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Message)){
            return false;
        }
        Message m = (Message) obj;
        return this.getMessage().equals(m.getMessage());
    }
    
    @Override
    public int hashCode() {
        return getMessage().hashCode();
    }

}
