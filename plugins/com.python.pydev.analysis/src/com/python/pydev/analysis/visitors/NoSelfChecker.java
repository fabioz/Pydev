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
import java.util.List;
import java.util.Map;

import org.python.pydev.ast.analysis.IAnalysisPreferences;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.ast.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IterTokenEntry;
import org.python.pydev.core.TokensList;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.FullRepIterable;
import org.python.pydev.shared_core.string.StringUtils;
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
    private boolean isSelfNeededInFunctions = true;

    public NoSelfChecker(OccurrencesVisitor visitor) {
        this.messagesManager = visitor.messagesManager;
        this.moduleName = visitor.moduleName;
        this.module = visitor.current;
        scope.push(Scope.SCOPE_TYPE_GLOBAL); //we start in the global scope
    }

    public void beforeClassDef(ClassDef node) {
        scope.push(Scope.SCOPE_TYPE_CLASS);

        FastStringBuffer buf = new FastStringBuffer();
        for (exprType base : node.bases) {
            if (base == null) {
                continue;
            }
            if (buf.length() > 0) {
                buf.append(",");
            }
            if (isSelfNeededInFunctions && isBaseZopeInterface(base)) {
                isSelfNeededInFunctions = false;
            }
            String rep = NodeUtils.getRepresentationString(base);
            if (rep != null) {
                buf.append(FullRepIterable.getLastPart(rep));
            }
        }
        classBases.push(buf.toString());
        maybeNoSelfDefinedItems.push(new HashMap<String, Tuple<Expected, FunctionDef>>());
    }

    private final boolean isBaseZopeInterface(exprType base) {
        String baseRep = NodeUtils.getFullRepresentationString(base);
        if (baseRep == null) {
            return false;
        }
        final String zopeInterface = "zope.interface.Interface";
        String[] baseParts = extractBaseParts(baseRep);
        TokensList tokenImportedModules = module.getTokenImportedModules();
        for (IterTokenEntry entry : tokenImportedModules) {
            if (entry.object instanceof SourceToken) {
                SourceToken token = (SourceToken) entry.object;
                if (token.getAst() instanceof Import) {
                    Import importNode = (Import) token.getAst();
                    for (aliasType alias : importNode.names) {
                        if (alias.name instanceof NameTok) {
                            NameTok nameTok = (NameTok) alias.name;
                            if (nameTok.id == null) {
                                continue;
                            }
                            FastStringBuffer compareBuf = new FastStringBuffer(nameTok.id, baseRep.length());
                            if (alias.asname instanceof NameTok) {
                                String asname = NodeUtils.getNameFromNameTok(alias.asname);
                                if (baseParts[0].equals(asname)) {
                                    for (int i = 1; i < baseParts.length; i++) {
                                        compareBuf.append('.').append(baseParts[i]);
                                    }
                                }
                                if (zopeInterface.equals(compareBuf.toString())) {
                                    return true;
                                }
                            } else if (zopeInterface.equals(baseRep)) {
                                return true;
                            }
                        }
                    }
                } else if (zopeInterface.equals(token.getOriginalRep())) {
                    return baseRep.equals(token.getRepresentation());
                }
            }
        }
        return false;
    }

    private String[] extractBaseParts(String baseRep) {
        List<String> dotSplit = StringUtils.dotSplit(baseRep);
        return dotSplit.toArray(new String[dotSplit.size()]);
    }

    public void afterClassDef(ClassDef node) {
        scope.pop();
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
                SourceToken token = AbstractVisitor.makeToken(entry.getValue().o2, moduleName, null);
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

            boolean isStaticMethod = !this.isSelfNeededInFunctions;
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

            //didn't have staticmethod decorator either
            String rep = NodeUtils.getRepresentationString(node);
            if (rep.equals("__new__")) {

                //__new__ could start wit cls or self
                if (!startsWithCls && !startsWithSelf) {
                    maybeNoSelfDefinedItems.peek().put(rep,
                            new Tuple<Expected, FunctionDef>(new Expected("self or cls", received), node));
                }

            } else if (!startsWithSelf && !startsWithCls && !isStaticMethod && !isClassMethod) {
                maybeNoSelfDefinedItems.peek().put(rep,
                        new Tuple<Expected, FunctionDef>(new Expected("self", received), node));

            } else if (startsWithCls && !isClassMethod && !isStaticMethod) {
                String classBase = classBases.peek();
                if (rep.equals("__init__") && "type".equals(classBase)) {
                    //ok, in this case, cls is expected
                } else {
                    maybeNoSelfDefinedItems.peek().put(rep,
                            new Tuple<Expected, FunctionDef>(new Expected("self", received), node));
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
