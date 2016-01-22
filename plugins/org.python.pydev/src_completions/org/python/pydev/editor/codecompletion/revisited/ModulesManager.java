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
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.ModulesKeyForZip;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.ModulesFoundStructure.ZipContents;
import org.python.pydev.editor.codecompletion.revisited.PyPublicTreeMap.Entry;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.JythonModulesManagerUtils;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.modules.EmptyModule;
import org.python.pydev.editor.codecompletion.revisited.modules.EmptyModuleForZip;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.cache.LRUMap;
import org.python.pydev.shared_core.callbacks.ICallbackListener;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.out_of_memory.OnExpectedOutOfMemory;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;

/**
 * This class manages the modules that are available
 *
 * @author Fabio Zadrozny
 */
public abstract class ModulesManager implements IModulesManager {

    /**
     * Note: MODULES_MANAGER_V1 had a bug when writing/reading ModulesKeyForZip entries.
     */
    private static final String MODULES_MANAGER_V2 = "MODULES_MANAGER_V2\n";

    private final static boolean DEBUG_BUILD = false;

    private final static boolean DEBUG_TEMPORARY_MODULES = false;

    private final static boolean DEBUG_ZIP = false;

    static {
        OnExpectedOutOfMemory.clearCacheOnOutOfMemory.registerListener(new ICallbackListener<Object>() {

            @Override
            public Object call(Object obj) {
                clearCache();
                return null;
            }
        });
    }

    public ModulesManager() {
    }

    private final static double ONE_MINUTE_IN_MILLIS = 60.0 * 1000.0;

    /**
     * This class is a cache to help in getting the managers that are referenced or referred.
     *
     * It will not actually make any computations (the managers must be set from the outside)
     */
    protected static class CompletionCache {
        public IModulesManager[] referencedManagers;

        public IModulesManager[] referredManagers;

        private long creationTime;
        private int calls = 0;

        public IModulesManager[] getManagers(boolean referenced) {
            calls += 1;
            if (calls % 30 == 0) {
                long diff = System.currentTimeMillis() - creationTime;
                if (diff > ONE_MINUTE_IN_MILLIS) {
                    String msg = String.format(
                            "Warning: the cache related to project dependencies is the same for %.2f minutes.",
                            (diff / ONE_MINUTE_IN_MILLIS));
                    Log.logInfo(msg);
                }
            }
            if (referenced) {
                return this.referencedManagers;
            } else {
                return this.referredManagers;
            }
        }

        public void setManagers(IModulesManager[] ret, boolean referenced) {
            if (this.creationTime == 0) {
                this.creationTime = System.currentTimeMillis();
            }
            if (referenced) {
                this.referencedManagers = ret;
            } else {
                this.referredManagers = ret;
            }
        }
    }

    /**
     * A stack for keeping the completion cache
     */
    protected volatile CompletionCache completionCache = null;
    protected final Object lockCompletionCache = new Object();

    private volatile int completionCacheI = 0;

    /**
     * This method starts a new cache for this manager, so that needed info is kept while the request is happening
     * (so, some info may not need to be re-asked over and over for requests)
     */
    public boolean startCompletionCache() {
        synchronized (lockCompletionCache) {
            if (completionCache == null) {
                completionCache = new CompletionCache();
            }
            completionCacheI += 1;
        }
        return true;
    }

    public void endCompletionCache() {
        synchronized (lockCompletionCache) {
            completionCacheI -= 1;
            if (completionCacheI == 0) {
                completionCache = null;
            } else if (completionCacheI < 0) {
                throw new RuntimeException("Completion cache negative (request unsynched)");
            }
        }
    }

    /**
     * Modules that we have in memory. This is persisted when saved.
     *
     * Keys are ModulesKey with the name of the module. Values are AbstractModule objects.
     *
     * Implementation changed to contain a cache, so that it does not grow to much (some out of memo errors
     * were thrown because of the may size when having too many modules).
     *
     * It is sorted so that we can get things in a 'subtree' faster
     */
    protected final PyPublicTreeMap<ModulesKey, ModulesKey> modulesKeys = new PyPublicTreeMap<ModulesKey, ModulesKey>();
    protected final Object modulesKeysLock = new Object();

    protected static final ModulesManagerCache cache = new ModulesManagerCache();

    /**
     * Helper for using the pythonpath. Also persisted.
     */
    protected final PythonPathHelper pythonPathHelper = new PythonPathHelper();

    public PythonPathHelper getPythonPathHelper() {
        return pythonPathHelper;
    }

    public void saveToFile(File workspaceMetadataFile) {
        if (workspaceMetadataFile.exists() && !workspaceMetadataFile.isDirectory()) {
            try {
                FileUtils.deleteFile(workspaceMetadataFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (!workspaceMetadataFile.exists()) {
            workspaceMetadataFile.mkdirs();
        }

        File modulesKeysFile = new File(workspaceMetadataFile, "modulesKeys");
        File pythonpatHelperFile = new File(workspaceMetadataFile, "pythonpath");
        FastStringBuffer buf;
        HashMap<String, Integer> commonTokens = new HashMap<String, Integer>();

        synchronized (modulesKeysLock) {
            buf = new FastStringBuffer(this.modulesKeys.size() * 50);
            buf.append(MODULES_MANAGER_V2);

            for (Iterator<ModulesKey> iter = this.modulesKeys.keySet().iterator(); iter.hasNext();) {
                ModulesKey next = iter.next();
                buf.append(next.name);
                if (next.file != null) {
                    buf.append("|");
                    if (next instanceof ModulesKeyForZip) {
                        ModulesKeyForZip modulesKeyForZip = (ModulesKeyForZip) next;
                        if (modulesKeyForZip.zipModulePath != null) {
                            String fileStr = next.file.toString();
                            Integer t = commonTokens.get(fileStr);
                            if (t == null) {
                                t = commonTokens.size();
                                commonTokens.put(fileStr, t);
                            }
                            buf.append(t);
                            buf.append("|");

                            buf.append(modulesKeyForZip.zipModulePath);
                            buf.append("|");
                            buf.append(modulesKeyForZip.isFile ? '1' : '0');
                        }
                    } else {
                        buf.append(next.file.toString());
                    }
                }
                buf.append('\n');
            }
        }
        if (commonTokens.size() > 0) {
            FastStringBuffer header = new FastStringBuffer(buf.length() + (commonTokens.size() * 50));
            header.append(MODULES_MANAGER_V2);
            header.append("--COMMON--\n");
            for (Map.Entry<String, Integer> entries : commonTokens.entrySet()) {
                header.append(entries.getValue());
                header.append('=');
                header.append(entries.getKey());
                header.append('\n');
            }
            header.append("--END-COMMON--\n");
            header.append(buf);
            buf = header;
        }
        FileUtils.writeStrToFile(buf.toString(), modulesKeysFile);

        this.pythonPathHelper.saveToFile(pythonpatHelperFile);
    }

    /**
     * @param systemModulesManager
     * @param workspaceMetadataFile
     * @throws IOException
     */
    public static void loadFromFile(ModulesManager modulesManager, File workspaceMetadataFile) throws IOException {
        if (workspaceMetadataFile.exists() && !workspaceMetadataFile.isDirectory()) {
            throw new IOException("Expecting: " + workspaceMetadataFile + " to be a directory.");
        }
        File modulesKeysFile = new File(workspaceMetadataFile, "modulesKeys");
        File pythonpatHelperFile = new File(workspaceMetadataFile, "pythonpath");
        if (!modulesKeysFile.isFile()) {
            throw new IOException("Expecting: " + modulesKeysFile + " to exist (and be a file).");
        }
        if (!pythonpatHelperFile.isFile()) {
            throw new IOException("Expecting: " + pythonpatHelperFile + " to exist (and be a file).");
        }

        String fileContents = FileUtils.getFileContents(modulesKeysFile);
        if (!fileContents.startsWith(MODULES_MANAGER_V2)) {
            throw new RuntimeException(
                    "Could not load modules manager from " + modulesKeysFile + " (version changed).");
        }

        HashMap<Integer, String> intToString = new HashMap<Integer, String>();
        fileContents = fileContents.substring(MODULES_MANAGER_V2.length());
        if (fileContents.startsWith("--COMMON--\n")) {
            String header = fileContents.substring("--COMMON--\n".length());
            header = header.substring(0, header.indexOf("--END-COMMON--\n"));
            fileContents = fileContents.substring(fileContents.indexOf("--END-COMMON--\n")
                    + "--END-COMMON--\n".length());

            for (String line : StringUtils.iterLines(header)) {
                line = line.trim();
                List<String> split = StringUtils.split(line, '=');
                if (split.size() == 2) {
                    try {
                        int i = Integer.parseInt(split.get(0));
                        intToString.put(i, split.get(1));
                    } catch (NumberFormatException e) {
                        Log.log(e);
                    }
                }

            }
            if (fileContents.startsWith(MODULES_MANAGER_V2)) {
                fileContents = fileContents.substring(MODULES_MANAGER_V2.length());
            }
        }

        handleFileContents(modulesManager, fileContents, intToString);

        if (modulesManager.pythonPathHelper == null) {
            throw new IOException("Pythonpath helper not properly restored. " + modulesManager.getClass().getName()
                    + " dir:" + workspaceMetadataFile);
        }
        modulesManager.pythonPathHelper.loadFromFile(pythonpatHelperFile);

        if (modulesManager.pythonPathHelper.getPythonpath() == null) {
            throw new IOException("Pythonpath helper pythonpath not properly restored. "
                    + modulesManager.getClass().getName() + " dir:" + workspaceMetadataFile);
        }

        if (modulesManager.pythonPathHelper.getPythonpath().size() == 0) {
            throw new IOException("Pythonpath helper pythonpath restored with no contents. "
                    + modulesManager.getClass().getName() + " dir:" + workspaceMetadataFile);
        }

        if (modulesManager.modulesKeys.size() < 2) { //if we have few modules, that may indicate a problem...
            //if the project is really small, modulesManager will be fast, otherwise, it'll fix the problem.
            //Note: changed to a really low value because we now make a check after it's restored anyways.
            throw new IOException("Only " + modulesManager.modulesKeys.size() + " modules restored in I/O. "
                    + modulesManager.getClass().getName() + " dir:" + workspaceMetadataFile);
        }

    }

    /**
     * This method was simply:
     *
     *  for(String line:StringUtils.iterLines(fileContents)){
     *      line = line.trim();
     *      List<String> split = StringUtils.split(line, '|');
     *      handleLineParts(modulesManager, intToString, split);
     *  }
     *
     *  and was changed to be faster (as this was one of the slow things in startup).
     */
    /*default*/@SuppressWarnings("rawtypes")
    static void handleFileContents(ModulesManager modulesManager, String fileContents,
            HashMap<Integer, String> intToString) {
        String string = fileContents;
        int len = string.length();

        final ArrayList<ModulesKey> lst = new ArrayList<ModulesKey>();

        char c;
        int start = 0;
        int i = 0;

        String[] parts = new String[4];
        int partsFound = 0;

        for (; i < len; i++) {
            c = string.charAt(i);

            if (c == '\r') {
                String trimmed = string.substring(start, i).trim();
                if (trimmed.length() > 0) {
                    parts[partsFound] = trimmed;
                    partsFound++;
                }
                handleLineParts(modulesManager, intToString, parts, partsFound, lst);
                partsFound = 0;

                if (i < len - 1 && string.charAt(i + 1) == '\n') {
                    i++;
                }
                start = i + 1;

            } else if (c == '\n') {
                String trimmed = string.substring(start, i).trim();
                if (trimmed.length() > 0) {
                    parts[partsFound] = trimmed;
                    partsFound++;
                }
                handleLineParts(modulesManager, intToString, parts, partsFound, lst);
                partsFound = 0;

                start = i + 1;

            } else if (c == '|') {
                String trimmed = string.substring(start, i).trim();
                if (trimmed.length() > 0) {
                    parts[partsFound] = trimmed;
                    partsFound++;
                }
                start = i + 1;
            }
        }

        if (start < len && start != i) {
            String trimmed = string.substring(start, i).trim();
            if (trimmed.length() > 0) {
                parts[partsFound] = trimmed;
                partsFound++;
            }
            handleLineParts(modulesManager, intToString, parts, partsFound, lst);
        }

        try {
            final int size = lst.size();
            //As we saved in sorted order, we can build in sorted order too (which is MUCH faster than adding items one
            //by one).
            modulesManager.modulesKeys.buildFromSorted(size, new Iterator() {

                private int i = 0;

                public boolean hasNext() {
                    return i < size;
                }

                public Object next() {
                    final ModulesKey next = lst.get(i);
                    i++;
                    return new Map.Entry() {

                        public Object getKey() {
                            return next;
                        }

                        public Object getValue() {
                            return next;
                        }

                        public Object setValue(Object value) {
                            throw new UnsupportedOperationException();
                        }
                    };
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            }, null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void handleLineParts(ModulesManager modulesManager, HashMap<Integer, String> intToString,
            String[] split, int size, ArrayList<ModulesKey> lst) {
        if (size > 0 && split[0].length() > 0) { //Just making sure we have something there.
            ModulesKey key;
            if (size == 1) {
                key = new ModulesKey(split[0], null);
                //restore with empty modules.
                lst.add(key);

            } else if (size == 2) {
                key = new ModulesKey(split[0], new File(split[1]));
                //restore with empty modules.
                lst.add(key);

            } else if (size == 4) {
                try {
                    key = new ModulesKeyForZip(split[0], //module name
                            new File(intToString.get(Integer.parseInt(split[1]))), //zip file (usually repeated over and over again)
                            split[2], //path in zip
                            split[3].equals("1")); //is file (false = folder)
                    //restore with empty modules.
                    lst.add(key);
                } catch (NumberFormatException e) {
                    Log.log(e);
                }
            }
        }
    }

    /**
     * @return Returns the modules.
     */
    protected Map<ModulesKey, AbstractModule> getModules() {
        throw new RuntimeException("Deprecated");
    }

    /**
     * Change the pythonpath (used for both: system and project)
     *
     * @param project: may be null
     * @param defaultSelectedInterpreter: may be null
     */
    public void changePythonPath(String pythonpath, final IProject project, IProgressMonitor monitor) {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        pythonPathHelper.setPythonPath(pythonpath);
        ModulesFoundStructure modulesFound = pythonPathHelper.getModulesFoundStructure(project, monitor);

        PyPublicTreeMap<ModulesKey, ModulesKey> keys = buildKeysFromModulesFound(monitor, modulesFound);
        onChangePythonpath(keys);

        synchronized (modulesKeysLock) {
            cache.clear();
            //assign to instance variable
            this.modulesKeys.clear();
            this.modulesKeys.putAll(keys);
        }

    }

    /**
     * @return a tuple with the new keys to be added to the modules manager (i.e.: found in keysFound but not in the
     * modules manager) and the keys to be removed from the modules manager (i.e.: found in the modules manager but
     * not in the keysFound)
     */
    public Tuple<List<ModulesKey>, List<ModulesKey>> diffModules(AbstractMap<ModulesKey, ModulesKey> keysFound) {
        ArrayList<ModulesKey> newKeys = new ArrayList<ModulesKey>();
        ArrayList<ModulesKey> removedKeys = new ArrayList<ModulesKey>();
        Iterator<ModulesKey> it = keysFound.keySet().iterator();

        synchronized (modulesKeysLock) {
            while (it.hasNext()) {
                ModulesKey next = it.next();
                ModulesKey modulesKey = modulesKeys.get(next);
                if (modulesKey == null || modulesKey.getClass() != next.getClass()) {
                    //Check the class because ModulesKey and ModulesKeyForZip are equal considering only the name.
                    newKeys.add(next);
                }
            }

            it = modulesKeys.keySet().iterator();
            while (it.hasNext()) {
                ModulesKey next = it.next();
                ModulesKey modulesKey = keysFound.get(next);
                if (modulesKey == null || modulesKey.getClass() != next.getClass()) {
                    removedKeys.add(next);
                }
            }
        }

        return new Tuple<List<ModulesKey>, List<ModulesKey>>(newKeys, removedKeys);
    }

    public static PyPublicTreeMap<ModulesKey, ModulesKey> buildKeysFromModulesFound(IProgressMonitor monitor,
            ModulesFoundStructure modulesFound) {
        //now, on to actually filling the module keys
        PyPublicTreeMap<ModulesKey, ModulesKey> keys = new PyPublicTreeMap<ModulesKey, ModulesKey>();
        buildKeysForRegularEntries(monitor, modulesFound, keys, false);

        for (ZipContents zipContents : modulesFound.zipContents) {
            if (monitor.isCanceled()) {
                break;
            }
            buildKeysForZipContents(keys, zipContents);
        }

        return keys;
    }

    public static void buildKeysForRegularEntries(IProgressMonitor monitor, ModulesFoundStructure modulesFound,
            PyPublicTreeMap<ModulesKey, ModulesKey> keys, boolean includeOnlySourceModules) {
        String[] dottedValidSourceFiles = FileTypesPreferencesPage.getDottedValidSourceFiles();

        int j = 0;
        FastStringBuffer buffer = new FastStringBuffer();
        //now, create in memory modules for all the loaded files (empty modules).
        for (Iterator<Map.Entry<File, String>> iterator = modulesFound.regularModules.entrySet().iterator(); iterator
                .hasNext() && monitor.isCanceled() == false; j++) {
            Map.Entry<File, String> entry = iterator.next();
            String m = entry.getValue();

            if (m != null) {
                if (j % 20 == 0) {
                    //no need to report all the time (that's pretty fast now)
                    buffer.clear();
                    monitor.setTaskName(buffer.append("Module resolved: ").append(m).toString());
                    monitor.worked(1);
                }

                //we don't load them at this time.
                File f = entry.getKey();

                if (includeOnlySourceModules) {
                    //check if we should include only source modules
                    if (!PythonPathHelper.isValidSourceFile(f.getName())) {
                        continue;
                    }
                }
                ModulesKey modulesKey = new ModulesKey(m, f);

                //no conflict (easy)
                if (!keys.containsKey(modulesKey)) {
                    keys.put(modulesKey, modulesKey);

                } else {
                    //we have a conflict, so, let's resolve which one to keep (the old one or this one)
                    if (PythonPathHelper.isValidSourceFile(f.getName(), dottedValidSourceFiles)) {
                        //source files have priority over other modules (dlls) -- if both are source, there is no real way to resolve
                        //this priority, so, let's just add it over.
                        keys.put(modulesKey, modulesKey);
                    }
                }
            }
        }
    }

    public static void buildKeysForZipContents(PyPublicTreeMap<ModulesKey, ModulesKey> keys, ZipContents zipContents) {
        for (String filePathInZip : zipContents.foundFileZipPaths) {
            String modName = StringUtils.stripExtension(filePathInZip).replace('/', '.');
            if (DEBUG_ZIP) {
                System.out.println("Found in zip:" + modName);
            }
            ModulesKey k = new ModulesKeyForZip(modName, zipContents.zipFile, filePathInZip, true);
            keys.put(k, k);

            if (zipContents.zipContentsType == ZipContents.ZIP_CONTENTS_TYPE_JAR) {
                //folder modules are only created for jars (because for python files, the __init__.py is required).
                for (String s : new FullRepIterable(FullRepIterable.getWithoutLastPart(modName))) { //the one without the last part was already added
                    k = new ModulesKeyForZip(s, zipContents.zipFile, s.replace('.', '/'), false);
                    keys.put(k, k);
                }
            }
        }
    }

    /**
     * Subclasses may do more things after the defaults were added to the cache (e.g.: the system modules manager may
     * add builtins)
     */
    protected void onChangePythonpath(SortedMap<ModulesKey, ModulesKey> keys) {
    }

    /**
     * This is the only method that should remove a module.
     * No other method should remove them directly.
     *
     * @param key this is the key that should be removed
     */
    protected void doRemoveSingleModule(ModulesKey key) {
        synchronized (modulesKeysLock) {
            if (DEBUG_BUILD) {
                System.out.println("Removing module:" + key + " - " + this.getClass());
            }
            this.modulesKeys.remove(key);
            ModulesManager.cache.remove(key, this);
        }
    }

    /**
     * This method that actually removes some keys from the modules.
     *
     * @param toRem the modules to be removed
     */
    protected void removeThem(Collection<ModulesKey> toRem) {
        //really remove them here.
        for (Iterator<ModulesKey> iter = toRem.iterator(); iter.hasNext();) {
            doRemoveSingleModule(iter.next());
        }
    }

    public void removeModules(Collection<ModulesKey> toRem) {
        removeThem(toRem);
    }

    public IModule addModule(final ModulesKey key) {
        AbstractModule ret = AbstractModule.createEmptyModule(key);
        doAddSingleModule(key, ret);
        return ret;
    }

    @Override
    public boolean hasModule(ModulesKey key) {
        synchronized (modulesKeysLock) {
            return this.modulesKeys.containsKey(key);
        }
    }

    /**
     * This is the only method that should add / update a module.
     * No other method should add it directly (unless it is loading or rebuilding it).
     *
     * @param key this is the key that should be added
     * @param n
     */
    public void doAddSingleModule(final ModulesKey key, AbstractModule n) {
        if (DEBUG_BUILD) {
            System.out.println("Adding module:" + key + " - " + this.getClass());
        }
        synchronized (modulesKeysLock) {
            this.modulesKeys.put(key, key);
            ModulesManager.cache.add(key, n, this);
        }
    }

    /**
     * @return a set of all module keys
     *
     * Note: addDependencies ignored at this point.
     */
    public Set<String> getAllModuleNames(boolean addDependencies, String partStartingWithLowerCase) {
        Set<String> s = new HashSet<String>();
        synchronized (modulesKeysLock) {
            for (ModulesKey key : this.modulesKeys.keySet()) {
                if (key.hasPartStartingWith(partStartingWithLowerCase)) {
                    s.add(key.name);
                }
            }
        }
        return s;
    }

    public SortedMap<ModulesKey, ModulesKey> getAllDirectModulesStartingWith(String strStartingWith) {
        if (strStartingWith.length() == 0) {
            synchronized (modulesKeysLock) {
                //we don't want it to be backed up by the same set (because it may be changed, so, we may get
                //a java.util.ConcurrentModificationException on places that use it)
                return new PyPublicTreeMap<ModulesKey, ModulesKey>(modulesKeys);
            }
        }
        ModulesKey startingWith = new ModulesKey(strStartingWith, null);
        ModulesKey endingWith = new ModulesKey(strStartingWith + "\uffff\uffff\uffff\uffff", null);
        synchronized (modulesKeysLock) {
            //we don't want it to be backed up by the same set (because it may be changed, so, we may get
            //a java.util.ConcurrentModificationException on places that use it)
            return new PyPublicTreeMap<ModulesKey, ModulesKey>(modulesKeys.subMap(startingWith, endingWith));
        }
    }

    public SortedMap<ModulesKey, ModulesKey> getAllModulesStartingWith(String strStartingWith) {
        return getAllDirectModulesStartingWith(strStartingWith);
    }

    public ModulesKey[] getOnlyDirectModules() {
        synchronized (modulesKeysLock) {
            return this.modulesKeys.keySet().toArray(new ModulesKey[0]);
        }
    }

    /**
     * Note: no dependencies at this point (so, just return the keys)
     */
    public int getSize(boolean addDependenciesSize) {
        synchronized (modulesKeysLock) {
            return this.modulesKeys.size();
        }
    }

    public IModule getModule(String name, IPythonNature nature, boolean dontSearchInit) {
        return getModule(true, name, nature, dontSearchInit);
    }

    /**
     * Note that the access must be synched.
     */
    public final Map<String, SortedMap<Integer, IModule>> temporaryModules = new HashMap<String, SortedMap<Integer, IModule>>();
    private final Object lockTemporaryModules = new Object();
    private int nextHandle = 0;

    /**
     * Returns the handle to be used to remove the module added later on!
     */
    public int pushTemporaryModule(String moduleName, IModule module) {
        synchronized (lockTemporaryModules) {
            SortedMap<Integer, IModule> map = temporaryModules.get(moduleName);
            if (map == null) {
                map = new TreeMap<Integer, IModule>(); //small initial size!
                temporaryModules.put(moduleName, map);
            }
            if (module instanceof AbstractModule) {
                module = decorateModule((AbstractModule) module, null);
            }
            nextHandle += 1; //Note: don't care about stack overflow!
            map.put(nextHandle, module);
            return nextHandle;
        }

    }

    public void popTemporaryModule(String moduleName, int handle) {
        synchronized (lockTemporaryModules) {
            SortedMap<Integer, IModule> stack = temporaryModules.get(moduleName);
            try {
                if (stack != null) {
                    stack.remove(handle);
                    if (stack.size() == 0) {
                        temporaryModules.remove(moduleName);
                    }
                }
            } catch (Throwable e) {
                Log.log(e);
            }
        }
    }

    /**
     * This method returns the module that corresponds to the path passed as a parameter.
     *
     * @param name the name of the module we're looking for  (e.g.: mod1.mod2)
     * @param dontSearchInit is used in a negative form because initially it was isLookingForRelative, but
     * it actually defines if we should look in __init__ modules too, so, the name matches the old signature.
     *
     * NOTE: isLookingForRelative description was: when looking for relative imports, we don't check for __init__
     * @return the module represented by this name
     */
    protected IModule getModule(boolean acceptCompiledModule, String name, IPythonNature nature,
            boolean dontSearchInit) {
        synchronized (lockTemporaryModules) {
            SortedMap<Integer, IModule> map = temporaryModules.get(name);
            if (map != null && map.size() > 0) {
                if (DEBUG_TEMPORARY_MODULES) {
                    System.out.println("Returning temporary module: " + name);
                }
                return map.get(map.lastKey());
            }
        }
        AbstractModule n = null;
        ModulesKey keyForCacheAccess = new ModulesKey(null, null);

        if (!dontSearchInit) {
            if (n == null) {
                keyForCacheAccess.name = (String) StringUtils.join(".", new String[] { name, "__init__" }, null);
                n = cache.getObj(keyForCacheAccess, this);
                if (n != null) {
                    name = keyForCacheAccess.name;
                }
            }
        }
        if (n == null) {
            keyForCacheAccess.name = name;
            n = cache.getObj(keyForCacheAccess, this);
        }

        if (n instanceof SourceModule) {
            //ok, module exists, let's check if it is synched with the filesystem version...
            SourceModule s = (SourceModule) n;
            if (!s.isSynched()) {
                //change it for an empty and proceed as usual.
                n = (AbstractModule) addModule(createModulesKey(s.getName(), s.getFile()));
            }
        }

        if (n instanceof EmptyModule) {
            EmptyModule e = (EmptyModule) n;
            if (e.f != null) {

                if (!e.f.exists()) {
                    //if the file does not exist anymore, just remove it.
                    keyForCacheAccess.name = name;
                    keyForCacheAccess.file = e.f;
                    doRemoveSingleModule(keyForCacheAccess);
                    n = null;

                } else {
                    //file exists
                    n = checkOverride(name, nature, n);

                    if (n instanceof EmptyModule) {
                        //ok, handle case where the file is actually from a zip file...
                        if (e instanceof EmptyModuleForZip) {
                            EmptyModuleForZip emptyModuleForZip = (EmptyModuleForZip) e;

                            if (emptyModuleForZip.pathInZip.endsWith(".class") || !emptyModuleForZip.isFile) {
                                //handle java class... (if it's a class or a folder in a jar)
                                try {
                                    n = JythonModulesManagerUtils.createModuleFromJar(emptyModuleForZip);
                                    n = decorateModule(n, nature);
                                } catch (Throwable e1) {
                                    Log.log("Unable to create module from jar (note: JDT is required for Jython development): "
                                            + emptyModuleForZip + " project: "
                                            + (nature != null ? nature.getProject() : "null"), e1);
                                    n = null;
                                }

                            } else if (FileTypesPreferencesPage.isValidDll(emptyModuleForZip.pathInZip)) {
                                //.pyd
                                n = new CompiledModule(name, this);
                                n = decorateModule(n, nature);

                            } else if (PythonPathHelper.isValidSourceFile(emptyModuleForZip.pathInZip)) {
                                //handle python file from zip... we have to create it getting the contents from the zip file
                                try {
                                    IDocument doc = FileUtilsFileBuffer.getDocFromZip(emptyModuleForZip.f,
                                            emptyModuleForZip.pathInZip);
                                    //NOTE: The nature (and so the grammar to be used) must be defined by this modules
                                    //manager (and not by the initial caller)!!
                                    n = AbstractModule.createModuleFromDoc(name, emptyModuleForZip.f, doc,
                                            this.getNature(), false);
                                    SourceModule zipModule = (SourceModule) n;
                                    zipModule.zipFilePath = emptyModuleForZip.pathInZip;
                                    n = decorateModule(n, nature);
                                } catch (Exception exc1) {
                                    Log.log(exc1);
                                    n = null;
                                }
                            }

                        } else {
                            //regular case... just go on and create it.
                            try {
                                //NOTE: The nature (and so the grammar to be used) must be defined by this modules
                                //manager (and not by the initial caller)!!
                                n = AbstractModule.createModule(name, e.f, this.getNature(), true);
                                n = decorateModule(n, nature);
                            } catch (IOException exc) {
                                keyForCacheAccess.name = name;
                                keyForCacheAccess.file = e.f;
                                doRemoveSingleModule(keyForCacheAccess);
                                n = null;
                            } catch (MisconfigurationException exc) {
                                Log.log(exc);
                                n = null;
                            }
                        }
                    }

                }

            } else { //ok, it does not have a file associated, so, we treat it as a builtin (this can happen in java jars)
                n = checkOverride(name, nature, n);
                if (n instanceof EmptyModule) {
                    if (acceptCompiledModule) {
                        n = new CompiledModule(name, this);
                        n = decorateModule(n, nature);
                    } else {
                        return null;
                    }
                }
            }

            if (n != null) {
                doAddSingleModule(createModulesKey(name, e.f), n);
            } else {
                Log.log(("The module " + name + " could not be found nor created!"));
            }
        }

        if (n instanceof EmptyModule) {
            throw new RuntimeException("Should not be an empty module anymore: " + n);
        }
        if (n instanceof SourceModule) {
            SourceModule sourceModule = (SourceModule) n;
            //now, here's a catch... it may be a bootstrap module...
            if (sourceModule.isBootstrapModule()) {
                //if it's a bootstrap module, we must replace it for the related compiled module.
                n = new CompiledModule(name, this);
                n = decorateModule(n, nature);
            }
        }

        return n;
    }

    /**
     * Called after the creation of any module. Used as a workaround for filling tokens that are in no way
     * available in the code-completion through the regular inspection.
     *
     * The django objects class is the reason why this happens... It's structure for the creation on a model class
     * follows no real patterns for the creation of the 'objects' attribute in the class, and thus, we have no
     * real generic way of discovering it (actually, even by looking at the class definition this is very obscure),
     * so, the solution found is creating the objects by decorating the module with that info.
     */
    private AbstractModule decorateModule(AbstractModule n, IPythonNature nature) {
        if (n instanceof SourceModule) {
            if ("django.db.models.base".equals(n.getName())) {
                SourceModule sourceModule = (SourceModule) n;
                SimpleNode ast = sourceModule.getAst();
                boolean found = false;
                Module module = (Module) ast;
                stmtType[] body = module.body;
                for (SimpleNode node : body) {
                    if (node instanceof ClassDef && "Model".equals(NodeUtils.getRepresentationString(node))) {
                        found = true;
                        Object[][] metaclassAttrs = new Object[][] {
                                { "objects", NodeUtils.makeAttribute("django.db.models.manager.Manager()") },
                                { "DoesNotExist", new Name("Exception", Name.Load, false) },
                                { "MultipleObjectsReturned", new Name("Exception", Name.Load, false) }, };

                        ClassDef classDef = (ClassDef) node;
                        stmtType[] newBody = new stmtType[classDef.body.length + metaclassAttrs.length];
                        System.arraycopy(classDef.body, 0, newBody, metaclassAttrs.length, classDef.body.length);

                        int i = 0;
                        for (Object[] objAndType : metaclassAttrs) {
                            //Note that the line/col is important so that we correctly acknowledge it inside the "class Model" scope.
                            Name name = new Name((String) objAndType[0], Name.Store, false);
                            name.beginColumn = classDef.beginColumn + 4;
                            name.beginLine = classDef.beginLine + 1;
                            newBody[i] = new Assign(new exprType[] { name }, (exprType) objAndType[1]);
                            newBody[i].beginColumn = classDef.beginColumn + 4;
                            newBody[i].beginLine = classDef.beginLine + 1;

                            i += 1;
                        }

                        classDef.body = newBody;
                        break;
                    }
                }

                if (found) {
                    stmtType[] newBody = new stmtType[body.length + 1];
                    System.arraycopy(body, 0, newBody, 1, body.length);
                    Import importNode = new Import(new aliasType[] { new aliasType(new NameTok(
                            "django.db.models.manager",
                            NameTok.ImportModule), null) });
                    newBody[0] = importNode;
                    module.body = newBody;
                }

            } else if ("django.db.models.manager".equals(n.getName())) {
                SourceModule sourceModule = (SourceModule) n;
                SimpleNode ast = sourceModule.getAst();
                Module module = (Module) ast;
                stmtType[] body = module.body;
                for (SimpleNode node : body) {
                    if (node instanceof ClassDef && "Manager".equals(NodeUtils.getRepresentationString(node))) {
                        ClassDef classDef = (ClassDef) node;
                        stmtType[] newBody = null;
                        try {
                            File managerBody = PydevPlugin.getBundleInfo().getRelativePath(
                                    new Path("pysrc/stubs/_django_manager_body.py"));
                            IDocument doc = FileUtilsFileBuffer.getDocFromFile(managerBody);
                            IGrammarVersionProvider provider = new IGrammarVersionProvider() {

                                public int getGrammarVersion() throws MisconfigurationException {
                                    return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0; // Always Python 3.0 here
                                }
                            };
                            ParseOutput obj = PyParser.reparseDocument(new PyParser.ParserInfo(doc, provider,
                                    "_django_manager_body", managerBody));
                            Module ast2 = (Module) obj.ast;
                            newBody = ast2.body;

                            for (stmtType b : newBody) {
                                //Note that the line/col is important so that we correctly acknowledge it inside the "class Manager" scope.
                                b.beginColumn = classDef.beginColumn + 4;
                                b.beginLine = classDef.beginLine + 1;
                            }

                            classDef.body = newBody;
                            break;
                        } catch (Exception e) {
                            Log.log(e);
                        }

                    }
                }
            }
        }
        return n;
    }

    /**
     * Hook called to give clients a chance to override the module created (still experimenting, so, it's not public).
     */
    private AbstractModule checkOverride(String name, IPythonNature nature, AbstractModule emptyModule) {
        return emptyModule;
    }

    private ModulesKey createModulesKey(String name, File f) {
        ModulesKey newEntry = new ModulesKey(name, f);
        synchronized (modulesKeysLock) {
            Entry<ModulesKey, ModulesKey> oldEntry = this.modulesKeys.getEntry(newEntry);
            if (oldEntry != null) {
                return oldEntry.getKey();
            } else {
                return newEntry;
            }
        }
    }

    /**
     * Passes through all the compiled modules in memory and clears its tokens (so that
     * we restore them when needed).
     */
    public static void clearCache() {
        ModulesManager.cache.clear();
    }

    /**
     * @see org.python.pydev.core.IProjectModulesManager#isInPythonPath(org.eclipse.core.resources.IResource, org.eclipse.core.resources.IProject)
     */
    public boolean isInPythonPath(IResource member, IProject container) {
        return resolveModule(member, container) != null;
    }

    /**
     * @see org.python.pydev.core.IProjectModulesManager#resolveModule(org.eclipse.core.resources.IResource, org.eclipse.core.resources.IProject)
     */
    public String resolveModule(IResource member, IProject container) {
        File inOs = member.getRawLocation().toFile();
        return pythonPathHelper.resolveModule(FileUtils.getFileAbsolutePath(inOs), false, container);
    }

    protected String getResolveModuleErr(IResource member) {
        return "Unable to find the path " + member + " in the project were it\n"
                + "is added as a source folder for pydev." + this.getClass();
    }

    /**
     * @param full
     * @return
     */
    public String resolveModule(String full) {
        return pythonPathHelper.resolveModule(full, false, null);
    }

    private final Object lockAccessCreateCompiledModuleLock = new Object();
    private final Map<String, Object> createCompiledModuleLock = new LRUMap<String, Object>(50);

    @Override
    public Object getCompiledModuleCreationLock(String name) {
        synchronized (lockAccessCreateCompiledModuleLock) {
            Object lock = createCompiledModuleLock.get(name);
            if (lock == null) {
                lock = new Object();
                createCompiledModuleLock.put(name, lock);
            }
            return lock;
        }
    }
}
