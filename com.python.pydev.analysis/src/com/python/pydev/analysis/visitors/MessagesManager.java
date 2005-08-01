/*
 * Created on 24/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.messages.CompositeMessage;
import com.python.pydev.analysis.messages.IMessage;
import com.python.pydev.analysis.messages.Message;

public class MessagesManager {

    /**
     * preferences for indicating the severities
     */
    private IAnalysisPreferences prefs;

    /**
     * this map should hold the generator source token and the messages that are generated for it
     */
    public Map<IToken, List<IMessage>> messages = new HashMap<IToken, List<IMessage>>();


    public List<IMessage> independentMessages = new ArrayList<IMessage>();
    
    public MessagesManager(IAnalysisPreferences prefs) {
        this.prefs = prefs;
    }
    
    /**
     * adds a message of some type given its formatting params
     */
    public void addMessage(int type, IToken generator, Object ...objects ) {
        independentMessages.add(new Message(type, objects, generator, prefs));
    }

    /**
     * adds a message of some type for a given token
     */
    public void addMessage(int type, IToken token) {
        List<IMessage> msgs = getMsgsList(token);
        msgs.add(new Message(type, token.getRepresentation(),token, prefs));
    }

    /**
     * adds a message of some type for some Found instance
     */
    public void addMessage(int type, IToken generator, IToken tok) {
        addMessage(type, generator, tok, tok.getRepresentation());
    }

    /**
     * adds a message of some type for some Found instance
     */
    public void addMessage(int type, IToken generator, IToken tok, String rep) {
        List<IMessage> msgs = getMsgsList(generator);
        msgs.add(new Message(type, rep, generator, prefs));
    }
    
    /**
     * @return the messages associated with a token
     */
    public List<IMessage> getMsgsList(IToken generator) {
        List<IMessage> msgs = messages.get(generator);
        if (msgs == null){
            msgs = new ArrayList<IMessage>();
            messages.put(generator, msgs);
        }
        return msgs;
    }

    
    /**
     * @param token adds a message saying that a token is not defined
     */
    public void addUndefinedMessage(IToken token) {
        if(token.getRepresentation().equals("_"))
            return; //TODO: check how to get tokens that are added to the builtins
        
        String rep = token.getRepresentation();
        int i;
        if((i = rep.indexOf('.')) != -1){
            rep = rep.substring(0,i);
        }
        addMessage(IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE, token, rep );
    }

    /**
     * adds a message for something that was not used
     */
    public void addUnusedMessage(Found f) {
        for (GenAndTok g : f){
            if(g.generator instanceof SourceToken){
                SimpleNode ast = ((SourceToken)g.generator).getAst();
                
                //it can be an unused import
                if(ast instanceof Import || ast instanceof ImportFrom){
                    addMessage(IAnalysisPreferences.TYPE_UNUSED_IMPORT, g.generator, g.tok);
                    continue; //finish it...
                }
            }
            //or unused variable
            addMessage(IAnalysisPreferences.TYPE_UNUSED_VARIABLE, g.generator, g.tok);
        }
    }

    /**
     * adds a message for a re-import
     */
    public void addReimportMessage(Found f) {
        for (GenAndTok g : f){
            if(g.generator instanceof SourceToken){
                addMessage(IAnalysisPreferences.TYPE_REIMPORT, g.generator, g.tok);
            }
        }
    }
    

    
    
    
    /**
     * @return the generated messages.
     */
    public IMessage[] getMessages() {
        
        List<IMessage> result = new ArrayList<IMessage>();
        
        //let's get the messages
        for (List<IMessage> l : messages.values()) {
            if(l.size() < 1){
                //we need at least one message
                continue;
            }
            
            Map<Integer, List<IMessage>> messagesByType = getMessagesByType(l);
            for (int type : messagesByType.keySet()) {
                l = messagesByType.get(type);
                
                //the values are guaranteed to have size at least equal to 1
                IMessage message = l.get(0);
                
                //messages are grouped by type, and the severity is set by type, so, this is ok...
                if(message.getSeverity() == IAnalysisPreferences.SEVERITY_IGNORE){
                    continue;
                }

                if(l.size() == 1){
                    result.add(message);
                    
                } else{
                    //the generator token has many associated messages - the messages may have different types,
                    //so, we need to get them by types
                    CompositeMessage compositeMessage = new CompositeMessage(message.getType(), message.getGenerator(), prefs);
                    for(IMessage m : l){
                        compositeMessage.addMessage(m);
                    }
                    result.add(compositeMessage);
                }
            }
            
        }
        
        for(IMessage message : independentMessages){
            if(message.getSeverity() == IAnalysisPreferences.SEVERITY_IGNORE){
                continue;
            }
            
            result.add(message);
        }
        return (IMessage[]) result.toArray(new IMessage[0]);
    }

    /**
     * @return a map with the messages separated by type (keys are the type)
     * 
     * the values are guaranteed to have size at least equal to 1
     */
    private Map<Integer,List<IMessage>> getMessagesByType(List<IMessage> l) {
        HashMap<Integer, List<IMessage>> messagesByType = new HashMap<Integer, List<IMessage>>();
        for (IMessage message : l) {
            
            List<IMessage> messages = messagesByType.get(message.getType());
            if(messages == null){
                messages = new ArrayList<IMessage>();
                messagesByType.put(message.getType(), messages);
            }
            messages.add(message);
        }
        return messagesByType;
    }

}
