/**
 * Copyright (c) 2018 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.mypy;

import java.io.File;
import java.net.MalformedURLException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.preferences.PyScopedPreferences;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;

import com.python.pydev.analysis.external.IExternalCodeAnalysisStream;

public class MypyPreferences {

    public static final String MYPY_FILE_LOCATION = "MYPY_FILE_LOCATION";

    public static final String USE_MYPY = "USE_MYPY";
    public static final boolean DEFAULT_USE_MYPY = false;

    public static final String MYPY_USE_CONSOLE = "MYPY_USE_CONSOLE";
    public static final boolean DEFAULT_MYPY_USE_CONSOLE = false;

    public static final String MYPY_ADD_PROJECT_FOLDERS_TO_MYPYPATH = "MYPY_ADD_PROJECT_FOLDERS_TO_MYPYPATH";
    public static final boolean DEFAULT_MYPY_ADD_PROJECT_FOLDERS_TO_MYPYPATH = true;

    public static final String SEARCH_MYPY_LOCATION = "SEARCH_MYPY_LOCATION";
    public static final String LOCATION_SEARCH = "SEARCH";
    public static final String LOCATION_SPECIFY = "SPECIFY";
    public static final String DEFAULT_SEARCH_MYPY_LOCATION = LOCATION_SEARCH;

    public static final String MYPY_ARGS = "MYPY_ARGS";
    public static final String DEFAULT_MYPY_ARGS = "--follow-imports=silent --show-column-numbers";

    public static boolean useMypy(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getBoolean(USE_MYPY, projectAdaptable);
    }

    public static boolean useMypyConsole(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getBoolean(MYPY_USE_CONSOLE, projectAdaptable);
    }

    public static boolean getAddProjectFoldersToMyPyPath(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getBoolean(MYPY_ADD_PROJECT_FOLDERS_TO_MYPYPATH, projectAdaptable);
    }

    public static String getMypyArgs(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getString(MYPY_ARGS, projectAdaptable);
    }

    public static boolean useMyPyFromPythonNature(IPythonNature pythonNature, IAdaptable projectAdaptable) {
        if (LOCATION_SPECIFY.equals(PyScopedPreferences.getString(SEARCH_MYPY_LOCATION, projectAdaptable))) {
            return false;
        }
        return true;
    }

    public static File getMypyLocation(PythonNature pythonNature) {
        IProject project = pythonNature.getProject();
        if (LOCATION_SPECIFY.equals(PyScopedPreferences.getString(SEARCH_MYPY_LOCATION, project))) {
            return new File(PyScopedPreferences.getString(MYPY_FILE_LOCATION, project));
        }
        return null;
    }

    public static ICallback<IExternalCodeAnalysisStream, IAdaptable> createMypyStream = ((
            IAdaptable projectAdaptable) -> {
        return null;
    });

    public static IExternalCodeAnalysisStream getConsoleOutputStream(IAdaptable projectAdaptable)
            throws MalformedURLException {
        return createMypyStream.call(projectAdaptable);
    }

}
