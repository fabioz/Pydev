/*
 * Created on Apr 9, 2006
 */
package org.python.pydev.core;

import java.lang.ref.WeakReference;
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
 * 
 * Note: should be safe to use in a threaded environment.
 */
public class ObjectsPool<S> {

    private Map<S, WeakReference<S>> weakHashMap = new WeakHashMap<S, WeakReference<S>>();
    
    /**
     * Returns an object equal to the one passed as a parameter and puts it in the pool
     * 
     * E.g.: If an integer with the value 1 is requested, it will se if that value already exists and return it.
     * If it doesn't exist, the parameter itself will be put in the pool.
     */
    @SuppressWarnings("unchecked")
	public synchronized S getFromPool(S o){
    	synchronized(weakHashMap){
            WeakReference<S> w = (WeakReference<S>)weakHashMap.get(o);
            if(w == null){
            	//garbage collected or still not there...
            	weakHashMap.put(o, new WeakReference<S>(o));
            	return o;
            	
            }else{
            	final S ret = w.get();
            	if(ret == null && o != null){
            		//garbage collected just in time hum?
            		weakHashMap.put(o, new WeakReference<S>(o));
            		return o;
            		
            	}else{
            		return ret;
            	}
            }
    	}
    }
}
