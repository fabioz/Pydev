package org.python.pydev.ast.codecompletion.revisited;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.python.pydev.ast.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IModule;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.cache.LRUMap;
import org.python.pydev.shared_core.string.FullRepIterable;

public class CachePyiModules {

    private static class CacheValue {

        private File pyInterfaceFile;
        private long lastModified;
        private SourceModule sourceModule;

        public CacheValue(File pyInterfaceFile, long lastModified, SourceModule ret) {
            this.pyInterfaceFile = pyInterfaceFile;
            this.lastModified = lastModified;
            this.sourceModule = ret;
        }

        public SourceModule get() {
            return sourceModule;
        }

        public boolean isValid() {
            return this.pyInterfaceFile.lastModified() == this.lastModified;
        }

    }

    private final Object cachePyiModulesLock = new Object();
    private final Map<File, CacheValue> cache = new LRUMap<>(50);

    public void clear() {
        synchronized (cachePyiModulesLock) {
            cache.clear();
        }
    }

    private static final IGrammarVersionProvider LATEST_PY3_GRAMMAR_VERSION_PROVIDER = new IGrammarVersionProvider() {

        @Override
        public int getGrammarVersion() throws MisconfigurationException {
            return IGrammarVersionProvider.LATEST_GRAMMAR_PY3_VERSION;
        }

        @Override
        public AdditionalGrammarVersionsToCheck getAdditionalGrammarVersions()
                throws MisconfigurationException {
            return null;
        }
    };

    public SourceModule getPyiStubModule(IModule module) {
        File file = module.getFile();
        if (file != null) {
            File parentFile = file.getParentFile();
            String definitionFileName = file.getName();
            File pyInterfaceFile = new File(parentFile,
                    FullRepIterable.getWithoutLastPart(definitionFileName) + ".pyi");
            CacheValue cacheValue;
            synchronized (cachePyiModulesLock) {
                cacheValue = cache.get(pyInterfaceFile);
            }
            if (cacheValue != null && cacheValue.isValid()) {
                return cacheValue.get();
            }
            if (pyInterfaceFile.exists()) {
                try {
                    long lastModified = pyInterfaceFile.lastModified();
                    SourceModule ret = SourceModule.createModuleFromDoc(module.getName(), pyInterfaceFile,
                            FileUtilsFileBuffer.getDocFromFile(pyInterfaceFile), module.getNature(), false,
                            LATEST_PY3_GRAMMAR_VERSION_PROVIDER);
                    synchronized (cachePyiModulesLock) {
                        cache.put(pyInterfaceFile, new CacheValue(pyInterfaceFile, lastModified, ret));
                    }
                    return ret;
                } catch (MisconfigurationException | IOException e) {
                    Log.log(e);
                }
            }
        }
        return null;
    }

}
