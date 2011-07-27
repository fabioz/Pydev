/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IToken;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor.ImportPartSourceToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.messages.CompositeMessage;
import com.python.pydev.analysis.messages.IMessage;
import com.python.pydev.analysis.messages.Message;

public final class MessagesManager {

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

    /**
     * This is the document
     */
    private IDocument document;

    public MessagesManager(IAnalysisPreferences prefs, String moduleName, IDocument doc) {
        this.prefs = prefs;
        this.moduleName = moduleName;
        this.document = doc;
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

        Message messageToAdd = new Message(type, string,token, prefs);
        
        String messageToIgnore = prefs.getRequiredMessageToIgnore(messageToAdd.getType());
        if(messageToIgnore != null){
            int startLine = messageToAdd.getStartLine(document) - 1;
            String line = PySelection.getLine(document, startLine);
            if(line.indexOf(messageToIgnore) != -1){
                //keep going... nothing to see here...
                return;
            }
        }

        msgs.add(messageToAdd);
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
     * @param token adds a message saying that a token gathered from assignment is a reserved keyword
     */
    public void onAddAssignmentToBuiltinMessage(IToken token, String rep) {
            addMessage(IAnalysisPreferences.TYPE_ASSIGNMENT_TO_BUILT_IN, token);
    }

    /**
     * Checks if some token is actually undefined and changes its representation if needed
     * @return a tuple indicating if it really is undefined and the representation that should be used.
     */
    protected Tuple<Boolean, String> isActuallyUndefined(IToken token, String rep){
        String tokenRepresentation = token.getRepresentation();
        if(tokenRepresentation != null){
            String firstPart = FullRepIterable.getFirstPart(tokenRepresentation);
            if(this.prefs.getTokensAlwaysInGlobals().contains(firstPart)){
                return new Tuple<Boolean, String>(false, firstPart); //ok firstPart in not really undefined... 
            }
        }
        
        boolean isActuallyUndefined = true;
        if(rep == null){
            rep = tokenRepresentation;
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
     * 
     * @param node the node representing the scope being closed when adding the
     *             unused message
     */
    public void addUnusedMessage(SimpleNode node, Found f) {
        for (GenAndTok g : f) {
            if (g.generator instanceof SourceToken) {
                SimpleNode ast = ((SourceToken) g.generator).getAst();

                // it can be an unused import
                if (ast instanceof Import || ast instanceof ImportFrom) {
                    if (AbstractVisitor.isWildImport(ast)) {
                        addMessage(IAnalysisPreferences.TYPE_UNUSED_WILD_IMPORT, g.generator, g.tok);
                    } else if(!(g.generator instanceof ImportPartSourceToken)){
                        addMessage(IAnalysisPreferences.TYPE_UNUSED_IMPORT, g.generator, g.tok);
                    }
                    continue; // finish it...
                }
            }

            // or unused variable
            // we have to check if this is a name we should ignore
            if (startsWithNamesToIgnore(g)) {
                int type = IAnalysisPreferences.TYPE_UNUSED_VARIABLE;

                if (g.tok instanceof SourceToken) {
                    SourceToken t = (SourceToken) g.tok;
                    SimpleNode ast = t.getAst();
                    if (ast instanceof NameTok) {
                        NameTok n = (NameTok) ast;
                        if (n.ctx == NameTok.KwArg || n.ctx == NameTok.VarArg || n.ctx == NameTok.KeywordName) {
                            type = IAnalysisPreferences.TYPE_UNUSED_PARAMETER;
                        }
                    } else if (ast instanceof Name) {
                        Name n = (Name) ast;
                        if (n.ctx == Name.Param || n.ctx == Name.KwOnlyParam) {
                            type = IAnalysisPreferences.TYPE_UNUSED_PARAMETER;
                        }
                    }
                }
                boolean addMessage = true;
                if (type == IAnalysisPreferences.TYPE_UNUSED_PARAMETER) {
                    // just add unused parameters in methods that have some content (not only 'pass' and 'strings')

                    if (node instanceof FunctionDef) {
                        addMessage = false;
                        FunctionDef def = (FunctionDef) node;
                        for (stmtType b : def.body) {
                            if (b instanceof Pass) {
                                continue;
                            }
                            if (b instanceof Expr) {
                                Expr expr = (Expr) b;
                                if (expr.value instanceof Str) {
                                    continue;
                                }
                            }
                            addMessage = true;
                            break;
                        }
                    }
                }//END if (type == IAnalysisPreferences.TYPE_UNUSED_PARAMETER)
                
                
                if (addMessage) {
                    addMessage(type, g.generator, g.tok);
                }
            }
        }
    }

    
    /**
     * a cache, so that we don't get the names to ignore over and over this is
     * ok, because every time we start an analysis session, this object is
     * re-created, and the options will not change all the time
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
            if(g.generator instanceof SourceToken && !(g.generator instanceof ImportPartSourceToken) && AbstractVisitor.isWildImport(g.generator) == false){
                addMessage(IAnalysisPreferences.TYPE_REIMPORT, g.generator, g.tok);
            }
        }
    }
    
    
    
    /**
     * @return the generated messages.
     */
    public List<IMessage> getMessages() {
        
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
                if(message.getSeverity() == IMarker.SEVERITY_INFO){
                    if(doIgnoreMessageIfJustInformational(message.getType())){
                        //ok, let's ignore it for real (and don't add it) as those are not likely to be
                        //used anyways for other actions)
                        continue;
                        
                    }
                }
                //we add even ignore messages because they might be used later in actions dependent on code analysis

                if(l.size() == 1){
//don't add additional info: not being used
//                    addAdditionalInfoToUnusedWildImport(message);
                    addToResult(result, message);
                    
                } else{
                    //the generator token has many associated messages - the messages may have different types,
                    //so, we need to get them by types
                    CompositeMessage compositeMessage = new CompositeMessage(message.getType(), message.getGenerator(), prefs);
                    for(IMessage m : l){
                        compositeMessage.addMessage(m);
                    }

//don't add additional info: not being used
//                    addAdditionalInfoToUnusedWildImport(compositeMessage);
                    addToResult(result, compositeMessage);
                }
            }
        }
        
        for(IMessage message : independentMessages){
            if(message.getSeverity() == IMarker.SEVERITY_INFO){
                if(doIgnoreMessageIfJustInformational(message.getType())){
                    //ok, let's ignore it for real (and don't add it) as those are not likely to be
                    //used anyways for other actions)
                    continue;
                    
                }
                //otherwise keep on and add it (needed for some actions)
            }
            
            addToResult(result, message);
        }
        
        return result;
    }

    private boolean doIgnoreMessageIfJustInformational(int type) {
        return type == IAnalysisPreferences.TYPE_UNUSED_PARAMETER || 
           type == IAnalysisPreferences.TYPE_INDENTATION_PROBLEM ||
           type == IAnalysisPreferences.TYPE_NO_EFFECT_STMT;
    }

    /**
     * @param result
     * @param message
     */
    private void addToResult(List<IMessage> result, IMessage message) {
        if(isUnusedImportMessage(message.getType())){
            IToken generator = message.getGenerator();
            if(generator instanceof SourceToken){
                String asAbsoluteImport = generator.getAsAbsoluteImport();
                if(asAbsoluteImport.indexOf("__future__.") != -1 || asAbsoluteImport.indexOf("__metaclass__") != -1){
                    //do not add from __future__ import xxx
                    return; 
                }
            }
        }
        result.add(message);
    }

    
// Comented out: we're not using this info (so, let's save some memory until we really need that)
//    /**
//     * @param message the message to which we will add additional info
//     */
//    private void addAdditionalInfoToUnusedWildImport(IMessage message) {
//        if(message.getType() == IAnalysisPreferences.TYPE_UNUSED_WILD_IMPORT){
//
//            //we have to add additional info on it, saying which tokens where used
//            if(AbstractVisitor.isWildImport(message.getGenerator())){
//
//                List<Tuple<String,Found>> usedItems = lastScope.getUsedItems();
//                for (Tuple<String, Found> tuple : usedItems) {
//                    if(tuple.o2.getSingle().generator == message.getGenerator()){
//                        message.addAdditionalInfo(tuple.o1);
//                    }
//                }
//            }
//        }
//    }

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
