/**
 * Copyright (c) 2018 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.ruff;

import java.io.File;
import java.net.MalformedURLException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.preferences.PyScopedPreferences;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;

import com.python.pydev.analysis.external.IExternalCodeAnalysisStream;

public class RuffPreferences {

    public static final String RUFF_FILE_LOCATION = "RUFF_FILE_LOCATION";

    public static final String USE_RUFF = "USE_RUFF";
    public static final boolean DEFAULT_USE_RUFF = false;

    public static final String RUFF_USE_CONSOLE = "RUFF_USE_CONSOLE";
    public static final boolean DEFAULT_RUFF_USE_CONSOLE = false;

    public static final String SEARCH_RUFF_LOCATION = "SEARCH_RUFF_LOCATION";
    public static final String LOCATION_SEARCH = "SEARCH";
    public static final String LOCATION_SPECIFY = "SPECIFY";
    public static final String DEFAULT_SEARCH_RUFF_LOCATION = LOCATION_SEARCH;

    public static final String RUFF_ARGS = "RUFF_ARGS";
    public static final String DEFAULT_RUFF_ARGS = "";

    public static boolean useRuff(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getBoolean(USE_RUFF, projectAdaptable);
    }

    public static boolean useRuffConsole(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getBoolean(RUFF_USE_CONSOLE, projectAdaptable);
    }

    public static String getRuffArgs(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getString(RUFF_ARGS, projectAdaptable);
    }

    public static boolean useRuffFromPythonNature(IPythonNature pythonNature, IAdaptable projectAdaptable) {
        if (LOCATION_SPECIFY.equals(PyScopedPreferences.getString(SEARCH_RUFF_LOCATION, projectAdaptable))) {
            return false;
        }
        return true;
    }

    public static File getRuffLocation(PythonNature pythonNature) {
        IProject project = pythonNature.getProject();
        if (LOCATION_SPECIFY.equals(PyScopedPreferences.getString(SEARCH_RUFF_LOCATION, project))) {
            return new File(PyScopedPreferences.getString(RUFF_FILE_LOCATION, project));
        }
        return null;
    }

    public static ICallback<IExternalCodeAnalysisStream, IAdaptable> createRuffStream = ((
            IAdaptable projectAdaptable) -> {
        return null;
    });

    public static IExternalCodeAnalysisStream getConsoleOutputStream(IAdaptable projectAdaptable)
            throws MalformedURLException {
        return createRuffStream.call(projectAdaptable);
    }

}
