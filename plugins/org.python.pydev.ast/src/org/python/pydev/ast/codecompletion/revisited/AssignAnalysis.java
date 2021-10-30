/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ast.codecompletion.revisited;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.python.pydev.ast.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.ast.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.ICompletionState.LookingFor;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IToken;
import org.python.pydev.core.ITypeInfo;
import org.python.pydev.core.IterTokenEntry;
import org.python.pydev.core.NoExceptionCloseable;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.UnpackInfo;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.BinOp;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Index;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.UnaryOp;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.TypeInfo;
import org.python.pydev.shared_core.string.FullRepIterable;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * This class is used to analyse the assigns in the code and bring actual completions for them.
 */
public class AssignAnalysis {

    /**
     * If we got here, either there really is no definition from the token
     * or it is not looking for a definition. This means that probably
     * it is something like.
     *
     * It also can happen in many scopes, so, first we have to check the current
     * scope and then pass to higher scopes
     *
     * e.g. foo = Foo()
     *      foo. | Ctrl+Space
     *
     * so, first thing is discovering in which scope we are (Storing previous scopes so
     * that we can search in other scopes as well).
     * @param localScope
     */
    public DefinitionAndCompletions getAssignCompletions(ICodeCompletionASTManager manager, IModule module,
            ICompletionState state, ILocalScope localScope) {
        state.pushAssign();
        try {
            TokensList ret = new TokensList();
            Definition[] defs = new Definition[0];
            if (module instanceof SourceModule) {
                SourceModule sourceModule = (SourceModule) module;

                try {
                    defs = sourceModule.findDefinition(state, state.getLine() + 1, state.getCol() + 1,
                            state.getNature());
                    if (defs.length > 0) {
                        for (int i = 0; i < defs.length; i++) {
                            //go through all definitions found and make a merge of it...
                            Definition definition = defs[i];
                            TokensList completionsFromDefinition = getCompletionsFromDefinition(definition, state,
                                    sourceModule, manager);
                            if (completionsFromDefinition != null && completionsFromDefinition.notEmpty()) {
                                if (definition instanceof AssignDefinition) {
                                    AssignDefinition assignDefinition = (AssignDefinition) definition;
                                    if (assignDefinition.nodeValue instanceof Call) {
                                        completionsFromDefinition
                                                .setLookingFor(LookingFor.LOOKING_FOR_INSTANCED_VARIABLE);
                                    }
                                }
                                ret.addAll(completionsFromDefinition);
                            }
                        }
                    } else {
                        if (localScope != null) {
                            TokensList tokens = searchInLocalTokens(manager, state, true, state.getLine() + 1,
                                    state.getCol() + 1,
                                    module, localScope, state.getActivationToken());
                            if (tokens != null) {
                                ret.addAll(tokens);
                            }
                        }
                    }

                } catch (CompletionRecursionException e) {
                    //thats ok
                } catch (Exception e) {
                    Log.log(e);
                    throw new RuntimeException("Error when getting assign completions for:" + module.getName(), e);
                } catch (Throwable t) {
                    throw new RuntimeException("A throwable exception has been detected " + t.getClass());
                }
            }
            return new DefinitionAndCompletions(defs, ret);
        } finally {
            state.popAssign();
        }
    }

    /**
     * @param definition
     * @param state
     * @param sourceModule this is not the definition module, but the module where we started searching for the definition.
     * @param manager
     * @return
     * @throws CompletionRecursionException
     */
    public TokensList getCompletionsFromDefinition(Definition definition, ICompletionState state,
            SourceModule sourceModule, ICodeCompletionASTManager manager) throws CompletionRecursionException {
        TokensList ret = new TokensList();
        if (state.getAlreadySearchedInAssign(definition.line, definition.col, definition.module,
                definition.value, state.getActivationToken())) {
            // It's possible that we have many assigns where it may be normal to have loops
            // i.e.: cp = self.x[:] ... self.x = cp, so, let's mark those places so that we don't recurse.
            // System.out.println("Skip: " + definition);
            return ret;
        }

        if (state.getLine() + 1 == definition.line && state.getCol() + 1 == definition.col) {
            //Check the module
            if (definition.module != null && definition.module.equals(sourceModule)) {
                //initial and final are the same
                if (state.checkFoudSameDefinition(definition.line, definition.col,
                        definition.module)) {
                    //We found the same place we found previously (so, we're recursing here... Just go on)
                    return ret;
                }
            }
        }

        AssignDefinition assignDefinition = null;
        if (definition instanceof AssignDefinition) {
            assignDefinition = (AssignDefinition) definition;
        }

        boolean foundAsParamWithTypingInfo = false;
        if (definition.scope != null) {
            SimpleNode ast = definition.ast;
            if (ast instanceof Assign) {
                Assign assign = (Assign) ast;
                if (assign.targets != null) {
                    ast = assign.targets[0];
                }
            }
            if (NodeUtils.isParamName(ast)) {
                Name name = (Name) ast;
                String scopeStackPathNames = definition.scope.getScopeStackPathNames();
                if (scopeStackPathNames != null && scopeStackPathNames.length() > 0) {
                    foundAsParamWithTypingInfo = computeCompletionsFromParameterTypingInfo(
                            manager, state, ret, foundAsParamWithTypingInfo, name,
                            scopeStackPathNames, definition.module);
                    if (!foundAsParamWithTypingInfo) {
                        IModule pyiStubModule = manager.getPyiStubModule(definition.module, state);
                        foundAsParamWithTypingInfo = computeCompletionsFromParameterTypingInfo(
                                manager, state, ret, foundAsParamWithTypingInfo, name,
                                scopeStackPathNames, pyiStubModule);
                    }
                }
            } else if (NodeUtils.isSelfAttribute(ast)) {
                String fullRepresentationString = NodeUtils
                        .getFullRepresentationString(ast);
                if (fullRepresentationString.contains(".")) {
                    // Remove the 'self'
                    String attributeWithoutSelf = FullRepIterable
                            .getLastPart(fullRepresentationString);
                    String scopeStackPathNames = definition.scope
                            .getScopeStackPathNamesToLastClassDef();
                    if (scopeStackPathNames != null && scopeStackPathNames.length() > 0) {
                        foundAsParamWithTypingInfo = computeCompletionsFromAttributeTypingInfo(
                                manager, state, ret, foundAsParamWithTypingInfo,
                                attributeWithoutSelf, scopeStackPathNames, definition.module);
                        if (!foundAsParamWithTypingInfo) {
                            IModule pyiStubModule = manager.getPyiStubModule(definition.module,
                                    state);
                            foundAsParamWithTypingInfo = computeCompletionsFromAttributeTypingInfo(
                                    manager, state, ret, foundAsParamWithTypingInfo,
                                    attributeWithoutSelf, scopeStackPathNames, pyiStubModule);
                        }
                    }
                }
            }
        }

        if (!foundAsParamWithTypingInfo) {
            if (definition.ast instanceof FunctionDef) {
                TokensList found = manager.getCompletionFromFuncDefReturn(
                        state, sourceModule, definition, false);
                ret.addAll(found);
            } else {
                TokensList found = getNonFunctionDefCompletionsFromAssign(manager, state, sourceModule,
                        definition, assignDefinition);
                //String spaces = new FastStringBuffer().appendN(' ', assignLevel).toString();
                //System.out.println(spaces + "Tok: " + state.getActivationToken());
                //System.out.println(spaces + "Def: " + definition);
                //System.out.println(spaces + "Adding: " + found.size());
                ret.addAll(found);
            }
        }
        return ret;
    }

    private boolean computeCompletionsFromParameterTypingInfo(ICodeCompletionASTManager manager, ICompletionState state,
            TokensList ret, boolean foundAsParamWithTypingInfo, Name name, String scopeStackPathNames,
            IModule pyiStubModule) throws CompletionRecursionException {
        if (pyiStubModule instanceof SourceModule) {
            SourceModule sourceModule = (SourceModule) pyiStubModule;
            SimpleNode ast = sourceModule.getAst();
            SimpleNode nodeFromPath = NodeUtils.getNodeFromPath(ast,
                    scopeStackPathNames);
            if (nodeFromPath != null) {
                TypeInfo info = NodeUtils.getTypeForParameterFromAST(
                        NodeUtils.getRepresentationString(name), nodeFromPath);
                if (info != null) {
                    List<ITypeInfo> lookForClass = new ArrayList<>();
                    lookForClass.add(info);
                    TokensList completions = manager.getCompletionsForClassInLocalScope(sourceModule, state,
                            true, false, lookForClass);
                    if (completions != null && completions.size() > 0) {
                        ret.addAll(completions);
                        foundAsParamWithTypingInfo = true;
                    }
                }
            }
        }
        return foundAsParamWithTypingInfo;
    }

    private boolean computeCompletionsFromAttributeTypingInfo(ICodeCompletionASTManager manager, ICompletionState state,
            TokensList ret, boolean foundAsParamWithTypingInfo, String attributeWithoutSelf,
            String scopeStackPathNames, IModule pyiStubModule) throws CompletionRecursionException {
        if (pyiStubModule instanceof SourceModule) {
            SourceModule sourceModule = (SourceModule) pyiStubModule;
            SimpleNode ast = sourceModule.getAst();
            SimpleNode nodeFromPath = NodeUtils.getNodeFromPath(ast,
                    scopeStackPathNames);
            if (nodeFromPath instanceof ClassDef) {
                TypeInfo info = NodeUtils.getTypeForClassDefAttribute(
                        attributeWithoutSelf, (ClassDef) nodeFromPath);
                if (info != null) {
                    List<ITypeInfo> lookForClass = new ArrayList<>();
                    lookForClass.add(info);
                    ret.addAll(manager.getCompletionsForClassInLocalScope(sourceModule, state,
                            true, false, lookForClass));
                    foundAsParamWithTypingInfo = true;
                }
            }
        }
        return foundAsParamWithTypingInfo;
    }

    /**
     * The user should be able to configure that, but let's leave it hard-coded until the next release...
     *
     * Names of methods that will return instance of the passed class -> index of class parameter.
     */
    public final static Map<String, Integer> CALLS_FOR_ASSIGN_WITH_RESULTING_CLASS = new HashMap<String, Integer>();

    static {
        //method factory that receives parameter with class -> class parameter index
        CALLS_FOR_ASSIGN_WITH_RESULTING_CLASS.put("adapt".toLowerCase(), 2);
        CALLS_FOR_ASSIGN_WITH_RESULTING_CLASS.put("GetSingleton".toLowerCase(), 1);
        CALLS_FOR_ASSIGN_WITH_RESULTING_CLASS.put("GetImplementation".toLowerCase(), 1);
        CALLS_FOR_ASSIGN_WITH_RESULTING_CLASS.put("GetAdapter".toLowerCase(), 1);
        CALLS_FOR_ASSIGN_WITH_RESULTING_CLASS.put("get_adapter".toLowerCase(), 1);
    }

    /**
     * This method will look into the right side of an assign and its definition and will try to gather the tokens for
     * it, knowing that it is dealing with a non-function def token for the definition found.
     *
     * @param ret the place where the completions should be added
     * @param assignDefinition may be null if it was not actually found as an assign
     * @return
     */
    private TokensList getNonFunctionDefCompletionsFromAssign(ICodeCompletionASTManager manager,
            ICompletionState state,
            SourceModule sourceModule, Definition definition, AssignDefinition assignDefinition)
            throws CompletionRecursionException {
        IModule module;
        TokensList ret = new TokensList();
        if (definition.ast instanceof ClassDef) {
            try (NoExceptionCloseable x = state.pushLookingFor(LookingFor.LOOKING_FOR_UNBOUND_VARIABLE)) {
                ret.addAll(((SourceModule) definition.module).getClassToks(state, manager, (ClassDef) definition.ast));
            }

        } else {
            boolean lookForAssign = true;

            //ok, see what we can do about adaptation here...
            //pyprotocols does adapt(xxx, Interface), so, knowing the type of the interface can get us to nice results...
            //the user can usually have other factory methods that do that too. E.g.: GetSingleton(Class) may return an
            //expected class and so on, so, this should be configured somehow
            if (assignDefinition != null) {

                Assign assign = (Assign) assignDefinition.ast;
                if (assign.value instanceof Call) {
                    Call call = (Call) assign.value;
                    String lastPart = FullRepIterable.getLastPart(assignDefinition.value);

                    if ("namedtuple".equals(lastPart)) {
                        //throw new AssertionError("deal with named tuple");
                        if (call.args != null && call.args.length > 1) {
                            exprType args = call.args[1];
                            exprType[] elts = null;
                            if (args instanceof org.python.pydev.parser.jython.ast.List) {
                                org.python.pydev.parser.jython.ast.List list = (org.python.pydev.parser.jython.ast.List) args;
                                elts = list.elts;
                            } else if (args instanceof org.python.pydev.parser.jython.ast.Tuple) {
                                org.python.pydev.parser.jython.ast.Tuple tuple = (org.python.pydev.parser.jython.ast.Tuple) args;
                                elts = tuple.elts;
                            } else if (args instanceof org.python.pydev.parser.jython.ast.Set) {
                                org.python.pydev.parser.jython.ast.Set set = (org.python.pydev.parser.jython.ast.Set) args;
                                elts = set.elts;
                            }
                            if (elts != null) {
                                for (exprType exprType : elts) {
                                    if (exprType instanceof Str) {
                                        ret.addAll(new TokensList(
                                                new IToken[] { new SourceToken(exprType, ((Str) exprType).s, "",
                                                        "", sourceModule.getName(), sourceModule.getNature()) }));
                                    }
                                }
                                return ret;
                            }
                            if (args instanceof Call) {
                                Call call2 = (Call) args;
                                if (call2.func instanceof Attribute) {
                                    Attribute attribute = (Attribute) call2.func;
                                    if ("split".equals(NodeUtils.getRepresentationString(attribute.attr))) {
                                        if (attribute.value instanceof Str) {
                                            Str str = (Str) attribute.value;
                                            if (str.s != null) {
                                                List<String> split = StringUtils.split(str.s, " ");
                                                for (String string : split) {
                                                    ret.addAll(new TokensList(new IToken[] { new SourceToken(str,
                                                            string, "",
                                                            "", sourceModule.getName(), sourceModule.getNature()) }));
                                                }
                                                return ret;
                                            }
                                        }
                                    }
                                }
                            }
                            if (args instanceof Str) {
                                Str str = (Str) args;
                                if (str.s != null) {
                                    for (String s : str.s.split("(\\s|,)+")) {
                                        if (!s.isEmpty()) {
                                            ret.addAll(new TokensList(new IToken[] { new SourceToken(str,
                                                    s, "",
                                                    "", sourceModule.getName(), sourceModule.getNature()) }));
                                        }
                                    }
                                    return ret;
                                }
                            }
                        }
                    }

                    Integer parameterIndex = CALLS_FOR_ASSIGN_WITH_RESULTING_CLASS.get(lastPart.toLowerCase());
                    if (parameterIndex != null && call.args.length >= parameterIndex) {
                        String rep = NodeUtils.getFullRepresentationString(call.args[parameterIndex - 1]);

                        List<ITypeInfo> lookForClass = new ArrayList<>();
                        lookForClass.add(new TypeInfo(rep));

                        TokensList completionsForClassInLocalScope = manager
                                .getCompletionsForClassInLocalScope(sourceModule, state, true, false, lookForClass);
                        if (completionsForClassInLocalScope.size() > 0) {
                            lookForAssign = false;
                            ret.addAll(completionsForClassInLocalScope);
                        }
                    }
                }

                if (lookForAssign) {
                    TokensList tokens = null;
                    if (assign.type != null) {
                        tokens = searchInLocalTokens(manager, state, lookForAssign,
                                definition.line, definition.col, definition.module, assignDefinition.scope,
                                assignDefinition.type);
                    } else if (assign.value != null) {
                        tokens = searchInLocalTokens(manager, state, lookForAssign,
                                definition.line, definition.col, definition.module, assignDefinition.scope,
                                assignDefinition.value);
                    }
                    if (tokens != null && tokens.size() > 0) {
                        ret.addAll(tokens);
                        lookForAssign = false;
                    }
                }
            }

            if (lookForAssign) {
                //TODO: we might want to extend that later to check the return of some function for code-completion purposes...
                state.setLookingFor(ICompletionState.LookingFor.LOOKING_FOR_ASSIGN);
                ICompletionState copy = state.getCopy();
                if (definition.ast instanceof Attribute) {
                    copy.setActivationToken(NodeUtils.getFullRepresentationString(definition.ast));
                } else if (definition.type != null && !definition.type.isEmpty()) {
                    copy.setActivationToken(definition.type);
                } else {
                    copy.setActivationToken(definition.value);
                }
                copy.setLine(definition.line - 1);
                copy.setCol(definition.col - 1);
                module = definition.module;

                state.checkDefinitionMemory(module, definition);

                if (assignDefinition != null) {
                    TokensList interfaceForLocal = assignDefinition.scope
                            .getInterfaceForLocal(assignDefinition.target);
                    ret.addAll(interfaceForLocal);
                }

                int unpackPos = -1;
                boolean unpackBackwards = false;
                if (assignDefinition != null) {
                    if (assignDefinition.nodeType != null) {
                        TokensList completions = new TokensList();
                        List<String> typingUnionValues = extractTypingUnionValues(manager, module,
                                assignDefinition.nodeType);
                        if (typingUnionValues != null && typingUnionValues.size() > 0) {
                            typingUnionValues.add(NodeUtils.getFullRepresentationString(assignDefinition.nodeType));
                            for (String value : typingUnionValues) {
                                ICompletionState customCopy = state.getCopyWithActTok(value);
                                TokensList valueComps = manager.getCompletionsForModule(module, customCopy);
                                completions.addAll(valueComps);
                            }
                        }
                        TokensList normalCompletions = manager.getCompletionsForModule(module, copy, true, true);
                        completions.addAll(normalCompletions);
                        if (completions != null && completions.size() > 0) {
                            ret.addAll(completions);
                            return ret;
                        }
                    }
                    unpackPos = assignDefinition.unpackPos;
                    // Let's see if we have
                    if (definition.ast instanceof Assign) {
                        Assign assign = (Assign) definition.ast;
                        if (assign.value instanceof Subscript) {
                            Subscript subscript = (Subscript) assign.value;
                            if (subscript.slice instanceof Index) {
                                Index index = (Index) subscript.slice;
                                exprType indexValue = index.value;
                                if (indexValue instanceof UnaryOp) {
                                    // i.e.: x = a[-1]
                                    UnaryOp unaryOp = (UnaryOp) indexValue;
                                    if (unaryOp.op == UnaryOp.USub) { //negative
                                        unpackBackwards = true;
                                    }
                                    indexValue = unaryOp.operand;

                                }

                                if (indexValue instanceof Num) {
                                    Num num = (Num) indexValue;
                                    // i.e.: x = a[0] or x = a[-1]
                                    String rep = NodeUtils.getRepresentationString(num);
                                    try {
                                        int subscriptIndex = Integer.parseInt(rep);
                                        unpackPos = subscriptIndex; // Note that we can be dealing with negative numbers!
                                    } catch (NumberFormatException e) {
                                        //ignore
                                    }
                                }
                            }
                        }
                    }
                }
                if (assignDefinition != null && unpackPos >= 0) {
                    TokensList tks = manager.getCompletionsUnpackingObject(
                            module, copy, assignDefinition.scope,
                            new UnpackInfo(false, unpackPos, unpackBackwards));
                    if (tks != null) {
                        ret.addAll(tks);
                    }
                } else {
                    TokensList tks = manager.getCompletionsForModule(module, copy, true, true);
                    ret.addAll(tks);
                }
            }
        }
        return ret;
    }

    private static List<String> extractTypingUnionValues(ICodeCompletionASTManager manager,
            IModule module, exprType node)
            throws CompletionRecursionException {
        if (manager.isNodeTypingUnionSubscript(module, node)) {
            Subscript subscript = (Subscript) node;
            return NodeUtils.extractValuesFromSubscriptSlice(subscript.slice);
        } else if (node instanceof BinOp) {
            BinOp binOp = (BinOp) node;
            return NodeUtils.extractValuesFromBinOp(binOp, BinOp.BitOr);
        }
        return null;
    }

    /**
     *
     * @param manager
     * @param state
     * @param lookForAssign
     * @param line starts at 1
     * @param col starts at 1
     * @param module
     * @param scope
     * @return
     * @throws CompletionRecursionException
     */
    public TokensList searchInLocalTokens(ICodeCompletionASTManager manager, ICompletionState state,
            boolean lookForAssign, int line, int col, IModule module, ILocalScope scope, String activationToken)
            throws CompletionRecursionException {
        //it may be declared as a global with a class defined in the local scope
        TokensList allLocalTokens = scope.getAllLocalTokens();
        for (IterTokenEntry entry : allLocalTokens) {
            IToken token = entry.getToken();
            if (token.getRepresentation().equals(activationToken)) {
                if (token instanceof SourceToken) {
                    SourceToken srcToken = (SourceToken) token;
                    SimpleNode ast = srcToken.getAst();
                    if (ast instanceof ClassDef && module instanceof SourceModule) {
                        TokensList classToks = ((SourceModule) module).getClassToks(state, manager, (ClassDef) ast);
                        if (classToks.notEmpty()) {
                            return classToks;
                        }
                    }
                }
            }
        }
        ICompletionState copy = state.getCopy();

        copy.setLine(line);
        copy.setCol(col);
        copy.setActivationToken(activationToken);

        TokensList tokens = manager.getCompletionsFromTokenInLocalScope(module, copy, false, false,
                scope);
        if (tokens != null && tokens.size() > 0) {
            return tokens;
        }
        return null;
    }

}
