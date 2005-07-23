/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.Name;
import org.python.parser.ast.VisitorBase;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.CompositeMessage;
import com.python.pydev.analysis.IMessage;
import com.python.pydev.analysis.Message;

public class OcurrencesVisitor extends VisitorBase{

    private PythonNature nature;
    private String moduleName;
    
    private Stack<Map<String,Found>> stack = new Stack<Map<String,Found>>();
    
    private AbstractModule current;
    public Map<IToken, List<IMessage>> messages = new HashMap<IToken, List<IMessage>>();
    
    public static class Found{
        /**
         * This is the token that is from the current module that created the token (if on some wild import)
         * 
         * May be equal to tok
         */
        public SourceToken generator;
        
        /**
         * This is the token that has been added to the namespace (may have been created on the current module or not).
         */
        public IToken tok;
        
        /**
         * Identifies if the current token has been used or not
         */
        public boolean used = false;
        
        Found(IToken tok, SourceToken generator){
            this.tok = tok;
            this.generator = generator;
        }
    }

    public IMessage[] getMessages() {
        endScope();
        List<IMessage> result = new ArrayList<IMessage>();
        
        for (List<IMessage> l : messages.values()) {
            if(l.size() > 1){
                IMessage message = l.get(0);
                CompositeMessage compositeMessage = new CompositeMessage(message.getType(), message.getSubType(), message.getGenerator());
                for(IMessage m : l){
                    compositeMessage.addMessage(m);
                }
                result.add(compositeMessage);
            }
            else if(l.size() == 1){
                result.add(l.get(0));
            }
        }
        return (IMessage[]) result.toArray(new IMessage[0]);
    }
    
    public OcurrencesVisitor(PythonNature nature, String moduleName, AbstractModule current) {
        this.nature = nature;
        this.moduleName = moduleName;
        startScope(); //initial scope 
    }
    
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }
    
    public Object visitClassDef(ClassDef node) throws Exception {
        startScope();
        Object object = super.visitClassDef(node);
        endScope();
        return object;
    }

    public Object visitFunctionDef(FunctionDef node) throws Exception {
        startScope();
        Object object = super.visitFunctionDef(node);
        endScope();
        return object;
    }
    
    public Object visitImport(Import node) throws Exception {
        List list = AbstractVisitor.makeImportToken(node, null, moduleName);
        addTokens(list, null);
        return null;
    }

    /**
     * @param list
     */
    private void addTokens(List list, IToken generator) {
        Map<String,Found> m = stack.peek();
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            IToken o = (IToken) iter.next();
            addToken(generator, m, o);
        }
    }

    /**
     * @param generator
     * @param m
     * @param o
     */
    private void addToken(IToken generator, IToken o) {
        Map<String,Found> m = stack.peek();
        addToken(generator, m, o);
        
    }
    
    /**
     * @param generator
     * @param m
     * @param o
     */
    private void addToken(IToken generator, Map<String, Found> m, IToken o) {
        String representation = o.getRepresentation();
        if (generator == null){
            m.put(representation, new Found(o,(SourceToken) o)); //the generator and the token are the same
        }else{
            m.put(representation, new Found(o,(SourceToken) generator));
        }
    }
    
    public Object visitImportFrom(ImportFrom node) throws Exception {
        if(AbstractVisitor.isWildImport(node)){
            IToken wildImport = AbstractVisitor.makeWildImportToken(node, null, moduleName);
            CompletionState state = new CompletionState(0,0,"", nature);
            List completionsForWildImport = nature.getAstManager().getCompletionsForWildImport(state, current, new ArrayList(), wildImport);
            addTokens(completionsForWildImport, wildImport);
        }else{
            List list = AbstractVisitor.makeImportToken(node, null, moduleName);
            addTokens(list, null);
        }
        return null;
    }
    
    private void startScope() {
        stack.push(new HashMap<String,Found>());
    }
    
    private void endScope() {
        Map m = (Map) stack.pop(); //clear the last
        for (Iterator iter = m.values().iterator(); iter.hasNext();) {
            Found f = (Found) iter.next();
            if(!f.used){
                SimpleNode ast = f.generator.getAst();
                if(ast instanceof Import || ast instanceof ImportFrom){
                    addMessage(IMessage.TYPE_WARNING, IMessage.SUB_UNUSED_IMPORT, f);
                }else{
                    addMessage(IMessage.TYPE_WARNING, IMessage.SUB_UNUSED_VARIABLE, f);
                }
            }
        }
    }

    /**
     * Visiting some name
     * 
     * @see org.python.parser.ast.VisitorIF#visitName(org.python.parser.ast.Name)
     */
    public Object visitName(Name node) throws Exception {
        //when visiting the global namespace, we don't go into any inner scope.
        if (node.ctx == Name.Store) {
            SourceToken token = AbstractVisitor.makeToken(node, moduleName);
            if(!token.getRepresentation().equals("self")){
                addToken(token,token);
            }
        }
        if (node.ctx == Name.Load) {
            markRead(node);
        }
        return null;
    }
    
    private void markRead(Name node) {
        for (Iterator iter = stack.iterator(); iter.hasNext();) {
            Map m = (Map) iter.next();
            Found f = (Found) m.get(node.id);
            if(f != null){
                f.used = true;
            }
        }
    }

    protected String[] getAllDefinedTokens() {
        List<String> strings = new ArrayList<String>();
        for(Map<String,Found> m : stack){

            for(String s : m.keySet()){
              strings.add(s);
            }
            
        }
        return (String[]) strings.toArray(new String[0]);
    }

    private void addMessage(int type, int subType, Found f) {
        List<IMessage> msgs = messages.get(f.generator);
        if (msgs == null){
            msgs = new ArrayList<IMessage>();
            messages.put(f.generator, msgs);
        }
        msgs.add(new Message(type, subType, f.tok.getRepresentation(),(SourceToken) f.tok));
    }
}
