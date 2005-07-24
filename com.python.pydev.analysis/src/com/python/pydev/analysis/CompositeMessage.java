/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;


public class CompositeMessage extends AbstractMessage{

    public CompositeMessage(int type, int subType, SourceToken generator) {
        super(type, subType, generator);
    }

    List<IMessage> msgs = new ArrayList<IMessage>();
    
    public void addMessage(IMessage msg){
        msgs.add(msg);
    }
    
    public int getSeverity() {
        return 0;
    }

    public int getSubType() {
        return 0;
    }

    public String getShortMessage() {
        StringBuffer buffer = new StringBuffer();
        
        for (Iterator<IMessage> iter = msgs.iterator(); iter.hasNext();) {
            IMessage msg = iter.next();
            buffer.append(msg.getShortMessage());
            if(iter.hasNext()){
                buffer.append(", ");
            }
        }

        return buffer.toString();
    }


}
