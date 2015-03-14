/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.visitors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.FastStack;
import org.python.pydev.shared_core.structure.OrderedSet;

import com.python.pydev.analysis.visitors.OccurrencesVisitor.TokenFoundStructure;

/**
 * @author Fabio
 *
 */
public final class ArgumentsChecker {

    private final IModule current;
    private final IPythonNature nature;
    private final ICompletionCache completionCache;
    private final MessagesManager messagesManager;

    public ArgumentsChecker(OccurrencesVisitor occurrencesVisitor) {
        this.current = occurrencesVisitor.current;
        this.nature = occurrencesVisitor.nature;
        this.completionCache = occurrencesVisitor.completionCache;
        this.messagesManager = occurrencesVisitor.messagesManager;
    }

    private static final int NO_STATIC_NOR_CLASSMETHOD = 0;
    private static final int CLASSMETHOD = 1;
    private static final int STATICMETHOD = 2;

    private int isStaticOrClassMethod(FunctionDef functionDefinitionReferenced) {
        if (functionDefinitionReferenced.decs != null) {
            for (decoratorsType dec : functionDefinitionReferenced.decs) {
                if (dec != null) {
                    String rep = NodeUtils.getRepresentationString(dec.func);

                    if (rep != null) {

                        if (rep.equals("staticmethod")) {
                            return STATICMETHOD;
                        } else if (rep.equals("classmethod")) {
                            return CLASSMETHOD;
                        }
                    }
                }
            }
        }

        //If it got here, there may still be an assign to it...
        if (functionDefinitionReferenced.parent instanceof ClassDef) {
            ClassDef classDef = (ClassDef) functionDefinitionReferenced.parent;
            stmtType[] body = classDef.body;
            if (body != null) {
                int len = body.length;

                String funcName = ((NameTok) functionDefinitionReferenced.name).id;
                for (int i = 0; i < len; i++) {
                    if (body[i] instanceof Assign) {
                        Assign assign = (Assign) body[i];
                        if (assign.targets == null) {
                            continue;
                        }

                        //we're looking for xxx = staticmethod(xxx)
                        if (assign.targets.length == 1) {
                            exprType t = assign.targets[0];
                            String rep = NodeUtils.getRepresentationString(t);
                            if (rep == null) {
                                continue;
                            }
                        }

                        exprType expr = assign.value;
                        if (expr instanceof Call) {
                            Call call = (Call) expr;
                            if (call.args.length == 1) {
                                String argRep = NodeUtils.getRepresentationString(call.args[0]);
                                if (argRep != null && argRep.equals(funcName)) {
                                    String funcCall = NodeUtils.getRepresentationString(call.func);

                                    if ("staticmethod".equals(funcCall)) {
                                        //ok, finally... it is a staticmethod after all...
                                        return STATICMETHOD;
                                    }
                                    if ("classmethod".equals(funcCall)) {
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

    @SuppressWarnings("unchecked")
    /*default*/void checkAttrFound(Call callNode, TokenFoundStructure found) throws Exception,
            CompletionRecursionException {
        FunctionDef functionDefinitionReferenced;
        SourceToken nameToken;
        boolean callingBoundMethod;
        if (found != null && found.defined && found.token instanceof SourceToken) {
            nameToken = (SourceToken) found.token;
            String rep = nameToken.getRepresentation();

            ArrayList<IDefinition> definition = new ArrayList<IDefinition>();
            SimpleNode ast = nameToken.getAst();
            try {
                PyRefactoringFindDefinition.findActualDefinition(null, this.current, rep, definition, ast.beginLine,
                        ast.beginColumn, this.nature, this.completionCache);
            } catch (Exception e) {
                Log.log(e);
                return;
            }

            for (IDefinition iDefinition : definition) {
                Definition d = (Definition) iDefinition;
                if (d.ast instanceof FunctionDef) {
                    functionDefinitionReferenced = (FunctionDef) d.ast;

                    String withoutLastPart = FullRepIterable.getWithoutLastPart(rep);
                    Boolean b = valToBounded.get(withoutLastPart);
                    if (b != null) {
                        callingBoundMethod = b;
                    } else {
                        int count = StringUtils.count(rep, '.');

                        if (count == 1 && rep.startsWith("self.")) {
                            FastStack<SimpleNode> scopeStack = d.scope.getScopeStack();
                            if (scopeStack.size() > 0 && scopeStack.peek() instanceof ClassDef) {
                                callingBoundMethod = true;
                            } else {
                                callingBoundMethod = false;
                            }

                        } else {
                            FastStack<SimpleNode> scopeStack = d.scope.getScopeStack();
                            if (scopeStack.size() > 1 && scopeStack.peek(1) instanceof ClassDef) {
                                callingBoundMethod = true;
                                String withoutLast = FullRepIterable.getWithoutLastPart(rep);
                                ArrayList<IDefinition> definition2 = new ArrayList<IDefinition>();
                                PyRefactoringFindDefinition.findActualDefinition(null, this.current, withoutLast,
                                        definition2, -1, -1, this.nature, this.completionCache);

                                for (IDefinition iDefinition2 : definition2) {
                                    Definition d2 = (Definition) iDefinition2;
                                    if (d2.ast instanceof ClassDef) {
                                        callingBoundMethod = false;
                                        break;
                                    }
                                }
                            } else {
                                callingBoundMethod = false;
                            }
                        }
                    }
                    analyzeCallAndFunctionMatch(callNode, functionDefinitionReferenced, nameToken, callingBoundMethod);
                    break;
                }
            }
        }
    }

    private final Map<ClassDef, SimpleNode> defToConsideredInit = new HashMap<ClassDef, SimpleNode>();

    /*default*/void checkNameFound(Call callNode, SourceToken sourceToken) throws Exception {
        FunctionDef functionDefinitionReferenced;
        boolean callingBoundMethod = false;
        SimpleNode ast = sourceToken.getAst();
        if (ast instanceof FunctionDef) {
            functionDefinitionReferenced = (FunctionDef) ast;
            analyzeCallAndFunctionMatch(callNode, functionDefinitionReferenced, sourceToken, callingBoundMethod);

        } else if (ast instanceof ClassDef) {
            ClassDef classDef = (ClassDef) ast;
            SimpleNode initNode = defToConsideredInit.get(classDef);
            callingBoundMethod = true;
            if (initNode == null) {
                String className = ((NameTok) classDef.name).id;

                Definition foundDef = sourceToken.getDefinition();
                IModule mod = this.current;
                if (foundDef != null) {
                    mod = foundDef.module;
                }
                SimpleNode n = NodeUtils.getNodeFromPath(classDef, "__init__");
                if (n instanceof FunctionDef) {
                    initNode = n;

                } else {
                    IDefinition[] definition = mod.findDefinition(CompletionStateFactory.getEmptyCompletionState(
                            className + ".__init__", this.nature, this.completionCache), -1, -1, this.nature);
                    for (IDefinition iDefinition : definition) {
                        Definition d = (Definition) iDefinition;
                        if (d.ast instanceof FunctionDef) {
                            initNode = d.ast;
                            defToConsideredInit.put(classDef, initNode);
                            break;
                        }
                    }
                }
            }

            if (initNode instanceof FunctionDef) {
                functionDefinitionReferenced = (FunctionDef) initNode;
                analyzeCallAndFunctionMatch(callNode, functionDefinitionReferenced, sourceToken, callingBoundMethod);
            }
        }
    }

    protected void analyzeCallAndFunctionMatch(Call callNode, FunctionDef functionDefinitionReferenced,
            IToken nameToken, boolean callingBoundMethod) throws Exception {
        int functionArgsLen = functionDefinitionReferenced.args.args != null ? functionDefinitionReferenced.args.args.length
                : 0;
        Collection<String> functionRequiredArgs = new OrderedSet<String>(functionArgsLen);
        Collection<String> functionOptionalArgs = new OrderedSet<String>(functionArgsLen);

        int staticOrClassMethod = isStaticOrClassMethod(functionDefinitionReferenced);
        boolean ignoreFirstParameter = callingBoundMethod;
        if (staticOrClassMethod != NO_STATIC_NOR_CLASSMETHOD) {
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

        for (int i = 0; i < functionArgsLen; i++) {
            if (i == 0 && ignoreFirstParameter) {
                continue; //Ignore first parameter when calling a bound method.
            }
            String rep = NodeUtils.getRepresentationString(functionDefinitionReferenced.args.args[i]);
            if (rep == null) {
                continue;
            }
            if (functionDefinitionReferenced.args.defaults == null
                    || (functionDefinitionReferenced.args.defaults.length > i && functionDefinitionReferenced.args.defaults[i] == null)) {
                //it's null, so, it's required
                functionRequiredArgs.add(rep);
            } else {
                //not null: optional with default value
                functionOptionalArgs.add(rep);
            }
        }
        exprType[] kwonlyargs = functionDefinitionReferenced.args.kwonlyargs;
        Collection<String> functionKeywordOnlyArgs = null;
        if (kwonlyargs != null) {
            functionKeywordOnlyArgs = new OrderedSet<String>(kwonlyargs.length);
            for (exprType exprType : kwonlyargs) {
                if (exprType != null) {
                    String representationString = NodeUtils.getRepresentationString(exprType);
                    if (representationString != null) {
                        functionKeywordOnlyArgs.add(representationString);
                    }
                }
            }
        }

        int callArgsLen = callNode.args != null ? callNode.args.length : 0;

        for (int i = 0; i < callArgsLen; i++) {

            if (functionRequiredArgs.size() > 0) {
                //Remove first one (no better api in collection...)
                Iterator<String> it = functionRequiredArgs.iterator();
                it.next();
                it.remove();
                continue;

            } else if (functionOptionalArgs.size() > 0) {
                Iterator<String> it = functionOptionalArgs.iterator();
                it.next();
                it.remove();
                continue;
            }

            //All 'regular' and 'optional' arguments consumed (i.e.: def m1(a, b, c=10)), so, it'll only
            //be possible to accept an item that's in *args at this point.
            if (functionDefinitionReferenced.args.vararg == null) {
                onArgumentsMismatch(nameToken, callNode);
                return; //Error reported, so, bail out of function!
            }

        }

        int callKeywordArgsLen = callNode.keywords != null ? callNode.keywords.length : 0;
        for (int i = 0; i < callKeywordArgsLen; i++) {
            String rep = NodeUtils.getRepresentationString(callNode.keywords[i].arg);
            if (rep != null) {
                //keyword argument (i.e.: call(a=10)), so, only accepted in kwargs or with some argument with that name.
                if (functionRequiredArgs.remove(rep)) {
                    continue;

                } else if (functionOptionalArgs.remove(rep)) {
                    continue;

                } else if (functionKeywordOnlyArgs != null && functionKeywordOnlyArgs.remove(rep)) {
                    continue;

                } else {
                    //An argument with that name was not found, so, it may only be handled through kwargs at this point!
                    if (functionDefinitionReferenced.args.kwarg == null) {
                        onArgumentsMismatch(nameToken, callNode);
                        return; //Error reported, so, bail out of function!
                    }
                }
            }
        }

        if (functionRequiredArgs.size() > 0 || (functionKeywordOnlyArgs != null && functionKeywordOnlyArgs.size() > 0)) {
            if (callNode.kwargs == null && callNode.starargs == null) {
                //Not all required parameters were consumed!
                onArgumentsMismatch(nameToken, callNode);
                return; //Error reported, so, bail out of function!
            }
        } else if (functionOptionalArgs.size() > 0) {

        } else {
            //required and optional size == 0
            if (callNode.kwargs != null && functionDefinitionReferenced.args.kwarg == null) {
                //We have more things that were not handled
                onArgumentsMismatch(nameToken, callNode);
                return; //Error reported, so, bail out of function!
            }
            if (callNode.starargs != null && functionDefinitionReferenced.args.vararg == null) {
                //We have more things that were not handled
                onArgumentsMismatch(nameToken, callNode);
                return; //Error reported, so, bail out of function!
            }
        }
    }

    private void onArgumentsMismatch(IToken node, Call callNode) {
        this.messagesManager.onArgumentsMismatch(node, callNode);
    }

    private Map<String, Boolean> valToBounded = new HashMap<String, Boolean>();

    public void visitAssign(Assign node) {
        boolean bounded = false;
        exprType[] targets = node.targets;
        if (node.value != null) {
            if (node.value instanceof Call) {
                bounded = true;
            }
        }

        if (targets != null) {
            int len = targets.length;
            for (int i = 0; i < len; i++) {
                exprType expr = targets[i];
                valToBounded.put(NodeUtils.getFullRepresentationString(expr), bounded);
            }
        }

    }

}
