/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.cache;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.python.pydev.core.FastBufferedReader;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.ModulesKeyForZip;
import org.python.pydev.core.ObjectsInternPool;
import org.python.pydev.core.ObjectsInternPool.ObjectsPoolMap;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * All this cache does is keep a map with the names of the modules we know and its last modification time.
 * Afterwards it's used to check if our indexes are consistent by comparing with the actual filesystem data.
 */
public final class DiskCache {

    private static final boolean DEBUG = false;

    public static final int VERSION = 2;

    private final Object lock = new Object();

    /**
     * This is the folder that the cache can use to persist its values (TODO: use lucene to index).
     */
    private String folderToPersist;

    /**
     * The keys will be in memory all the time... only the values will come and go to the disk.
     */
    private Map<CompleteIndexKey, CompleteIndexKey> keys = new HashMap<CompleteIndexKey, CompleteIndexKey>();

    /**
     * Writes this cache in a format that may later be restored with loadFrom.
     */
    public void writeTo(FastStringBuffer tempBuf) {
        tempBuf.append("-- START DISKCACHE_" + DiskCache.VERSION + "\n");
        tempBuf.append(folderToPersist);
        tempBuf.append('\n');
        for (CompleteIndexKey key : keys.values()) {
            ModulesKey modKey = key.key;
            tempBuf.append(modKey.name);
            tempBuf.append('|');
            tempBuf.append(key.lastModified);
            tempBuf.append('|');
            if (modKey.file != null) {
                tempBuf.append(modKey.file.toString());
            } else {
                //could be null!
            }
            if (modKey instanceof ModulesKeyForZip) {
                ModulesKeyForZip modulesKeyForZip = (ModulesKeyForZip) modKey;
                tempBuf.append('|');
                tempBuf.append(modulesKeyForZip.zipModulePath);
                tempBuf.append('|');
                tempBuf.append(modulesKeyForZip.isFile ? '0' : '1');
            }
            tempBuf.append('\n');
        }
        tempBuf.append("-- END DISKCACHE\n");
    }

    /**
     * Loads from a reader a string that was acquired from writeTo.
     * @param objectsPoolMap
     */
    public static DiskCache loadFrom(FastBufferedReader reader, ObjectsPoolMap objectsPoolMap) throws IOException {
        DiskCache diskCache = new DiskCache();

        FastStringBuffer line = reader.readLine();
        if (line.startsWith("-- ")) {
            throw new RuntimeException("Unexpected line: " + line);
        }
        diskCache.folderToPersist = line.toString();

        FastStringBuffer buf = new FastStringBuffer();
        CompleteIndexKey key = null;
        char[] internalCharsArray = line.getInternalCharsArray();
        while (true) {
            line = reader.readLine();
            key = null;
            if (line == null || line.startsWith("-- ")) {
                if (line != null && line.startsWith("-- END DISKCACHE")) {
                    return diskCache;
                }
                throw new RuntimeException("Unexpected line: " + line);
            } else {
                int length = line.length();
                int part = 0;
                for (int i = 0; i < length; i++) {
                    char c = internalCharsArray[i];
                    if (c == '|') {
                        switch (part) {
                            case 0:
                                key = new CompleteIndexKey(
                                        ObjectsInternPool.internLocal(objectsPoolMap, buf.toString()));
                                diskCache.add(key);
                                break;
                            case 1:
                                key.lastModified = org.python.pydev.shared_core.string.StringUtils
                                        .parsePositiveLong(buf);
                                break;
                            case 2:
                                if (buf.length() > 0) {
                                    key.key.file = new File(
                                            ObjectsInternPool.internLocal(objectsPoolMap, buf.toString()));
                                }
                                break;
                            case 3:
                                //path in zip
                                key.key = new ModulesKeyForZip(key.key.name, key.key.file,
                                        ObjectsInternPool.internLocal(objectsPoolMap, buf.toString()), true);
                                break;
                            case 4:
                                //isfile in zip
                                if (buf.toString().equals(0)) {
                                    ((ModulesKeyForZip) key.key).isFile = true;
                                }
                                break;
                            default:
                                throw new RuntimeException("Unexpected part in line: " + line);
                        }
                        part++;
                        buf.clear();
                    } else {
                        buf.append(c);
                    }
                }

                // Found end of line... this is the last part and depends on where we stopped previously.
                if (buf.length() > 0) {
                    switch (part) {
                        case 1:
                            key.lastModified = StringUtils.parsePositiveLong(buf);
                            break;
                        case 2:
                            //File also written.
                            key.key.file = new File(ObjectsInternPool.internLocal(objectsPoolMap, buf.toString()));
                            break;
                        case 3:
                            //path in zip
                            key.key = new ModulesKeyForZip(key.key.name, key.key.file,
                                    ObjectsInternPool.internLocal(objectsPoolMap, buf.toString()), true);
                            break;
                        case 4:
                            //isfile in zip
                            if (buf.toString().equals(0)) {
                                ((ModulesKeyForZip) key.key).isFile = true;
                            }
                            break;
                    }
                    buf.clear();
                }
            }
        }
    }

    private DiskCache() {
        //private constructor (only used for internal restore of data).
    }

    public DiskCache(File folderToPersist, String suffix) {
        this();
        this.folderToPersist = FileUtils.getFileAbsolutePath(folderToPersist);
    }

    /**
     * Removes both: from the memory and from the disk
     */
    public void remove(CompleteIndexKey key) {
        synchronized (lock) {
            if (DEBUG) {
                System.out.println("Disk cache - Removing: " + key);
            }
            keys.remove(key);
        }
    }

    /**
     * Adds to both: the memory and the disk
     */
    public void add(CompleteIndexKey key) {
        synchronized (lock) {
            if (DEBUG) {
                System.out.println("Disk cache - Adding: " + key);
            }
            keys.put(key, key);
        }
    }

    /**
     * Clear the whole cache.
     */
    public void clear() {
        synchronized (lock) {
            if (DEBUG) {
                System.out.println("Disk cache - clear");
            }
            keys.clear();
        }
    }

    /**
     * @return a copy of the keys available
     */
    public Map<CompleteIndexKey, CompleteIndexKey> keys() {
        synchronized (lock) {
            return new HashMap<CompleteIndexKey, CompleteIndexKey>(keys);
        }
    }

    public void setFolderToPersist(String folderToPersist) {
        synchronized (lock) {
            File file = new File(folderToPersist);
            if (!file.exists()) {
                file.mkdirs();
            }
            if (DEBUG) {
                System.out.println("Disk cache - persist :" + folderToPersist);
            }
            this.folderToPersist = folderToPersist;
        }
    }

    public String getFolderToPersist() {
        synchronized (lock) {
            return folderToPersist;
        }
    }

}
