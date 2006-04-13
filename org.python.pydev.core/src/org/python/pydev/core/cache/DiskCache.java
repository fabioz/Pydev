package org.python.pydev.core.cache;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.python.pydev.core.REF;

/**
 * This is a cache that will put its values in the disk for low-memory consumption, so that its size never passses
 * the maxSize specified (so, when retrieving an object from the disk, it might have to store another one before
 * doing so). 
 * 
 * There is a 'catch': its keys must be Strings, as its name will be used as the name of the entry in the disk,
 * so, a 'miss' in memory will try to get it from the disk (and a miss from the disk will mean there is no such key).
 * 
 * -- And yes, the cache itself is serializable! 
 */
public class DiskCache extends LRUCache<String, Serializable> implements Serializable{

	private static final long serialVersionUID = 1L;
	
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
     * Custom deserialization is needed.
     */
    @SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream aStream) throws IOException, ClassNotFoundException {
    	
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
	
	public DiskCache(int maxSize, File folderToPersist, String suffix) {
		super(maxSize);
		this.folderToPersist = REF.getFileAbsolutePath(folderToPersist);
		this.suffix = suffix;
	}
	
	
	public synchronized Serializable getObj(String key) {
		synchronized(cache){
			Serializable v = super.getObj(key);
			if(v == null && keys.contains(key)){
				//miss in memory... get from disk
				File file = getFileForKey(key);
                v = (Serializable) REF.readFromFile(file);
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

	private synchronized File getFileForKey(String o) {
		return new File(folderToPersist, o+suffix);
	}

	/**
	 * Removes both: from the memory and from the disk
	 */
	public synchronized void remove(String key) {
		synchronized(cache){
			super.remove(key);
			File fileForKey = getFileForKey(key);
			fileForKey.delete();
			keys.remove(key);
		}
	}

	/**
	 * Adds to both: the memory and the disk
	 */
	public synchronized void add(String key, Serializable n) {
		synchronized(cache){
			super.add(key, n);
			File fileForKey = getFileForKey(key);
			REF.writeToFile(n, fileForKey);
			keys.add(key);
		}
	}

	/**
	 * Clear the whole cache.
	 */
	public synchronized void clear() {
		synchronized(cache){
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
	public synchronized Set<String> keys() {
		synchronized(cache){
			return new HashSet<String>(keys);
		}
	}


	public void setFolderToPersist(String folderToPersist) {
		this.folderToPersist = folderToPersist;
	}


	public String getFolderToPersist() {
		return folderToPersist;
	}
}
