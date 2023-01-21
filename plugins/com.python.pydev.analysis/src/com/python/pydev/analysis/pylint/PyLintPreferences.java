package com.python.pydev.analysis.pylint;

import java.io.File;
import java.net.MalformedURLException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.preferences.PyScopedPreferences;
import org.python.pydev.shared_core.callbacks.ICallback;

import com.python.pydev.analysis.external.IExternalCodeAnalysisStream;

public class PyLintPreferences {

    public static final String PYLINT_FILE_LOCATION = "PYLINT_FILE_LOCATION";

    public static final String USE_PYLINT = "USE_PYLINT";
    public static final boolean DEFAULT_USE_PYLINT = false;

    public static final int SEVERITY_IGNORE = -1;

    // errors
    public static final String SEVERITY_ERRORS = "SEVERITY_ERRORS";
    public static final int DEFAULT_SEVERITY_ERRORS = IMarker.SEVERITY_ERROR;

    //warnings
    public static final String SEVERITY_WARNINGS = "SEVERITY_WARNINGS";
    public static final int DEFAULT_SEVERITY_WARNINGS = IMarker.SEVERITY_WARNING;

    //fatal
    public static final String SEVERITY_FATAL = "SEVERITY_FATAL";
    public static final int DEFAULT_SEVERITY_FATAL = IMarker.SEVERITY_ERROR;

    //coding std
    public static final String SEVERITY_CODING_STANDARD = "SEVERITY_CODING_STANDARD";
    public static final int DEFAULT_SEVERITY_CODING_STANDARD = SEVERITY_IGNORE;

    //refactor
    public static final String SEVERITY_REFACTOR = "SEVERITY_REFACTOR";
    public static final int DEFAULT_SEVERITY_REFACTOR = SEVERITY_IGNORE;

    //informational
    public static final String SEVERITY_INFO = "SEVERITY_INFO";
    public static final int DEFAULT_SEVERITY_INFO = IMarker.SEVERITY_INFO;

    //console
    public static final String USE_CONSOLE = "USE_CONSOLE";
    public static final boolean DEFAULT_USE_CONSOLE = false;

    public static final String SEARCH_PYLINT_LOCATION = "SEARCH_PYLINT_LOCATION";
    public static final String LOCATION_SEARCH = "SEARCH";
    public static final String LOCATION_SPECIFY = "SPECIFY";
    public static final String DEFAULT_SEARCH_PYLINT_LOCATION = LOCATION_SEARCH;

    //args
    public static final String PYLINT_ARGS = "PYLINT_ARGS";
    public static final String DEFAULT_PYLINT_ARGS = "";

    /**
     * should we use py lint?
     *
     * @return
     */
    public static boolean usePyLint(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getBoolean(USE_PYLINT, projectAdaptable);
    }

    public static boolean useConsole(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getBoolean(USE_CONSOLE, projectAdaptable);
    }

    public static String getPyLintArgs(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getString(PYLINT_ARGS, projectAdaptable);
    }

    public static int wSeverity(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getInt(SEVERITY_WARNINGS, projectAdaptable, DEFAULT_SEVERITY_WARNINGS);
    }

    public static boolean usePyLintFromPythonNature(IPythonNature pythonNature, IAdaptable projectAdaptable) {
        if (LOCATION_SPECIFY.equals(PyScopedPreferences.getString(SEARCH_PYLINT_LOCATION, projectAdaptable))) {
            // This means that we should use PyLint from the given location.
            return false;
        }
        return true;

    }

    /**
     * @return the executable specified or null if we should run it with 'python -m pylint ...'
     */
    public static File getPyLintLocation(IPythonNature pythonNature, IAdaptable projectAdaptable) {
        if (LOCATION_SPECIFY.equals(PyScopedPreferences.getString(SEARCH_PYLINT_LOCATION, projectAdaptable))) {
            return new File(PyScopedPreferences.getString(PYLINT_FILE_LOCATION, projectAdaptable));
        }
        return null;
    }

    public static int eSeverity(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getInt(SEVERITY_ERRORS, projectAdaptable, DEFAULT_SEVERITY_ERRORS);
    }

    public static int fSeverity(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getInt(SEVERITY_FATAL, projectAdaptable, DEFAULT_SEVERITY_FATAL);
    }

    public static int cSeverity(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getInt(SEVERITY_CODING_STANDARD, projectAdaptable, DEFAULT_SEVERITY_CODING_STANDARD);
    }

    public static int rSeverity(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getInt(SEVERITY_REFACTOR, projectAdaptable, DEFAULT_SEVERITY_REFACTOR);
    }

    public static int iSeverity(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getInt(SEVERITY_INFO, projectAdaptable, DEFAULT_SEVERITY_INFO);
    }

    public static ICallback<IExternalCodeAnalysisStream, IAdaptable> createPyLintStream = ((
            IAdaptable projectAdaptable) -> {
        return null;
    });

    public static IExternalCodeAnalysisStream getConsoleOutputStream(IAdaptable projectAdaptable)
            throws MalformedURLException {
        return createPyLintStream.call(projectAdaptable);
    }

}
