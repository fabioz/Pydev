package org.python.pydev.core.cache;

import java.io.File;
import java.io.StringReader;

import org.python.pydev.core.FastBufferedReader;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.ModulesKeyForZip;
import org.python.pydev.core.ObjectsInternPool.ObjectsPoolMap;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;

import junit.framework.TestCase;

public class DiskCacheTest extends TestCase {
    private File baseDir;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        baseDir = FileUtils.getTempFileAt(new File("."), "data_disk_cache_test");
        if (baseDir.exists()) {
            FileUtils.deleteDirectoryTree(baseDir);
        }
        baseDir.mkdir();
    }

    @Override
    public void tearDown() throws Exception {
        if (baseDir.exists()) {
            FileUtils.deleteDirectoryTree(baseDir);
        }
        super.tearDown();
    }

    public void testDiskCacheWithZipModulesKey() throws Exception {
        DiskCache cache = new DiskCache(new File(baseDir, ".cache"), "_test_disk_cache");
        cache.add(new CompleteIndexKey(new ModulesKey("mod1", new File(baseDir, "f1")), 100));
        cache.add(new CompleteIndexKey(new ModulesKey("modnull", null), 100));
        cache.add(new CompleteIndexKey(new ModulesKeyForZip("mod2", new File(baseDir, "my.zip"), "path", true), 100));
        cache.add(new CompleteIndexKey(new ModulesKeyForZip("mod3", new File(baseDir, "my.zip"), "path2", false), 100));

        FastStringBuffer tempBuf = new FastStringBuffer();
        cache.writeTo(tempBuf);

        FastBufferedReader reader = new FastBufferedReader(new StringReader(tempBuf.toString()));
        FastStringBuffer line = reader.readLine(); //
        assertEquals(line.toString(), "-- START DISKCACHE_" + DiskCache.VERSION);
        ObjectsPoolMap objectsPoolMap = new ObjectsPoolMap();
        DiskCache loadFrom = DiskCache.loadFrom(reader, objectsPoolMap);

        assertEquals(cache.keys(), loadFrom.keys());
        assertEquals(cache.getFolderToPersist(), loadFrom.getFolderToPersist());

        CompleteIndexKey mod = cache.keys().get(new CompleteIndexKey("mod2"));
        ModulesKeyForZip zip = (ModulesKeyForZip) mod.key;
        assertEquals(zip.zipModulePath, "path");

        mod = loadFrom.keys().get(new CompleteIndexKey("mod2"));
        zip = (ModulesKeyForZip) mod.key;
        assertEquals(zip.zipModulePath, "path");

        mod = loadFrom.keys().get(new CompleteIndexKey("modnull"));
        assertNull(mod.key.file);

        mod = loadFrom.keys().get(new CompleteIndexKey("mod3"));
        zip = (ModulesKeyForZip) mod.key;
        assertEquals(zip.zipModulePath, "path2");
        assertTrue(zip.isFile);
    }
}
