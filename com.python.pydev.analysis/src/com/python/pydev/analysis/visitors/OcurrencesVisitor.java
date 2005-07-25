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
import org.python.parser.ast.Attribute;
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
     * Stack for names that should not generate warnings, such as builtins, method names, etc.
     */
    private Stack<Map<String,IToken>> stackNamesToIgnore = new Stack<Map<String,IToken>>();
    
    /**
     * this should get the tokens that are probably not used, but may be if they are defined
     * later (e.g.: if we have a method call inside a scope and the method is defined later)
     * 
     * objects should not be added to it if we are at the global scope.
     */
    private List<IToken> probablyNotDefined = new ArrayList<IToken>();
    
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
        List<IToken> builtinCompletions = nature.getAstManager().getBuiltinCompletions(getEmptyCompletionState(), new ArrayList());
        for(IToken t : builtinCompletions){
            stackNamesToIgnore.peek().put(t.getRepresentation(), t);
        }
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
        
        addToNamesToIgnore(node);
        
        return object;
    }

    /**
     * used so that the token is added to the names to ignore...
     */
    private void addToNamesToIgnore(SimpleNode node) {
        SourceToken token = AbstractVisitor.makeToken(node, "");
        stackNamesToIgnore.peek().put(token.getRepresentation(), token);
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
        addToNamesToIgnore(node);
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
            addToken(generator, m, o, o.getRepresentation());
        }
    }

    /**
     * @param generator
     * @param m
     * @param o
     */
    private void addToken(IToken generator, IToken o) {
        Map<String,Found> m = stack.peek();
        addToken(generator, m, o, o.getRepresentation());
        
    }
    
    /**
     * @param generator
     * @param m
     * @param o
     */
    private void addToken(IToken generator, Map<String, Found> m, IToken o, String rep) {
        Found found = m.get(rep);
        if(found != null && !found.used){ //it will be removed from the scope
            addUnusedMessage(found);
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
            CompletionState state = getEmptyCompletionState();
            state.builtinsGotten = true; //we don't want any builtins
            List completionsForWildImport = nature.getAstManager().getCompletionsForWildImport(state, current, new ArrayList(), wildImport);
            addTokens(completionsForWildImport, wildImport);
        }else{
            List list = AbstractVisitor.makeImportToken(node, null, moduleName);
            addTokens(list, null);
        }
        return null;
    }

    /**
     * @return a default completion state for globals (empty act. token)
     */
    private CompletionState getEmptyCompletionState() {
        return new CompletionState(0,0,"", nature);
    }
    
    /**
     * initializes a new scope
     */
    private void startScope() {
        stack.push(new HashMap<String,Found>());
        Map<String, IToken> item = new HashMap<String, IToken>();
        stackNamesToIgnore.push(item);
    }
    
    /**
     * finalizes the current scope
     */
    private void endScope() {
        Map<String,Found> m = stack.pop(); //clear the last
        for (Found f : m.values()) {
            if(!f.used){
                addUnusedMessage(f);
            }
        }
        
        //ok, this was the last scope, so, let's check for the probably not defined, but
        //that might have been defined in the global scope
        if(stack.size() == 0){
            for(IToken n : probablyNotDefined){
                String rep = n.getRepresentation();
                if(!stackNamesToIgnore.get(0).containsKey(rep)){
                    addUndefinedMessage(n);
                }
            }
        }
        
        stackNamesToIgnore.pop();
        
    }

    /**
     * adds a message for something that was not used
     */
    private void addUnusedMessage(Found f) {
        if(f.generator instanceof SourceToken){
            SimpleNode ast = ((SourceToken)f.generator).getAst();
            
            //it can be an unused import
            if(ast instanceof Import || ast instanceof ImportFrom){
                messagesManager.addMessage(IAnalysisPreferences.TYPE_UNUSED_IMPORT, f);
                return; //finish it...
            }
        }
        //or unused variable
        messagesManager.addMessage(IAnalysisPreferences.TYPE_UNUSED_VARIABLE, f);
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
            String rep = token.getRepresentation();
            if(!rep.equals("self")){
                addToken(token,token);
            }else{
                addToNamesToIgnore(node); //ignore self
            }
            
            
        } else if (node.ctx == Name.Load) {
            markRead(token);
        }
        return null;
    }
    
    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        SourceToken token = AbstractVisitor.makeFullNameToken(node, moduleName);
        String fullRep = token.getRepresentation();
        int i = fullRep.indexOf('.', 0);
        String sub = fullRep.substring(0,i);

        if (node.ctx == Attribute.Store) {
            //in a store attribute, the first part is always a load
            markRead(token, sub, true);
            
        } else if (node.ctx == Attribute.Load) {
    
            while(i >= 0){
                sub = fullRep.substring(0,i);
                i = fullRep.indexOf('.', i+1);
            
                boolean found = markRead(token, sub, false);
                if(found){
                    break;
                }
                if(i == -1){ //check for the full attribute
                    markRead(token, fullRep, true);
                    break;
                }
            }
        }

        return null;
    }

    /**
     * @param found
     * @param name
     * @return
     */
    private boolean find(boolean found, String name) {
        for (Map<String,Found> m : stack) {
            
            Found f = m.get(name);
            if(f != null){
                f.used = true;
                found = true;
            }
        }
        return found;
    }
    
    /**
     * we just found a token, so let's mark the correspondent tokens read (or undefined)
     */
    private void markRead(IToken token) {
        String rep = token.getRepresentation();
        markRead(token, rep, true);
    }

    /**
     * @param token
     * @param rep
     * @return 
     */
    private boolean markRead(IToken token, String rep, boolean addToNotDefined) {
        boolean found = false;
        found = find(found, rep);
        
        //this token might not be defined...
        int i;
        if((i = rep.indexOf('.')) != -1){
            //if it is an attribute, we have to check the names to ignore just with its first part
            rep = rep.substring(0, i);
        }
        if(addToNotDefined && !found && !isInNamesToIgnore(rep)){
            if(stack.size() > 1){
                probablyNotDefined.add(token); //we are not in the global scope, so it might be defined later...
            }else{
                //global scope, so, even if it is defined later, this is an error...
                addUndefinedMessage(token);
            }
        }
        return found;
    }

    /**
     * @param token
     */
    private void addUndefinedMessage(IToken token) {
        messagesManager.addMessage(IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE, token);
    }

    /**
     * checks if there is some token in the names that are defined (but should be ignored)
     */
    private boolean isInNamesToIgnore(String rep) {
        for(Map<String,IToken> m : this.stackNamesToIgnore){
            if(m.containsKey(rep)){
                return true;
            }
        }
        return false;
    }

}
