/**
 * Copyright (c) 2025 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.pyright;

import java.io.File;
import java.net.MalformedURLException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.preferences.PyScopedPreferences;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;

import com.python.pydev.analysis.external.IExternalCodeAnalysisStream;

public class PyrightPreferences {

    public static final String PYRIGHT_FILE_LOCATION = "PYRIGHT_FILE_LOCATION";

    public static final String USE_PYRIGHT = "USE_PYRIGHT";
    public static final boolean DEFAULT_USE_PYRIGHT = false;

    public static final String PYRIGHT_USE_CONSOLE = "PYRIGHT_USE_CONSOLE";
    public static final boolean DEFAULT_PYRIGHT_USE_CONSOLE = false;

    public static final String SEARCH_PYRIGHT_LOCATION = "SEARCH_PYRIGHT_LOCATION";
    public static final String LOCATION_SEARCH = "SEARCH";
    public static final String LOCATION_SPECIFY = "SPECIFY";
    public static final String DEFAULT_SEARCH_PYRIGHT_LOCATION = LOCATION_SEARCH;

    public static final String PYRIGHT_ARGS = "PYRIGHT_ARGS";
    public static final String DEFAULT_PYRIGHT_ARGS = "--outputformat json";

    public static boolean usePyright(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getBoolean(USE_PYRIGHT, projectAdaptable);
    }

    public static boolean usePyrightConsole(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getBoolean(PYRIGHT_USE_CONSOLE, projectAdaptable);
    }

    public static String getPyrightArgs(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getString(PYRIGHT_ARGS, projectAdaptable);
    }

    public static boolean usePyrightFromPythonNature(IPythonNature pythonNature, IAdaptable projectAdaptable) {
        if (LOCATION_SPECIFY.equals(PyScopedPreferences.getString(SEARCH_PYRIGHT_LOCATION, projectAdaptable))) {
            return false;
        }
        return true;
    }

    public static File getPyrightLocation(PythonNature pythonNature) {
        IProject project = pythonNature.getProject();
        if (LOCATION_SPECIFY.equals(PyScopedPreferences.getString(SEARCH_PYRIGHT_LOCATION, project))) {
            return new File(PyScopedPreferences.getString(PYRIGHT_FILE_LOCATION, project));
        }
        return null;
    }

    public static ICallback<IExternalCodeAnalysisStream, IAdaptable> createPyrightStream = ((
            IAdaptable projectAdaptable) -> {
        return null;
    });

    public static IExternalCodeAnalysisStream getConsoleOutputStream(IAdaptable projectAdaptable)
            throws MalformedURLException {
        return createPyrightStream.call(projectAdaptable);
    }

}
