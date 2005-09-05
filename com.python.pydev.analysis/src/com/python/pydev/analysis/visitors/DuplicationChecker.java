/*
 * Created on 24/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.parser.visitors.NodeUtils;

import com.python.pydev.analysis.IAnalysisPreferences;

/**
 * Used to check for duplicated signatures
 * 
 * @author Fabio
 */
public class DuplicationChecker {

    /**
     * used to know the defined signatures
     */
    private Stack<Map<String,String>> stack = new Stack<Map<String,String>>();
    
    /**
     * used to manage the messages
     */
    private MessagesManager messagesManager;

    /**
     * constructor
     */
    public DuplicationChecker(MessagesManager messagesManager) {
        this.messagesManager = messagesManager;
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
            String exists = stack.peek().get(name);
            if(exists != null){
                SourceToken token = AbstractVisitor.makeToken(node, "");
                messagesManager.addMessage(IAnalysisPreferences.TYPE_DUPLICATED_SIGNATURE, token, name );
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
