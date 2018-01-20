/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Nov 11, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.List;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.structure.FastStack;

/**
 * This class visits only the global context. Other visitors should visit contexts inside of this one.
 * 
 * @author Fabio Zadrozny
 */
public final class GlobalModelVisitor extends AbstractVisitor {

    private final int visitWhat;
    private final FastStack<Assign> lastAssign = new FastStack<Assign>(20);
    private final boolean onlyAllowTokensIn__all__;
    private final Map<String, SourceToken> repToTokenWithArgs = new HashMap<String, SourceToken>();

    private SourceToken __all__;
    private Assign __all__Assign;
    private exprType[] __all__AssignTargets;

    public GlobalModelVisitor(int visitWhat, String moduleName, boolean onlyAllowTokensIn__all__,
            IPythonNature nature) {
        this(visitWhat, moduleName, onlyAllowTokensIn__all__, false, nature);
    }

    /**
     * @param moduleName
     * @param global_tokens2
     */
    public GlobalModelVisitor(int visitWhat, String moduleName, boolean onlyAllowTokensIn__all__,
            boolean lookingInLocalContext, IPythonNature nature) {
        super(nature);
        this.visitWhat = visitWhat;
        this.moduleName = moduleName;
        this.onlyAllowTokensIn__all__ = onlyAllowTokensIn__all__;
        this.tokens
                .add(new SourceToken(new Name("__dict__", Name.Load, false), "__dict__", "", "", moduleName, nature));
        if (moduleName != null && moduleName.endsWith("__init__")) {
            this.tokens.add(
                    new SourceToken(new Name("__path__", Name.Load, false), "__path__", "", "", moduleName, nature));
        }
        if (!lookingInLocalContext && ((this.visitWhat & GLOBAL_TOKENS) != 0)) {
            //__file__ is always available for any module
            this.tokens.add(
                    new SourceToken(new Name("__file__", Name.Load, false), "__file__", "", "", moduleName, nature));
            this.tokens.add(
                    new SourceToken(new Name("__name__", Name.Load, false), "__name__", "", "", moduleName, nature));
        }
    }

    @Override
    protected SourceToken addToken(SimpleNode node) {
        SourceToken tok = super.addToken(node);
        if (tok.getArgs().length() > 0) {
            this.repToTokenWithArgs.put(tok.getRepresentation(), tok);
        }
        return tok;
    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        //when visiting the global namespace, we don't go into any inner scope.
        if ((this.visitWhat & GLOBAL_TOKENS) != 0) {
            addToken(node);
        }
        return null;
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        //when visiting the global namespace, we don't go into any inner scope.
        if ((this.visitWhat & GLOBAL_TOKENS) != 0) {
            addToken(node);
        }
        return null;
    }

    /**
     * Name should be within assign.
     * 
     * @see org.python.pydev.parser.jython.ast.VisitorIF#visitAssign(org.python.pydev.parser.jython.ast.Assign)
     */
    @Override
    public Object visitAssign(Assign node) throws Exception {
        lastAssign.push(node);
        node.traverse(this);
        lastAssign.pop();
        return null;
    }

    /**
     * Visiting some name
     * 
     * @see org.python.pydev.parser.jython.ast.VisitorIF#visitName(org.python.pydev.parser.jython.ast.Name)
     */
    @Override
    public Object visitName(Name node) throws Exception {
        //when visiting the global namespace, we don't go into any inner scope.
        if ((this.visitWhat & GLOBAL_TOKENS) != 0) {
            if (node.ctx == Name.Store) {
                SourceToken added = addToken(node);
                if (lastAssign.size() > 0) {
                    Assign last = lastAssign.peek();
                    if (added.getRepresentation().equals("__all__") && __all__Assign == null) {
                        __all__ = added;
                        __all__Assign = last;
                        __all__AssignTargets = last.targets;
                    } else {
                        if (last.value != null && !(last.value instanceof Call)) {
                            //Checking if it's a Call, because we don't want to enter in the use-case:
                            //def method(a, b):
                            //   ...
                            //other = method()

                            String rep = NodeUtils.getRepresentationString(last.value);
                            if (rep != null) {
                                SourceToken methodTok = repToTokenWithArgs.get(rep);
                                if (methodTok != null) {
                                    //The use case is the following: we have a method and an assign to it:
                                    //def method(a, b):
                                    //   ...
                                    //other = method
                                    //
                                    //and later on, we want the arguments for 'other' to be the same arguments for 'method'.
                                    added.updateAliasToken(methodTok);
                                }
                            }
                        }
                    }
                }
            } else if (node.ctx == Name.Load) {
                if (node.id.equals("__all__")) {
                    //if we find __all__ more than once, let's clear it (we can only have __all__ = list of strings... if later
                    //an append, extend, etc is done in it, we have to skip this heuristic).
                    __all__AssignTargets = null;
                }
            }
        }
        return null;
    }

    /**
     * Visiting some import from
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitImportFrom(org.python.pydev.parser.jython.ast.ImportFrom)
     */
    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        if ((this.visitWhat & WILD_MODULES) != 0) {
            makeWildImportToken(node, this.tokens, moduleName, nature);
        }

        if ((this.visitWhat & ALIAS_MODULES) != 0) {
            makeImportToken(node, this.tokens, moduleName, true, nature);
        }
        return null;
    }

    /**
     * Visiting some import
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitImport(org.python.pydev.parser.jython.ast.Import)
     */
    @Override
    public Object visitImport(Import node) throws Exception {
        if ((this.visitWhat & ALIAS_MODULES) != 0) {
            makeImportToken(node, this.tokens, moduleName, true, nature);
        }
        return null;
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitStr(org.python.pydev.parser.jython.ast.Str)
     */
    @Override
    public Object visitStr(Str node) throws Exception {
        if ((this.visitWhat & MODULE_DOCSTRING) != 0) {
            this.tokens.add(new SourceToken(node, node.s, "", "", moduleName, nature));
        }
        return null;
    }

    /**
     * Overridden to check __all__
     */
    @Override
    protected void finishVisit() {
        if (onlyAllowTokensIn__all__) {
            filterAll(this.tokens);
        }
    }

    /**
     * This method will filter the passed tokens given the __all__ that was found when visiting.
     * 
     * @param tokens the tokens to be filtered (IN and OUT parameter)
     */
    public void filterAll(java.util.List<IToken> tokens) {
        if (__all__ != null) {
            SimpleNode ast = __all__.getAst();
            //just checking it
            if (__all__AssignTargets != null && __all__AssignTargets.length == 1 && __all__AssignTargets[0] == ast) {
                HashSet<String> validTokensInAll = new HashSet<String>();
                exprType value = __all__Assign.value;
                exprType[] elts = null;
                if (value instanceof List) {
                    List valueList = (List) value;
                    if (valueList.elts != null) {
                        elts = valueList.elts;
                    }
                } else if (value instanceof Tuple) {
                    Tuple valueList = (Tuple) value;
                    if (valueList.elts != null) {
                        elts = valueList.elts;
                    }
                }

                if (elts != null) {
                    int len = elts.length;
                    for (int i = 0; i < len; i++) {
                        exprType elt = elts[i];
                        if (elt instanceof Str) {
                            Str str = (Str) elt;
                            validTokensInAll.add(str.s);
                        }
                    }
                }

                if (validTokensInAll.size() > 0) {
                    int len = tokens.size();
                    for (int i = 0; i < len; i++) {
                        IToken tok = tokens.get(i);
                        if (!validTokensInAll.contains(tok.getRepresentation())) {
                            tokens.remove(i);
                            //update the len and current pos to reflect the removal.
                            i--;
                            len--;
                        }
                    }
                }
            }
        }
    }
}