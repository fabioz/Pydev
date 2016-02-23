/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.cache;

import java.util.HashMap;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

public class PyPreferencesCache implements IPropertyChangeListener {

    private IPreferenceStore preferenceStore;
    private HashMap<String, Object> cache = new HashMap<String, Object>();

    public PyPreferencesCache(IPreferenceStore preferenceStore) {
        this.preferenceStore = preferenceStore;
        this.preferenceStore.addPropertyChangeListener(this);
    }

    public boolean getBoolean(String key) {
        Boolean b = (Boolean) cache.get(key);
        if (b == null) {
            b = this.preferenceStore.getBoolean(key);
            cache.put(key, b);
        }
        return b;
    }

    /**
     * This is for a 'special case', when the value must be higher than 0
     *  
     * @param key this is the key we're interested in
     * @param defaultIfZeroOrLess the value to be returned if the actual value found is 0 or less
     */
    public int getInt(String key, int defaultIfZeroOrLess) {
        Integer b = (Integer) cache.get(key);

        if (b == null || b <= 0) {
            b = this.preferenceStore.getInt(key);

            if (b <= 0) {
                b = defaultIfZeroOrLess;
            }
            cache.put(key, b);
        }

        return b;
    }

    public int getInt(String key) {
        Integer b = (Integer) cache.get(key);
        if (b == null) {
            b = this.preferenceStore.getInt(key);
            cache.put(key, b);
        }
        return b;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        final Object newValue = event.getNewValue();
        cache.put(event.getProperty(), newValue); //simply override the cache (do not care about whether it is null, Boolean, etc).
    }

    /**
     * Can be used to force clearing some value from the cache.
     */
    public void clear(String key) {
        cache.put(key, null);
    }

}
