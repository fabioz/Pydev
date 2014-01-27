/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.cache;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.python.pydev.core.FastBufferedReader;
import org.python.pydev.core.ObjectsPool;
import org.python.pydev.core.ObjectsPool.ObjectsPoolMap;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * This is a cache that will put its values in the disk for low-memory consumption, so that its size never passes
 * the maxSize specified (so, when retrieving an object from the disk, it might have to store another one before
 * doing so). 
 * 
 * There is a 'catch': its keys must be Strings, as its name will be used as the name of the entry in the disk,
 * so, a 'miss' in memory will try to get it from the disk (and a miss from the disk will mean there is no such key).
 * 
 * -- And yes, the cache itself is Serializable! 
 */
public final class DiskCache implements Serializable {

    /**
     * Updated on 3.1.0: we no longer use hold internal caches.
     */
    private static final long serialVersionUID = 5L;

    private static final boolean DEBUG = false;

    private transient Object lock;

    /**
     * This is the folder that the cache can use to persist its values (TODO: use lucene to index).
     */
    private String folderToPersist;

    /**
     * The keys will be in memory all the time... only the values will come and go to the disk.
     */
    private Map<CompleteIndexKey, CompleteIndexKey> keys = new HashMap<CompleteIndexKey, CompleteIndexKey>();

    /**
     * Custom deserialization is needed.
     */
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream aStream) throws IOException, ClassNotFoundException {
        lock = new Object(); //It's transient, so, we must restore it.
        aStream.defaultReadObject();
        keys = (Map<CompleteIndexKey, CompleteIndexKey>) aStream.readObject();
        folderToPersist = (String) aStream.readObject();

        if (DEBUG) {
            System.out.println("Disk cache - read: " + keys.size() + " - " + folderToPersist);
        }
    }

    /**
     * Writes this cache in a format that may later be restored with loadFrom.
     */
    public void writeTo(FastStringBuffer tempBuf) {
        tempBuf.append("-- START DISKCACHE\n");
        tempBuf.append(folderToPersist);
        tempBuf.append('\n');
        for (CompleteIndexKey key : keys.values()) {
            tempBuf.append(key.key.name);
            tempBuf.append('|');
            tempBuf.append(key.lastModified);
            if (key.key.file != null) {
                tempBuf.append('|');
                tempBuf.append(key.key.file.toString());
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
                                key = new CompleteIndexKey(ObjectsPool.internLocal(objectsPoolMap, buf.toString()));
                                diskCache.add(key);
                                break;
                            case 1:
                                key.lastModified = org.python.pydev.shared_core.string.StringUtils
                                        .parsePositiveLong(buf);
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

                if (buf.length() > 0) {
                    switch (part) {
                        case 1:
                            key.lastModified = StringUtils.parsePositiveLong(buf);
                            break;
                        case 2:
                            //File also written.
                            key.key.file = new File(ObjectsPool.internLocal(objectsPoolMap, buf.toString()));
                            break;

                    }
                    buf.clear();
                }
            }
        }
    }

    /**
     * Custom serialization is needed.
     */
    private void writeObject(ObjectOutputStream aStream) throws IOException {
        synchronized (lock) {
            aStream.defaultWriteObject();
            //write only the keys
            aStream.writeObject(keys);
            //the folder to persist
            aStream.writeObject(folderToPersist);

            //the cache will be re-created in a 'clear' state

            if (DEBUG) {
                System.out.println("Disk cache - write: " + keys.size() + " - " + folderToPersist);
            }
        }
    }

    private DiskCache() {
        //private constructor (only used for internal restore of data).
        lock = new Object(); //It's transient, so, we must restore it.
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
