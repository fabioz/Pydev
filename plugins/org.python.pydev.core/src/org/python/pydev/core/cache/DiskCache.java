/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.core.FastBufferedReader;
import org.python.pydev.core.ObjectsPool;
import org.python.pydev.core.ObjectsPool.ObjectsPoolMap;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.structure.FastStringBuffer;

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
     * Updated on 2.1.1 (fixed issue when restoring deltas: add was not OK.)
     */
    private static final long serialVersionUID = 4L;

    private static final boolean DEBUG = false;

    private transient Object lock;

    /**
     * Maximum number of modules to have in memory (when reaching that limit, a module will have to be removed
     * before another module is loaded).
     */
    public static final int DISK_CACHE_IN_MEMORY = 100;

    /**
     * This is the folder that the cache can use to persist its values
     */
    private String folderToPersist;

    /**
     * The keys will be in memory all the time... only the values will come and go to the disk.
     */
    private Map<CompleteIndexKey, CompleteIndexKey> keys = new HashMap<CompleteIndexKey, CompleteIndexKey>();

    private transient Cache<CompleteIndexKey, CompleteIndexValue> cache;

    /**
     * The files persisted should have this suffix (should start with .)
     */
    private String suffix;

    /**
     * When serialized, this must be set later on...
     */
    public transient ICallback<CompleteIndexValue, String> readFromFileMethod;

    /**
     * When serialized, this must be set later on...
     */
    public transient ICallback<String, CompleteIndexValue> toFileMethod;

    private transient Job scheduleRemoveStale;

    private class JobRemoveStale extends Job {

        public JobRemoveStale() {
            super("Clear stale references");
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            synchronized (lock) {
                if (cache != null) {
                    cache.removeStaleEntries();
                }
            }
            return Status.OK_STATUS;
        }

    }

    /**
     * Custom deserialization is needed.
     */
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream aStream) throws IOException, ClassNotFoundException {
        lock = new Object(); //It's transient, so, we must restore it.
        aStream.defaultReadObject();
        keys = (Map<CompleteIndexKey, CompleteIndexKey>) aStream.readObject();
        folderToPersist = (String) aStream.readObject();
        suffix = (String) aStream.readObject();

        cache = createCache();
        scheduleRemoveStale = new JobRemoveStale();
        if (DEBUG) {
            System.out.println("Disk cache - read: " + keys.size() + " - " + folderToPersist);
        }
    }

    protected Cache<CompleteIndexKey, CompleteIndexValue> createCache() {
        return new SoftHashMapCache<CompleteIndexKey, CompleteIndexValue>();
        //        return new LRUCache<CompleteIndexKey, CompleteIndexValue>(DISK_CACHE_IN_MEMORY);
    }

    /**
     * Writes this cache in a format that may later be restored with loadFrom.
     */
    public void writeTo(FastStringBuffer tempBuf) {
        tempBuf.append("-- START DISKCACHE\n");
        tempBuf.append(folderToPersist);
        tempBuf.append('\n');
        tempBuf.append(suffix);
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

        line = reader.readLine();
        if (line.startsWith("-- ")) {
            throw new RuntimeException("Unexpected line: " + line);
        }
        diskCache.suffix = line.toString();

        Map<CompleteIndexKey, CompleteIndexKey> diskKeys = diskCache.keys;
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
                                break;
                            case 1:
                                key.lastModified = StringUtils.parsePositiveLong(buf);
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
            //the suffix 
            aStream.writeObject(suffix);

            //the cache will be re-created in a 'clear' state

            if (DEBUG) {
                System.out.println("Disk cache - write: " + keys.size() + " - " + folderToPersist);
            }
        }
    }

    private DiskCache() {
        //private constructor (only used for internal restore of data).
        lock = new Object(); //It's transient, so, we must restore it.
        this.scheduleRemoveStale = new JobRemoveStale();
        this.cache = createCache();
    }

    public DiskCache(File folderToPersist, String suffix, ICallback<CompleteIndexValue, String> readFromFileMethod,
            ICallback<String, CompleteIndexValue> toFileMethod) {
        this();
        this.folderToPersist = REF.getFileAbsolutePath(folderToPersist);
        this.suffix = suffix;
        this.readFromFileMethod = readFromFileMethod;
        this.toFileMethod = toFileMethod;
    }

    /**
     * Returns a tuple with the values in-memory and not in memory.
     * 
     * The first value in the returned tuple contains the keys/values in memory and
     * the second contains a list of the values not in memory 
     */
    public Tuple<List<Tuple<CompleteIndexKey, CompleteIndexValue>>, Collection<CompleteIndexKey>> getInMemoryInfo() {
        synchronized (lock) {
            List<Tuple<CompleteIndexKey, CompleteIndexValue>> ret0 = new ArrayList<Tuple<CompleteIndexKey, CompleteIndexValue>>();
            List<CompleteIndexKey> ret1 = new ArrayList<CompleteIndexKey>();

            //Note: no need to iterate in a copy since we're with the lock access.

            //Important: MUST iterate in the values, as the key may have the outdated values (i.e.: even though it's
            //a map val=val, the val that represents the 'key' may not be updated).
            for (CompleteIndexKey key : keys.values()) {
                CompleteIndexValue value = cache.getObj(key);
                if (value != null) {
                    ret0.add(new Tuple<CompleteIndexKey, CompleteIndexValue>(key, value));
                } else {
                    ret1.add(key);
                }
            }
            scheduleRemoveStale();
            return new Tuple<List<Tuple<CompleteIndexKey, CompleteIndexValue>>, Collection<CompleteIndexKey>>(ret0,
                    ret1);
        }
    }

    public CompleteIndexValue getObj(CompleteIndexKey key) {
        synchronized (lock) {
            scheduleRemoveStale();
            CompleteIndexValue v = cache.getObj(key);
            if (v == null && keys.containsKey(key)) {
                //miss in memory... get from disk
                File file = getFileForKey(key);
                if (file.exists()) {
                    String fileContents = REF.getFileContents(file);
                    v = (CompleteIndexValue) readFromFileMethod.call(fileContents);
                } else {
                    if (DEBUG) {
                        System.out.println("File: " + file
                                + " is in the cache but does not exist (so, it will be removed).");
                    }
                }
                if (v == null) {
                    this.remove(key);
                    return null;
                }
                //put it back in memory
                cache.add(key, v);
            }
            return v;
        }
    }

    private File getFileForKey(CompleteIndexKey o) {
        synchronized (lock) {
            String name = o.key.name;
            String md5 = StringUtils.md5(name);
            name += "_" + md5.substring(0, 4); //Just add 4 chars to it...
            return new File(folderToPersist, name + suffix);
        }
    }

    /**
     * Removes both: from the memory and from the disk
     */
    public void remove(CompleteIndexKey key) {
        synchronized (lock) {
            scheduleRemoveStale();
            if (DEBUG) {
                System.out.println("Disk cache - Removing: " + key);
            }
            cache.remove(key);
            File fileForKey = getFileForKey(key);
            fileForKey.delete();
            keys.remove(key);
        }
    }

    /**
     * Adds to both: the memory and the disk
     */
    public void add(CompleteIndexKey key, CompleteIndexValue n) {
        synchronized (lock) {
            scheduleRemoveStale();
            if (n != null) {
                cache.add(key, n);
                File fileForKey = getFileForKey(key);
                if (DEBUG) {
                    System.out.println("Disk cache - Adding: " + key + " file: " + fileForKey);
                }
                REF.writeStrToFile(toFileMethod.call(n), fileForKey);
            } else {
                if (DEBUG) {
                    System.out.println("Disk cache - Adding: " + key + " with empty value (computed on demand).");
                }
            }
            keys.put(key, key);
        }
    }

    protected void scheduleRemoveStale() {
        this.scheduleRemoveStale.schedule(1000);
    }

    /**
     * Clear the whole cache.
     */
    public void clear() {
        synchronized (lock) {
            if (DEBUG) {
                System.out.println("Disk cache - clear");
            }
            for (CompleteIndexKey key : keys.keySet()) {
                File fileForKey = getFileForKey(key);
                fileForKey.delete();
            }
            keys.clear();
            cache.clear();
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
