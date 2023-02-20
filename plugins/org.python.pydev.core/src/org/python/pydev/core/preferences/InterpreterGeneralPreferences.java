package org.python.pydev.core.preferences;

import org.python.pydev.core.IPythonNature;

public class InterpreterGeneralPreferences {

    public static final String NOTIFY_NO_INTERPRETER = "NOTIFY_NO_INTERPRETER_";

    public static final String NOTIFY_NO_INTERPRETER_PY = NOTIFY_NO_INTERPRETER
            + IPythonNature.INTERPRETER_TYPE_PYTHON;
    public final static boolean DEFAULT_NOTIFY_NO_INTERPRETER_PY = true;

    public static final String NOTIFY_NO_INTERPRETER_JY = NOTIFY_NO_INTERPRETER
            + IPythonNature.INTERPRETER_TYPE_JYTHON;
    public final static boolean DEFAULT_NOTIFY_NO_INTERPRETER_JY = true;

    public static final String NOTIFY_NO_INTERPRETER_IP = NOTIFY_NO_INTERPRETER
            + IPythonNature.INTERPRETER_TYPE_IRONPYTHON;
    public final static boolean DEFAULT_NOTIFY_NO_INTERPRETER_IP = true;

    public static final String CHECK_CONSISTENT_ON_STARTUP = "CHECK_CONSISTENT_ON_STARTUP";
    public final static boolean DEFAULT_CHECK_CONSISTENT_ON_STARTUP = true;

    public static final String UPDATE_INTERPRETER_INFO_ON_FILESYSTEM_CHANGES = "UPDATE_INTERPRETER_INFO_ON_FILESYSTEM_CHANGES";
    public final static boolean DEFAULT_UPDATE_INTERPRETER_INFO_ON_FILESYSTEM_CHANGES = true;

    public static final String USE_TYPESHED = "USE_TYPESHED";
    public final static boolean DEFAULT_USE_TYPESHED = true;

    public static boolean getCheckConsistentOnStartup() {
        return PydevPrefs.getEclipsePreferences().getBoolean(CHECK_CONSISTENT_ON_STARTUP,
                DEFAULT_CHECK_CONSISTENT_ON_STARTUP);
    }

    public static boolean getReCheckOnFilesystemChanges() {
        return PydevPrefs.getEclipsePreferences().getBoolean(UPDATE_INTERPRETER_INFO_ON_FILESYSTEM_CHANGES,
                DEFAULT_UPDATE_INTERPRETER_INFO_ON_FILESYSTEM_CHANGES);
    }

    public static Boolean FORCE_USE_TYPESHED = null; // For use in tests.

    public static boolean getUseTypeshed() {
        if (FORCE_USE_TYPESHED != null) {
            return FORCE_USE_TYPESHED;
        }
        return PydevPrefs.getEclipsePreferences().getBoolean(USE_TYPESHED,
                DEFAULT_USE_TYPESHED);
    }

}
