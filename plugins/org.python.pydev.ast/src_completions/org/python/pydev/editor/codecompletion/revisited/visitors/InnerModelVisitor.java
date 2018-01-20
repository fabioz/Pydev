/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Dec 21, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;

/**
 * This class is used to visit the inner context of class or a function. 
 * 
 * @author Fabio Zadrozny
 */
public class InnerModelVisitor extends AbstractVisitor {

    /**
     * List that contains heuristics to find attributes.
     */
    private final List<HeuristicFindAttrs> attrsHeuristics = new ArrayList<HeuristicFindAttrs>();

    private final Map<String, SourceToken> repToTokenWithArgs = new HashMap<String, SourceToken>();

    @Override
    protected SourceToken addToken(SimpleNode node) {
        SourceToken tok = super.addToken(node);
        if (tok.getArgs().length() > 0) {
            this.repToTokenWithArgs.put(tok.getRepresentation(), tok);
        }
        return tok;
    }

    public InnerModelVisitor(String moduleName, ICompletionState state, IPythonNature nature) {
        super(nature);
        this.moduleName = moduleName;
        attrsHeuristics.add(new HeuristicFindAttrs(HeuristicFindAttrs.WHITIN_METHOD_CALL,
                HeuristicFindAttrs.IN_KEYWORDS, "properties.create", moduleName, state, this.repToTokenWithArgs,
                nature));
        attrsHeuristics.add(new HeuristicFindAttrs(HeuristicFindAttrs.WHITIN_ANY, HeuristicFindAttrs.IN_ASSIGN, "",
                moduleName, state, this.repToTokenWithArgs, nature));
    }

    /**
     * This should be changed as soon as we know what should we visit.
     */
    private static int VISITING_NOTHING = -1;

    /**
     * When visiting class, get attributes and methods
     */
    private static int VISITING_CLASS = 0;

    /**
     * Initially, we're visiting nothing.
     */
    private int visiting = VISITING_NOTHING;

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#unhandled_node(org.python.pydev.parser.jython.SimpleNode)
     */
    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#traverse(org.python.pydev.parser.jython.SimpleNode)
     */
    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        if (visiting == VISITING_NOTHING) {
            visiting = VISITING_CLASS;
            node.traverse(this);

        } else if (visiting == VISITING_CLASS) {
            //that's a class within the class we're visiting
            addToken(node);
        }

        return null;
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        if (visiting == VISITING_CLASS) {
            addToken(node);

            //iterate heuristics to find attributes
            for (Iterator<HeuristicFindAttrs> iter = attrsHeuristics.iterator(); iter.hasNext();) {
                HeuristicFindAttrs element = iter.next();
                element.visitFunctionDef(node);
                addElementTokens(element);
            }

        }
        return null;
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitAssign(org.python.pydev.parser.jython.ast.Assign)
     */
    @Override
    public Object visitAssign(Assign node) throws Exception {
        if (visiting == VISITING_CLASS) {

            //iterate heuristics to find attributes
            for (Iterator<HeuristicFindAttrs> iter = attrsHeuristics.iterator(); iter.hasNext();) {
                HeuristicFindAttrs element = iter.next();
                element.visitAssign(node);
                addElementTokens(element);
            }
        }
        return null;
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitCall(org.python.pydev.parser.jython.ast.Call)
     */
    @Override
    public Object visitCall(Call node) throws Exception {
        if (visiting == VISITING_CLASS) {

            //iterate heuristics to find attributes
            for (Iterator<HeuristicFindAttrs> iter = attrsHeuristics.iterator(); iter.hasNext();) {
                HeuristicFindAttrs element = iter.next();
                element.visitCall(node);
                addElementTokens(element);
            }
        }
        return null;
    }

    /**
     * @param element
     */
    private void addElementTokens(HeuristicFindAttrs element) {
        tokens.addAll(element.tokens);
        element.tokens.clear();
    }

}
