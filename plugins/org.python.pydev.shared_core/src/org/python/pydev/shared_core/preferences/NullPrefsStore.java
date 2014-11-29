/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.preferences;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;

/**
 * Stub that does nothing (same idea of a NullProgressMonitor)
 */
public class NullPrefsStore implements IPreferenceStore {

    Map<String, Object> nameToVal = new HashMap<>();

    public void addPropertyChangeListener(IPropertyChangeListener listener) {

    }

    public boolean contains(String name) {

        return false;
    }

    public void firePropertyChangeEvent(String name, Object oldValue, Object newValue) {

    }

    public boolean getBoolean(String name) {

        return false;
    }

    public boolean getDefaultBoolean(String name) {

        return false;
    }

    public double getDefaultDouble(String name) {

        return 0;
    }

    public float getDefaultFloat(String name) {

        return 0;
    }

    public int getDefaultInt(String name) {

        return 0;
    }

    public long getDefaultLong(String name) {

        return 0;
    }

    public String getDefaultString(String name) {

        return null;
    }

    public double getDouble(String name) {

        return 0;
    }

    public float getFloat(String name) {

        return 0;
    }

    public int getInt(String name) {
        Object val = nameToVal.get(name);
        if (val != null) {
            return (int) val;
        }
        return 0;
    }

    public long getLong(String name) {

        return 0;
    }

    public String getString(String name) {

        return null;
    }

    public boolean isDefault(String name) {

        return false;
    }

    public boolean needsSaving() {

        return false;
    }

    public void putValue(String name, String value) {

    }

    public void removePropertyChangeListener(IPropertyChangeListener listener) {

    }

    public void setDefault(String name, double value) {

    }

    public void setDefault(String name, float value) {

    }

    public void setDefault(String name, int value) {

    }

    public void setDefault(String name, long value) {

    }

    public void setDefault(String name, String defaultObject) {

    }

    public void setDefault(String name, boolean value) {

    }

    public void setToDefault(String name) {

    }

    public void setValue(String name, double value) {

    }

    public void setValue(String name, float value) {

    }

    public void setValue(String name, int value) {
        this.nameToVal.put(name, value);
    }

    public void setValue(String name, long value) {

    }

    public void setValue(String name, String value) {

    }

    public void setValue(String name, boolean value) {

    }

}
