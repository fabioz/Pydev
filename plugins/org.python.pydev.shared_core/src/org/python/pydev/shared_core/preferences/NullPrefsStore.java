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

    @Override
    public void addPropertyChangeListener(IPropertyChangeListener listener) {

    }

    @Override
    public boolean contains(String name) {

        return false;
    }

    @Override
    public void firePropertyChangeEvent(String name, Object oldValue, Object newValue) {

    }

    @Override
    public boolean getBoolean(String name) {

        return false;
    }

    @Override
    public boolean getDefaultBoolean(String name) {

        return false;
    }

    @Override
    public double getDefaultDouble(String name) {

        return 0;
    }

    @Override
    public float getDefaultFloat(String name) {

        return 0;
    }

    @Override
    public int getDefaultInt(String name) {

        return 0;
    }

    @Override
    public long getDefaultLong(String name) {

        return 0;
    }

    @Override
    public String getDefaultString(String name) {

        return null;
    }

    @Override
    public double getDouble(String name) {

        return 0;
    }

    @Override
    public float getFloat(String name) {

        return 0;
    }

    @Override
    public int getInt(String name) {
        Object val = nameToVal.get(name);
        if (val != null) {
            return (int) val;
        }
        return 0;
    }

    @Override
    public long getLong(String name) {

        return 0;
    }

    @Override
    public String getString(String name) {

        return null;
    }

    @Override
    public boolean isDefault(String name) {

        return false;
    }

    @Override
    public boolean needsSaving() {

        return false;
    }

    @Override
    public void putValue(String name, String value) {

    }

    @Override
    public void removePropertyChangeListener(IPropertyChangeListener listener) {

    }

    @Override
    public void setDefault(String name, double value) {

    }

    @Override
    public void setDefault(String name, float value) {

    }

    @Override
    public void setDefault(String name, int value) {

    }

    @Override
    public void setDefault(String name, long value) {

    }

    @Override
    public void setDefault(String name, String defaultObject) {

    }

    @Override
    public void setDefault(String name, boolean value) {

    }

    @Override
    public void setToDefault(String name) {

    }

    @Override
    public void setValue(String name, double value) {

    }

    @Override
    public void setValue(String name, float value) {

    }

    @Override
    public void setValue(String name, int value) {
        this.nameToVal.put(name, value);
    }

    @Override
    public void setValue(String name, long value) {

    }

    @Override
    public void setValue(String name, String value) {

    }

    @Override
    public void setValue(String name, boolean value) {

    }

}
