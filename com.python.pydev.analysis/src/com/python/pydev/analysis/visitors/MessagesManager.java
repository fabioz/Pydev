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
            
            IMessage message = l.get(0);
            if(message.getSeverity() == IAnalysisPreferences.SEVERITY_IGNORE){
                continue;
            }
            
            if(l.size() == 1){
                result.add(message);
                
            } else{
                //the generator token has many associated messages
                CompositeMessage compositeMessage = new CompositeMessage(message.getType(), message.getGenerator(), prefs);
                for(IMessage m : l){
                    compositeMessage.addMessage(m);
                }
                result.add(compositeMessage);
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

}
