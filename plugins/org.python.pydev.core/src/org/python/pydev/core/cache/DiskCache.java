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
import java.util.HashSet;
import java.util.Set;

import org.python.pydev.core.REF;
import org.python.pydev.core.callbacks.ICallback;

/**
 * This is a cache that will put its values in the disk for low-memory consumption, so that its size never passses
 * the maxSize specified (so, when retrieving an object from the disk, it might have to store another one before
 * doing so). 
 * 
 * There is a 'catch': its keys must be Strings, as its name will be used as the name of the entry in the disk,
 * so, a 'miss' in memory will try to get it from the disk (and a miss from the disk will mean there is no such key).
 * 
 * -- And yes, the cache itself is Serializable! 
 */
public final class DiskCache<X> extends LRUCache<String, X> implements Serializable{

    private static final long serialVersionUID = 1L;

    private static final boolean DEBUG = false;
    
    private transient Object lock = new Object();
    
    /**
     * This is the folder that the cache can use to persist its values
     */
    private String folderToPersist;
    
    /**
     * The keys will be in memory all the time... only the values will come and go to the disk.
     */
    private Set<String> keys = new HashSet<String>();
    
    /**
     * The files persisted should have this suffix (should start with .)
     */
    private String suffix;

    /**
     * When serialized, this must be set later on...
     */
    public transient ICallback<X, String> readFromFileMethod;

    /**
     * When serialized, this must be set later on...
     */
    public transient ICallback<String, X> toFileMethod;

    /**
     * Custom deserialization is needed.
     */
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream aStream) throws IOException, ClassNotFoundException {
        lock = new Object(); //It's transient, so, we must restore it.
        aStream.defaultReadObject();
        keys = (Set<String>) aStream.readObject();
        folderToPersist = (String) aStream.readObject();
        suffix = (String) aStream.readObject();
        maxSize = aStream.readInt();
        
        //and re-create the map itself.
        cache = createMap(maxSize);
    }

    /**
     * Custom serialization is needed.
     */
    private void writeObject(ObjectOutputStream aStream) throws IOException {
        synchronized (lock) {
            aStream.defaultWriteObject();
            //write only the keys
            aStream.writeObject(keys());
            //the folder to persist
            aStream.writeObject(folderToPersist);
            //the suffix 
            aStream.writeObject(suffix);
            //and the maxSize 
            aStream.writeInt(maxSize);
            
            //the cache will be re-created in a 'clear' state
        }
    }
    
    public DiskCache(int maxSize, File folderToPersist, String suffix, ICallback<X, String> readFromFileMethod, ICallback<String, X> toFileMethod) {
        super(maxSize);
        this.folderToPersist = REF.getFileAbsolutePath(folderToPersist);
        this.suffix = suffix;
        this.readFromFileMethod = readFromFileMethod;
        this.toFileMethod = toFileMethod;
    }
    
    
    public X getObj(String key) {
        synchronized(lock){
            X v = super.getObj(key);
            if(v == null && keys.contains(key)){
                //miss in memory... get from disk
                File file = getFileForKey(key);
                if(file.exists()){
                    String fileContents = REF.getFileContents(file);
                    v = (X) readFromFileMethod.call(fileContents);
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
                super.add(key, v);
            }
            return v;
        }
    }

    private File getFileForKey(String o) {
        synchronized(lock){
            return new File(folderToPersist, o+suffix);
        }
    }

    /**
     * Removes both: from the memory and from the disk
     */
    public void remove(String key) {
        synchronized(lock){
            if(DEBUG){
                System.out.println("Disk cache - Removing: "+key);
            }
            super.remove(key);
            File fileForKey = getFileForKey(key);
            fileForKey.delete();
            keys.remove(key);
        }
    }

    /**
     * Adds to both: the memory and the disk
     */
    public void add(String key, X n) {
        synchronized(lock){
            super.add(key, n);
            File fileForKey = getFileForKey(key);
            if(DEBUG){
                System.out.println("Disk cache - Adding: "+key+" file: "+fileForKey);
            }
            REF.writeStrToFile(toFileMethod.call(n), fileForKey);
            keys.add(key);
        }
    }

    /**
     * Clear the whole cache.
     */
    public void clear() {
        synchronized(lock){
            for(String key : keys){
                super.remove(key);
                File fileForKey = getFileForKey(key);
                fileForKey.delete();
            }
            keys.clear();
        }        
    }

    /**
     * @return a copy of the keys available 
     */
    public Set<String> keys() {
        synchronized(lock){
            return new HashSet<String>(keys);
        }
    }


    public void setFolderToPersist(String folderToPersist) {
        synchronized(lock){
            this.folderToPersist = folderToPersist;
        }
    }


    public String getFolderToPersist() {
        synchronized(lock){
            return folderToPersist;
        }
    }
}
