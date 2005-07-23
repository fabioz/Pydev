/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis;

import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;

public class Message extends AbstractMessage {


    private String message;

    public Message(int type, int subType, String message, SourceToken generator) {
        super(type, subType, generator);
        this.message = message;
    }



    public String getShortMessage() {
        return message;
    }

}
