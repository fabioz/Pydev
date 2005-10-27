/*
 * Created on 24/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.parser.visitors.NodeUtils;

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

    /**
     * Should be used to give the name of the module we are visiting
     */
    private String moduleName;
    
    public MessagesManager(IAnalysisPreferences prefs, String moduleName) {
        this.prefs = prefs;
        this.moduleName = moduleName;
    }
    
    /**
     * @return whether we should add an unused import message to the module being analyzed
     */
    public boolean shouldAddUnusedImportMessage(){
        if(moduleName == null){
            return true;
        }
        String onlyModName = FullRepIterable.headAndTail(moduleName, true)[1];
        Set<String> patternsToBeIgnored = this.prefs.getModuleNamePatternsToBeIgnored();
        for (String pattern : patternsToBeIgnored) {
            if(onlyModName.matches(pattern)){
                return false;
            }
        }
        return true;
    }
    
    /**
     * adds a message of some type given its formatting params
     */
    public void addMessage(int type, IToken generator, Object ...objects ) {
        if(isUnusedImportMessage(type)){
            if (!shouldAddUnusedImportMessage()){
                return;
            }
        }
        doAddMessage(independentMessages, type, objects, generator);
    }

    /**
     * @param type the type of the message
     * @return whether it is an unused import message
     */
    private boolean isUnusedImportMessage(int type) {
        return type == IAnalysisPreferences.TYPE_UNUSED_IMPORT || type == IAnalysisPreferences.TYPE_UNUSED_WILD_IMPORT;
    }

    /**
     * adds a message of some type for a given token
     */
    public void addMessage(int type, IToken token) {
        List<IMessage> msgs = getMsgsList(token);
        doAddMessage(msgs, type, token.getRepresentation(),token);
    }

    /**
     * checks if the message should really be added and does the add.
     */
    private void doAddMessage(List<IMessage> msgs, int type, Object string, IToken token){
        if(isUnusedImportMessage(type)){
            if (!shouldAddUnusedImportMessage()){
                return;
            }
        }
        msgs.add(new Message(type, string,token, prefs));
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
        doAddMessage(msgs, type, rep, generator);
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

    public void addUndefinedMessage(IToken token) {
    	addUndefinedMessage(token, null);
    }
    
    /**
     * @param token adds a message saying that a token is not defined
     */
    public void addUndefinedMessage(IToken token, String rep) {
    	Tuple<Boolean, String> undef = isActuallyUndefined(token, rep);
        if(undef.o1){
        	addMessage(IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE, token, undef.o2 );
        }
    }

    /**
     * @param token adds a message saying that a token gathered from an import is not defined
     */
	public void addUndefinedVarInImportMessage(IToken token, String rep) {
    	Tuple<Boolean, String> undef = isActuallyUndefined(token, rep);
        if(undef.o1){
        	addMessage(IAnalysisPreferences.TYPE_UNDEFINED_IMPORT_VARIABLE, token, undef.o2 );
        }
	}
    
    /**
     * Checks if some token is actually undefined and changes its representation if needed
     * @return a tuple indicating if it really is undefined and the representation that should be used.
     */
    protected Tuple<Boolean, String> isActuallyUndefined(IToken token, String rep){
    	boolean isActuallyUndefined = true;
    	
        if(token.getRepresentation().equals("_"))
        	isActuallyUndefined = false; //TODO: check how to get tokens that are added to the builtins
        
        if(rep == null){
        	rep = token.getRepresentation();
        }
        
        int i;
        if((i = rep.indexOf('.')) != -1){
            rep = rep.substring(0,i);
        }

        String builtinType = NodeUtils.getBuiltinType(rep);
        if(builtinType != null){
        	isActuallyUndefined = false; //this is a builtin, so, it is defined after all
        }
        return new Tuple<Boolean, String>(isActuallyUndefined, rep);
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
                    if(AbstractVisitor.isWildImport(ast)){
                        addMessage(IAnalysisPreferences.TYPE_UNUSED_WILD_IMPORT, g.generator, g.tok);
                    }else{
                        addMessage(IAnalysisPreferences.TYPE_UNUSED_IMPORT, g.generator, g.tok);
                    }
                    continue; //finish it...
                }
            }
            
            //or unused variable
            //we have to check if this is a name we should ignore
            if(startsWithNamesToIgnore(g)){
                addMessage(IAnalysisPreferences.TYPE_UNUSED_VARIABLE, g.generator, g.tok);
            }
        }
    }

    
    /**
     * a cache, so that we don't get the names to ignore over and over
     * this is ok, because every time we start an analysis session, this object is re-created, and the options
     * will not change all the time
     */
    private Set<String> namesToIgnoreCache = null;

    /**
     * This is the last scope. It is set after all the analysis ended, so that we can generate some
     * additional info based on it.
     */
    private ScopeItems lastScope;
    
    /**
     * @param g the generater that will generate an unused variable message
     * @return true if we should not add the message
     */
    private boolean startsWithNamesToIgnore(GenAndTok g) {
        if(namesToIgnoreCache == null){
            namesToIgnoreCache = prefs.getNamesIgnoredByUnusedVariable();
        }
        String representation = g.tok.getRepresentation();
        
        boolean addIt = true;
        for (String str : namesToIgnoreCache) {
            if(representation.startsWith(str)){
                addIt = false;
                break;
            }
        }
        return addIt;
    }

    /**
     * adds a message for a re-import
     */
    public void addReimportMessage(Found f) {
        for (GenAndTok g : f){
            //we don't want to add reimport messages if they are found in a wild import
            if(g.generator instanceof SourceToken && AbstractVisitor.isWildImport(g.generator) == false){
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
                //if(message.getSeverity() == IAnalysisPreferences.SEVERITY_IGNORE){
                //    continue;
                //}
                //we add even ignore messages because they might be used later in actions dependent on code analysis

                if(l.size() == 1){
                    addAdditionalInfoToUnusedWildImport(message);
                    result.add(message);
                    
                } else{
                    //the generator token has many associated messages - the messages may have different types,
                    //so, we need to get them by types
                    CompositeMessage compositeMessage = new CompositeMessage(message.getType(), message.getGenerator(), prefs);
                    for(IMessage m : l){
                        compositeMessage.addMessage(m);
                    }
                    addAdditionalInfoToUnusedWildImport(compositeMessage);
                    result.add(compositeMessage);
                }
            }
            
        }
        
        for(IMessage message : independentMessages){
            //if(message.getSeverity() == IAnalysisPreferences.SEVERITY_IGNORE){
            //    continue;
            //}
            // we add even ignore messages because they might be used later in actions dependent on code analysis
            
            result.add(message);
        }
        
        return (IMessage[]) result.toArray(new IMessage[0]);
    }

    /**
     * @param message the message to which we will add additional info
     */
    private void addAdditionalInfoToUnusedWildImport(IMessage message) {
        if(message.getType() == IAnalysisPreferences.TYPE_UNUSED_WILD_IMPORT){

            //we have to add additional info on it, saying which tokens where used
            if(AbstractVisitor.isWildImport(message.getGenerator())){

                List<Tuple<String,Found>> usedItems = lastScope.getUsedItems();
                for (Tuple<String, Found> tuple : usedItems) {
                    if(tuple.o2.getSingle().generator == message.getGenerator()){
                        message.addAdditionalInfo(tuple.o1);
                    }
                }
            }
        }
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

    public void setLastScope(ScopeItems m) {
        this.lastScope = m;
    }

}
