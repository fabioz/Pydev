/**
 * Copyright (c) 2021 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.flake8;

import java.io.File;
import java.net.MalformedURLException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.preferences.PyScopedPreferences;
import org.python.pydev.core.preferences.PydevPrefs;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;

import com.python.pydev.analysis.external.IExternalCodeAnalysisStream;

public class Flake8Preferences {

    public static final String FLAKE8_FILE_LOCATION = "FLAKE8_FILE_LOCATION";

    public static final String USE_FLAKE8 = "USE_FLAKE8";
    public static final boolean DEFAULT_USE_FLAKE8 = false;

    public static final int SEVERITY_IGNORE = -1;

    public static final String FLAKE8_E_SEVERITY = "PYFLAKES_E_SEVERITY";
    public static final int DEFAULT_FLAKE8_E_SEVERITY = IMarker.SEVERITY_ERROR;

    public static final String FLAKE8_F_SEVERITY = "PYFLAKES_F_SEVERITY";
    public static final int DEFAULT_FLAKE8_F_SEVERITY = IMarker.SEVERITY_ERROR;

    public static final String FLAKE8_W_SEVERITY = "PYFLAKES_W_SEVERITY";
    public static final int DEFAULT_FLAKE8_W_SEVERITY = IMarker.SEVERITY_WARNING;

    public static final String FLAKE8_C_SEVERITY = "PYFLAKES_C_SEVERITY";
    public static final int DEFAULT_FLAKE8_C_SEVERITY = IMarker.SEVERITY_INFO;

    public static final String FLAKE8_USE_CONSOLE = "FLAKE8_USE_CONSOLE";
    public static final boolean DEFAULT_FLAKE8_USE_CONSOLE = false;

    public static final String SEARCH_FLAKE8_LOCATION = "SEARCH_FLAKE8_LOCATION";
    public static final String LOCATION_SEARCH = "SEARCH";
    public static final String LOCATION_SPECIFY = "SPECIFY";
    public static final String DEFAULT_SEARCH_FLAKE8_LOCATION = LOCATION_SEARCH;

    public static final String FLAKE8_ARGS = "FLAKE8_ARGS";
    public static final String DEFAULT_FLAKE8_ARGS = "";

    public static boolean useFlake8(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getBoolean(USE_FLAKE8, projectAdaptable);
    }

    public static boolean useFlake8Console(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getBoolean(FLAKE8_USE_CONSOLE, projectAdaptable);
    }

    public static String getFlake8Args(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getString(FLAKE8_ARGS, projectAdaptable);
    }

    public static File getFlake8Location(PythonNature pythonNature) {
        IProject project = pythonNature.getProject();
        String searchlocation = PyScopedPreferences.getString(SEARCH_FLAKE8_LOCATION, project);
        switch (searchlocation) {
            case LOCATION_SPECIFY:
                return new File(PyScopedPreferences.getString(FLAKE8_FILE_LOCATION, project));
            default:
                try {
                    return pythonNature.getProjectInterpreter().searchExecutableForInterpreter("flake8", false);
                } catch (Exception e) {
                    Log.log(e);
                    return null;
                }

        }
    }

    public static ICallback<IExternalCodeAnalysisStream, IAdaptable> createFlake8Stream = ((
            IAdaptable projectAdaptable) -> {
        return null;
    });

    public static IExternalCodeAnalysisStream getConsoleOutputStream(IAdaptable projectAdaptable)
            throws MalformedURLException {
        return createFlake8Stream.call(projectAdaptable);
    }

    public static int eSeverity() {
        return PydevPrefs.getEclipsePreferences().getInt(FLAKE8_E_SEVERITY, DEFAULT_FLAKE8_E_SEVERITY);
    }

    public static int fSeverity() {
        return PydevPrefs.getEclipsePreferences().getInt(FLAKE8_F_SEVERITY, DEFAULT_FLAKE8_F_SEVERITY);
    }

    public static int cSeverity() {
        return PydevPrefs.getEclipsePreferences().getInt(FLAKE8_C_SEVERITY, DEFAULT_FLAKE8_C_SEVERITY);
    }

    public static int wSeverity() {
        return PydevPrefs.getEclipsePreferences().getInt(FLAKE8_W_SEVERITY, DEFAULT_FLAKE8_W_SEVERITY);
    }
}
