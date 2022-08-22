/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ast.codecompletion.revisited;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.ast.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.ast.codecompletion.revisited.modules.EmptyModule;
import org.python.pydev.ast.codecompletion.revisited.modules.PredefinedSourceModule;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.ast.interpreter_managers.InterpreterInfo;
import org.python.pydev.ast.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.ast.interpreter_managers.TypeshedLoader;
import org.python.pydev.core.DeltaSaver;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModuleRequestState;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.IToken;
import org.python.pydev.core.IterTokenEntry;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.shared_core.cache.LRUCache;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.FullRepIterable;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * @author Fabio Zadrozny
 */
public final class SystemModulesManager extends ModulesManagerWithBuild implements ISystemModulesManager {

    private static final String DIR_NAME_FOR_COMPILED_CACHE = "shell";

    /**
     * The system modules manager may have a nature if we create a SystemASTManager
     */
    private transient IPythonNature nature;

    /**
     * This is the place where we store the info related to this manager
     */
    private InterpreterInfo info;

    public SystemModulesManager(InterpreterInfo info) {
        this.info = info;
    }

    public void setInfo(InterpreterInfo info) {
        //Should only be used in tests (in general the info should be passed in the constructor and never changed again).
        this.info = info;
    }

    public InterpreterInfo getInfo() {
        return info;
    }

    @Override
    public void endProcessing() {
        save();
    }

    @Override
    public IModulesManager[] getManagersInvolved(boolean checkSystemManager) {
        return new IModulesManager[] { this };
    }

    /**
     * @see org.python.pydev.core.ISystemModulesManager#getBuiltins()
     */
    @Override
    public String[] getBuiltins() {
        return this.info.getBuiltins();
    }

    public Set<String> getBuiltinsAsSet() {
        return this.info.getBuiltinsAsSet();
    }

    @Override
    public void setPythonNature(IPythonNature nature) {
        Assert.isTrue(nature instanceof SystemPythonNature);
        Assert.isTrue(((SystemPythonNature) nature).info == this.info);

        this.nature = nature;
    }

    @Override
    public IPythonNature getNature() {
        if (nature == null) {
            IInterpreterManager manager = getInterpreterManager();
            nature = new SystemPythonNature(manager, this.info);
        }
        return nature;
    }

    @Override
    public IInterpreterManager getInterpreterManager() {
        int interpreterType = this.info.getInterpreterType();
        switch (interpreterType) {
            case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                return InterpreterManagersAPI.getJythonInterpreterManager();

            case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                return InterpreterManagersAPI.getPythonInterpreterManager();

            case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                return InterpreterManagersAPI.getIronpythonInterpreterManager();

            default:
                throw new RuntimeException("Don't know how to handle: " + interpreterType);
        }
    }

    @Override
    public ISystemModulesManager getSystemModulesManager() {
        return this; //itself
    }

    @Override
    public IModule getModule(String name, IPythonNature nature, boolean checkSystemManager, boolean dontSearchInit,
            IModuleRequestState moduleRequest) {
        return getModule(name, nature, dontSearchInit, moduleRequest);
    }

    @Override
    public String resolveModule(String full, boolean checkSystemManager) {
        return super.resolveModule(full);
    }

    @Override
    public List<String> getCompletePythonPath(IInterpreterInfo interpreter, IInterpreterManager manager) {
        if (interpreter == null) {
            throw new RuntimeException("The interpreter must be specified (received null)");
        } else {
            return interpreter.getPythonPath();
        }
    }

    @Override
    public IModule getRelativeModule(String name, IPythonNature nature, IModuleRequestState moduleRequest) {
        return super.getModule(name, nature, true, moduleRequest);
    }

    /**
     * Called after the pythonpath is changed.
     */
    @Override
    protected void onChangePythonpath(SortedMap<ModulesKey, ModulesKey> keys) {
        //create the builtin modules
        String[] builtins = getBuiltins();
        if (builtins != null) {
            for (int i = 0; i < builtins.length; i++) {
                String name = builtins[i];
                final ModulesKey k = new ModulesKey(name, null);
                keys.put(k, k);
            }
        }
        super.onChangePythonpath(keys);
    }

    /**
     * This is a cache with the name of a builtin pointing to itself (so, it works basically as a set), it's used
     * so that when we find a builtin that does not have a __file__ token we do not try to recreate it again later.
     */
    private final LRUCache<String, String> builtinsNotConsidered = new LRUCache<String, String>(500);

    /**
     * @return true if there is a token that has rep as its representation.
     */
    private boolean contains(TokensList tokens, String rep) {
        for (IterTokenEntry entry : tokens) {
            IToken token = entry.getToken();
            if (token.getRepresentation().equals(rep)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Files only get here if we were unable to parse them.
     */
    private transient Map<File, Long> predefinedFilesNotParsedToTimestamp;

    @Override
    public AbstractModule getBuiltinModule(String name, boolean dontSearchInit,
            IModuleRequestState moduleRequest) {
        AbstractModule n = null;

        //check for supported builtins these don't have files associated.
        //they are the first to be passed because the user can force a module to be builtin, because there
        //is some information that is only useful when you have builtins, such as os and wxPython (those can
        //be source modules, but they have so much runtime info that it is almost impossible to get useful information
        //from statically analyzing them).
        String[] builtins = getBuiltins();
        if (builtins == null || this.info == null) {
            //still on startup
            return null;
        }

        //for temporary access (so that we don't generate many instances of it)
        ModulesKey keyForCacheAccess = new ModulesKey(null, null);

        //A different choice for users that want more complete information on the libraries they're dealing
        //with is using predefined modules.
        File predefinedModule = this.info.getPredefinedModuleFile(name, moduleRequest);
        boolean found = predefinedModule != null && predefinedModule.exists();
        if (!found && !name.endsWith(".__init__")) {
            final String nameWithInit = new FastStringBuffer(name, 10).append(".__init__").toString();
            predefinedModule = this.info.getPredefinedModuleFile(nameWithInit, moduleRequest);
            found = predefinedModule != null && predefinedModule.exists();
            if (found) {
                name = nameWithInit;
            }
        }
        final String finalName = name;
        if (found) {
            keyForCacheAccess.name = finalName;
            keyForCacheAccess.file = predefinedModule;
            n = cachePredefined.getObj(keyForCacheAccess, this);
            if ((n instanceof PredefinedSourceModule)) {
                PredefinedSourceModule predefinedSourceModule = (PredefinedSourceModule) n;
                if (predefinedSourceModule.isSynched()) {
                    return n;
                }
                //otherwise (not PredefinedSourceModule or not synched), just keep going to create
                //it as a predefined source module
            }

            boolean tryToParse = true;
            Long lastModified = null;
            if (predefinedFilesNotParsedToTimestamp == null) {
                predefinedFilesNotParsedToTimestamp = new HashMap<File, Long>();
            } else {
                Long lastTimeChanged = predefinedFilesNotParsedToTimestamp.get(predefinedModule);
                if (lastTimeChanged != null) {
                    lastModified = FileUtils.lastModified(predefinedModule);
                    if (lastTimeChanged.equals(lastModified)) {
                        tryToParse = false;
                    } else {
                        predefinedFilesNotParsedToTimestamp.remove(predefinedModule);
                    }
                }
            }

            if (tryToParse) {
                IDocument doc;
                try {
                    doc = FileUtilsFileBuffer.getDocFromFile(predefinedModule);
                    IGrammarVersionProvider provider = new IGrammarVersionProvider() {

                        @Override
                        public int getGrammarVersion() throws MisconfigurationException {
                            return IGrammarVersionProvider.LATEST_GRAMMAR_PY3_VERSION; // Always Python 3.0 here
                        }

                        @Override
                        public AdditionalGrammarVersionsToCheck getAdditionalGrammarVersions()
                                throws MisconfigurationException {
                            return null;
                        }
                    };
                    ParseOutput obj = PyParser.reparseDocument(new PyParser.ParserInfo(doc, provider,
                            finalName, predefinedModule));
                    if (obj.error != null) {
                        if (lastModified == null) {
                            lastModified = FileUtils.lastModified(predefinedModule);
                        }
                        predefinedFilesNotParsedToTimestamp.put(predefinedModule, lastModified);
                        Log.log("Unable to parse: " + predefinedModule, obj.error);

                    } else if (obj.ast != null) {
                        SimpleNode ast = (SimpleNode) obj.ast;
                        if ("builtins".equals(finalName)) {
                            TypeshedLoader.fixBuiltinsAST(ast, this, info);
                        } else if ("typing".equals(finalName)) {
                            TypeshedLoader.fixTypingAST(ast, this, info);
                        } else {
                            TypeshedLoader.fixAST(ast, this, info);

                        }
                        n = new PredefinedSourceModule(finalName, predefinedModule, ast, obj.error);
                        cachePredefined.add(keyForCacheAccess, n, this);
                        // Note: use a separate cache (because we don't want to mess the regular modules
                        // and in general we just want to find the predefined modules through this API
                        // -- in particular, we don't want typeshed entries if not accepting typeshed entries
                        // when looking for a definition).
                        // doAddSingleModule(keyForCacheAccess, n);
                        return n;
                    }
                    //keep on going
                } catch (Throwable e) {
                    Log.log(e);
                }
            }
        }

        Set<String> builtinsAsSet = getBuiltinsAsSet();
        Iterator<String> it = new FullRepIterable(finalName, true).iterator();
        if (it.hasNext()) {
            // First match (exact)
            String check = it.next();
            if (builtinsAsSet.contains(check)) {
                keyForCacheAccess.name = check;
                n = cache.getObj(keyForCacheAccess, this);

                if (n == null && dontSearchInit == false) {
                    keyForCacheAccess.name = new FastStringBuffer(check, 10).append(".__init__").toString();
                    n = cache.getObj(keyForCacheAccess, this);
                }

                if (n == null || n instanceof EmptyModule || n instanceof SourceModule) {
                    //still not created or not defined as compiled module (as it should be)
                    n = new CompiledModule(check, this, this.getNature());
                    doAddSingleModule(new ModulesKey(n.getName(), null), n);
                    return n;
                }
            }
            if (n instanceof CompiledModule) {
                return n;
            }
        }

        if (builtinsNotConsidered.getObj(finalName) != null) {
            return null;
        }

        while (it.hasNext()) {
            String check = it.next();
            if (builtinsAsSet.contains(check)) {
                // i.e.: Found starting with builtin.

                //ok, just add it if it is some module that actually exists
                n = new CompiledModule(finalName, this, this.getNature());
                TokensList globalTokens = n.getGlobalTokens();
                //if it does not contain the __file__, this means that it's not actually a module
                //(but may be a token from a compiled module, so, clients wanting it must get the module
                //first and only then go on to this token).
                //done: a cache with those tokens should be kept, so that we don't actually have to create
                //the module to see its return values (because that's slow)
                if (globalTokens.size() > 0 && contains(globalTokens, "__file__")) {
                    doAddSingleModule(new ModulesKey(finalName, null), n);
                    return n;
                } else {
                    builtinsNotConsidered.add(finalName, finalName);
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * In the system modules manager, we also have to check for the builtins
     */
    @Override
    public IModule getModule(String name, IPythonNature nature, boolean dontSearchInit,
            IModuleRequestState moduleRequest) {
        AbstractModule n = getBuiltinModule(name, dontSearchInit, moduleRequest);
        if (n != null) {
            return n;
        }

        return super.getModule(name, nature, dontSearchInit, moduleRequest);
    }

    @Override
    public IModule getModuleWithoutBuiltins(String name, IPythonNature nature, boolean dontSearchInit,
            IModuleRequestState moduleRequest) {
        return super.getModule(name, nature, dontSearchInit, moduleRequest);
    }

    @Override
    public Tuple<IModule, IModulesManager> getModuleAndRelatedModulesManager(String name, IPythonNature nature,
            boolean checkSystemManager, boolean dontSearchInit, IModuleRequestState moduleRequest) {
        IModule module = this.getModule(name, nature, checkSystemManager, dontSearchInit, moduleRequest);
        if (module != null) {
            return new Tuple<IModule, IModulesManager>(module, this);
        }
        return null;
    }

    @Override
    public void load() throws IOException {
        final File workspaceMetadataFile = getIoDirectory();
        ModulesManager.loadFromFile(this, workspaceMetadataFile);

        DeltaSaver<ModulesKey> d = this.deltaSaver = new DeltaSaver<ModulesKey>(this.getIoDirectory(),
                "v1_sys_astdelta", readFromFileMethod,
                toFileMethod);
        d.processDeltas(this); //process the current deltas (clears current deltas automatically and saves it when the processing is concluded)
    }

    @Override
    public void save() {
        final File workspaceMetadataFile = getIoDirectory();
        DeltaSaver<ModulesKey> d = deltaSaver;
        if (d != null) {
            d.clearAll(); //When save is called, the deltas don't need to be used anymore.
        }
        this.saveToFile(workspaceMetadataFile);

    }

    @Override
    public File getIoDirectory() {
        return info.getIoDirectory();
    }

    /**
     * @param keysFound
     */
    public void updateKeysAndSave(PyPublicTreeMap<ModulesKey, ModulesKey> keysFound) {
        synchronized (modulesKeysLock) {
            modulesKeys.clear();
            modulesKeys.putAll(keysFound);
        }
        this.save();
    }

    @Override
    public void changePythonPath(String pythonpath, IProject project, IProgressMonitor monitor) {
        try {
            //Clear the cached files related to compiled modules.
            File ioDirectory = getIoDirectory();
            if (ioDirectory != null) {
                File d = new File(ioDirectory, DIR_NAME_FOR_COMPILED_CACHE);
                if (d.exists()) {
                    File[] files = d.listFiles();
                    if (files != null) {

                        for (int i = 0; i < files.length; ++i) {
                            File f = files[i];

                            if (f.isFile()) {
                                try {
                                    FileUtils.deleteFile(f);
                                } catch (IOException e) {
                                    Log.log(e);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }
        super.changePythonPath(pythonpath, project, monitor);
    }

    /**
     * Gets the directory where compiled modules should be saved.
     */
    @Override
    public File getCompiledModuleCacheFile(String name) {
        File ioDirectory = getIoDirectory();
        if (ioDirectory != null) {
            File d = new File(ioDirectory, DIR_NAME_FOR_COMPILED_CACHE);
            if (!d.exists()) {
                d.mkdirs();
            }
            int len = name.length();
            String pre = "";
            if (len >= 3) {
                pre = name.substring(0, 3);

            } else if (len >= 2) {
                pre = name.substring(0, 2);

            } else if (len >= 1) {
                pre = name.substring(0, 1);

            }

            //Already separate dotted from non dotted (i.e.: top level) modules.
            String post = name.contains(".") ? ".top" : ".inn";
            return new File(d, StringUtils.join("", pre, "_", StringUtils.md5(name), post));
        }
        return null;
    }

}
