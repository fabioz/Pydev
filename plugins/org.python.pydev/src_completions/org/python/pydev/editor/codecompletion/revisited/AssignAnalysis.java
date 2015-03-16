/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IToken;
import org.python.pydev.core.UnpackInfo;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ReturnVisitor;

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
     */
    public AssignCompletionInfo getAssignCompletions(ICodeCompletionASTManager manager, IModule module,
            ICompletionState state) {
        int assignLevel = state.pushAssign();
        try {
            ArrayList<IToken> ret = new ArrayList<IToken>();
            Definition[] defs = new Definition[0];
            if (module instanceof SourceModule) {
                SourceModule s = (SourceModule) module;

                try {
                    defs = s.findDefinition(state, state.getLine() + 1, state.getCol() + 1, state.getNature());
                    for (int i = 0; i < defs.length; i++) {
                        //go through all definitions found and make a merge of it...
                        Definition definition = defs[i];
                        if (state.getAlreadySearchedInAssign(definition.line, definition.col, definition.module,
                                definition.value,
                                state.getActivationToken())) {
                            // It's possible that we have many assigns where it may be normal to have loops
                            // i.e.: cp = self.x[:] ... self.x = cp, so, let's mark those places so that we don't recurse.
                            // System.out.println("Skip: " + definition);
                            continue;
                        }

                        if (state.getLine() == definition.line && state.getCol() == definition.col) {
                            //Check the module
                            if (definition.module != null && definition.module.equals(s)) {
                                //initial and final are the same
                                if (state.checkFoudSameDefinition(definition.line, definition.col, definition.module)) {
                                    //We found the same place we found previously (so, we're recursing here... Just go on)
                                    continue;
                                }
                            }
                        }

                        AssignDefinition assignDefinition = null;
                        if (definition instanceof AssignDefinition) {
                            assignDefinition = (AssignDefinition) definition;
                        }

                        if (definition.ast instanceof FunctionDef) {
                            List<IToken> found = addFunctionDefCompletionsFromReturn(manager, state, s, definition);
                            ret.addAll(found);
                        } else {
                            List<IToken> found = getNonFunctionDefCompletionsFromAssign(manager, state, s, definition,
                                    assignDefinition);
                            //String spaces = new FastStringBuffer().appendN(' ', assignLevel).toString();
                            //System.out.println(spaces + "Tok: " + state.getActivationToken());
                            //System.out.println(spaces + "Def: " + definition);
                            //System.out.println(spaces + "Adding: " + found.size());
                            ret.addAll(found);
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
            return new AssignCompletionInfo(defs, ret);
        } finally {
            state.popAssign();
        }

    }

    private List<IToken> addFunctionDefCompletionsFromReturn(ICodeCompletionASTManager manager, ICompletionState state,
            SourceModule s, Definition definition) throws CompletionRecursionException {
        ArrayList<IToken> ret = new ArrayList<IToken>();
        FunctionDef functionDef = (FunctionDef) definition.ast;

        String type = NodeUtils.getReturnTypeFromDocstring(functionDef);
        if (type != null) {
            ICompletionState copy = state.getCopy();
            copy.setActivationToken(type);
            stmtType[] body = functionDef.body;
            if (body.length > 0) {
                copy.setLine(body[0].beginLine - 1);
                copy.setCol(body[0].beginColumn - 1);
            }
            IModule module = definition.module;

            state.checkDefinitionMemory(module, definition);
            IToken[] tks = manager.getCompletionsForModule(module, copy);
            if (tks.length > 0) {
                ret.addAll(Arrays.asList(tks));
                return ret; //Ok, resolved rtype!
            } else {
                //Try to deal with some token that's not imported
                List<IPyDevCompletionParticipant> participants = ExtensionHelper
                        .getParticipants(ExtensionHelper.PYDEV_COMPLETION);
                for (IPyDevCompletionParticipant participant : participants) {
                    Collection<IToken> collection = participant.getCompletionsForType(copy);
                    if (collection != null && collection.size() > 0) {
                        ret.addAll(collection);
                        return ret; //Ok, resolved rtype!
                    }
                }
            }
        }

        for (Return return1 : ReturnVisitor.findReturns(functionDef)) {
            ICompletionState copy = state.getCopy();
            String act = NodeUtils.getFullRepresentationString(return1.value);
            if (act == null) {
                continue; //may happen if the return we're seeing is a return without anything (keep on going to check other returns)
            }
            copy.setActivationToken(act);
            copy.setLine(return1.value.beginLine - 1);
            copy.setCol(return1.value.beginColumn - 1);
            IModule module = definition.module;

            state.checkDefinitionMemory(module, definition);

            IToken[] tks = manager.getCompletionsForModule(module, copy);
            if (tks.length > 0) {
                ret.addAll(Arrays.asList(tks));
            }
        }
        return ret;
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
    private List<IToken> getNonFunctionDefCompletionsFromAssign(ICodeCompletionASTManager manager,
            ICompletionState state,
            SourceModule sourceModule, Definition definition, AssignDefinition assignDefinition)
            throws CompletionRecursionException {
        IModule module;
        ArrayList<IToken> ret = new ArrayList<IToken>();
        if (definition.ast instanceof ClassDef) {
            state.setLookingFor(ICompletionState.LOOKING_FOR_UNBOUND_VARIABLE);
            ret.addAll(((SourceModule) definition.module).getClassToks(state, manager, definition.ast));

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
                    Integer parameterIndex = CALLS_FOR_ASSIGN_WITH_RESULTING_CLASS.get(lastPart.toLowerCase());
                    if (parameterIndex != null && call.args.length >= parameterIndex) {
                        String rep = NodeUtils.getFullRepresentationString(call.args[parameterIndex - 1]);

                        HashSet<IToken> hashSet = new HashSet<IToken>();
                        List<String> lookForClass = new ArrayList<String>();
                        lookForClass.add(rep);

                        manager.getCompletionsForClassInLocalScope(sourceModule, state, true, false, lookForClass,
                                hashSet);
                        if (hashSet.size() > 0) {
                            lookForAssign = false;
                            ret.addAll(hashSet);
                        }
                    }
                }

                if (lookForAssign && assignDefinition.foundAsGlobal) {
                    //it may be declared as a global with a class defined in the local scope
                    IToken[] allLocalTokens = assignDefinition.scope.getAllLocalTokens();
                    for (IToken token : allLocalTokens) {
                        if (token.getRepresentation().equals(assignDefinition.value)) {
                            if (token instanceof SourceToken) {
                                SourceToken srcToken = (SourceToken) token;
                                if (srcToken.getAst() instanceof ClassDef) {
                                    List<IToken> classToks = ((SourceModule) assignDefinition.module).getClassToks(
                                            state, manager, srcToken.getAst());
                                    if (classToks.size() > 0) {
                                        lookForAssign = false;
                                        ret.addAll(classToks);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (lookForAssign) {
                //TODO: we might want to extend that later to check the return of some function for code-completion purposes...
                state.setLookingFor(ICompletionState.LOOKING_FOR_ASSIGN);
                ICompletionState copy = state.getCopy();
                if (definition.ast instanceof Attribute) {
                    copy.setActivationToken(NodeUtils.getFullRepresentationString(definition.ast));
                } else {
                    copy.setActivationToken(definition.value);
                }
                copy.setLine(definition.line);
                copy.setCol(definition.col);
                module = definition.module;

                state.checkDefinitionMemory(module, definition);

                if (assignDefinition != null) {
                    Collection<IToken> interfaceForLocal = assignDefinition.scope
                            .getInterfaceForLocal(assignDefinition.target);
                    ret.addAll(interfaceForLocal);
                }

                if (assignDefinition != null && assignDefinition.unpackPos >= 0) {
                    IToken[] tks = manager.getCompletionsUnpackingObject(
                            module, copy, assignDefinition.scope,
                            new UnpackInfo(false, assignDefinition.unpackPos));
                    if (tks != null) {
                        ret.addAll(Arrays.asList(tks));
                    }
                } else {
                    IToken[] tks = manager.getCompletionsForModule(module, copy);
                    ret.addAll(Arrays.asList(tks));
                }
            }
        }
        return ret;
    }

}
