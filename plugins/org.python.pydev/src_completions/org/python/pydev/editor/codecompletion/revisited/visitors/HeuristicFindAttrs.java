/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
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

import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.python.pydev.core.ICompletionState;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * This class defines how we should find attributes. 
 * 
 * Heuristics provided allow someone to find an attr inside a function definition (IN_INIT or IN_ANY)
 * or inside a method call (e.g. a method called properties.create(x=0) - that's what I use, so, that's specific).
 * Other uses may be customized later, once we know which other uses are done.
 * 
 * @author Fabio Zadrozny
 */
public class HeuristicFindAttrs extends AbstractVisitor {

    /**
     * Whether we should add the attributes that are added as 'self.xxx = 10'
     */
    private boolean discoverSelfAttrs = true;

    private final Map<String, SourceToken> repToTokenWithArgs;

    /**
     * @param where
     * @param how
     * @param methodCall
     * @param state 
     */
    public HeuristicFindAttrs(int where, int how, String methodCall, String moduleName, ICompletionState state,
            Map<String, SourceToken> repToTokenWithArgs) {
        this.where = where;
        this.how = how;
        this.methodCall = methodCall;
        this.moduleName = moduleName;
        this.repToTokenWithArgs = repToTokenWithArgs;
        if (state != null) {
            if (state.getLookingFor() == ICompletionState.LOOKING_FOR_CLASSMETHOD_VARIABLE) {
                this.discoverSelfAttrs = false;
            }
        }
    }

    public Stack<SimpleNode> stack = new Stack<SimpleNode>();

    public static final int WHITIN_METHOD_CALL = 0;
    public static final int WHITIN_INIT = 1;
    public static final int WHITIN_ANY = 2;

    public int where = -1;

    public static final int IN_ASSIGN = 0;
    public static final int IN_KEYWORDS = 1;

    public int how = -1;

    private boolean entryPointCorrect = false;

    private boolean inAssing = false;
    private boolean inFuncDef = false;

    /**
     * This is the method that can be used to declare them (e.g. properties.create)
     * It's only used it it is a method call.
     */
    public String methodCall = "";

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
    protected SourceToken addToken(SimpleNode node) {
        SourceToken tok = super.addToken(node);
        if (tok.getArgs().length() > 0) {
            this.repToTokenWithArgs.put(tok.getRepresentation(), tok);
        }
        return tok;
    }

    //ENTRY POINTS
    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitCall(org.python.pydev.parser.jython.ast.Call)
     */
    @Override
    public Object visitCall(Call node) throws Exception {
        if (entryPointCorrect == false && methodCall.length() > 0) {
            entryPointCorrect = true;

            if (node.func instanceof Attribute) {
                List<String> c = StringUtils.dotSplit(methodCall);

                Attribute func = (Attribute) node.func;
                if (((NameTok) func.attr).id.equals(c.get(1))) {

                    if (func.value instanceof Name) {
                        Name name = (Name) func.value;
                        if (name.id.equals(c.get(0))) {
                            for (int i = 0; i < node.keywords.length; i++) {
                                addToken(node.keywords[i]);
                            }
                        }
                    }
                }
            }

            entryPointCorrect = false;
        }
        return null;
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitFunctionDef(org.python.pydev.parser.jython.ast.FunctionDef)
     */
    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        stack.push(node);
        if (entryPointCorrect == false) {
            entryPointCorrect = true;
            inFuncDef = true;

            if (where == WHITIN_ANY) {
                node.traverse(this);

            } else if (where == WHITIN_INIT && node.name != null && ((NameTok) node.name).id.equals("__init__")) {
                node.traverse(this);
            }
            entryPointCorrect = false;
            inFuncDef = false;
        }
        stack.pop();

        return null;
    }

    //END ENTRY POINTS

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        stack.push(node);
        Object r = super.visitClassDef(node);
        stack.pop();
        return r;
    }

    /**
     * Name should be within assign.
     * 
     * @see org.python.pydev.parser.jython.ast.VisitorIF#visitAssign(org.python.pydev.parser.jython.ast.Assign)
     */
    @Override
    public Object visitAssign(Assign node) throws Exception {
        if (how == IN_ASSIGN) {
            inAssing = true;

            exprType value = node.value;
            String rep = NodeUtils.getRepresentationString(value);
            SourceToken methodTok = null;
            if (rep != null) {
                methodTok = repToTokenWithArgs.get(rep);
                //The use case is the following: we have a method and an assign to it:
                //def method(a, b):
                //   ...
                //other = method
                //
                //and later on, we want the arguments for 'other' to be the same arguments for 'method'.
            }

            for (int i = 0; i < node.targets.length; i++) {
                if (node.targets[i] instanceof Attribute) {
                    visitAttribute((Attribute) node.targets[i]);

                } else if (node.targets[i] instanceof Name && inFuncDef == false) {
                    String id = ((Name) node.targets[i]).id;
                    if (id != null) {
                        SourceToken added = addToken(node.targets[i]);
                        if (methodTok != null) {
                            added.updateAliasToken(methodTok);
                        }
                    }

                } else if (node.targets[i] instanceof Tuple && inFuncDef == false) {
                    //that's for finding the definition: a,b,c = range(3) inside a class definition
                    Tuple tuple = (Tuple) node.targets[i];
                    for (exprType t : tuple.elts) {
                        if (t instanceof Name) {
                            String id = ((Name) t).id;
                            if (id != null) {
                                addToken(t);
                            }
                        }
                    }

                }
            }

            inAssing = false;
        }
        return null;
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitAttribute(org.python.pydev.parser.jython.ast.Attribute)
     */
    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        if (how == IN_ASSIGN && inAssing) {
            if (node.value instanceof Name) {
                String id = ((Name) node.value).id;
                if (id != null) {
                    if (this.discoverSelfAttrs) {
                        if (id.equals("self")) {
                            addToken(node);

                        }
                    } else {
                        if (id.equals("cls")) {
                            addToken(node);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorIF#visitIf(org.python.pydev.parser.jython.ast.If)
     */
    @Override
    public Object visitIf(If node) throws Exception {
        node.traverse(this);
        return null;
    }

}
