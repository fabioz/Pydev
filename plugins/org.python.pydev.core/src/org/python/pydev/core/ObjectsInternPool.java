/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
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
public final class ObjectsInternPool {

    private ObjectsInternPool() {
    }

    private static final Map<String, WeakReference<String>> weakHashMap = new WeakHashMap<String, WeakReference<String>>();
    public static final Object lock = new Object();

    /**
     * This is a way to intern a String in the regular heap (instead of the String.intern which uses the perm-gen).
     */
    public static String intern(String o) {
        if (o == null) {
            return null;
        }
        synchronized (lock) {
            WeakReference<String> w = (WeakReference<String>) weakHashMap.get(o);
            if (w == null) {
                //Yes, the String constructor will do things properly, so, if a big string is actually backed up by the one
                //passed, it'll create a new array only with the parts we want.
                o = new String(o);
                //garbage collected or still not there...
                weakHashMap.put(o, new WeakReference<String>(o));
                return o;

            } else {
                final String ret = w.get();
                if (ret == null && o != null) {
                    //garbage collected just in time hum?
                    o = new String(o);
                    weakHashMap.put(o, new WeakReference<String>(o));
                    return o;

                } else {
                    return ret;
                }
            }
        }
    }

    /**
     * Same thing as intern, but the client is responsible for synchronizing on the lock object of this class!
     * 
     * Note that this should be done on a fast process where many objects will be added (but only on fast processes
     * that want to avoid synchronizing at each step).
     */
    public static String internUnsynched(String o) {
        if (o == null) {
            return null;
        }
        WeakReference<String> w = (WeakReference<String>) weakHashMap.get(o);
        if (w == null) {
            //Yes, the String constructor will do things properly, so, if a big string is actually backed up by the one
            //passed, it'll create a new array only with the parts we want.
            o = new String(o);
            //garbage collected or still not there...
            weakHashMap.put(o, new WeakReference<String>(o));
            return o;

        } else {
            final String ret = w.get();
            if (ret == null && o != null) {
                //garbage collected just in time hum?
                o = new String(o);
                weakHashMap.put(o, new WeakReference<String>(o));
                return o;

            } else {
                return ret;
            }
        }
    }

    /**
     * Class used to store items interned locally in a map (without weak references)
     */
    public static final class ObjectsPoolMap extends HashMap<String, String> {

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
    public static String internLocal(ObjectsPoolMap mapWithInternedStrings, String string) {
        String existing = mapWithInternedStrings.get(string);
        if (existing != null) {
            return existing;
        }
        mapWithInternedStrings.put(string, string);
        return string;

    }
}
