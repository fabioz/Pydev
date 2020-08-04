/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 2, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ast.codecompletion.revisited;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.ast.codecompletion.PyCodeCompletionPreferences;
import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.NoExceptionCloseable;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.structure.Tuple3;

/**
 * @author Fabio Zadrozny
 */
public final class CompletionState implements ICompletionState {

    private String activationToken;
    private int line = -1;
    private int col = -1;
    private IPythonNature nature;
    private String qualifier;
    private int levelGetCompletionsUnpackingObject = 0;

    private final Memo<String> memory = new Memo<String>();
    private final Memo<Definition> definitionMemory = new Memo<Definition>();
    private final Memo<IModule> wildImportMemory = new Memo<IModule>();
    private final Memo<String> importedModsCalled = new Memo<String>();
    private final Memo<String> findMemory = new Memo<String>();
    private final Memo<String> resolveImportMemory = new Memo<String>();
    private final Memo<String> findDefinitionMemory = new Memo<String>();
    private final Memo<String> findLocalDefinedDefinitionMemory = new Memo<String>();
    private Stack<Memo<IToken>> findResolveImportMemory = new Stack<Memo<IToken>>();
    private final Memo<String> findModuleCompletionsMemory = new Memo<String>();
    private final Memo<String> findSourceFromCompiledMemory = new Memo<String>(1); //max is 1 for this one!

    private boolean builtinsGotten = false;
    private boolean localImportsGotten = false;
    private boolean isInCalltip = false;

    private LookingFor lookingForInstance = LookingFor.LOOKING_FOR_INSTANCE_UNDEFINED;
    private TokensList tokenImportedModules;
    private ICompletionCache completionCache;
    private String fullActivationToken;
    private long initialMillis = 0;
    private long maxMillisToComplete;

    private IProgressMonitor cancelMonitor;

    @Override
    public void setCancelMonitor(IProgressMonitor cancelMonitor) {
        this.cancelMonitor = cancelMonitor;
    }

    @Override
    public ICompletionState getCopy() {
        return new CompletionStateWrapper(this);
    }

    @Override
    public ICompletionState getCopyForResolveImportWithActTok(String actTok) {
        CompletionState state = (CompletionState) CompletionStateFactory.getEmptyCompletionState(actTok, this.nature,
                this.completionCache);
        state.nature = nature;
        state.findResolveImportMemory = findResolveImportMemory;

        return state;
    }

    /**
     * this is a class that can act as a memo and check if something is defined more than 'n' times
     *
     * @author Fabio Zadrozny
     */
    private static class Memo<E> {

        private int max;

        public Memo() {
            this.max = MAX_NUMBER_OF_OCURRENCES;
        }

        public Memo(int max) {
            this.max = max;
        }

        /**
         * if more than this number of ocurrences is found, we are in a recursion
         */
        private static final int MAX_NUMBER_OF_OCURRENCES = 5;

        public Map<IModule, Map<E, Integer>> memo = new HashMap<IModule, Map<E, Integer>>();

        public boolean isInRecursion(IModule caller, E def) {
            Map<E, Integer> val;

            boolean occuredMoreThanMax = false;
            if (!memo.containsKey(caller)) {

                //still does not exist, let's create the structure...
                val = new HashMap<E, Integer>();
                memo.put(caller, val);

            } else {
                val = memo.get(caller);

                if (val.containsKey(def)) { //may be a recursion
                    Integer numberOfOccurences = val.get(def);

                    //should never be null...
                    if (numberOfOccurences > max) {
                        occuredMoreThanMax = true; //ok, we are recursing...
                    }
                }
            }

            //let's raise the number of ocurrences anyway
            Integer numberOfOccurences = val.get(def);
            if (numberOfOccurences == null) {
                val.put(def, 1); //this is the first ocurrence
            } else {
                val.put(def, numberOfOccurences + 1);
            }

            return occuredMoreThanMax;
        }
    }

    /**
     * @param line2 starting at 0
     * @param col2 starting at 0
     * @param token
     * @param qual
     * @param nature2
     */
    public CompletionState(int line2, int col2, String token, IPythonNature nature2, String qualifier) {
        this(line2, col2, token, nature2, qualifier, new CompletionCache());
    }

    /**
     * @param line2 starting at 0
     * @param col2 starting at 0
     * @param token
     * @param qual
     * @param nature2
     */
    public CompletionState(int line2, int col2, String token, IPythonNature nature2, String qualifier,
            ICompletionCache completionCache) {
        this.line = line2;
        this.col = col2;
        this.activationToken = token;
        this.nature = nature2;
        this.qualifier = qualifier;
        Assert.isNotNull(completionCache);
        this.completionCache = completionCache;
    }

    public CompletionState() {

    }

    /**
     * @param module
     * @param base
     */
    @Override
    public void checkWildImportInMemory(IModule caller, IModule wild) throws CompletionRecursionException {
        if (this.wildImportMemory.isInRecursion(caller, wild)) {
            throw new CompletionRecursionException(
                    "Possible recursion found -- probably programming error -- (caller: " + caller.getName()
                            + ", import: " + wild.getName() + " ) - stopping analysis.");
        }

    }

    /**
     * @param module
     * @param definition
     */
    @Override
    public void checkDefinitionMemory(IModule module, IDefinition definition) throws CompletionRecursionException {
        if (this.definitionMemory.isInRecursion(module, (Definition) definition)) {
            throw new CompletionRecursionException(
                    "Possible recursion found -- probably programming error --  (module: " + module.getName()
                            + ", token: " + definition + ") - stopping analysis.");
        }

    }

    /**
     * @param module
     */
    @Override
    public void checkFindMemory(IModule module, String value) throws CompletionRecursionException {
        if (this.findMemory.isInRecursion(module, value)) {
            throw new CompletionRecursionException(
                    "Possible recursion found -- probably programming error --  (module: " + module.getName()
                            + ", value: " + value + ") - stopping analysis.");
        }

    }

    /**
     * @param module
     * @throws CompletionRecursionException
     */
    @Override
    public void checkResolveImportMemory(IModule module, String value) throws CompletionRecursionException {
        if (this.resolveImportMemory.isInRecursion(module, value)) {
            throw new CompletionRecursionException(
                    "Possible recursion found -- probably programming error --  (module: " + module.getName()
                            + ", value: " + value + ") - stopping analysis.");
        }

    }

    @Override
    public void checkFindDefinitionMemory(IModule mod, String tok) throws CompletionRecursionException {
        if (this.findDefinitionMemory.isInRecursion(mod, tok)) {
            throw new CompletionRecursionException(
                    "Possible recursion found -- probably programming error --  (module: " + mod.getName()
                            + ", value: " + tok + ") - stopping analysis.");
        }
    }

    @Override
    public void checkFindLocalDefinedDefinitionMemory(IModule mod, String tok) throws CompletionRecursionException {
        if (this.findLocalDefinedDefinitionMemory.isInRecursion(mod, tok)) {
            throw new CompletionRecursionException(
                    "Possible recursion found -- probably programming error --  (module: " + mod.getName()
                            + ", value: " + tok + ") - stopping analysis.");
        }
    }

    /**
     * @param module
     * @param base
     */
    @Override
    public void checkMemory(IModule module, String base) throws CompletionRecursionException {
        if (this.memory.isInRecursion(module, base)) {
            throw new CompletionRecursionException(
                    "Possible recursion found -- probably programming error --  (module: " + module.getName()
                            + ", token: " + base + ") - stopping analysis.");
        }
    }

    @Override
    public void checkMaxTimeForCompletion() throws CompletionRecursionException {
        if (cancelMonitor != null) {
            if (cancelMonitor.isCanceled()) {
                throw new CompletionRecursionException(
                        "Completion cancelled.");
            }
        }
        if (this.initialMillis <= 0) {
            this.initialMillis = System.currentTimeMillis();
            if (SharedCorePlugin.inTestMode()) {
                this.maxMillisToComplete = Long.MAX_VALUE; //2 * 1000; //In test mode the max is 2 seconds.
            } else {
                this.maxMillisToComplete = PyCodeCompletionPreferences
                        .getMaximumNumberOfMillisToCompleteCodeCompletionRequest();
            }
        } else {
            long diff = System.currentTimeMillis() - this.initialMillis;
            if (diff > this.maxMillisToComplete) {
                throw new CompletionRecursionException(
                        "Stopping analysis: completion took too much time to complete. Max set to: "
                                + this.maxMillisToComplete + " millis. Current: " + diff + " millis. Note: this "
                                + "value may be changed in the code-completion preferences.");
            }
        }
    };

    Set<Tuple3<Integer, Integer, IModule>> foundSameDefinitionMemory = new HashSet<Tuple3<Integer, Integer, IModule>>();

    @Override
    public boolean checkFoudSameDefinition(int line, int col, IModule mod) {
        Tuple3<Integer, Integer, IModule> key = new Tuple3<Integer, Integer, IModule>(line, col, mod);
        if (foundSameDefinitionMemory.contains(key)) {
            return true;
        }
        foundSameDefinitionMemory.add(key);
        return false;
    }

    @Override
    public boolean canStillCheckFindSourceFromCompiled(IModule mod, String tok) {
        if (!findSourceFromCompiledMemory.isInRecursion(mod, tok)) {
            return true;
        }
        return false;
    }

    /**
     * This check is a bit different from the others because of the context it will work in...
     *
     *  This check is used when resolving things from imports, so, it may check for recursions found when in previous context, but
     *  if a recursion is found in the current context, that's ok (because it's simply trying to get the actual representation for a token)
     */
    @Override
    public void checkFindResolveImportMemory(IToken token) throws CompletionRecursionException {
        Iterator<Memo<IToken>> it = findResolveImportMemory.iterator();
        while (it.hasNext()) {
            Memo<IToken> memo = it.next();
            if (memo.isInRecursion(null, token)) {
                //                if(it.hasNext()){
                throw new CompletionRecursionException(
                        "Possible recursion found -- probably programming error --  (token: " + token
                                + ") - stopping analysis.");
                //                }
            }
        }
    }

    @Override
    public void popFindResolveImportMemoryCtx() {
        findResolveImportMemory.pop();
    }

    @Override
    public void pushFindResolveImportMemoryCtx() {
        findResolveImportMemory.push(new Memo<IToken>());
    }

    /**
     * @param module
     * @param base
     */
    @Override
    public void checkFindModuleCompletionsMemory(IModule mod, String tok) throws CompletionRecursionException {
        if (this.findModuleCompletionsMemory.isInRecursion(mod, tok)) {
            throw new CompletionRecursionException(
                    "Possible recursion found -- probably programming error --  (module: " + mod.getName()
                            + ", token: " + tok + ") - stopping analysis.");
        }
    }

    @Override
    public String getActivationToken() {
        return activationToken;
    }

    @Override
    public IPythonNature getNature() {
        return nature;
    }

    @Override
    public void setActivationToken(String string) {
        activationToken = string;
    }

    @Override
    public String getFullActivationToken() {
        return this.fullActivationToken;
    }

    @Override
    public void setFullActivationToken(String act) {
        this.fullActivationToken = act;
    }

    @Override
    public void setBuiltinsGotten(boolean b) {
        builtinsGotten = b;
    }

    /**
     * @param i: starting at 0
     */
    @Override
    public void setCol(int i) {
        col = i;
    }

    /**
     * @param i: starting at 0
     */
    @Override
    public void setLine(int i) {
        line = i;
    }

    @Override
    public void setLocalImportsGotten(boolean b) {
        localImportsGotten = b;
    }

    @Override
    public boolean getLocalImportsGotten() {
        return localImportsGotten;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public int getCol() {
        return col;
    }

    @Override
    public boolean getBuiltinsGotten() {
        return builtinsGotten;
    }

    @Override
    public void raiseNFindTokensOnImportedModsCalled(IModule mod, String tok) throws CompletionRecursionException {
        if (this.importedModsCalled.isInRecursion(mod, tok)) {
            throw new CompletionRecursionException("Possible recursion found (mod: " + mod.getName() + ", tok: " + tok
                    + " ) - stopping analysis.");
        }
    }

    @Override
    public boolean getIsInCalltip() {
        return isInCalltip;
    }

    @Override
    public void setLookingFor(LookingFor b) {
        this.setLookingFor(b, false);
    }

    @Override
    public void setLookingFor(LookingFor b, boolean force) {
        //the 1st is the one that counts (or it can be forced)
        if (this.lookingForInstance == ICompletionState.LookingFor.LOOKING_FOR_INSTANCE_UNDEFINED || force) {
            this.lookingForInstance = b;
        }
    }

    @Override
    public LookingFor getLookingFor() {
        return this.lookingForInstance;
    }

    @Override
    public ICompletionState getCopyWithActTok(String value) {
        ICompletionState copy = getCopy();
        copy.setActivationToken(value);
        return copy;
    }

    /**
     * @param line starting at 0
     * @param col starting at 0
     */
    @Override
    public ICompletionState getCopyWithActTok(String value, int line, int col) {
        ICompletionState copy = getCopy();
        copy.setLine(line);
        copy.setCol(col);
        copy.setActivationToken(value);
        return copy;
    }

    @Override
    public String getQualifier() {
        return this.qualifier;
    }

    @Override
    public void setIsInCalltip(boolean isInCalltip) {
        this.isInCalltip = isInCalltip;
    }

    @Override
    public void setTokenImportedModules(TokensList tokenImportedModules) {
        if (tokenImportedModules != null) {
            if (this.tokenImportedModules == null) {
                this.tokenImportedModules = tokenImportedModules.copy(); //keep a copy of it
            }
        }
    }

    @Override
    public TokensList getTokenImportedModules() {
        return this.tokenImportedModules;
    }

    // ICompletionCache interface implementation -----------------------------------------------------------------------

    @Override
    public void add(Object key, Object n) {
        this.completionCache.add(key, n);
    }

    @Override
    public Object getObj(Object o) {
        return this.completionCache.getObj(o);
    }

    @Override
    public void remove(Object key) {
        this.completionCache.remove(key);
    }

    @Override
    public void removeStaleEntries() {
        this.completionCache.removeStaleEntries();
    }

    @Override
    public void clear() {
        this.completionCache.clear();
    }

    private static class AlreadySerched {

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((actTok == null) ? 0 : actTok.hashCode());
            result = prime * result + col;
            result = prime * result + line;
            result = prime * result + ((module == null) ? 0 : module.hashCode());
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            AlreadySerched other = (AlreadySerched) obj;
            if (actTok == null) {
                if (other.actTok != null) {
                    return false;
                }
            } else if (!actTok.equals(other.actTok)) {
                return false;
            }
            if (col != other.col) {
                return false;
            }
            if (line != other.line) {
                return false;
            }
            if (module == null) {
                if (other.module != null) {
                    return false;
                }
            } else if (!module.equals(other.module)) {
                return false;
            }
            if (value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!value.equals(other.value)) {
                return false;
            }
            return true;
        }

        private final int line;
        private final int col;
        private final IModule module;
        private final String actTok;
        private final String value;

        AlreadySerched(int line, int col, IModule module, String value, String actTok) {
            this.line = line;
            this.col = col;
            this.module = module;
            this.actTok = actTok;
            this.value = value;
        }
    }

    private Set<AlreadySerched> alreadySearchedInAssign = new HashSet<CompletionState.AlreadySerched>();

    @Override
    public boolean getAlreadySearchedInAssign(int line, int col, IModule module, String value, String actTok) {
        AlreadySerched s = new AlreadySerched(line, col, module, value, actTok);
        if (alreadySearchedInAssign.contains(s)) {
            return true;
        }
        alreadySearchedInAssign.add(s);
        return false;
    }

    int assign = 0;

    @Override
    public int pushAssign() {
        assign += 1;
        return assign;
    }

    @Override
    public void popAssign() {
        assign -= 1;
        if (assign == 0) {
            // When we get to level 0, clear anything searched previously
            alreadySearchedInAssign.clear();
        }
    }

    @Override
    public void pushGetCompletionsUnpackingObject() throws CompletionRecursionException {
        levelGetCompletionsUnpackingObject += 1;
        if (levelGetCompletionsUnpackingObject > 15) {
            throw new CompletionRecursionException(
                    "Error: recursion detected getting completions unpacking object. Activation token: "
                            + this.getActivationToken());
        }
    }

    @Override
    public void popGetCompletionsUnpackingObject() {
        levelGetCompletionsUnpackingObject -= 1;
    }

    private final Map<IModule, ModuleHandleOrNotGotten> pyIStubModule = new HashMap<>();

    @Override
    public ModuleHandleOrNotGotten getPyiStubModule(IModule module) {
        return pyIStubModule.get(module);
    }

    @Override
    public void setPyIStubModule(IModule module, IModule pyIModule) {
        this.pyIStubModule.put(module, new ModuleHandleOrNotGotten(pyIModule));
    }

    @Override
    public NoExceptionCloseable pushLookingFor(LookingFor lookingFor) {
        LookingFor prev = getLookingFor();
        if (lookingFor != null) {
            setLookingFor(lookingFor, true);
        }
        return new NoExceptionCloseable() {

            @Override
            public void close() {
                setLookingFor(prev, true);
            }
        };
    }
}
