/******************************************************************************
* Copyright (C) 2012  Jonah Graham
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.debug.model;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.shared_core.SharedCorePlugin;

public class PyVariablesPreferences {

    public static final String DEBUG_UI_VARIABLES_SHOW_PRIVATE_REFERENCES = "DEBUG_UI_VARIABLES_SHOW_PRIVATE_REFERENCES";
    public static final boolean DEBUG_UI_VARIABLES_DEFAULT_SHOW_PRIVATE_REFERENCES = true;
    public static final String DEBUG_UI_VARIABLES_SHOW_CAPITALIZED_REFERENCES = "DEBUG_UI_VARIABLES_SHOW_CAPITALIZED_REFERENCES";
    public static final boolean DEBUG_UI_VARIABLES_DEFAULT_SHOW_CAPITALIZED_REFERENCES = true;
    public static final String DEBUG_UI_VARIABLES_SHOW_ALLUPPERCASE_REFERENCES = "DEBUG_UI_VARIABLES_SHOW_ALLUPPERCASE_REFERENCES";
    public static final boolean DEBUG_UI_VARIABLES_DEFAULT_SHOW_ALLUPPERCASE_REFERENCES = true;
    public static final String DEBUG_UI_VARIABLES_SHOW_FUNCTION_AND_MODULE_REFERENCES = "DEBUG_UI_VARIABLES_SHOW_FUNCTION_AND_MODULE_REFERENCES";
    public static final boolean DEBUG_UI_VARIABLES_DEFAULT_SHOW_FUNCTION_AND_MODULE_REFERENCES = true;

    private static boolean getHelper(String key, boolean defaultValue) {
        if (SharedCorePlugin.inTestMode()) {
            return defaultValue;
        }

        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        IPreferenceStore preferenceStore = plugin.getPreferenceStore();
        return preferenceStore.getBoolean(key);
    }

    private static void setHelper(String key, boolean value) {
        if (SharedCorePlugin.inTestMode()) {
            // ignore set
        } else {
            PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
            IPreferenceStore preferenceStore = plugin.getPreferenceStore();
            preferenceStore.setValue(key, value);
        }
    }

    private static void setDefaultHelper(String key, boolean value) {
        if (SharedCorePlugin.inTestMode()) {
            // ignore set
        } else {
            PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
            IPreferenceStore preferenceStore = plugin.getPreferenceStore();
            preferenceStore.setDefault(key, value);
        }
    }

    public static boolean isShowPrivateReferences() {
        return getHelper(PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_PRIVATE_REFERENCES,
                PyVariablesPreferences.DEBUG_UI_VARIABLES_DEFAULT_SHOW_PRIVATE_REFERENCES);
    }

    public static void setShowPrivateReferences(boolean value) {
        setHelper(PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_PRIVATE_REFERENCES, value);
    }

    public static boolean isShowCapitalizedReferences() {
        return getHelper(PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_CAPITALIZED_REFERENCES,
                PyVariablesPreferences.DEBUG_UI_VARIABLES_DEFAULT_SHOW_CAPITALIZED_REFERENCES);
    }

    public static void setShowCapitalizedReferences(boolean value) {
        setHelper(PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_CAPITALIZED_REFERENCES, value);
    }

    public static boolean isShowAllUppercaseReferences() {
        return getHelper(PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_ALLUPPERCASE_REFERENCES,
                PyVariablesPreferences.DEBUG_UI_VARIABLES_DEFAULT_SHOW_ALLUPPERCASE_REFERENCES);
    }

    public static void setShowAllUppercaseReferences(boolean value) {
        setHelper(PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_ALLUPPERCASE_REFERENCES, value);
    }

    public static boolean isShowFunctionAndModuleReferences() {
        return getHelper(PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_FUNCTION_AND_MODULE_REFERENCES,
                PyVariablesPreferences.DEBUG_UI_VARIABLES_DEFAULT_SHOW_FUNCTION_AND_MODULE_REFERENCES);
    }

    public static void setShowFunctionAndModuleReferences(boolean value) {
        setHelper(PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_FUNCTION_AND_MODULE_REFERENCES, value);
    }

    public static void initializeDefaultPreferences() {
        setDefaultHelper(PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_PRIVATE_REFERENCES,
                PyVariablesPreferences.DEBUG_UI_VARIABLES_DEFAULT_SHOW_PRIVATE_REFERENCES);
        setDefaultHelper(PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_CAPITALIZED_REFERENCES,
                PyVariablesPreferences.DEBUG_UI_VARIABLES_DEFAULT_SHOW_CAPITALIZED_REFERENCES);
        setDefaultHelper(PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_ALLUPPERCASE_REFERENCES,
                PyVariablesPreferences.DEBUG_UI_VARIABLES_DEFAULT_SHOW_ALLUPPERCASE_REFERENCES);
        setDefaultHelper(PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_FUNCTION_AND_MODULE_REFERENCES,
                PyVariablesPreferences.DEBUG_UI_VARIABLES_DEFAULT_SHOW_FUNCTION_AND_MODULE_REFERENCES);

    }

    public static void removePropertyChangeListener(IPropertyChangeListener listener) {
        if (SharedCorePlugin.inTestMode()) {
            // ignore remove (we ignored the add too)
        } else {
            PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
            plugin.getPreferenceStore().removePropertyChangeListener(listener);
        }
    }

    public static void addPropertyChangeListener(IPropertyChangeListener listener) {
        if (SharedCorePlugin.inTestMode()) {
            // ignore add
        } else {
            PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
            plugin.getPreferenceStore().addPropertyChangeListener(listener);
        }
    }

}
