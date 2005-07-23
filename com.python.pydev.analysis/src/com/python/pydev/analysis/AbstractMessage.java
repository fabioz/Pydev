/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis;

import java.util.HashMap;
import java.util.Map;

import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;

public abstract class AbstractMessage implements IMessage{

    public static final Map<Integer, String> messages = new HashMap<Integer, String>();

    private int type;

    private int subType;

    private SourceToken generator;

    public AbstractMessage(int type, int subType, SourceToken generator) {
        this.type = type;
        this.subType = subType;
        this.generator = generator;
    }

    private String getTypeStr() {
        if (messages.size() == 0) {
            messages.put(IMessage.SUB_UNUSED_IMPORT, "Unused import(s): %s");
            messages.put(IMessage.SUB_UNUSED_VARIABLE, "Unused variable: %s");
        }
        return messages.get(getSubType());

    }
    
    public int getType() {
        return type;
    }

    public int getSubType() {
        return subType;
    }

    public int getStartLine() {
        return generator.getLineDefinition();
    }

    public int getStartCol() {
        return generator.getColDefinition();
    }

    public int getEndLine() {
        return -1;
    }

    public int getEndCol() {
        return -1;
    }


    public String toString() {
        return getMessage();
    }

    public String getMessage() {
        String typeStr = getTypeStr();
        if(typeStr == null){
            throw new AssertionError("Unable to get message for type: "+getType());
        }
        String shortMessage = getShortMessage();
        if(shortMessage == null){
            throw new AssertionError("Unable to get shortMessage ("+typeStr+")");
        }
        return String.format(typeStr, shortMessage);
    }
    
    public SourceToken getGenerator() {
        return generator;
    }
}
