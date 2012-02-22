/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.OrderedSet;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assert;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.AugAssign;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Compare;
import org.python.pydev.parser.jython.ast.Comprehension;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Lambda;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Print;
import org.python.pydev.parser.jython.ast.Raise;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.Yield;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.messages.IMessage;
import com.python.pydev.analysis.scopeanalysis.AbstractScopeAnalyzerVisitor;

/**
 * this visitor marks the used/ unused tokens and generates the messages related
 * 
 * @author Fabio
 */
public final class OccurrencesVisitor extends AbstractScopeAnalyzerVisitor{

    
    /**
     * Used to manage the messages
     */
    protected MessagesManager messagesManager;

    /**
     * used to check for duplication in signatures
     */
    protected DuplicationChecker duplicationChecker;
    
    /**
     * used to check if a signature from a method starts with self (if it is not a staticmethod)
     */
    protected NoSelfChecker noSelfChecker;
    
    public OccurrencesVisitor(IPythonNature nature, String moduleName, IModule current, IAnalysisPreferences prefs, IDocument document, IProgressMonitor monitor) {
        super(nature, moduleName, current, document, monitor);
        this.analyzeArgumentsMismatch = true;
        this.messagesManager = new MessagesManager(prefs, moduleName, document);
        this.duplicationChecker = new DuplicationChecker(this);
        this.noSelfChecker = new NoSelfChecker(this, moduleName);
    }

    private int isInTestScope = 0;
    
    @Override
    public Object visitCompare(Compare node) throws Exception {
        Object ret = super.visitCompare(node);
        if(isInTestScope == 0){
            SourceToken token = AbstractVisitor.makeToken(node, moduleName);
            messagesManager.addMessage(IAnalysisPreferences.TYPE_NO_EFFECT_STMT, token);
        }
        return ret;
    }
    
    public void traverse(If node) throws Exception {
        checkStop();
        if (node.test != null){
            isInTestScope += 1;
            node.test.accept(this);
            isInTestScope -= 1;
        }
        
        if (node.body != null) {
            for (int i = 0; i < node.body.length; i++) {
                if (node.body[i] != null)
                    node.body[i].accept(this);
            }
        }
        if (node.orelse != null) {
            node.orelse.accept(this);
        }
    }
    
    
    @Override
    public Object visitTuple(org.python.pydev.parser.jython.ast.Tuple node) throws Exception {
        isInTestScope += 1;
        Object ret = super.visitTuple(node);
        isInTestScope -= 1;
        return ret;
    }
    
    public void traverse(While node) throws Exception {
        checkStop();
        OccurrencesVisitor visitor = this;
        if (node.test != null){
            isInTestScope += 1;
            node.test.accept(visitor);
            isInTestScope -= 1;
        }
        
        if (node.body != null) {
            for (int i = 0; i < node.body.length; i++) {
                if (node.body[i] != null)
                    node.body[i].accept(visitor);
            }
        }
        if (node.orelse != null)
            node.orelse.accept(visitor);
    }
    
    @Override
    public Object visitRaise(Raise node) throws Exception {
        isInTestScope+=1;
        Object r = super.visitRaise(node);
        isInTestScope-=1;
        return r;
    }
    
    @Override
    public Object visitComprehension(Comprehension node) throws Exception {
        isInTestScope+=1;
        Object r = super.visitComprehension(node);
        isInTestScope-=1;
        return r;

    }
    
    @Override
    public Object visitAssert(Assert node) throws Exception {
        isInTestScope+=1;
        Object r = super.visitAssert(node);
        isInTestScope-=1;
        return r;
    }
    
    @Override
    public Object visitPrint(Print node) throws Exception {
        isInTestScope+=1;
        Object r = super.visitPrint(node);
        isInTestScope-=1;
        return r;
    }
    
    @Override
    public Object visitAssign(Assign node) throws Exception {
        isInTestScope+=1;
        Object r = super.visitAssign(node);
        isInTestScope-=1;
        return r;
    }
    
    @Override
    public Object visitYield(Yield node) throws Exception {
        isInTestScope+=1;
        Object r = super.visitYield(node);
        isInTestScope-=1;
        return r;
    }
    
    @Override
    public Object visitAugAssign(AugAssign node) throws Exception {
        isInTestScope+=1;
        Object r = super.visitAugAssign(node);
        isInTestScope-=1;
        return r;
    }
    
    @Override
    public Object visitCall(Call node) throws Exception {
        isInTestScope+=1;
        Object r = super.visitCall(node);
        isInTestScope-=1;
        return r;
    }
    
    @Override
    public Object visitReturn(Return node) throws Exception {
        isInTestScope+=1;
        Object r = super.visitReturn(node);
        isInTestScope-=1;
        return r;
    }
    
    @Override
    protected void handleDecorator(decoratorsType dec) throws Exception {
        isInTestScope+=1;
        dec.accept(this);
        isInTestScope-=1;
    }

    @Override
    public Object visitLambda(Lambda node) throws Exception {
        isInTestScope+=1;
        Object ret = super.visitLambda(node);
        isInTestScope-=1;
        return ret;
    }
    
    public void traverse(SimpleNode node) throws Exception {
        if(node instanceof If){
            traverse((If)node);
        }else if(node instanceof While){
            traverse((While)node);
        }else if(node instanceof ListComp){
            this.visitListComp((ListComp) node);
        }else{
            super.traverse(node);
        }
    }
    
    /**
     * @return the generated messages.
     */
    public List<IMessage> getMessages() {
        endScope(null); //have to end the scope that started when we created the class.
        
        return messagesManager.getMessages();
    }
    
    /**
     * @param foundTok
     */
    protected void onAddUndefinedVarInImportMessage(IToken foundTok, Found foundAs) {
        messagesManager.addUndefinedVarInImportMessage(foundTok, foundTok.getRepresentation());
    }


    /**
     * @param foundTok
     */
    protected void onAddAssignmentToBuiltinMessage(IToken foundTok, String representation) {
        messagesManager.onAddAssignmentToBuiltinMessage(foundTok, representation);
    }

    @Override
    public void onArgumentsMismatch(IToken node, Call callNode){
        messagesManager.onArgumentsMismatch(node, callNode);
    }
    
    /**
     * @param token
     */
    protected void onAddUndefinedMessage(IToken token, Found foundAs) {
        if("...".equals(token.getRepresentation())){
            return; //Ellipsis -- when found in the grammar, it's added as a name, which we can safely ignore at this point.
        }

        //global scope, so, even if it is defined later, this is an error...
        messagesManager.addUndefinedMessage(token);
    }
    
    /**
     * @param m
     */
    protected void onLastScope(ScopeItems m) {
        for(Found n : probablyNotDefined){
            String rep = n.getSingle().tok.getRepresentation();
            Map<String, Tuple<IToken, Found>> lastInStack = m.namesToIgnore;
            if(scope.findInNamesToIgnore(rep, lastInStack) == null){
                onAddUndefinedMessage(n.getSingle().tok, n);
            }
        }
        messagesManager.setLastScope(m);
    }
    /**
     * @param reportUnused
     * @param m
     */
    protected void onAfterEndScope(SimpleNode node, ScopeItems m) {
        boolean reportUnused = true;
        if(node != null && node instanceof FunctionDef){
            reportUnused = !isVirtual((FunctionDef) node);
        }
        
        if(reportUnused){
            //so, now, we clear the unused
            int scopeType = m.getScopeType();
            for (List<Found> list : m.getAll()) {
                int len = list.size();
                for (int i = 0; i < len; i++) {
                    Found f=list.get(i);
                    if(!f.isUsed()){
                        // we don't get unused at the global scope or class definition scope unless it's an import
                        if((scopeType & Scope.ACCEPTED_METHOD_AND_LAMBDA) != 0 || f.isImport()){ //only within methods do we put things as unused 
                            messagesManager.addUnusedMessage(node, f);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * A method is virtual if it contains only raise and string statements 
     */
    protected boolean isVirtual(FunctionDef node) {
        if(node.body != null){
            int len = node.body.length;
            for (int i = 0; i < len; i++) {
                SimpleNode n = node.body[i];
                if(n instanceof Raise){
                    continue;
                }
                if(n instanceof Expr){
                    if(((Expr)n).value instanceof Str){
                        continue;
                    }
                }
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onAfterStartScope(int newScopeType, SimpleNode node) {
        if(newScopeType == Scope.SCOPE_TYPE_CLASS){
            duplicationChecker.beforeClassDef((ClassDef) node);
            noSelfChecker.beforeClassDef((ClassDef) node);
            
        }else if((newScopeType & Scope.SCOPE_TYPE_METHOD) != 0){
            duplicationChecker.beforeFunctionDef((FunctionDef) node); //duplication checker
            noSelfChecker.beforeFunctionDef((FunctionDef) node);
        }
    }
    
    @Override
    protected void onBeforeEndScope(SimpleNode node) {
        if(node instanceof ClassDef){
            noSelfChecker.afterClassDef((ClassDef) node);
            duplicationChecker.afterClassDef((ClassDef) node);
            
        } else if(node instanceof FunctionDef){
            duplicationChecker.afterFunctionDef((FunctionDef) node);//duplication checker
            noSelfChecker.afterFunctionDef((FunctionDef) node);
        }
    }

    @Override
    public void onAddUnusedMessage(SimpleNode node, Found found) {
        messagesManager.addUnusedMessage(node, found);
    }

    @Override
    public void onAddReimportMessage(Found newFound) {
        messagesManager.addReimportMessage(newFound);
    }

    @Override
    public void onAddUnresolvedImport(IToken token) {
        messagesManager.addMessage(IAnalysisPreferences.TYPE_UNRESOLVED_IMPORT, token);
    }

    @Override
    public void onAddDuplicatedSignature(SourceToken token, String name) {
        messagesManager.addMessage(IAnalysisPreferences.TYPE_DUPLICATED_SIGNATURE, token, name );
    }

    @Override
    public void onAddNoSelf(SourceToken token, Object[] objects) {
        messagesManager.addMessage(IAnalysisPreferences.TYPE_NO_SELF, token, objects);
   }


    @Override
    protected void onAfterVisitAssign(Assign node) {
        noSelfChecker.visitAssign(node);
    }
    
    
    private static final int NO_STATIC_NOR_CLASSMETHOD = 0;
    private static final int CLASSMETHOD = 1;
    private static final int STATICMETHOD = 2;

    private int isStaticOrClassMethod(FunctionDef functionDefinitionReferenced) {
        if(functionDefinitionReferenced.decs != null){
            for (decoratorsType dec : functionDefinitionReferenced.decs) {
                if(dec != null){
                    String rep = NodeUtils.getRepresentationString(dec.func);
                
                    if(rep != null){
                        
                        if(rep.equals("staticmethod")){
                            return STATICMETHOD;
                        } else if(rep.equals("classmethod")){
                            return CLASSMETHOD;
                        }
                    }
                }
            }
        }
        
        //If it got here, there may still be an assign to it...
        if(functionDefinitionReferenced.parent instanceof ClassDef){
            ClassDef classDef = (ClassDef) functionDefinitionReferenced.parent;
            stmtType[] body = classDef.body;
            if(body != null){
                int len = body.length;
                
                String funcName = ((NameTok)functionDefinitionReferenced.name).id;
                for(int i=0;i<len;i++){
                    if(body[i] instanceof Assign){
                        Assign assign = (Assign) body[i];
                        if(assign.targets == null){
                            continue;
                        }
                        
                        //we're looking for xxx = staticmethod(xxx)
                        if(assign.targets.length == 1){
                            exprType t = assign.targets[0];
                            String rep = NodeUtils.getRepresentationString(t);
                            if(rep == null){
                                continue;
                            }
                        }
                        
                        exprType expr = assign.value;
                        if(expr instanceof Call){
                            Call call = (Call) expr;
                            if(call.args.length == 1){
                                String argRep = NodeUtils.getRepresentationString(call.args[0]);
                                if(argRep != null && argRep.equals(funcName)){
                                    String funcCall = NodeUtils.getRepresentationString(call.func);
                                    
                                    if("staticmethod".equals(funcCall)){
                                        //ok, finally... it is a staticmethod after all...
                                        return STATICMETHOD;
                                    }
                                    if("classmethod".equals(funcCall)){
                                        return CLASSMETHOD;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
        }
        return NO_STATIC_NOR_CLASSMETHOD;
    }


    @Override
    protected void analyzeCallAndFunctionMatch(Call callNode, FunctionDef functionDefinitionReferenced, IToken nameToken, boolean callingBoundMethod) throws Exception {
        int functionArgsLen = functionDefinitionReferenced.args.args != null?functionDefinitionReferenced.args.args.length:0;
        Collection<String> functionRequiredArgs = new OrderedSet<String>(functionArgsLen);
        Collection<String> functionOptionalArgs = new OrderedSet<String>(functionArgsLen);
        
        int staticOrClassMethod = isStaticOrClassMethod(functionDefinitionReferenced);
        boolean ignoreFirstParameter = callingBoundMethod;
        if(staticOrClassMethod != NO_STATIC_NOR_CLASSMETHOD){
            switch (staticOrClassMethod) {
                case STATICMETHOD:
                    ignoreFirstParameter = false;
                    break;
                case CLASSMETHOD:
                    ignoreFirstParameter = true;
                    break;
                default:
                    throw new AssertionError("Unexpected condition.");
            }
        }
        
        for(int i=0;i<functionArgsLen;i++){
            if(i==0 && ignoreFirstParameter){
                continue; //Ignore first parameter when calling a bound method.
            }
            String rep = NodeUtils.getRepresentationString(functionDefinitionReferenced.args.args[i]);
            if(functionDefinitionReferenced.args.defaults == null || 
               (functionDefinitionReferenced.args.defaults.length > i && 
               functionDefinitionReferenced.args.defaults[i] == null)){
                //it's null, so, it's required
                functionRequiredArgs.add(rep);
            }else{
                //not null: optional with default value
                functionOptionalArgs.add(rep);
            }
        }
        exprType[] kwonlyargs = functionDefinitionReferenced.args.kwonlyargs;
        Collection<String> functionKeywordOnlyArgs = null;
        if(kwonlyargs != null){
            functionKeywordOnlyArgs =  new OrderedSet<String>(kwonlyargs.length);
            for (exprType exprType : kwonlyargs) {
                if(exprType != null){
                    functionKeywordOnlyArgs.add(NodeUtils.getRepresentationString(exprType));
                }
            }
        }

        
        int callArgsLen = callNode.args != null?callNode.args.length:0;
        
        for(int i=0;i<callArgsLen;i++){

            if(functionRequiredArgs.size() > 0){
                //Remove first one (no better api in collection...)
                Iterator<String> it = functionRequiredArgs.iterator();
                it.next();
                it.remove();
                continue;
                
            }else if(functionOptionalArgs.size() > 0){
                Iterator<String> it = functionOptionalArgs.iterator();
                it.next();
                it.remove();
                continue;
            }
            
            //All 'regular' and 'optional' arguments consumed (i.e.: def m1(a, b, c=10)), so, it'll only
            //be possible to accept an item that's in *args at this point.
            if(functionDefinitionReferenced.args.vararg == null){
                onArgumentsMismatch(nameToken, callNode);
                return; //Error reported, so, bail out of function!
            }
                
        }
        
        
        int callKeywordArgsLen = callNode.keywords != null?callNode.keywords.length:0;
        for(int i=0;i<callKeywordArgsLen;i++){
            String rep = NodeUtils.getRepresentationString(callNode.keywords[i].arg);
            //keyword argument (i.e.: call(a=10)), so, only accepted in kwargs or with some argument with that name.
            if(functionRequiredArgs.remove(rep)){
                continue;
                
            }else if(functionOptionalArgs.remove(rep)){
                continue;
                
            }else if(functionKeywordOnlyArgs != null && functionKeywordOnlyArgs.remove(rep)){
                continue;
                
            }else{
                //An argument with that name was not found, so, it may only be handled through kwargs at this point!
                if(functionDefinitionReferenced.args.kwarg == null){
                    onArgumentsMismatch(nameToken, callNode); 
                    return; //Error reported, so, bail out of function!
                }
            }
        }
        
        if(functionRequiredArgs.size() > 0 || (functionKeywordOnlyArgs != null && functionKeywordOnlyArgs.size() > 0)){
            if(callNode.kwargs == null && callNode.starargs == null){
                //Not all required parameters were consumed!
                onArgumentsMismatch(nameToken, callNode); 
                return; //Error reported, so, bail out of function!
            }
        }else if(functionOptionalArgs.size() > 0){
            
        }else{
            //required and optional size == 0
            if(callNode.kwargs != null && functionDefinitionReferenced.args.kwarg == null){
                //We have more things that were not handled
                onArgumentsMismatch(nameToken, callNode); 
                return; //Error reported, so, bail out of function!
            }
            if(callNode.starargs != null && functionDefinitionReferenced.args.vararg == null){
                //We have more things that were not handled
                onArgumentsMismatch(nameToken, callNode); 
                return; //Error reported, so, bail out of function!
            }
        }
    }
    
    
}
