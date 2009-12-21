/*
 * Created on 24/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.HashMap;
import java.util.Map;

import org.python.pydev.core.structure.FastStack;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.NodeUtils;

import com.python.pydev.analysis.scopeanalysis.AbstractScopeAnalyzerVisitor;

/**
 * Used to check for duplicated signatures
 * 
 * @author Fabio
 */
public final class DuplicationChecker {

    /**
     * used to know the defined signatures
     */
    private FastStack<Map<String,String>> stack = new FastStack<Map<String,String>>();
    private AbstractScopeAnalyzerVisitor visitor;
    
    /**
     * constructor
     * @param visitor 
     */
    public DuplicationChecker(AbstractScopeAnalyzerVisitor visitor) {
        this.visitor = visitor;
        startScope("", null); 
    }

    /**
     * we are starting a new scope (method or class)
     */
    private void startScope(String name, SimpleNode node) {
        checkDuplication(name, node);
        Map<String, String> item = new HashMap<String, String>();
        stack.push(item);
    }

    /**
     * we are ending a scope
     */
    private void endScope(String name) {
        stack.pop();
        stack.peek().put(name, name);
    }
    
    /**
     * checks if some name is already defined (and therefore, this can be a duplication)
     */
    private void checkDuplication(String name, SimpleNode node) {
        if(stack.size() > 0){
            if(!visitor.scope.getPrevScopeItems().getIsInSubSubScope()){
                String exists = stack.peek().get(name);
                if(exists != null){
                    SourceToken token = AbstractVisitor.makeToken(node, "");
                    visitor.onAddDuplicatedSignature(token, name);
                }
            }
        }
    }

    public void beforeClassDef(ClassDef node) {
        startScope(NodeUtils.getRepresentationString(node), node); 
    }

    public void afterClassDef(ClassDef node) {
        endScope(NodeUtils.getRepresentationString(node));
    }


    public void beforeFunctionDef(FunctionDef node) {
        startScope(NodeUtils.getRepresentationString(node), node); 
    }

    public void afterFunctionDef(FunctionDef node) {
        endScope(NodeUtils.getRepresentationString(node));
    }

    
}
