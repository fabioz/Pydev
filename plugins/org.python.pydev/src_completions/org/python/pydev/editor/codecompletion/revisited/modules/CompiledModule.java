/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Nov 18, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.ObjectsInternPool;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.concurrency.IRunnableWithMonitor;
import org.python.pydev.core.concurrency.RunnableAsJobsPoolThread;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.shared_core.cache.LRUCache;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * @author Fabio Zadrozny
 */
public class CompiledModule extends AbstractModule {

    public static boolean COMPILED_MODULES_ENABLED = true;

    public static final boolean TRACE_COMPILED_MODULES = false;

    private Map<String, Map<String, IToken>> cache = new HashMap<String, Map<String, IToken>>();

    private static final Definition[] EMPTY_DEFINITION = new Definition[0];

    private static final Map<String, String> BUILTIN_REPLACEMENTS = new HashMap<String, String>();
    static {
        BUILTIN_REPLACEMENTS.put("open", "file");
        BUILTIN_REPLACEMENTS.put("dir", "list");
        BUILTIN_REPLACEMENTS.put("filter", "list");
        BUILTIN_REPLACEMENTS.put("raw_input", "str");
        BUILTIN_REPLACEMENTS.put("input", "str");
        BUILTIN_REPLACEMENTS.put("locals", "dict");
        BUILTIN_REPLACEMENTS.put("map", "list");
        BUILTIN_REPLACEMENTS.put("range", "list");
        BUILTIN_REPLACEMENTS.put("repr", "str");
        BUILTIN_REPLACEMENTS.put("reversed", "list");
        BUILTIN_REPLACEMENTS.put("sorted", "list");
        BUILTIN_REPLACEMENTS.put("zip", "list");

        BUILTIN_REPLACEMENTS.put("str.capitalize", "str");
        BUILTIN_REPLACEMENTS.put("str.center", "str");
        BUILTIN_REPLACEMENTS.put("str.decode", "str");
        BUILTIN_REPLACEMENTS.put("str.encode", "str");
        BUILTIN_REPLACEMENTS.put("str.expandtabs", "str");
        BUILTIN_REPLACEMENTS.put("str.format", "str");
        BUILTIN_REPLACEMENTS.put("str.join", "str");
        BUILTIN_REPLACEMENTS.put("str.ljust", "str");
        BUILTIN_REPLACEMENTS.put("str.lower", "str");
        BUILTIN_REPLACEMENTS.put("str.lstrip", "str");
        BUILTIN_REPLACEMENTS.put("str.partition", "tuple");
        BUILTIN_REPLACEMENTS.put("str.replace", "str");
        BUILTIN_REPLACEMENTS.put("str.rjust", "str");
        BUILTIN_REPLACEMENTS.put("str.rpartition", "tuple");
        BUILTIN_REPLACEMENTS.put("str.rsplit", "list");
        BUILTIN_REPLACEMENTS.put("str.rstrip", "str");
        BUILTIN_REPLACEMENTS.put("str.split", "list");
        BUILTIN_REPLACEMENTS.put("str.splitlines", "list");
        BUILTIN_REPLACEMENTS.put("str.strip", "str");
        BUILTIN_REPLACEMENTS.put("str.swapcase", "str");
        BUILTIN_REPLACEMENTS.put("str.title", "str");
        BUILTIN_REPLACEMENTS.put("str.translate", "str");
        BUILTIN_REPLACEMENTS.put("str.upper", "str");
        BUILTIN_REPLACEMENTS.put("str.zfill", "str");
    }

    /**
     * These are the tokens the compiled module has.
     */
    private Map<String, IToken> tokens = null;

    /**
     * A map with the definitions that have already been found for this compiled module.
     */
    private LRUCache<String, Definition[]> definitionsFoundCache = new LRUCache<String, Definition[]>(30);

    private File file;

    private final boolean isPythonBuiltin;

    @Override
    public File getFile() {
        return file;
    }

    public boolean hasFutureImportAbsoluteImportDeclared() {
        return false;
    }

    /**
     * 
     * @param module - module from where to get completions.
     */
    @SuppressWarnings("unchecked")
    public CompiledModule(String name, IModulesManager manager) {
        super(name);

        isPythonBuiltin = ("__builtin__".equals(name) || "builtins".equals(name));

        Tuple<File, IToken[]> info = getCached(name, manager);
        if (info != null) {
            this.file = info.o1;
            this.tokens = asMap(info.o2);
            return;
        }

        Object lock = manager.getCompiledModuleCreationLock(name);
        synchronized (lock) {
            //Try to get from the cache again (in case someone has gotten the info in the meantime).
            info = getCached(name, manager);
            if (info != null) {
                this.file = info.o1;
                this.tokens = asMap(info.o2);
                return;
            }
            if (COMPILED_MODULES_ENABLED) {

                try {
                    info = createTokensFromServer(name, manager);
                    this.file = info.o1;
                    this.tokens = asMap(info.o2);

                    if (info != null) {
                        updateCache(name, manager, info);
                    }
                } catch (Exception e) {
                    tokens = new HashMap<String, IToken>();
                    Log.log(e);
                }
            } else {
                //not used if not enabled.
                tokens = new HashMap<String, IToken>();
            }
        }

        //Notify out of the lock (if it didn't get from the cache).
        List<IModulesObserver> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_MODULES_OBSERVER);
        if (participants != null) {
            for (IModulesObserver observer : participants) {
                observer.notifyCompiledModuleCreated(this, manager);
            }
        }
    }

    /**
     * @return the file to be used to write/read the cache.
     */
    private static File getCacheFile(String name, IModulesManager manager) {
        if (manager instanceof ISystemModulesManager) {
            ISystemModulesManager systemModulesManager = (ISystemModulesManager) manager;
            return systemModulesManager.getCompiledModuleCacheFile(name);
        }
        return null;
    }

    /**
     * Updates the file with the cache to have the given information.
     */
    private static void updateCache(final String name, IModulesManager manager, final Tuple<File, IToken[]> info) {
        try {
            if (info != null && info.o2 != null && info.o2.length > 10) { //Don't cache anything less than 10 tokens.
                File f = getCacheFile(name, manager);

                //Only cache modules that are in the system modules manager.
                if (f == null && !(manager instanceof ISystemModulesManager)) {
                    ISystemModulesManager systemModulesManager = manager.getSystemModulesManager();
                    manager = null; //i.e.: just making sure it won't be used later on...

                    //Only cache it if we discover it as being a part of the modules manager (i.e.: if it's a part of
                    //a project we don't cache it for now).
                    for (String part : new FullRepIterable(name)) {
                        if (systemModulesManager.hasModule(new ModulesKey(part, null))) {
                            f = getCacheFile(name, systemModulesManager);
                            break;
                        }
                        if (!part.contains(".")) {
                            part += ".__init__";
                            if (systemModulesManager.hasModule(new ModulesKey(part, null))) {
                                f = getCacheFile(name, systemModulesManager);
                                break;
                            }
                        }
                    }
                }

                if (f != null) {
                    final File cacheFile = f;
                    IRunnableWithMonitor runnable = new IRunnableWithMonitor() {

                        @Override
                        public void run() {
                            try (OutputStream out = new FileOutputStream(cacheFile)) {
                                try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                                    try (BufferedOutputStream buf = new BufferedOutputStream(gzip)) {
                                        try (ObjectOutputStream stream = new ObjectOutputStream(buf)) {
                                            stream.writeObject(name);
                                            stream.writeObject(info.o1);

                                            IToken[] toks = info.o2;
                                            int size = toks.length;
                                            stream.writeInt(size);

                                            //Write in 2 batches (leave the docstring in a separate batch as it's usually
                                            //the big part of the info -- that way we can partially read it without reading
                                            //the docstrings later on).
                                            for (int i = 0; i < size; i++) {
                                                IToken tok = toks[i];
                                                stream.writeObject(tok.getRepresentation());
                                                stream.writeInt(tok.getType());
                                                stream.writeObject(tok.getArgs());
                                                stream.writeObject(tok.getParentPackage());
                                            }
                                            for (int i = 0; i < size; i++) {
                                                stream.writeObject(toks[i].getDocStr());
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.log(e);
                            }
                        }

                        @Override
                        public void setMonitor(IProgressMonitor monitor) {
                        }
                    };
                    RunnableAsJobsPoolThread.getSingleton().scheduleToRun(runnable, "Cache module: " + name);
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

    /**
     * Gets cached information for the given name. Could be a dotted or non-dotted name.
     */
    private static Tuple<File, IToken[]> getCached(String name, IModulesManager manager) {
        File f = getCacheFile(name, manager.getSystemModulesManager());

        if (f != null && f.exists()) {
            try {
                IToken[] toks = null;
                File file = null;
                try (FileInputStream fin = new FileInputStream(f)) {
                    try (InputStream in = new BufferedInputStream(new GZIPInputStream(fin))) {
                        try (ObjectInputStream stream = new ObjectInputStream(in)) {
                            ObjectsInternPool.ObjectsPoolMap map = new ObjectsInternPool.ObjectsPoolMap();
                            @SuppressWarnings("unused")
                            Object _name = stream.readObject(); //we already have the name set (so, it's only there for completeness). 
                            file = (File) stream.readObject();
                            int size = stream.readInt();

                            toks = new IToken[size];
                            for (int i = 0; i < size; i++) {
                                //Note intern (we probably have many empty strings -- or the same for parentPackage)
                                String rep = ObjectsInternPool.internLocal(map, (String) stream.readObject());
                                int type = stream.readInt();
                                String args = ObjectsInternPool.internLocal(map, (String) stream.readObject());
                                String parentPackage = ObjectsInternPool.internLocal(map, (String) stream.readObject());
                                toks[i] = new CompiledToken(rep, "", args, parentPackage, type);
                            }
                            for (int i = 0; i < size; i++) {
                                toks[i].setDocStr(ObjectsInternPool.internLocal(map, (String) stream.readObject()));
                            }
                        }
                    }
                }
                return new Tuple<File, IToken[]>(file, toks);
            } catch (Exception e) {
                Log.log("Unable to read contents from: " + f, e); //Unable to read: just log it
            }
        }
        return null;
    }

    private static IToken[] createInnerFromServer(ICodeCompletionASTManager manager, final IPythonNature nature,
            String act,
            String tokenToCompletion) throws Exception, MisconfigurationException, PythonNatureWithoutProjectException {
        IToken[] toks;
        AbstractShell shell = AbstractShell.getServerShell(nature, AbstractShell.getShellId());
        List<String[]> completions = shell.getImportCompletions(tokenToCompletion,
                getCompletePythonpath(manager.getModulesManager(), nature)).o2;

        List<IToken> lst = new ArrayList<IToken>();

        for (Iterator<String[]> iter = completions.iterator(); iter.hasNext();) {
            String[] element = iter.next();
            if (element.length >= 4) {//it might be a server error
                IToken t = new CompiledToken(element[0], element[1], element[2], act,
                        Integer.parseInt(element[3]));
                lst.add(t);
            }
        }
        toks = lst.toArray(new CompiledToken[0]);
        return toks;
    }

    private static List<String> getCompletePythonpath(IModulesManager manager, final IPythonNature nature)
            throws MisconfigurationException, PythonNatureWithoutProjectException {
        return manager.getCompletePythonPath(nature.getProjectInterpreter(), nature.getRelatedInterpreterManager());
    }

    private static Tuple<File, IToken[]> createTokensFromServer(String name, IModulesManager manager)
            throws IOException,
            Exception,
            CoreException {
        if (TRACE_COMPILED_MODULES) {
            Log.log(IStatus.INFO, ("Compiled modules: getting info for:" + name), null);
        }
        final IPythonNature nature = manager.getNature();
        AbstractShell shell = AbstractShell.getServerShell(nature, AbstractShell.getShellId());
        Tuple<String, List<String[]>> completions = shell.getImportCompletions(name,
                getCompletePythonpath(manager, nature)); //default

        if (TRACE_COMPILED_MODULES) {
            Log.log(IStatus.INFO, ("Compiled modules: " + name + " file: " + completions.o1 + " found: "
                    + completions.o2.size() + " completions."), null);
        }
        File file = null;
        String fPath = completions.o1;
        if (fPath != null) {
            if (!fPath.equals("None")) {
                file = new File(fPath);
            }

            String f = fPath;
            if (f.toLowerCase().endsWith(".pyc")) {
                f = f.substring(0, f.length() - 1); //remove the c from pyc
                File f2 = new File(f);
                if (f2.exists()) {
                    file = f2;
                }
            }
        }
        ArrayList<IToken> array = new ArrayList<IToken>();

        for (String[] element : completions.o2) {
            //let's make this less error-prone.
            try {
                String o1 = element[0]; //this one is really, really needed
                String o2 = "";
                String o3 = "";

                if (element.length > 0) {
                    o2 = element[1];
                }

                if (element.length > 0) {
                    o3 = element[2];
                }

                IToken t;
                if (element.length > 0) {
                    t = new CompiledToken(o1, o2, o3, name, Integer.parseInt(element[3]));
                } else {
                    t = new CompiledToken(o1, o2, o3, name, IToken.TYPE_BUILTIN);
                }

                array.add(t);
            } catch (Exception e) {
                String received = "";
                for (int i = 0; i < element.length; i++) {
                    received += element[i];
                    received += "  ";
                }

                Log.log(IStatus.ERROR, ("Error getting completions for compiled module " + name + " received = '"
                        + received + "'"), e);
            }
        }

        //as we will use it for code completion on sources that map to modules, the __file__ should also
        //be added...
        if (array.size() > 0 && (name.equals("__builtin__") || name.equals("builtins"))) {
            array.add(new CompiledToken("__file__", "", "", name, IToken.TYPE_BUILTIN));
            array.add(new CompiledToken("__name__", "", "", name, IToken.TYPE_BUILTIN));
            array.add(new CompiledToken("__builtins__", "", "", name, IToken.TYPE_BUILTIN));
            array.add(new CompiledToken("__dict__", "", "", name, IToken.TYPE_BUILTIN));
        }

        return new Tuple<File, IToken[]>(file, array.toArray(new IToken[array.size()]));
    }

    /**
     * Adds tokens to the internal HashMap
     * 
     * @param array The array of tokens to be added (maps representation -> token), so, existing tokens with the
     * same representation will be replaced.
     * @return 
     */
    private static Map<String, IToken> asMap(IToken[] array) {
        Map<String, IToken> tokens = new HashMap<String, IToken>();
        if (array != null) {
            for (IToken token : array) {
                tokens.put(token.getRepresentation(), token);
            }
        }
        return tokens;
    }

    /**
     * Compiled modules do not have imports to be seen
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getWildImportedModules()
     */
    @Override
    public IToken[] getWildImportedModules() {
        return new IToken[0];
    }

    /**
     * Compiled modules do not have imports to be seen
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getTokenImportedModules()
     */
    @Override
    public IToken[] getTokenImportedModules() {
        return new IToken[0];
    }

    /**
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getGlobalTokens()
     */
    @Override
    public IToken[] getGlobalTokens() {
        if (tokens == null) {
            return new IToken[0];
        }

        Collection<IToken> values = tokens.values();
        return values.toArray(new IToken[values.size()]);
    }

    /**
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getDocString()
     */
    @Override
    public String getDocString() {
        return "compiled extension";
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#getGlobalTokens(java.lang.String)
     */
    @Override
    public IToken[] getGlobalTokens(ICompletionState state, ICodeCompletionASTManager manager) {
        String activationToken = state.getActivationToken();
        if (activationToken.length() == 0) {
            return getGlobalTokens();
        }

        Map<String, IToken> v = cache.get(activationToken);
        if (v != null) {
            Collection<IToken> values = v.values();
            return values.toArray(new IToken[values.size()]);
        }

        IToken[] toks = new IToken[0];

        if (COMPILED_MODULES_ENABLED) {
            try {
                final IPythonNature nature = manager.getNature();

                String act = name + '.' + activationToken;
                String tokenToCompletion = act;
                if (isPythonBuiltin) {
                    String replacement = BUILTIN_REPLACEMENTS.get(activationToken);
                    if (replacement != null) {
                        tokenToCompletion = name + '.' + replacement;
                    }
                }

                Tuple<File, IToken[]> cached = getCached(tokenToCompletion, manager.getModulesManager());
                if (cached != null) {
                    HashMap<String, IToken> map = new HashMap<String, IToken>();
                    for (IToken token : cached.o2) {
                        map.put(token.getRepresentation(), token);
                    }
                    cache.put(activationToken, map);
                    return cached.o2;
                }

                toks = createInnerFromServer(manager, nature, act, tokenToCompletion);

                //Put it in the cache for the next time.
                updateCache(tokenToCompletion, manager.getModulesManager(), new Tuple<File, IToken[]>(null, toks));
                Map<String, IToken> map = asMap(toks);
                cache.put(activationToken, map);
            } catch (Exception e) {
                Log.log("Error while getting info for module:" + this.name + ". Project: "
                        + manager.getNature().getProject(), e);
            }
        }
        return toks;
    }

    @Override
    public boolean isInDirectGlobalTokens(String tok, ICompletionCache completionCache) {
        if (this.tokens != null) {
            return this.tokens.containsKey(tok);
        }
        return false;
    }

    @Override
    public boolean isInGlobalTokens(String tok, IPythonNature nature, ICompletionCache completionCache) {
        //we have to override because there is no way to check if it is in some import from some other place if it has dots on the tok...

        if (tok.indexOf('.') == -1) {
            return isInDirectGlobalTokens(tok, completionCache);
        } else {
            ICompletionState state = CompletionStateFactory.getEmptyCompletionState(nature, completionCache);
            String[] headAndTail = FullRepIterable.headAndTail(tok);
            state.setActivationToken(headAndTail[0]);
            String head = headAndTail[1];
            IToken[] globalTokens = getGlobalTokens(state, nature.getAstManager());
            for (IToken token : globalTokens) {
                if (token.getRepresentation().equals(head)) {
                    return true;
                }
            }
        }
        return false;

    }

    /**
     * @param findInfo 
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#findDefinition(java.lang.String, int, int)
     */
    @Override
    public Definition[] findDefinition(ICompletionState state, int line, int col, IPythonNature nature)
            throws Exception {
        String token = state.getActivationToken();

        if (TRACE_COMPILED_MODULES) {
            System.out.println("CompiledModule.findDefinition:" + token);
        }
        Definition[] found = this.definitionsFoundCache.getObj(token);
        if (found != null) {
            if (TRACE_COMPILED_MODULES) {
                System.out.println("CompiledModule.findDefinition: found in cache.");
            }
            return found;
        }

        AbstractShell shell = AbstractShell.getServerShell(nature, AbstractShell.getShellId());
        Tuple<String[], int[]> def = shell.getLineCol(this.name, token, nature.getAstManager().getModulesManager()
                .getCompletePythonPath(nature.getProjectInterpreter(), nature.getRelatedInterpreterManager())); //default
        if (def == null) {
            if (TRACE_COMPILED_MODULES) {
                System.out.println("CompiledModule.findDefinition:" + token + " = empty");
            }
            this.definitionsFoundCache.add(token, EMPTY_DEFINITION);
            return EMPTY_DEFINITION;
        }
        String fPath = def.o1[0];
        if (fPath.equals("None")) {
            if (TRACE_COMPILED_MODULES) {
                System.out.println("CompiledModule.findDefinition:" + token + " = None");
            }
            Definition[] definition = new Definition[] { new Definition(def.o2[0], def.o2[1], token, null, null,
                    this) };
            this.definitionsFoundCache.add(token, definition);
            return definition;
        }
        File f = new File(fPath);
        String foundModName = nature.resolveModule(f);
        String foundAs = def.o1[1];

        IModule mod;
        if (foundModName == null) {
            //this can happen in a case where we have a definition that's found from a compiled file which actually
            //maps to a file that's outside of the pythonpath known by Pydev.
            String n = FullRepIterable.getFirstPart(f.getName());
            mod = AbstractModule.createModule(n, f, nature, true);
        } else {
            mod = nature.getAstManager().getModule(foundModName, nature, true);
        }

        if (TRACE_COMPILED_MODULES) {
            System.out.println("CompiledModule.findDefinition: found at:" + mod.getName());
        }
        int foundLine = def.o2[0];
        if (foundLine == 0 && foundAs != null && foundAs.length() > 0 && mod != null
                && state.canStillCheckFindSourceFromCompiled(mod, foundAs)) {
            //TODO: The nature (and so the grammar to be used) must be defined by the file we'll parse
            //(so, we need to know the system modules manager that actually created it to know the actual nature)
            IModule sourceMod = AbstractModule.createModuleFromDoc(mod.getName(), f,
                    new Document(FileUtils.getPyFileContents(f)), nature, true);
            if (sourceMod instanceof SourceModule) {
                Definition[] definitions = (Definition[]) sourceMod.findDefinition(
                        state.getCopyWithActTok(foundAs), -1, -1, nature);
                if (definitions.length > 0) {
                    this.definitionsFoundCache.add(token, definitions);
                    return definitions;
                }
            }
        }
        if (mod == null) {
            mod = this;
        }
        int foundCol = def.o2[1];
        if (foundCol < 0) {
            foundCol = 0;
        }
        if (TRACE_COMPILED_MODULES) {
            System.out.println("CompiledModule.findDefinition: found compiled at:" + mod.getName());
        }
        Definition[] definitions = new Definition[] { new Definition(foundLine + 1, foundCol + 1, token, null,
                null, mod) };
        this.definitionsFoundCache.add(token, definitions);
        return definitions;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CompiledModule)) {
            return false;
        }
        CompiledModule m = (CompiledModule) obj;

        if (name == null || m.name == null) {
            if (name != m.name) {
                return false;
            }
            //both null at this point
        } else if (!name.equals(m.name)) {
            return false;
        }

        if (file == null || m.file == null) {
            if (file != m.file) {
                return false;
            }
            //both null at this point
        } else if (!file.equals(m.file)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 33;
        if (file != null) {
            hash += file.hashCode();
        }
        if (name != null) {
            hash += name.hashCode();
        }
        return hash;
    }

}
