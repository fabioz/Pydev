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

import org.eclipse.core.runtime.IStatus;
import org.python.parser.SimpleNode;
import org.python.parser.ast.Assign;
import org.python.parser.ast.Attribute;
import org.python.parser.ast.Call;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Global;
import org.python.parser.ast.If;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.ListComp;
import org.python.parser.ast.Name;
import org.python.parser.ast.NameTok;
import org.python.parser.ast.Subscript;
import org.python.parser.ast.TryExcept;
import org.python.parser.ast.TryFinally;
import org.python.parser.ast.VisitorBase;
import org.python.parser.ast.While;
import org.python.parser.ast.argumentsType;
import org.python.parser.ast.decoratorsType;
import org.python.parser.ast.exprType;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.plugin.PydevPlugin;
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
     * used to check if a signature from a method starts with self (if it is not a staticmethod)
     */
    private NoSelfChecker noSelfChecker;
    
    /**
     * used to check for invalid imports
     */
    private ImportChecker importChecker;

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
        this.messagesManager = new MessagesManager(prefs, moduleName);
        this.scope = new Scope(this.messagesManager);
        this.duplicationChecker = new DuplicationChecker(this.messagesManager);
        this.noSelfChecker = new NoSelfChecker(this.messagesManager, moduleName);
        this.importChecker = new ImportChecker(this.messagesManager);
        
        startScope(Scope.SCOPE_TYPE_GLOBAL); //initial scope - there is only one 'global' 
        List<IToken> builtinCompletions = nature.getAstManager().getBuiltinCompletions(CompletionState.getEmptyCompletionState(nature), new ArrayList());
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
        addToNamesToIgnore(node);

        startScope(Scope.SCOPE_TYPE_CLASS);
        duplicationChecker.beforeClassDef(node);
        noSelfChecker.beforeClassDef(node);
        Object object = super.visitClassDef(node);
        noSelfChecker.afterClassDef(node);
        duplicationChecker.afterClassDef(node);
        endScope();
        
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
        addToNamesToIgnore(node);

        OcurrencesVisitor visitor = this;
        argumentsType args = node.args;

        //visit the defaults first (before starting the scope, because this is where the load of variables from other scopes happens)
        if(args.defaults != null){
            for(exprType expr : args.defaults){
                if(expr != null){
                    expr.accept(visitor);
                }
            }
        }
        
        //then the decorators (no, still not in method scope)
        for (decoratorsType dec : node.decs){
            if(dec != null){
                dec.accept(visitor);
            }
        }

        startScope(Scope.SCOPE_TYPE_METHOD);
        duplicationChecker.beforeFunctionDef(node); //duplication checker
        noSelfChecker.beforeFunctionDef(node);


        scope.isInMethodDefinition = true;
        //visit regular args
        if (args.args != null){
            for(exprType expr : args.args){
                expr.accept(visitor);
            }
        }

        //visit varargs
        if(args.vararg != null){
            NameTok nameTok = (NameTok)args.vararg;
            Name name = new Name((nameTok).id, Name.Load);
            name.beginLine = nameTok.beginLine;
            name.beginColumn = nameTok.beginColumn;
            SourceToken token = AbstractVisitor.makeToken(name, moduleName);
            scope.addToken(token, token, (nameTok).id);
        }
        
        //visit kwargs
        if(args.kwarg != null){
            NameTok nameTok = (NameTok)args.kwarg;
            Name name = new Name((nameTok).id, Name.Load);
            name.beginLine = nameTok.beginLine;
            name.beginColumn = nameTok.beginColumn;
            SourceToken token = AbstractVisitor.makeToken(name, moduleName);
            scope.addToken(token, token, (nameTok).id);
        }
        scope.isInMethodDefinition = false;
        
        //visit the body
        if (node.body != null) {
            for (int i = 0; i < node.body.length; i++) {
                if (node.body[i] != null){
                    node.body[i].accept(visitor);
                }
            }
        }

        duplicationChecker.afterFunctionDef(node);//duplication checker
        noSelfChecker.afterFunctionDef(node);
        endScope();
        return null;
    }
    
    /**
     * when visiting an import, just make the token and add it
     * 
     * e.g.: if it is an import such as 'os.path', it will return 2 tokens, one for 'os' and one for 'os.path',
     *  
     * @see org.python.parser.ast.VisitorIF#visitImport(org.python.parser.ast.Import)
     */
    public Object visitImport(Import node) throws Exception {
        List <IToken>list = AbstractVisitor.makeImportToken(node, null, moduleName, true);
        for (IToken token : list) {
            scope.addToken(token, token);
            importChecker.visitImportToken(token, nature, moduleName);
        }
//        //it is only a single import for the import checker
//        if(list.size() > 0){
//            IToken token = list.get(list.size() -1);
//            importChecker.visitImportToken(token, nature, moduleName);
//        }
        return null;
    }

    /**
     * visit some import 
     * @see org.python.parser.ast.VisitorIF#visitImportFrom(org.python.parser.ast.ImportFrom)
     */
    public Object visitImportFrom(ImportFrom node) throws Exception {
        try {
            
            if(AbstractVisitor.isWildImport(node)){
                IToken wildImport = AbstractVisitor.makeWildImportToken(node, null, moduleName);
                importChecker.visitImportToken(wildImport, nature, moduleName);
                
                CompletionState state = CompletionState.getEmptyCompletionState(nature);
                state.builtinsGotten = true; //we don't want any builtins
                List completionsForWildImport = nature.getAstManager().getCompletionsForWildImport(state, current, new ArrayList(), wildImport);
                scope.addTokens(completionsForWildImport, wildImport);
            }else{
                List<IToken> list = AbstractVisitor.makeImportToken(node, null, moduleName, true);
                
                for (IToken token : list) {
					importChecker.visitImportToken(token, nature, moduleName);
				}
//                //it is only a single import for the import checker
//                if(list.size() > 0){
//                    IToken token = list.get(list.size() -1);
//                    importChecker.visitImportToken(token, nature, moduleName);
//                }
                scope.addTokens(list, null);
            }
            
        } catch (Exception e) {
            PydevPlugin.log(IStatus.ERROR, "Error when analyzing module "+moduleName, e);
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
            boolean inNamesToIgnore = isInNamesToIgnore(rep);
            
            if(!inNamesToIgnore){
                
                if(!rep.equals("self")){ //TODO: after knowing about decorations, self should only be allowed in methods from a class
                    scope.addToken(token,token);
                }else{
                    addToNamesToIgnore(node); //ignore self
                }
            }
            
            
        } else if (node.ctx == Name.Load) {
            markRead(token);
        }
        return null;
    }
    
    @Override
    public Object visitGlobal(Global node) throws Exception {
        for(String name :node.names){
            Name nameAst = new Name(name, Name.Store);
            nameAst.beginLine = node.beginLine;
            nameAst.beginColumn = -1;

            SourceToken token = AbstractVisitor.makeToken(nameAst, moduleName);
            scope.addTokenToGlobalScope(token);
            addToNamesToIgnore(nameAst); // it is global, so, ignore it...
        }
        return null;
    }
    
    /**
     * visiting some attribute, as os.path or math().val or (10,10).__class__
     *  
     * @see org.python.parser.ast.VisitorIF#visitAttribute(org.python.parser.ast.Attribute)
     */
    public Object visitAttribute(Attribute node) throws Exception {
        SourceToken token = AbstractVisitor.makeFullNameToken(node, moduleName);
        if(token.getRepresentation().equals("")){
            return null;
        }
        String fullRep = token.getRepresentation();

        if (node.ctx == Attribute.Store) {
            //in a store attribute, the first part is always a load
            int i = fullRep.indexOf('.', 0);
            String sub = fullRep.substring(0,i);
            markRead(token, sub, true);
            
        } else if (node.ctx == Attribute.Load) {
    
            Iterator<String> it = new FullRepIterable(fullRep).iterator();
            boolean found = false;
            
            while(it.hasNext()){
                String sub = it.next();
                if( markRead(token, sub, false) ){
                    if (found == false){
                        found = true;
                    }
                }
                if(!it.hasNext()){
                    markRead(token, fullRep, !found); //only set it to add to not defined if it was still not found
                }
            }
        }

        if(node.value instanceof Call){
            visitCallAttr(node);
        }
        if(node.value instanceof Subscript){
            this.traverse(((Subscript) node.value).slice);
        }
        return null;
    }

    /**
     * used if we want to visit all in a call but the func itself (that's the call name).
     */
    private void visitCallAttr(Attribute node) throws Exception {
        //now, visit all inside it but the func itself 
        Call c = (Call)node.value;
        OcurrencesVisitor visitor = this;
        if (c.args != null) {
            for (int i = 0; i < c.args.length; i++) {
                if (c.args[i] != null)
                    c.args[i].accept(visitor);
            }
        }
        if (c.keywords != null) {
            for (int i = 0; i < c.keywords.length; i++) {
                if (c.keywords[i] != null)
                    c.keywords[i].accept(visitor);
            }
        }
        if (c.starargs != null)
            c.starargs.accept(visitor);
        if (c.kwargs != null)
            c.kwargs.accept(visitor);
    }

    /**
     * overriden because we want the value to be visited before the targets 
     * @see org.python.parser.ast.VisitorIF#visitAssign(org.python.parser.ast.Assign)
     */
    public Object visitAssign(Assign node) throws Exception {
        OcurrencesVisitor visitor = this;
        
        //in 'm = a', this is 'a'
        if (node.value != null)
            node.value.accept(visitor);

        //in 'm = a', this is 'm'
        if (node.targets != null) {
            for (int i = 0; i < node.targets.length; i++) {
                if (node.targets[i] != null)
                    node.targets[i].accept(visitor);
            }
        }
        noSelfChecker.visitAssign(node);
        return null;
    }
    
    /**
     * overriden because we need to know about if scopes
     */
    public Object visitIf(If node) throws Exception {
        scope.addIfSubScope();
        Object r = super.visitIf(node);
        scope.removeIfSubScope();
        return r;
    }
    
    /**
     * overriden because we need to know about while scopes
     */
    public Object visitWhile(While node) throws Exception {
        scope.addIfSubScope();
        Object r =  super.visitWhile(node);
        scope.removeIfSubScope();
        return r;
    }

    @Override
    public Object visitTryExcept(TryExcept node) throws Exception {
        scope.addIfSubScope();
        Object r = super.visitTryExcept(node);
        scope.removeIfSubScope();
        return r;
    }
    
    @Override
    public Object visitTryFinally(TryFinally node) throws Exception {
        scope.addIfSubScope();
        Object r = super.visitTryFinally(node);
        scope.removeIfSubScope();
        return r;
    }
    
    /**
     * overriden because we need to visit the generators first
     * 
     * @see org.python.parser.ast.VisitorIF#visitListComp(org.python.parser.ast.ListComp)
     */
    public Object visitListComp(ListComp node) throws Exception {
        if (node.generators != null) {
            for (int i = 0; i < node.generators.length; i++) {
                if (node.generators[i] != null)
                    node.generators[i].accept(this);
            }
        }
        if (node.elt != null)
            node.elt.accept(this);

        return null;
    }
    
    /**
     * initializes a new scope
     */
    private void startScope(int newScopeType) {
        scope.startScope(newScopeType);
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
            
            List<Found> foundItems = find(m, rep);
            boolean setUsed = false;
            for (Found found : foundItems) {
                //the scope where it is defined must be an outer scope so that we can say it was defined later...
                if(found.getSingle().scopeId < n.getSingle().scopeId){
                    found.setUsed(true);
                    setUsed = true;
                }
            }
            if(setUsed){
                it.remove();
            }
        }
        
        //ok, this was the last scope, so, the ones probably not defined are really not defined at this
        //point
        if(scope.size() == 0){
            
            for(Found n : probablyNotDefined){
                String rep = n.getSingle().tok.getRepresentation();
                Map<String, IToken> lastInStack = stackNamesToIgnore.peek();
                if(!findInNamesToIgnore(rep, lastInStack)){
                    messagesManager.addUndefinedMessage(n.getSingle().tok);
                }
            }
            messagesManager.setLastScope(m);
        }
        
        //so, now, we clear the unused
        int scopeType = m.getScopeType();
        for (Found f : m.values()) {
            if(!f.isUsed()){
                // we don't get unused at the global scope or class definition scope unless it's an import
                if(scopeType == Scope.SCOPE_TYPE_METHOD || f.isImport()){ //only within methods do we put things as unused 
                    messagesManager.addUnusedMessage(f);
                }
            }
        }
        
        stackNamesToIgnore.pop();
        
    }

    /**
     * find out if an item is in the names to ignore given its full representation
     */
    private boolean findInNamesToIgnore(String fullRep, Map<String, IToken> lastInStack) {
        
        int i = fullRep.indexOf('.', 0);

        while(i >= 0){
            String sub = fullRep.substring(0,i);
            i = fullRep.indexOf('.', i+1);
            if(lastInStack.containsKey(sub)){
                return true;
            }
        }

        return lastInStack.containsKey(fullRep);
    }

    
    /**
     * Finds an item given its full representation (so, os.path can be found as 'os' and 'os.path')
     */
    private List<Found> find(ScopeItems m, String fullRep) {
        ArrayList<Found> foundItems = new ArrayList<Found>();
        if(m == null){
            return foundItems;
        }
        
        int i = fullRep.indexOf('.', 0);

        while(i >= 0){
            String sub = fullRep.substring(0,i);
            i = fullRep.indexOf('.', i+1);
            Found found = m.get(sub);
            if(found != null){
                foundItems.add(found);
            }
        }
        
        Found found = m.get(fullRep);
        if(found != null){
            foundItems.add(found);
        }
        return foundItems;
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
        Iterator it = new FullRepIterable(rep, true).iterator();
        boolean found = false;
        while (found == false && it.hasNext()){
            found = scope.findFirst((String) it.next(), true) != null;
        }
        
        //this token might not be defined...
        int i;
        if((i = rep.indexOf('.')) != -1){
            //if it is an attribute, we have to check the names to ignore just with its first part
            rep = rep.substring(0, i);
        }
        if(addToNotDefined && !found && !isInNamesToIgnore(rep)){
            if(scope.size() > 1){
                probablyNotDefined.add(new Found(token, token, scope.getCurrScopeId(), scope.getCurrScopeItems())); //we are not in the global scope, so it might be defined later...
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
            if(findInNamesToIgnore(rep, m)){
                return true;
            }
        }
        return false;
    }

}
