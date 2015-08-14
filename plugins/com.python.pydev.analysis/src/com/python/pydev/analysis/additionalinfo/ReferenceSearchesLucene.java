/**
 * Copyright (c) 2015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.ModulesKeyForZip;
import org.python.pydev.core.cache.CompleteIndexKey;
import org.python.pydev.core.cache.DiskCache;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.ModulesKeyForJava;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.index.IndexApi;
import org.python.pydev.shared_core.index.IndexApi.DocumentInfo;
import org.python.pydev.shared_core.index.IndexApi.IDocumentsVisitor;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.OrderedMap;

public class ReferenceSearchesLucene implements IReferenceSearches {

    private static final Object lock = new Object();
    private static final Map<File, IndexApi> indexDirToApi = new HashMap<File, IndexApi>();

    public static void disposeAll() {
        synchronized (lock) {
            try {
                Set<Entry<File, IndexApi>> entrySet = indexDirToApi.entrySet();
                for (Entry<File, IndexApi> entry : entrySet) {
                    try {
                        entry.getValue().dispose();
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }
            } finally {
                indexDirToApi.clear();
            }
        }

    }

    private static final boolean DEBUG = false;
    private WeakReference<AbstractAdditionalDependencyInfo> abstractAdditionalDependencyInfo;
    private IndexApi indexApi;

    public ReferenceSearchesLucene(AbstractAdditionalDependencyInfo abstractAdditionalDependencyInfo) {
        this.abstractAdditionalDependencyInfo = new WeakReference<>(abstractAdditionalDependencyInfo);
    }

    @Override
    public void dispose() {
        if (indexApi != null) {
            indexApi = null;
        }
    }

    @Override
    public synchronized List<ModulesKey> search(IProject project,
            final OrderedMap<String, Set<String>> fieldNameToValues, IProgressMonitor monitor)
                    throws OperationCanceledException {
        try {
            return internalSearch(project, fieldNameToValues, monitor);
        } finally {
            monitor.done();
        }
    }

    public synchronized List<ModulesKey> internalSearch(IProject project,
            final OrderedMap<String, Set<String>> fieldNameToValues, IProgressMonitor monitor)
                    throws OperationCanceledException {

        final List<ModulesKey> ret = new ArrayList<ModulesKey>();
        PythonNature nature = PythonNature.getPythonNature(project);
        if (nature == null) {
            Log.log("Project :" + project + " does not have Python nature configured.");
            return ret;
        }

        // Make sure that its information is synchronized.
        AbstractAdditionalDependencyInfo abstractAdditionalDependencyInfo = this.abstractAdditionalDependencyInfo.get();
        if (abstractAdditionalDependencyInfo == null) {
            Log.log("AbstractAdditionalDependencyInfo already collected!");
            return ret;
        }
        boolean mustCommitChange = false;

        final String name = "Search modules with token in: " + abstractAdditionalDependencyInfo.getUIRepresentation();
        monitor.beginTask(name, 7);
        monitor.setTaskName(name);

        DiskCache completeIndex = abstractAdditionalDependencyInfo.completeIndex;

        // Note: we should be able to deal with entries already deleted!
        boolean applyAllDeletes = false;

        if (indexApi == null) {
            String folderToPersist = completeIndex.getFolderToPersist();
            synchronized (lock) {
                File indexDir = new File(folderToPersist, "lc");
                indexApi = indexDirToApi.get(indexDir);
                if (indexApi == null) {
                    try {
                        indexApi = new IndexApi(indexDir, applyAllDeletes);
                        indexDirToApi.put(indexDir, indexApi);
                    } catch (Exception e) {
                        Log.log(e);
                        return ret;
                    }
                }
            }
        }

        synchronized (indexApi.getLock()) {
            final Map<ModulesKey, CompleteIndexKey> indexMap = new HashMap<>(); // Key to CompleteIndexKey (has modified time).

            IDocumentsVisitor visitor = new IDocumentsVisitor() {

                @Override
                public void visit(DocumentInfo documentInfo) {
                    ModulesKey keyFromIO = ModulesKey.fromIO(documentInfo.get(FIELD_MODULES_KEY_IO));
                    String modifiedTime = documentInfo.get(FIELD_MODIFIED_TIME);
                    indexMap.put(keyFromIO, new CompleteIndexKey(keyFromIO, Long.parseLong(modifiedTime)));
                }
            };
            try {
                indexApi.visitAllDocs(visitor, FIELD_MODULES_KEY_IO, FIELD_MODIFIED_TIME);
            } catch (IOException e) {
                Log.log(e);
            }

            incrementAndCheckProgress("Visited current index", monitor);

            Set<CompleteIndexKey> docsToRemove = new HashSet<>();
            Set<CompleteIndexKey> modulesToAdd = new HashSet<>();
            Map<File, Set<CompleteIndexKey>> zipModulesToAdd = new HashMap<>();

            // Wait for the integrity check before getting the keys!
            abstractAdditionalDependencyInfo.waitForIntegrityCheck();

            final Map<CompleteIndexKey, CompleteIndexKey> currentKeys = completeIndex.keys();

            // Step 1: remove entries which were in the index but are already removed
            // from the modules (or have a different time).
            for (Entry<ModulesKey, CompleteIndexKey> entryInIndex : indexMap.entrySet()) {
                CompleteIndexKey indexModule = entryInIndex.getValue();

                CompleteIndexKey currentModule = currentKeys.get(indexModule);
                if (currentModule == null || currentModule.key == null || currentModule.key.file == null) {
                    docsToRemove.add(indexModule);

                } else {
                    // exists, but we also need to check the modified time
                    boolean changed = currentModule.lastModified != indexModule.lastModified;
                    if (!changed) {
                        ModulesKey keyCurrentModule = currentModule.key;
                        ModulesKey keyIndexModule = indexModule.key;
                        boolean currentIsZip = keyCurrentModule instanceof ModulesKeyForZip;
                        boolean indexIsZip = keyIndexModule instanceof ModulesKeyForZip;
                        changed = currentIsZip != indexIsZip;

                        if (!changed) {
                            changed = !currentModule.key.file.equals(indexModule.key.file);
                        }
                    }

                    if (changed) {
                        // remove and add
                        docsToRemove.add(indexModule);

                        add(modulesToAdd, zipModulesToAdd, currentModule);
                    }
                }
            }
            // --- Progress
            incrementAndCheckProgress("Updating for removal", monitor);

            // Step 2: add new entries in current and not in the index
            for (Entry<CompleteIndexKey, CompleteIndexKey> currentEntry : currentKeys.entrySet()) {
                CompleteIndexKey completeIndexKey = currentEntry.getValue();
                if (!indexMap.containsKey(completeIndexKey.key)) {
                    ModulesKey modulesKey = completeIndexKey.key;
                    if (modulesKey instanceof ModulesKeyForJava || modulesKey.file == null
                            || !modulesKey.file.isFile()) {
                        //ignore this one (we can't do anything with it).
                        continue;
                    }

                    if (modulesKey instanceof ModulesKeyForZip) {
                        ModulesKeyForZip modulesKeyForZip = (ModulesKeyForZip) modulesKey;
                        if (!modulesKeyForZip.isFile) {
                            continue; // Ignore folders in zips (happens for jython folders which may not have an __init__.py)
                        }
                    }

                    add(modulesToAdd, zipModulesToAdd, completeIndexKey);
                }
            }
            // --- Progress
            incrementAndCheckProgress("Updating for addition", monitor);

            Map<String, Collection<String>> fieldToValuesToRemove = new HashMap<>();
            Collection<String> lstToRemove = new ArrayList<>(docsToRemove.size());

            FastStringBuffer tempBuf = new FastStringBuffer();
            for (Iterator<CompleteIndexKey> it = docsToRemove.iterator(); it.hasNext();) {
                it.next().key.toIO(tempBuf.clear());
                lstToRemove.add(tempBuf.toString());
            }

            incrementAndCheckProgress("Removing outdated entries", monitor);
            if (lstToRemove.size() > 0) {
                fieldToValuesToRemove.put(FIELD_MODULES_KEY_IO, lstToRemove);
                try {
                    mustCommitChange = true;
                    if (DEBUG) {
                        System.out.println("Removing: " + fieldToValuesToRemove);
                    }
                    indexApi.removeDocs(fieldToValuesToRemove);
                } catch (IOException e) {
                    Log.log(e);
                }
            }

            incrementAndCheckProgress("Indexing new entries", monitor);
            if (modulesToAdd.size() > 0) {
                mustCommitChange = true;
                for (CompleteIndexKey key : modulesToAdd) {
                    File f = key.key.file;
                    if (f.exists()) {
                        if (DEBUG) {
                            System.out.println("Indexing: " + f);
                        }
                        try (BufferedReader reader = new BufferedReader(new FileReader(f));) {
                            indexApi.index(createFieldsToIndex(key, tempBuf), reader, FIELD_CONTENTS);
                        } catch (Exception e) {
                            Log.log(e);
                        }
                    }
                }
            }

            Set<Entry<File, Set<CompleteIndexKey>>> entrySet = zipModulesToAdd.entrySet();
            for (Entry<File, Set<CompleteIndexKey>> entry : entrySet) {
                File f = entry.getKey();
                if (f.exists()) {
                    try (ZipFile zipFile = new ZipFile(f, ZipFile.OPEN_READ);) {
                        Set<CompleteIndexKey> value = entry.getValue();
                        for (CompleteIndexKey completeIndexKey2 : value) {
                            ModulesKeyForZip forZip = (ModulesKeyForZip) completeIndexKey2.key;
                            try (InputStream inputStream = zipFile
                                    .getInputStream(zipFile.getEntry(forZip.zipModulePath));) {
                                InputStreamReader reader = new InputStreamReader(inputStream, "utf-8");
                                mustCommitChange = true;
                                if (DEBUG) {
                                    System.out.println("Indexing: " + completeIndexKey2);
                                }
                                indexApi.index(createFieldsToIndex(completeIndexKey2, tempBuf), reader, FIELD_CONTENTS);
                            }
                        }
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }
            }

            incrementAndCheckProgress("Committing result", monitor);
            if (mustCommitChange) {
                if (DEBUG) {
                    System.out.println("Commit result");
                }
                try {
                    indexApi.commit();
                } catch (IOException e) {
                    Log.log(e);
                }
            }

            // Ok, things should be in-place at this point... let's actually do the search now
            incrementAndCheckProgress("Searching index", monitor);

            try {
                if (DEBUG) {
                    System.out.println("Searching: " + fieldNameToValues);
                }
                visitor = new IDocumentsVisitor() {

                    @Override
                    public void visit(DocumentInfo documentInfo) {
                        try {
                            String modKey = documentInfo.get(FIELD_MODULES_KEY_IO);
                            String modTime = documentInfo.get(FIELD_MODIFIED_TIME);
                            if (modKey != null && modTime != null) {
                                ModulesKey fromIO = ModulesKey.fromIO(modKey);
                                CompleteIndexKey existing = currentKeys.get(new CompleteIndexKey(fromIO));
                                // Deal with deleted entries still hanging around.
                                if (existing != null && existing.lastModified == Long.parseLong(modTime)) {
                                    // Ok, we have a match!
                                    ret.add(existing.key);
                                }
                            }
                        } catch (Exception e) {
                            Log.log(e);
                        }
                    }
                };
                indexApi.searchWildcard(fieldNameToValues, applyAllDeletes, visitor, null, FIELD_MODULES_KEY_IO,
                        FIELD_MODIFIED_TIME);
            } catch (Exception e) {
                Log.log(e);
            }
        }
        return ret;
    }

    private void incrementAndCheckProgress(String msg, IProgressMonitor monitor) throws OperationCanceledException {
        // monitor.setTaskName(msg);
        monitor.worked(1);
        if (monitor.isCanceled()) {
            throw new OperationCanceledException();
        }
    }

    public Map<String, String> createFieldsToIndex(CompleteIndexKey key, FastStringBuffer buf) {
        key.key.toIO(buf.clear());
        Map<String, String> fieldsToIndex = new HashMap<>();
        fieldsToIndex.put(FIELD_MODULES_KEY_IO, buf.toString());
        fieldsToIndex.put(FIELD_MODULE_NAME, key.key.name);
        fieldsToIndex.put(FIELD_MODIFIED_TIME, String.valueOf(key.lastModified));
        return fieldsToIndex;
    }

    public void add(Set<CompleteIndexKey> modulesToAdd, Map<File, Set<CompleteIndexKey>> zipModulesToAdd,
            CompleteIndexKey currentModule) {
        if (currentModule.key instanceof ModulesKeyForZip) {
            Set<CompleteIndexKey> set = zipModulesToAdd.get(currentModule.key.file);
            if (set == null) {
                set = new HashSet<>();
                zipModulesToAdd.put(currentModule.key.file, set);
            }
            set.add(currentModule);

        } else {
            modulesToAdd.add(currentModule);
        }
    }

}
