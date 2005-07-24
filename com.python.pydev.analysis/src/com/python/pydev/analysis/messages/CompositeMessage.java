/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.messages;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;

import com.python.pydev.analysis.IAnalysisPreferences;



public class CompositeMessage extends AbstractMessage{

    public CompositeMessage(int type, SourceToken generator, IAnalysisPreferences prefs) {
        super(type, generator, prefs);
    }

    List<IMessage> msgs = new ArrayList<IMessage>();
    
    public void addMessage(IMessage msg){
        msgs.add(msg);
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
