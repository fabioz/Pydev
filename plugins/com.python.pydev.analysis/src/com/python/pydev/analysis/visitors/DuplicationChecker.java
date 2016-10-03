/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.HashMap;
import java.util.Map;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.structure.FastStack;

import com.python.pydev.analysis.IAnalysisPreferences;

/**
 * Used to check for duplicated signatures
 * 
 * @author Fabio
 */
public final class DuplicationChecker {

    /**
     * used to know the defined signatures
     */
    private final FastStack<Map<String, String>> stack = new FastStack<Map<String, String>>(10);
    private final Scope scope;
    private final MessagesManager messagesManager;
    private final IPythonNature nature;

    /**
     * constructor
     * @param visitor 
     */
    public DuplicationChecker(OccurrencesVisitor visitor) {
        startScope("", null);
        this.scope = visitor.scope;
        this.messagesManager = visitor.messagesManager;
        this.nature = visitor.nature;
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
        if (stack.size() > 0) {
            if (!scope.getPrevScopeItems().getIsInSubSubScope()) {
                String exists = stack.peek().get(name);
                if (exists != null) {
                    if (node instanceof FunctionDef) {
                        FunctionDef functionDef = (FunctionDef) node;
                        if (functionDef.decs != null && functionDef.decs.length > 0) {
                            for (decoratorsType dec : functionDef.decs) {
                                if (dec.func != null) {
                                    String fullRepresentationString = NodeUtils.getFullRepresentationString(dec.func);
                                    if (fullRepresentationString.startsWith(name + ".")) {
                                        return;
                                    }
                                }
                            }
                        }

                    }
                    SourceToken token = AbstractVisitor.makeToken(node, "", nature);
                    messagesManager.addMessage(IAnalysisPreferences.TYPE_DUPLICATED_SIGNATURE, token, name);
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
