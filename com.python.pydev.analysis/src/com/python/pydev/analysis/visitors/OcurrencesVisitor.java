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
import org.python.parser.ast.If;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.Name;
import org.python.parser.ast.VisitorBase;
import org.python.parser.ast.argumentsType;
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
     * manage the scopes...
     */
    private Scope scope;
    
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
    private List<Found> probablyNotDefined = new ArrayList<Found>();
    
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
        this.scope = new Scope(this.messagesManager);
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
        duplicationChecker.beforeFunctionDef(node); //duplication checker
        
        argumentsType args = node.args;
        
        if(args.kwarg != null){
            Name name = new Name(args.kwarg, Name.Load);
            SourceToken token = AbstractVisitor.makeToken(name, moduleName);
            scope.addToken(token, token, args.kwarg);
        }
        if(args.vararg != null){
            Name name = new Name(args.vararg, Name.Load);
            SourceToken token = AbstractVisitor.makeToken(name, moduleName);
            scope.addToken(token, token, args.vararg);
        }
        Object object = super.visitFunctionDef(node);
        
        duplicationChecker.afterFunctionDef(node);//duplication checker
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
        scope.addTokens(list, null);
        return null;
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
            scope.addTokens(completionsForWildImport, wildImport);
        }else{
            List list = AbstractVisitor.makeImportToken(node, null, moduleName);
            scope.addTokens(list, null);
        }
        return null;
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
                scope.addToken(token,token);
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

    
    @Override
    public Object visitIf(If node) throws Exception {
        scope.addIfSubScope();
        Object r = super.visitIf(node);
        scope.removeIfSubScope();
        return r;
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
        scope.startScope();
        Map<String, IToken> item = new HashMap<String, IToken>();
        stackNamesToIgnore.push(item);
    }
    
    /**
     * finalizes the current scope
     */
    private void endScope() {
        ScopeItems m = scope.endScope(); //clear the last scope
        
        for(Iterator<Found> it = probablyNotDefined.iterator(); it.hasNext();){
            Found n = it.next();
            
            IToken tok = n.getSingle().tok;
            String rep = tok.getRepresentation();
            //we also get a last pass to the unused to see if they might have been defined later on the higher scope
            
            Found found = find(m, rep);
            if(found != null && found.getSingle().scopeId != n.getSingle().scopeId){
                found.setUsed(true);
                it.remove();
            }
        }
        
        //ok, this was the last scope, so, the ones probably not defined are really not defined at this
        //point
        if(scope.size() == 0){
            
            for(Found n : probablyNotDefined){
                String rep = n.getSingle().tok.getRepresentation();
                if(!stackNamesToIgnore.get(0).containsKey(rep)){
                    messagesManager.addUndefinedMessage(n.getSingle().tok);
                }
            }
            
        }
        
        //so, now, we clear the unused
        for (Found f : m.values()) {
            if(!f.isUsed()){
                messagesManager.addUnusedMessage(f);
            }
        }
        
        stackNamesToIgnore.pop();
        
    }

    
    /**
     * Finds an item given its full representation (so, os.path can be found as solely os)
     */
    private Found find(ScopeItems m, String fullRep) {
        if(m == null)
            return null;
        
        int i = fullRep.indexOf('.', 0);

        while(i >= 0){
            String sub = fullRep.substring(0,i);
            i = fullRep.indexOf('.', i+1);
            Found found = m.get(sub);
            if(found != null){
                return found;
            }
        }
        
        return m.get(fullRep);
    }

    
    /**
     * we just found a token, so let's mark the correspondent tokens read (or undefined)
     */
    private void markRead(IToken token) {
        String rep = token.getRepresentation();
        markRead(token, rep, true);
    }

    /**
     * marks a token as read given its representation
     * 
     * @param token the token to be added
     * @param rep the token representation
     * @param addToNotDefined determines if it should be added to the 'not defined tokens' stack or not 
     * @return true if it was found
     */
    private boolean markRead(IToken token, String rep, boolean addToNotDefined) {
        boolean found = scope.find(rep);
        
        //this token might not be defined...
        int i;
        if((i = rep.indexOf('.')) != -1){
            //if it is an attribute, we have to check the names to ignore just with its first part
            rep = rep.substring(0, i);
        }
        if(addToNotDefined && !found && !isInNamesToIgnore(rep)){
            if(scope.size() > 1){
                probablyNotDefined.add(new Found(token, token, scope.getCurrScopeId())); //we are not in the global scope, so it might be defined later...
            }else{
                //global scope, so, even if it is defined later, this is an error...
                messagesManager.addUndefinedMessage(token);
            }
        }
        return found;
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
