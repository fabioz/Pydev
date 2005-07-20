/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis;

public class Message implements IMessage{

    private int type;
    private int subType;
    private String message;

    public Message(int type, int subType, String message) {
        this.type = type;
        this.subType = subType;
        this.message = message;
    }
    
    public int getType() {
        return type;
    }

    public int getSubType() {
        return subType;
    }

    public String getMessage() {
        return message;
    }

}
