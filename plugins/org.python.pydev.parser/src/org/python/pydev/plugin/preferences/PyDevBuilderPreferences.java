package org.python.pydev.plugin.preferences;

import org.python.pydev.parser.PyParserManager;

public class PyDevBuilderPreferences {

    public static final boolean DEFAULT_USE_PYDEV_BUILDERS = true;
    public static final String USE_PYDEV_BUILDERS = "USE_PYDEV_BUILDERS";

    public static final boolean DEFAULT_USE_PYDEV_ONLY_ON_DOC_SAVE = false;
    public static final String USE_PYDEV_ANALYSIS_ONLY_ON_DOC_SAVE = PyParserManager.USE_PYDEV_ANALYSIS_ONLY_ON_DOC_SAVE;

    public static final String PYDEV_ELAPSE_BEFORE_ANALYSIS = PyParserManager.PYDEV_ELAPSE_BEFORE_ANALYSIS;
    public static final int DEFAULT_PYDEV_ELAPSE_BEFORE_ANALYSIS = 3000;

    public static final String ANALYZE_ONLY_ACTIVE_EDITOR = "ANALYZE_ONLY_ACTIVE_EDITOR_2"; //Changed to _2 because we changed this behavior and the default is now true!
    public static final boolean DEFAULT_ANALYZE_ONLY_ACTIVE_EDITOR = true;

    public static final String REMOVE_ERRORS_WHEN_EDITOR_IS_CLOSED = "REMOVE_ERRORS_WHEN_EDITOR_IS_CLOSED_2"; //Changed to _2
    public static final boolean DEFAULT_REMOVE_ERRORS_WHEN_EDITOR_IS_CLOSED = true;

    public static final String PYC_DELETE_HANDLING = "PYC_DELETE_HANDLING";
    public static final int PYC_ALWAYS_DELETE = 0;
    public static final int PYC_DELETE_WHEN_PY_IS_DELETED = 1;
    public static final int PYC_NEVER_DELETE = 2;
    public static final int DEFAULT_PYC_DELETE_HANDLING = PYC_ALWAYS_DELETE;

    public static boolean usePydevBuilders() {
        return PydevPrefs.getEclipsePreferences().getBoolean(USE_PYDEV_BUILDERS, DEFAULT_USE_PYDEV_BUILDERS);
    }

    public static boolean useAnalysisOnlyOnDocSave() {
        return PyParserManager.getPyParserManager(PydevPrefs.getEclipsePreferences()).useAnalysisOnlyOnDocSave();
    }

    public static boolean getAnalyzeOnlyActiveEditor() {
        return PydevPrefs.getEclipsePreferences().getBoolean(ANALYZE_ONLY_ACTIVE_EDITOR,
                DEFAULT_ANALYZE_ONLY_ACTIVE_EDITOR);
    }

    public static boolean getRemoveErrorsWhenEditorIsClosed() {
        return PydevPrefs.getEclipsePreferences().getBoolean(REMOVE_ERRORS_WHEN_EDITOR_IS_CLOSED,
                DEFAULT_REMOVE_ERRORS_WHEN_EDITOR_IS_CLOSED);
    }

    public static void setAnalyzeOnlyActiveEditor(boolean b) {
        PydevPrefs.getEclipsePreferences().putBoolean(ANALYZE_ONLY_ACTIVE_EDITOR, b);
    }

    public static int getElapseMillisBeforeAnalysis() {
        return PyParserManager.getPyParserManager(PydevPrefs.getEclipsePreferences()).getElapseMillisBeforeAnalysis();
    }

    public static int getPycDeleteHandling() {
        return PydevPrefs.getEclipsePreferences().getInt(PYC_DELETE_HANDLING, DEFAULT_PYC_DELETE_HANDLING);
    }

}
