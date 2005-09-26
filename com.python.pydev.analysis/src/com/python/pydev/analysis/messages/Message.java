/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis.messages;

import org.python.pydev.editor.codecompletion.revisited.IToken;

import com.python.pydev.analysis.IAnalysisPreferences;


public class Message extends AbstractMessage {


    private Object message;

    public Message(int type, Object message, IToken generator, IAnalysisPreferences prefs) {
        super(type, generator, prefs);
        this.message = message;
    }

    public Object getShortMessage() {
        return message;
    }

}
