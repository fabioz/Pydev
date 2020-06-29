package com.python.pydev.analysis.pylint;

import java.io.File;
import java.net.MalformedURLException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.preferences.PydevPrefs;
import org.python.pydev.shared_core.callbacks.ICallback0;

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
    public static final String SEVERITY_INFORMATIONAL = "SEVERITY_INFORMATIONAL";
    public static final int DEFAULT_SEVERITY_INFORMATIONAL = SEVERITY_IGNORE;

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
    public static boolean usePyLint() {
        return PydevPrefs.getEclipsePreferences().getBoolean(USE_PYLINT, DEFAULT_USE_PYLINT);
    }

    public static boolean useConsole() {
        return PydevPrefs.getEclipsePreferences().getBoolean(USE_CONSOLE, DEFAULT_USE_CONSOLE);
    }

    public static String getPyLintArgs() {
        return PydevPrefs.getEclipsePreferences().get(PYLINT_ARGS, DEFAULT_PYLINT_ARGS);
    }

    public static int wSeverity() {
        return PydevPrefs.getEclipsePreferences().getInt(SEVERITY_WARNINGS, DEFAULT_SEVERITY_WARNINGS);
    }

    public static File getPyLintLocation(IPythonNature pythonNature) {
        IEclipsePreferences preferences = PydevPrefs.getEclipsePreferences();
        if (LOCATION_SPECIFY.equals(preferences.get(SEARCH_PYLINT_LOCATION, DEFAULT_SEARCH_PYLINT_LOCATION))) {
            return new File(preferences.get(PYLINT_FILE_LOCATION, ""));
        }
        try {
            return pythonNature.getProjectInterpreter().searchExecutableForInterpreter("pylint", false);
        } catch (Exception e) {
            Log.log(e);
            return null;
        }
    }

    public static int eSeverity() {
        return PydevPrefs.getEclipsePreferences().getInt(SEVERITY_ERRORS, DEFAULT_SEVERITY_ERRORS);
    }

    public static int fSeverity() {
        return PydevPrefs.getEclipsePreferences().getInt(SEVERITY_FATAL, DEFAULT_SEVERITY_FATAL);
    }

    public static int cSeverity() {
        return PydevPrefs.getEclipsePreferences().getInt(SEVERITY_CODING_STANDARD, DEFAULT_SEVERITY_CODING_STANDARD);
    }

    public static int rSeverity() {
        return PydevPrefs.getEclipsePreferences().getInt(SEVERITY_REFACTOR, DEFAULT_SEVERITY_REFACTOR);
    }

    public static int iSeverity() {
        return PydevPrefs.getEclipsePreferences().getInt(SEVERITY_INFORMATIONAL, DEFAULT_SEVERITY_INFORMATIONAL);
    }

    public static ICallback0<IExternalCodeAnalysisStream> createPyLintStream = (() -> {
        return null;
    });

    public static IExternalCodeAnalysisStream getConsoleOutputStream() throws MalformedURLException {
        return createPyLintStream.call();
    }

}
