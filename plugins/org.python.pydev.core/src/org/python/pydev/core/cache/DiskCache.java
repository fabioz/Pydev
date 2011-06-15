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
import java.util.HashMap;
import java.util.Map;

import org.python.pydev.core.REF;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.core.docutils.StringUtils;

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
public final class DiskCache implements Serializable{

    /**
     * Updated on 2.1.1 (fixed issue when restoring deltas: add was not OK.)
     */
    private static final long serialVersionUID = 4L;

    private static final boolean DEBUG = false;
    
    private transient Object lock = new Object();
    
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
    
    private transient LRUCache<CompleteIndexKey, CompleteIndexValue> cache;
    
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
        
        cache = new LRUCache<CompleteIndexKey, CompleteIndexValue>(DISK_CACHE_IN_MEMORY);
        if(DEBUG){
            System.out.println("Disk cache - read: "+keys.size()+ " - "+folderToPersist);
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
            
            if(DEBUG){
                System.out.println("Disk cache - write: "+keys.size()+ " - "+folderToPersist);
            }
        }
    }
    
    public DiskCache(File folderToPersist, String suffix, ICallback<CompleteIndexValue, String> readFromFileMethod, ICallback<String, CompleteIndexValue> toFileMethod) {
        this.cache = new LRUCache<CompleteIndexKey, CompleteIndexValue>(DISK_CACHE_IN_MEMORY);
        this.folderToPersist = REF.getFileAbsolutePath(folderToPersist);
        this.suffix = suffix;
        this.readFromFileMethod = readFromFileMethod;
        this.toFileMethod = toFileMethod;
    }
    
    
    public CompleteIndexValue getObj(CompleteIndexKey key) {
        synchronized(lock){
            CompleteIndexValue v = cache.getObj(key);
            if(v == null && keys.containsKey(key)){
                //miss in memory... get from disk
                File file = getFileForKey(key);
                if(file.exists()){
                    String fileContents = REF.getFileContents(file);
                    v = (CompleteIndexValue) readFromFileMethod.call(fileContents);
                }else{
                    if(DEBUG){
                        System.out.println("File: "+file+" is in the cache but does not exist (so, it will be removed).");
                    }
                }
                if(v == null){
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
        synchronized(lock){
            String name = o.key.name;
            String md5 = StringUtils.md5(name);
            name += "_"+md5.substring(0, 4); //Just add 4 chars to it...
            return new File(folderToPersist, name+suffix);
        }
    }

    /**
     * Removes both: from the memory and from the disk
     */
    public void remove(CompleteIndexKey key) {
        synchronized(lock){
            if(DEBUG){
                System.out.println("Disk cache - Removing: "+key);
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
        synchronized(lock){
            if(n != null){
                cache.add(key, n);
                File fileForKey = getFileForKey(key);
                if(DEBUG){
                    System.out.println("Disk cache - Adding: "+key+" file: "+fileForKey);
                }
                REF.writeStrToFile(toFileMethod.call(n), fileForKey);
            }else{
                if(DEBUG){
                    System.out.println("Disk cache - Adding: "+key+" with empty value (computed on demand).");
                }
            }
            keys.put(key, key);
        }
    }

    /**
     * Clear the whole cache.
     */
    public void clear() {
        synchronized(lock){
            if(DEBUG){
                System.out.println("Disk cache - clear");
            }
            for(CompleteIndexKey key : keys.keySet()){
                cache.remove(key);
                File fileForKey = getFileForKey(key);
                fileForKey.delete();
            }
            keys.clear();
        }        
    }

    /**
     * @return a copy of the keys available 
     */
    public Map<CompleteIndexKey, CompleteIndexKey> keys() {
        synchronized(lock){
            return new HashMap<CompleteIndexKey, CompleteIndexKey>(keys);
        }
    }


    public void setFolderToPersist(String folderToPersist) {
        synchronized(lock){
            File file = new File(folderToPersist);
            if(!file.exists()){
                file.mkdirs();
            }
            if(DEBUG){
                System.out.println("Disk cache - persist :"+folderToPersist);
            }
            this.folderToPersist = folderToPersist;
        }
    }


    public String getFolderToPersist() {
        synchronized(lock){
            return folderToPersist;
        }
    }
}
