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

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.messages.IMessage;

/**
 * this visitor marks the used/ unused tokens and generates the messages related
 * 
 * @author Fabio
 */
public class OcurrencesVisitor extends VisitorBase{

    /**
     * nature is needed for imports
     */
    private PythonNature nature;
    
    /**
     * this is the name of the module we are visiting
     */
    private String moduleName;
    
    /**
     * this stack is used to hold the scope. when we enter a scope, an item is added, and when we
     * exit, it is removed (and the analysis of unused tokens should happen at this time).
     */
    private Stack<Map<String,Found>> stack = new Stack<Map<String,Found>>();
    
    /**
     * this is the module we are visiting
     */
    private AbstractModule current;

    /**
     * used to check for duplication in signatures
     */
    private DuplicationChecker duplicationChecker;

    /**
     * Used to manage the messages
     */
    private MessagesManager messagesManager;
    
    /**
     * Constructor
     * @param prefs 
     */
    public OcurrencesVisitor(PythonNature nature, String moduleName, AbstractModule current, IAnalysisPreferences prefs) {
        this.nature = nature;
        this.moduleName = moduleName;
        this.messagesManager = new MessagesManager(prefs);
        this.duplicationChecker = new DuplicationChecker(this.messagesManager);
        
        startScope(); //initial scope 
    }
    
    /**
     * @return the generated messages.
     */
    public IMessage[] getMessages() {
        endScope(); //have to end the scope that started when we created the class.
        
        return messagesManager.getMessages();
    }
    
    /**
     * nothing is additionally handled here 
     * @see org.python.parser.ast.VisitorBase#unhandled_node(org.python.parser.SimpleNode)
     */
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    /**
     * transverse the node 
     * @see org.python.parser.ast.VisitorBase#traverse(org.python.parser.SimpleNode)
     */
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }
    
    
    /**
     * we are starting a new scope when visiting a class 
     * @see org.python.parser.ast.VisitorIF#visitClassDef(org.python.parser.ast.ClassDef)
     */
    public Object visitClassDef(ClassDef node) throws Exception {
        startScope();
        duplicationChecker.beforeClassDef(node);
        Object object = super.visitClassDef(node);
        duplicationChecker.afterClassDef(node);
        endScope();
        return object;
    }

    /**
     * we are starting a new scope when visiting a function 
     * @see org.python.parser.ast.VisitorIF#visitFunctionDef(org.python.parser.ast.FunctionDef)
     */
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        startScope();
        duplicationChecker.beforeFunctionDef(node);
        Object object = super.visitFunctionDef(node);
        duplicationChecker.afterFunctionDef(node);
        endScope();
        return object;
    }
    
    /**
     * when visiting an import, just make the token and add it 
     * @see org.python.parser.ast.VisitorIF#visitImport(org.python.parser.ast.Import)
     */
    public Object visitImport(Import node) throws Exception {
        List list = AbstractVisitor.makeImportToken(node, null, moduleName);
        addTokens(list, null);
        return null;
    }

    /**
     * Adds many tokens at once (created by the same token -- wild import)
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
        String rep = o.getRepresentation();
        Found found = m.get(rep);
        if(found != null && !found.used){ //it will be removed from the scope
            addMessage(found);
        }
        if (generator == null){
            m.put(rep, new Found(o,(SourceToken) o)); //the generator and the token are the same
        }else{
            m.put(rep, new Found(o,(SourceToken) generator));
        }
    }
    
    /**
     * visit some import 
     * @see org.python.parser.ast.VisitorIF#visitImportFrom(org.python.parser.ast.ImportFrom)
     */
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
    
    /**
     * initializes a new scope
     */
    private void startScope() {
        stack.push(new HashMap<String,Found>());
    }
    
    /**
     * finalizes the current scope
     */
    private void endScope() {
        Map m = (Map) stack.pop(); //clear the last
        for (Iterator iter = m.values().iterator(); iter.hasNext();) {
            Found f = (Found) iter.next();
            if(!f.used){
                addMessage(f);
            }
        }
    }

    /**
     * @param f
     */
    private void addMessage(Found f) {
        SimpleNode ast = f.generator.getAst();
        if(ast instanceof Import || ast instanceof ImportFrom){
            messagesManager.addMessage(IAnalysisPreferences.TYPE_UNUSED_IMPORT, f);
        }else{
            messagesManager.addMessage(IAnalysisPreferences.TYPE_UNUSED_VARIABLE, f);
        }
    }

    /**
     * Visiting some name
     * 
     * @see org.python.parser.ast.VisitorIF#visitName(org.python.parser.ast.Name)
     */
    public Object visitName(Name node) throws Exception {
        //when visiting the global namespace, we don't go into any inner scope.
        SourceToken token = AbstractVisitor.makeToken(node, moduleName);
        if (node.ctx == Name.Store) {
            if(!token.getRepresentation().equals("self")){
                addToken(token,token);
            }
        }
        if (node.ctx == Name.Load) {
            markRead(token);
        }
        return null;
    }
    
    /**
     * we just found a token, so let's mark the correspondent tokens read (or undefined)
     */
    private void markRead(SourceToken token) {
        for (Map<String,Found> m : stack) {
            
            Found f = m.get(token.getRepresentation());
            if(f != null){
                f.used = true;
            }else{ //this token was not defined...
                messagesManager.addMessage(IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE, token);
            }
        }
    }

}
