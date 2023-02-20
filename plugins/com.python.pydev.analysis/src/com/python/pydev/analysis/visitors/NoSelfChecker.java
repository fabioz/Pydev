/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 28/08/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.HashMap;
import java.util.Map;

import org.python.pydev.ast.analysis.IAnalysisPreferences;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.ast.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.IModule;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.FullRepIterable;
import org.python.pydev.shared_core.structure.FastStack;
import org.python.pydev.shared_core.structure.Tuple;

public final class NoSelfChecker {

    public static class Expected {
        public String expected;
        public String received;

        public Expected(String expected, String received) {
            this.expected = expected;
            this.received = received;
        }
    }

    private final FastStack<Integer> scope = new FastStack<Integer>(10);
    private final FastStack<HashMap<String, Tuple<Expected, FunctionDef>>> maybeNoSelfDefinedItems = new FastStack<HashMap<String, Tuple<Expected, FunctionDef>>>(
            10);

    /**
     * Stack with the names of the classes
     */
    private FastStack<String> classBases = new FastStack<String>(10);

    private final String moduleName;
    private final MessagesManager messagesManager;
    private final IModule module;
    private final FastStack<ZopeInterfaceComputer> zopeInterfaceComputers = new FastStack<ZopeInterfaceComputer>(3);
    private final ICompletionCache completionCache;

    public NoSelfChecker(OccurrencesVisitor visitor) {
        this.messagesManager = visitor.messagesManager;
        this.moduleName = visitor.moduleName;
        this.module = visitor.current;
        this.completionCache = visitor.completionCache;
        scope.push(Scope.SCOPE_TYPE_GLOBAL); //we start in the global scope
    }

    public void beforeClassDef(ClassDef node) {
        scope.push(Scope.SCOPE_TYPE_CLASS);
        zopeInterfaceComputers.push(new ZopeInterfaceComputer(node, module, completionCache));

        FastStringBuffer buf = new FastStringBuffer();
        for (exprType base : node.bases) {
            if (base == null) {
                continue;
            }
            if (buf.length() > 0) {
                buf.append(",");
            }
            String rep = NodeUtils.getRepresentationString(base);
            if (rep != null) {
                buf.append(FullRepIterable.getLastPart(rep));
            }
        }
        classBases.push(buf.toString());
        maybeNoSelfDefinedItems.push(new HashMap<String, Tuple<Expected, FunctionDef>>());
    }

    public void afterClassDef(ClassDef node) {
        scope.pop();
        zopeInterfaceComputers.pop();
        classBases.pop();
        creteMessagesForStack(maybeNoSelfDefinedItems);
    }

    /**
     * @param stack
     * @param shouldBeDefined
     */
    private void creteMessagesForStack(FastStack<HashMap<String, Tuple<Expected, FunctionDef>>> stack) {
        HashMap<String, Tuple<Expected, FunctionDef>> noDefinedItems = stack.pop();
        for (Map.Entry<String, Tuple<Expected, FunctionDef>> entry : noDefinedItems.entrySet()) {
            Expected expected = entry.getValue().o1;
            if (!expected.expected.equals(expected.received)) {
                SourceToken token = AbstractVisitor.makeToken(entry.getValue().o2, moduleName, null, module);
                messagesManager.addMessage(IAnalysisPreferences.TYPE_NO_SELF, token,
                        new Object[] { token, entry.getValue().o1.expected });
            }
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

        if (scope.peek().equals(Scope.SCOPE_TYPE_CLASS)) {
            //let's check if we have to start with self or cls
            boolean startsWithSelf = false;
            boolean startsWithCls = false;
            String received = "";
            if (node.args != null) {

                if (node.args.args.length > 0) {
                    exprType arg = node.args.args[0];

                    if (arg instanceof Name) {
                        Name n = (Name) arg;

                        if (n.id.equals("self")) {
                            startsWithSelf = true;
                        } else if (n.id.equals("cls")) {
                            startsWithCls = true;
                        }
                        received = n.id;
                    }
                }
            }

            boolean isStaticMethod = false;
            boolean isClassMethod = false;
            if (node.decs != null) {
                for (decoratorsType dec : node.decs) {

                    if (dec != null) {
                        String rep = NodeUtils.getRepresentationString(dec.func);

                        if (rep != null) {

                            if (rep.equals("staticmethod")) {
                                isStaticMethod = true;
                            } else if (rep.equals("classmethod")) {
                                isClassMethod = true;
                            }
                        }
                    }
                }
            }

            ZopeInterfaceComputer zopeInterfaceComputer = zopeInterfaceComputers.peek();

            //didn't have staticmethod decorator either
            String rep = NodeUtils.getRepresentationString(node);
            if (rep.equals("__new__")) {
                //__new__ could start wit cls or self
                if (!startsWithCls && !startsWithSelf) {
                    if (!zopeInterfaceComputer.isZopeInterface()) { // zope check is more expensive, so, do as the last thing.
                        maybeNoSelfDefinedItems.peek().put(rep,
                                new Tuple<Expected, FunctionDef>(new Expected("self or cls", received), node));
                    }
                }

            } else if (!startsWithSelf && !startsWithCls && !isStaticMethod && !isClassMethod) {
                if (!zopeInterfaceComputer.isZopeInterface()) { // zope check is more expensive, so, do as the last thing.
                    maybeNoSelfDefinedItems.peek().put(rep,
                            new Tuple<Expected, FunctionDef>(new Expected("self", received), node));
                }

            } else if (startsWithCls && !isClassMethod && !isStaticMethod) {
                String classBase = classBases.peek();
                if (rep.equals("__init__") && "type".equals(classBase)) {
                    //ok, in this case, cls is expected
                } else {
                    if (!zopeInterfaceComputer.isZopeInterface()) { // zope check is more expensive, so, do as the last thing.
                        maybeNoSelfDefinedItems.peek().put(rep,
                                new Tuple<Expected, FunctionDef>(new Expected("self", received), node));
                    }
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
        if (node.targets.length == 1) {
            exprType t = node.targets[0];
            String rep = NodeUtils.getRepresentationString(t);
            if (rep == null) {
                return;
            }

            if (scope.peek() != Scope.SCOPE_TYPE_CLASS) {
                //we must be in a class scope
                return;
            }

            Tuple<Expected, FunctionDef> tup = maybeNoSelfDefinedItems.peek().get(rep);
            if (tup == null) {
                return;
            }

            FunctionDef def = tup.o2;
            if (def == null) {
                return;
            }

            //ok, it may be a staticmethod, let's check its value (should be a call)
            exprType expr = node.value;
            if (expr instanceof Call) {
                Call call = (Call) expr;
                if (call.args.length == 1) {
                    String argRep = NodeUtils.getRepresentationString(call.args[0]);
                    if (argRep != null && argRep.equals(rep)) {
                        String funcCall = NodeUtils.getRepresentationString(call.func);

                        if (def != null && funcCall != null && funcCall.equals("staticmethod")) {
                            //ok, finally... it is a staticmethod after all...
                            maybeNoSelfDefinedItems.peek().remove(rep);

                        } else if (funcCall != null && funcCall.equals("classmethod")) {
                            //ok, finally... it is a classmethod after all...
                            tup.o1.expected = "cls";
                        }
                    }
                }
            }
        }
    }
}
