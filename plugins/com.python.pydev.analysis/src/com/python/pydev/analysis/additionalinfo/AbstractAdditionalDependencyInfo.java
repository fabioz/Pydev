/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 28/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.FastBufferedReader;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.ModulesKeyForZip;
import org.python.pydev.core.ObjectsPool;
import org.python.pydev.core.ObjectsPool.ObjectsPoolMap;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.cache.CompleteIndexKey;
import org.python.pydev.core.cache.CompleteIndexValue;
import org.python.pydev.core.cache.DiskCache;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.codecompletion.revisited.PyPublicTreeMap;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.logging.DebugSettings;
import org.python.pydev.parser.jython.SimpleNode;

/**
 * Adds dependency information to the interpreter information. This should be used only for
 * classes that are part of a project (this info will not be gotten for the system interpreter) 
 * 
 * (Basically, it will index all the names that are found in a module so that we can easily know all the
 * places where some name exists)
 * 
 * This index was removed for now... it wasn't working properly because the AST info could be only partial
 * when it arrived here, thus, it didn't really serve its purpose well (this will have to be redone properly
 * later on).
 * 
 * @author Fabio
 */
public abstract class AbstractAdditionalDependencyInfo extends AbstractAdditionalTokensInfo {

    public static boolean TESTING = false;

    public static final boolean DEBUG = false;

    /**
     * indexes all the names that are available
     * 
     * Note that the key in the disk cache is the module name and each
     * module points to a Set<Strings>
     * 
     * So the key is the module name and the value is a Set of the strings it contains.
     */
    public DiskCache completeIndex;

    /**
     * default constructor
     * @throws MisconfigurationException 
     */
    public AbstractAdditionalDependencyInfo() throws MisconfigurationException {
        init();
    }

    public AbstractAdditionalDependencyInfo(boolean callInit) throws MisconfigurationException {
        if (callInit) {
            init();
        }
    }

    private static ICallback<CompleteIndexValue, String> readFromFileMethod = new ICallback<CompleteIndexValue, String>() {

        public CompleteIndexValue call(String arg) {
            CompleteIndexValue entry = new CompleteIndexValue();
            if (arg.equals("0")) {
                return entry;
            }
            //The set was written!
            HashSet<String> hashSet = new HashSet<String>();
            if (arg.length() > 0) {
                StringUtils.splitWithIntern(arg, '\n', hashSet);
            }
            entry.entries = hashSet;

            return entry;
        }
    };

    private static ICallback<String, CompleteIndexValue> toFileMethod = new ICallback<String, CompleteIndexValue>() {

        public String call(CompleteIndexValue arg) {
            FastStringBuffer buf;
            if (arg.entries == null) {
                return "0";
            }
            buf = new FastStringBuffer(arg.entries.size() * 20);

            for (String s : arg.entries) {
                buf.append(s);
                buf.append('\n');
            }
            return buf.toString();
        }
    };

    /**
     * Initializes the internal DiskCache with the indexes.
     * @throws MisconfigurationException 
     */
    protected void init() throws MisconfigurationException {
        File persistingFolder = getCompleteIndexPersistingFolder();

        completeIndex = new DiskCache(persistingFolder, ".v1_indexcache", readFromFileMethod, toFileMethod);
    }

    /**
     * @return a folder where the index should be persisted
     * @throws MisconfigurationException 
     */
    protected File getCompleteIndexPersistingFolder() throws MisconfigurationException {
        File persistingFolder = getPersistingFolder();
        persistingFolder = new File(persistingFolder, "v1_indexcache");

        if (persistingFolder.exists()) {
            if (!persistingFolder.isDirectory()) {
                persistingFolder.delete();
            }
        }
        if (!persistingFolder.exists()) {
            persistingFolder.mkdirs();
        }
        return persistingFolder;
    }

    @Override
    public void clearAllInfo() {
        synchronized (lock) {
            super.clearAllInfo();
            try {
                completeIndex.clear();
            } catch (NullPointerException e) {
                //that's ok... because it might be called before actually having any values
            }
        }
    }

    public void updateKeysIfNeededAndSave(PyPublicTreeMap<ModulesKey, ModulesKey> keysFound) {
        Map<CompleteIndexKey, CompleteIndexKey> keys = this.completeIndex.keys();

        ArrayList<ModulesKey> newKeys = new ArrayList<ModulesKey>();
        ArrayList<ModulesKey> removedKeys = new ArrayList<ModulesKey>();

        //temporary
        CompleteIndexKey tempKey = new CompleteIndexKey((ModulesKey) null);

        Iterator<ModulesKey> it = keysFound.values().iterator();
        while (it.hasNext()) {
            ModulesKey next = it.next();
            if (next.file != null) {
                long lastModified = next.file.lastModified();
                if (lastModified != 0) {
                    tempKey.key = next;
                    CompleteIndexKey completeIndexKey = keys.get(tempKey);
                    boolean canAddAstInfoFor = PythonPathHelper.canAddAstInfoFor(next);
                    if (completeIndexKey == null) {
                        if (canAddAstInfoFor) {
                            newKeys.add(next);
                        }
                    } else {
                        if (canAddAstInfoFor) {
                            if (completeIndexKey.lastModified != lastModified) {
                                //Just re-add it if the time changed!
                                newKeys.add(next);
                            }
                        } else {
                            //It's there but it's not valid: Remove it!
                            removedKeys.add(next);
                        }
                    }
                }
            }
        }

        Iterator<CompleteIndexKey> it2 = keys.values().iterator();
        while (it2.hasNext()) {
            CompleteIndexKey next = it2.next();
            if (!keysFound.containsKey(next.key) || !PythonPathHelper.canAddAstInfoFor(next.key)) {
                removedKeys.add(next.key);
            }
        }

        boolean hasNew = newKeys.size() != 0;
        boolean hasRemoved = removedKeys.size() != 0;

        if (hasNew) {
            for (ModulesKey newKey : newKeys) {
                try {
                    this.addAstInfo(newKey, false);
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        }

        if (hasRemoved) {
            for (ModulesKey removedKey : removedKeys) {
                this.removeInfoFromModule(removedKey.name, false);
            }
        }

        if (hasNew || hasRemoved) {
            if (DebugSettings.DEBUG_INTERPRETER_AUTO_UPDATE) {
                Log.toLogFile(this,
                        StringUtils.format("Additional info modules. Added: %s Removed: %s", newKeys, removedKeys));
            }
            save();
        }
    }

    @Override
    public List<ModulesKey> getModulesWithToken(String token, IProgressMonitor monitor) {
        FastStringBuffer temp = new FastStringBuffer();
        ArrayList<ModulesKey> ret = new ArrayList<ModulesKey>();
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        if (token == null || token.length() == 0) {
            return ret;
        }

        for (int i = 0; i < token.length(); i++) {
            if (!Character.isJavaIdentifierPart(token.charAt(i))) {
                throw new RuntimeException(StringUtils.format("Token: %s is not a valid token to search for.", token));
            }
        }
        synchronized (lock) {
            FastStringBuffer bufProgress = new FastStringBuffer();
            //Note that this operation is not as fast as the others, as it relies on a cache that is optimized
            //for space and not for speed (but still, should be faster than having to do a text-search to know the 
            //tokens when the cache is available).

            Tuple<List<Tuple<CompleteIndexKey, CompleteIndexValue>>, Collection<CompleteIndexKey>> memoryInfo = completeIndex
                    .getInMemoryInfo();

            long last = System.currentTimeMillis();
            int worked = 0;
            try {
                monitor.beginTask("Get modules with token", memoryInfo.o1.size() + memoryInfo.o2.size());
                for (Tuple<CompleteIndexKey, CompleteIndexValue> tup : memoryInfo.o1) {
                    CompleteIndexKey indexKey = tup.o1;
                    CompleteIndexValue obj = tup.o2;

                    worked++;
                    if (monitor.isCanceled()) {
                        return ret;
                    }
                    long current = System.currentTimeMillis();
                    if (last + 200 < current) {
                        last = current;
                        monitor.setTaskName(bufProgress.clear().append("Searching: ").append(indexKey.key.name)
                                .toString());
                        monitor.worked(worked);
                    }
                    check(indexKey, obj, temp, token, ret);
                }

                for (CompleteIndexKey indexKey : memoryInfo.o2) {
                    worked++;
                    if (monitor.isCanceled()) {
                        return ret;
                    }
                    long current = System.currentTimeMillis();
                    if (last + 200 < current) {
                        last = current;
                        monitor.setTaskName(bufProgress.clear().append("Searching: ").append(indexKey.key.name)
                                .toString());
                        monitor.worked(worked);
                    }
                    check(indexKey, null, temp, token, ret);
                }
            } finally {
                monitor.done();
            }
        }
        return ret;
    }

    private void check(CompleteIndexKey indexKey, CompleteIndexValue obj, FastStringBuffer temp, String token,
            ArrayList<ModulesKey> ret) {
        if (obj == null) {
            obj = completeIndex.getObj(indexKey);
        }
        boolean canAddAstInfoFor = PythonPathHelper.canAddAstInfoFor(indexKey.key);
        if (obj == null) {
            if (canAddAstInfoFor) {
                try {
                    //Should be there (recreate the entry in the index and in the actual AST)
                    this.addAstInfo(indexKey.key, true);
                } catch (Exception e) {
                    Log.log(e);
                }

                obj = new CompleteIndexValue();
            } else {
                if (DEBUG) {
                    System.out.println("Removing (file does not exist or is not a valid source module): "
                            + indexKey.key.name);
                }
                this.removeInfoFromModule(indexKey.key.name, true);
                return;
            }
        }

        long lastModified = indexKey.key.file.lastModified();
        if (lastModified == 0 || !canAddAstInfoFor) {
            //File no longer exists or is not a valid source module.
            if (DEBUG) {
                System.out.println("Removing (file no longer exists or is not a valid source module): "
                        + indexKey.key.name + " indexKey.key.file: " + indexKey.key.file + " exists: "
                        + indexKey.key.file.exists());
            }
            this.removeInfoFromModule(indexKey.key.name, true);
            return;
        }

        //if it got here, it must be a valid source module!

        if (obj.entries != null) {
            if (lastModified != indexKey.lastModified) {
                obj = new CompleteIndexValue();
                try {
                    //Recreate the entry on the new time (recreate the entry in the index and in the actual AST)
                    this.addAstInfo(indexKey.key, true);
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        }

        //The actual values are always recreated lazily (in the case that it's really needed).
        if (obj.entries == null) {
            FastStringBuffer buf;
            ModulesKey key = indexKey.key;
            try {
                if (key instanceof ModulesKeyForZip) {
                    ModulesKeyForZip modulesKeyForZip = (ModulesKeyForZip) key;
                    buf = (FastStringBuffer) REF.getCustomReturnFromZip(modulesKeyForZip.file,
                            modulesKeyForZip.zipModulePath, FastStringBuffer.class);
                } else {
                    buf = (FastStringBuffer) REF.getFileContentsCustom(key.file, FastStringBuffer.class);
                }
            } catch (Exception e) {
                Log.log(e);
                return;
            }

            HashSet<String> set = new HashSet<String>();
            temp = temp.clear();
            int length = buf.length();
            for (int i = 0; i < length; i++) {
                char c = buf.charAt(i);
                if (Character.isJavaIdentifierStart(c)) {
                    temp.clear();
                    temp.append(c);
                    i++;
                    for (; i < length; i++) {
                        c = buf.charAt(i);
                        if (c == ' ' || c == '\t') {
                            break; //Fast forward through the most common case...
                        }
                        if (Character.isJavaIdentifierPart(c)) {
                            temp.append(c);
                        } else {
                            break;
                        }
                    }
                    String str = temp.toString();
                    if (PySelection.ALL_KEYWORD_TOKENS.contains(str)) {
                        continue;
                    }
                    set.add(str);
                }
            }

            obj.entries = set;
            indexKey.lastModified = lastModified;
            completeIndex.add(indexKey, obj); //Serialize the new contents
        }

        if (obj.entries != null && obj.entries.contains(token)) {
            ret.add(indexKey.key);
        }

    }

    @Override
    public List<IInfo> addAstInfo(SimpleNode node, ModulesKey key, boolean generateDelta) {
        List<IInfo> addAstInfo = new ArrayList<IInfo>();
        if (node == null || key == null || key.name == null) {
            return addAstInfo;
        }
        try {
            synchronized (lock) {
                addAstInfo = super.addAstInfo(node, key, generateDelta);

                if (key.file != null) {
                    completeIndex.add(new CompleteIndexKey(key), new CompleteIndexValue());
                }

            }
        } catch (Exception e) {
            Log.log(e);
        }
        return addAstInfo;
    }

    @Override
    public void removeInfoFromModule(String moduleName, boolean generateDelta) {
        synchronized (lock) {
            if (moduleName == null) {
                throw new AssertionError("The module name may not be null.");
            }
            completeIndex.remove(new CompleteIndexKey(moduleName));
            super.removeInfoFromModule(moduleName, generateDelta);
        }
    }

    @Override
    protected void saveTo(OutputStreamWriter writer, FastStringBuffer tempBuf, File pathToSave) throws IOException {
        synchronized (lock) {
            completeIndex.writeTo(tempBuf);
            writer.write(tempBuf.getInternalCharsArray(), 0, tempBuf.length());
            tempBuf.clear();

            super.saveTo(writer, tempBuf, pathToSave);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected void restoreSavedInfo(Object o) throws MisconfigurationException {
        synchronized (lock) {
            Tuple readFromFile = (Tuple) o;
            if (!(readFromFile.o1 instanceof Tuple3)) {
                throw new RuntimeException("Type Error: the info must be regenerated (changed across versions).");
            }

            completeIndex = (DiskCache) readFromFile.o2;
            if (completeIndex == null) {
                throw new RuntimeException(
                        "Type Error (index == null): the info must be regenerated (changed across versions).");
            }
            completeIndex.readFromFileMethod = readFromFileMethod;
            completeIndex.toFileMethod = toFileMethod;

            String shouldBeOn = REF.getFileAbsolutePath(getCompleteIndexPersistingFolder());
            if (!completeIndex.getFolderToPersist().equals(shouldBeOn)) {
                //this can happen if the user moves its .metadata folder (so, we have to validate it).
                completeIndex.setFolderToPersist(shouldBeOn);
            }

            super.restoreSavedInfo(readFromFile.o1);
        }
    }

    /**
     * actually does the load
     * @return true if it was successfully loaded and false otherwise
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected boolean load() {

        Throwable errorFound = null;
        synchronized (lock) {
            File file;
            try {
                file = getPersistingLocation();
            } catch (MisconfigurationException e) {
                Log.log("Unable to restore previous info... (persisting location not available).", e);
                return false;
            }
            if (file.exists() && file.isFile()) {
                try {
                    return loadContentsFromFile(file) != null;
                } catch (Throwable e) {
                    errorFound = e;
                }
            }
        }
        try {
            String msg = "Info: Rebuilding internal caches: " + this.getPersistingLocation();
            if (errorFound == null) {
                msg += " (Expected error to be provided and got no error!)";
                Log.log(IStatus.ERROR, msg, errorFound);

            } else {
                Log.log(IStatus.INFO, msg, errorFound);
            }
        } catch (Exception e1) {
            Log.log("Rebuilding internal caches (error getting persisting location).");
        }
        return false;
    }

    private Object loadContentsFromFile(File file) throws FileNotFoundException, IOException, MisconfigurationException {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            //            Timer timer = new Timer();
            String expected = "-- VERSION_" + AbstractAdditionalTokensInfo.version; //X is the version
            InputStreamReader reader = new InputStreamReader(fileInputStream);
            FastBufferedReader bufferedReader = new FastBufferedReader(reader);
            FastStringBuffer string = bufferedReader.readLine();
            ObjectsPoolMap objectsPoolMap = new ObjectsPool.ObjectsPoolMap();
            if (string != null && string.startsWith("-- VERSION_")) {
                Tuple tupWithResults = new Tuple(new Tuple3(null, null, null), null);
                Tuple3 superTupWithResults = (Tuple3) tupWithResults.o1;
                //tupWithResults.o2 = DiskCache
                if (string.toString().equals(expected)) {
                    //OK, proceed with new I/O format!
                    try {
                        try {
                            FastStringBuffer line;
                            Map<Integer, String> dictionary = null;
                            FastStringBuffer tempBuf = new FastStringBuffer(1024);
                            while ((line = bufferedReader.readLine()) != null) {
                                if (line.startsWith("-- ")) {

                                    if (line.startsWith("-- START TREE 1")) {
                                        superTupWithResults.o1 = TreeIO.loadTreeFrom(bufferedReader, dictionary,
                                                tempBuf.clear(), objectsPoolMap);

                                    } else if (line.startsWith("-- START TREE 2")) {
                                        superTupWithResults.o2 = TreeIO.loadTreeFrom(bufferedReader, dictionary,
                                                tempBuf.clear(), objectsPoolMap);

                                    } else if (line.startsWith("-- START DICTIONARY")) {
                                        dictionary = TreeIO.loadDictFrom(bufferedReader, tempBuf.clear(),
                                                objectsPoolMap);

                                    } else if (line.startsWith("-- START DISKCACHE")) {
                                        tupWithResults.o2 = DiskCache.loadFrom(bufferedReader, objectsPoolMap);

                                    } else if (line.startsWith("-- VERSION_")) {
                                        if (!line.endsWith("3")) {
                                            throw new RuntimeException("Expected the version to be 3.");
                                        }
                                    } else if (line.startsWith("-- END TREE")) {
                                        //just skip it in this situation.
                                    } else {
                                        throw new RuntimeException("Unexpected line: " + line);
                                    }
                                }
                            }
                        } finally {
                            bufferedReader.close();
                        }
                    } finally {
                        reader.close();
                    }

                    restoreSavedInfo(tupWithResults);
                    //                    timer.printDiff("Time taken");
                    return tupWithResults;
                } else {
                    throw new RuntimeException("Version does not match. Found: " + string);
                }

            } else {
                //Try the old way of loading it (backward compatibility).
                fileInputStream.close();
                //                Timer timer2 = new Timer();
                Object tupWithResults = IOUtils.readFromFile(file);
                restoreSavedInfo(tupWithResults);
                //                timer2.printDiff("IOUtils time");
                save(); //Save in new format!
                return tupWithResults;
            }

        } finally {
            try {
                fileInputStream.close();
            } catch (Exception e) {
                //Ignore error closing.
            }
        }
    }

    protected void addInfoToModuleOnRestoreInsertCommand(Tuple<ModulesKey, List<IInfo>> data) {
        completeIndex.add(new CompleteIndexKey(data.o1), null);

        //current way (saves a list of iinfo)
        for (Iterator<IInfo> it = data.o2.iterator(); it.hasNext();) {
            IInfo info = it.next();
            if (info.getPath() == null || info.getPath().length() == 0) {
                this.add(info, TOP_LEVEL);

            } else {
                this.add(info, INNER);
            }
        }
    }

}
