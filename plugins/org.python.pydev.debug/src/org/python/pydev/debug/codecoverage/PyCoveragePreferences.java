/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.codecoverage;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.shared_core.SharedCorePlugin;

/**
 * @author Fabio Zadrozny
 */
public class PyCoveragePreferences {

    private static boolean internalAllRunsDoCoverage = false;
    private static boolean clearCoverageInfoOnNextLaunch = true;
    private static boolean refreshAfterNextLaunch = true;
    private static IContainer lastChosenDir;
    private static int DEFAULT_NUMBER_OF_COLUMNS_FOR_NAME = 40;

    public static boolean getAllRunsDoCoverage() {
        return getInternalAllRunsDoCoverage() && lastChosenDir != null && lastChosenDir.exists();
    }

    /*default*/static boolean getInternalAllRunsDoCoverage() {
        return internalAllRunsDoCoverage;
    }

    /*default*/static void setInternalAllRunsDoCoverage(boolean selection) {
        internalAllRunsDoCoverage = selection;
    }

    /*default*/static void setRefreshAfterNextLaunch(boolean selection) {
        refreshAfterNextLaunch = selection;
    }

    public static boolean getRefreshAfterNextLaunch() {
        return refreshAfterNextLaunch;
    }

    /*default*/static void setClearCoverageInfoOnNextLaunch(boolean selection) {
        clearCoverageInfoOnNextLaunch = selection;
    }

    public static boolean getClearCoverageInfoOnNextLaunch() {
        return clearCoverageInfoOnNextLaunch;
    }

    /*default*/static void setLastChosenDir(IContainer container) {
        lastChosenDir = container;
    }

    public static IContainer getLastChosenDir() {
        return lastChosenDir;
    }

    public static void setNameNumberOfColumns(int columns) {
        IPreferenceStore preferenceStore = PydevDebugPlugin.getDefault().getPreferenceStore();
        preferenceStore.setValue("PY_COVERAGE_NAME_COLUMNS_TO_USE", columns);
    }

    public static int getNameNumberOfColumns() {
        if (SharedCorePlugin.inTestMode()) {
            return DEFAULT_NUMBER_OF_COLUMNS_FOR_NAME;
        }
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        IPreferenceStore preferenceStore = plugin.getPreferenceStore();
        int i = preferenceStore.getInt("PY_COVERAGE_NAME_COLUMNS_TO_USE");
        if (i <= 5) {
            return DEFAULT_NUMBER_OF_COLUMNS_FOR_NAME;
        }
        if (i > 256) {
            i = 256;
        }
        return i;
    }

}
