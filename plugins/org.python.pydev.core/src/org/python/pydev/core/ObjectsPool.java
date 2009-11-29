/*
 * Created on Apr 9, 2006
 */
package org.python.pydev.core;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * This pool is to be regarded as a way to have less object instances for a given class,
 * so, if you have tons of equal strings, you could pass them here and make them be the same
 * to save memory. Note that it is created with weak-references for both, the key and the value,
 * so, it should be safe to assume that it will be available for garbage collecting once
 * no other place has a reference to the same string.
 * 
 * Still, use this with care...
 */
public final class ObjectsPool {
    
    private ObjectsPool(){
    }

    private static Map<String, WeakReference<String>> weakHashMap = new WeakHashMap<String, WeakReference<String>>();
    
    /**
     * This is a way to intern a String in the regular heap (instead of the String.intern which uses the perm-gen).
     */
    public static String intern(String o){
        synchronized(weakHashMap){
            WeakReference<String> w = (WeakReference<String>)weakHashMap.get(o);
            if(w == null){
                //garbage collected or still not there...
                weakHashMap.put(o, new WeakReference<String>(o));
                return o;
                
            }else{
                final String ret = w.get();
                if(ret == null && o != null){
                    //garbage collected just in time hum?
                    weakHashMap.put(o, new WeakReference<String>(o));
                    return o;
                    
                }else{
                    return ret;
                }
            }
        }
    }
    
    /**
     * Class used to store items interned locally in a map (without weak references)
     */
    public static class ObjectsPoolMap extends HashMap<String, String>{

        private static final long serialVersionUID = 1L;
        
    }
    
    
    /**
     * Makes an intern unsynched and without weak-references in the passed map.
     * Use when creating strings in objects that generate strings and when the map with
     * the strings will be garbage-collected. 
     * 
     * This is a balance from the regular intern which uses weak references and is global to
     * a local one that is faster (unsynched and doesn't use weak references).
     */
    public static String internLocal(ObjectsPoolMap mapWithInternedStrings, String string){
        String existing = mapWithInternedStrings.get(string);
        if(existing != null){
            return existing;
        }
        mapWithInternedStrings.put(string, string);
        return string;

    }
}
