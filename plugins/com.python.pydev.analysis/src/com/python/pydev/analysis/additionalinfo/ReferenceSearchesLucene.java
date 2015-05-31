package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.io.IOException;
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.ModulesKeyForZip;
import org.python.pydev.core.cache.CompleteIndexKey;
import org.python.pydev.core.cache.DiskCache;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.ModulesKeyForJava;
import org.python.pydev.shared_core.index.IFields;
import org.python.pydev.shared_core.index.IndexApi;
import org.python.pydev.shared_core.index.IndexApi.DocumentInfo;
import org.python.pydev.shared_core.index.IndexApi.IDocumentsVisitor;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class ReferenceSearchesLucene implements IReferenceSearches {

    private WeakReference<AbstractAdditionalDependencyInfo> abstractAdditionalDependencyInfo;
    private IndexApi indexApi;

    public ReferenceSearchesLucene(AbstractAdditionalDependencyInfo abstractAdditionalDependencyInfo) {
        this.abstractAdditionalDependencyInfo = new WeakReference<>(abstractAdditionalDependencyInfo);
    }

    // These are the indexed fields we use.
    private static String FIELD_MODULES_KEY = "modules_key";
    private static String FIELD_MODIFIED_TIME = IFields.MODIFIED_TIME;
    private static String FIELD_CONTENTS = IFields.GENERAL_CONTENTS;

    @Override
    public synchronized List<ModulesKey> search(IProject project, String token, IProgressMonitor monitor) {
        final List<ModulesKey> ret = new ArrayList<ModulesKey>();
        AbstractAdditionalDependencyInfo abstractAdditionalDependencyInfo = this.abstractAdditionalDependencyInfo.get();
        if (abstractAdditionalDependencyInfo == null) {
            Log.log("AbstractAdditionalDependencyInfo alreeady collected!");
            return ret;
        }

        DiskCache completeIndex = abstractAdditionalDependencyInfo.completeIndex;

        // Note: we should be able to deal with entries already deleted!
        boolean applyAllDeletes = false;

        if (indexApi == null) {
            String folderToPersist = completeIndex.getFolderToPersist();
            try {
                indexApi = new IndexApi(new File(folderToPersist, "lc"), applyAllDeletes);
            } catch (Exception e) {
                Log.log(e);
                return ret;
            }
        }
        Map<CompleteIndexKey, CompleteIndexKey> currentKeys = completeIndex.keys();
        final Map<ModulesKey, CompleteIndexKey> indexMap = new HashMap<>(); // Key to CompleteIndexKey (has modified time).

        IDocumentsVisitor visitor = new IDocumentsVisitor() {

            @Override
            public void visit(DocumentInfo documentInfo) {
                ModulesKey keyFromIO = ModulesKey.fromIO(documentInfo.get(FIELD_MODULES_KEY));
                String modifiedTime = documentInfo.get(FIELD_MODIFIED_TIME);
                indexMap.put(keyFromIO, new CompleteIndexKey(keyFromIO, Long.parseLong(modifiedTime)));
            }
        };
        try {
            indexApi.visitAllDocs(visitor, FIELD_MODULES_KEY, FIELD_MODIFIED_TIME);
        } catch (IOException e) {
            Log.log(e);
        }

        Set<CompleteIndexKey> docsToRemove = new HashSet<>();
        Set<CompleteIndexKey> modulesToAdd = new HashSet<>();

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
                    modulesToAdd.add(currentModule);
                }
            }
        }

        // Step 2: add new entries in current and not in the index
        for (Entry<CompleteIndexKey, CompleteIndexKey> currentEntry : currentKeys.entrySet()) {
            CompleteIndexKey completeIndexKey = currentEntry.getValue();
            if (!indexMap.containsKey(completeIndexKey)) {
                ModulesKey modulesKey = completeIndexKey.key;
                if (modulesKey instanceof ModulesKeyForJava || modulesKey.file == null || !modulesKey.file.isFile()) {
                    //ignore this one (we can't do anything with it).
                    continue;
                }

                modulesToAdd.add(completeIndexKey);
            }
        }

        Map<String, Collection<String>> fieldToValuesToRemove = new HashMap<>();
        Collection<String> lstToRemove = new ArrayList<>(docsToRemove.size());

        FastStringBuffer buf = new FastStringBuffer();
        for (Iterator<CompleteIndexKey> it = docsToRemove.iterator(); it.hasNext();) {
            it.next().key.toIO(buf.clear());
            lstToRemove.add(buf.toString());
        }

        fieldToValuesToRemove.put(FIELD_MODULES_KEY, lstToRemove);
        try {
            indexApi.removeDocs(fieldToValuesToRemove);
        } catch (IOException e) {
            Log.log(e);
        }

        for (CompleteIndexKey key : modulesToAdd) {
            Map<String, String> fieldsToIndex = new HashMap<>();

            indexApi.index(fieldsToIndex, reader, FIELD_CONTENTS);
        }
        return ret;
    }

}
