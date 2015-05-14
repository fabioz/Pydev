/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 14, 2006
 */
package org.python.pydev.core;

import java.util.List;

import org.python.pydev.core.structure.CompletionRecursionException;

public interface ICompletionState extends ICompletionCache {

    String getActivationToken();

    String getFullActivationToken();

    IPythonNature getNature();

    ICompletionState getCopy();

    /**
     * This is the activation token with callables changed to the reference.
     *
     * E.g.: if we had Grinder.grinder.getLogger(), this would be: Grinder.grinder.getLogger
     * And if we had x.ClassA(), this would be x.ClassA
     */
    void setActivationToken(String act);

    /**
     * This is the full activation token (e.g.: Grinder.grinder.getLogger().getIt())
     * Only actually set if the activation token changes.
     * Note that it's only used to pass on to the java completion engine.
     */
    void setFullActivationToken(String act);

    void setBuiltinsGotten(boolean b);

    void raiseNFindTokensOnImportedModsCalled(IModule mod, String tok) throws CompletionRecursionException;

    /**
     * @param i: starting at 0
     */
    void setCol(int i);

    /**
     * @param i: starting at 0
     */
    void setLine(int i);

    void setLocalImportsGotten(boolean b);

    boolean getLocalImportsGotten();

    /**
     * @return the line for the request (starting at 0)
     */
    int getLine();

    /**
     * @return the col for the request (starting at 0)
     */
    int getCol();

    void checkDefinitionMemory(IModule module, IDefinition definition) throws CompletionRecursionException;

    void checkWildImportInMemory(IModule current, IModule mod) throws CompletionRecursionException;

    public void checkResolveImportMemory(IModule module, String value) throws CompletionRecursionException;

    boolean getBuiltinsGotten();

    void checkMemory(IModule module, String base) throws CompletionRecursionException;

    void checkFindMemory(IModule module, String value) throws CompletionRecursionException;

    void checkFindDefinitionMemory(IModule mod, String tok) throws CompletionRecursionException;

    void checkFindLocalDefinedDefinitionMemory(IModule mod, String tok) throws CompletionRecursionException;

    void checkFindModuleCompletionsMemory(IModule mod, String tok) throws CompletionRecursionException;

    void checkFindResolveImportMemory(IToken tok) throws CompletionRecursionException;

    void checkMaxTimeForCompletion() throws CompletionRecursionException;

    /**
     * Doesn't throw an exception, returns true if the given line and column have already been found previously.
     */
    boolean checkFoudSameDefinition(int line, int col, IModule mod);

    /**
     * Unlike other checks, it won't throw an exception, but'll see if the given module was already checked for
     * a given token (this happens when we're looking for a token that has been found in a compiled module and
     * we want to translate to an actual position... but if we loop for some reason, it has to be stopped and
     * the actual compiled module is the source of the definition).
     */
    boolean canStillCheckFindSourceFromCompiled(IModule mod, String tok);

    boolean getIsInCalltip();

    public static final int LOOKING_FOR_INSTANCE_UNDEFINED = 0;
    public static final int LOOKING_FOR_INSTANCED_VARIABLE = 1;
    public static final int LOOKING_FOR_UNBOUND_VARIABLE = 2;
    public static final int LOOKING_FOR_CLASSMETHOD_VARIABLE = 3;
    public static final int LOOKING_FOR_ASSIGN = 4;

    /**
     * Identifies if we should be looking for an instance (in which case, self should not
     * be added to the parameters -- otherwise, it should)
     */
    void setLookingFor(int lookingFor);

    /**
     * Used so that we can force it...
     */
    void setLookingFor(int lookingFor, boolean force);

    ICompletionState getCopyWithActTok(String value);

    String getQualifier();

    int getLookingFor();

    void setIsInCalltip(boolean isInCalltip);

    ICompletionState getCopyForResolveImportWithActTok(String representation);

    void pushFindResolveImportMemoryCtx();

    void popFindResolveImportMemoryCtx();

    /**
     * This method will save the list with the tokens for the imported modules.
     *
     * The attribute that stores it will not be copied when a copy is gotten.
     * If already set, this function should not override a previous value.
     */
    void setTokenImportedModules(List<IToken> tokenImportedModules);

    /**
     * May be null
     */
    public List<IToken> getTokenImportedModules();

    int pushAssign();

    void popAssign();

    boolean getAlreadySearchedInAssign(int line, int col, IModule module, String value, String actTok);

    void pushGetCompletionsUnpackingObject() throws CompletionRecursionException;

    void popGetCompletionsUnpackingObject();

}