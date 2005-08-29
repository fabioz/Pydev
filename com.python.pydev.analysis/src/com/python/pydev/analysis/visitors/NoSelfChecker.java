/*
 * Created on 28/08/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.python.parser.ast.Assign;
import org.python.parser.ast.Call;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Name;
import org.python.parser.ast.decoratorsType;
import org.python.parser.ast.exprType;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.parser.visitors.NodeUtils;

import com.python.pydev.analysis.IAnalysisPreferences;

public class NoSelfChecker {

    private Stack<Integer> scope = new Stack<Integer>();
    private Stack<HashMap<String, FunctionDef>> maybeNoSelfDefinedItems = new Stack<HashMap<String, FunctionDef>>();
    
    private MessagesManager messagesManager;
    private String moduleName;

    public NoSelfChecker(MessagesManager messagesManager, String moduleName) {
        this.messagesManager = messagesManager;
        this.moduleName = moduleName;
        scope.push(Scope.SCOPE_TYPE_GLOBAL); //we start in the global scope
    }

    public void beforeClassDef(ClassDef node) {
        scope.push(Scope.SCOPE_TYPE_CLASS);
        maybeNoSelfDefinedItems.push(new HashMap<String, FunctionDef>());
    }

    public void afterClassDef(ClassDef node) {
        scope.pop();
        
        HashMap<String, FunctionDef> noSelfDefinedItems = maybeNoSelfDefinedItems.pop();
        for (Map.Entry<String, FunctionDef> entry : noSelfDefinedItems.entrySet()) {
            SourceToken token = AbstractVisitor.makeToken(entry.getValue(), moduleName);
            messagesManager.addMessage(IAnalysisPreferences.TYPE_NO_SELF, token);
        }
    }

    /**
     * when a class is declared inside a function scope, it must start with self if it does
     * not start with the self parameter, unless it has a staticmethod decoration or is
     * later assigned to a staticmethod.
     * 
     * @param node
     */
    public void beforeFunctionDef(FunctionDef node) {
        
        
        if(scope.peek().equals(Scope.SCOPE_TYPE_CLASS)){
            //let's check if we have to start with self
            boolean startsWithSelf = false;
            if(node.args != null){
                if(node.args.args.length > 0){
                    exprType arg = node.args.args[0];
                    if(arg instanceof Name){
                        Name n = (Name) arg;
                        if(n.id.equals("self")){
                            startsWithSelf = true;
                        }
                    }
                }
            }
            
            if(!startsWithSelf){
                if(node.decs != null){
                    for (decoratorsType dec : node.decs) {
                        if(dec != null){
                            String rep = NodeUtils.getRepresentationString(dec.func);
                            if(rep != null){
                                if(rep.equals("staticmethod")){
                                    startsWithSelf = true;
                                }
                            }
                        }
                    }
                }
                //didn't have staticmethod decorator either
                if(!startsWithSelf){
                    maybeNoSelfDefinedItems.peek().put(node.name, node);
                }
            }
        }
        scope.push(Scope.SCOPE_TYPE_METHOD);
    }

    public void afterFunctionDef(FunctionDef node) {
        scope.pop();
    }

    public void visitAssign(Assign node) {
        
        //we're looking for xxx = staticmethod(xxx)
        if(node.targets.length == 1){
            exprType t = node.targets[0];
            String rep = NodeUtils.getRepresentationString(t);
            if(rep == null){
                return;
            }
            
            if(scope.peek() != Scope.SCOPE_TYPE_CLASS){
                //we must be in a class scope
                return;
            }
            
            FunctionDef def = maybeNoSelfDefinedItems.peek().get(rep);
            if(def != null){
                //ok, it may be a staticmethod, let's check its value (should be a call)
                exprType expr = node.value;
                if(expr instanceof Call){
                    Call call = (Call) expr;
                    if(call.args.length == 1){
                        String argRep = NodeUtils.getRepresentationString(call.args[0]);
                        if(argRep != null && argRep.equals(rep)){
                            String funcCall = NodeUtils.getRepresentationString(call.func);
                            if(funcCall != null && funcCall.equals("staticmethod")){
                                //ok, finally... it is a staticmethod after all...
                                maybeNoSelfDefinedItems.peek().remove(rep);
                            }
                        }
                    }
                }
            }
        }
    }

}
