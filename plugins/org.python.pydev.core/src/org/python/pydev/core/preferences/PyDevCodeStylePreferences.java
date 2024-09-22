package org.python.pydev.core.preferences;

import org.python.pydev.shared_core.SharedCorePlugin;

public class PyDevCodeStylePreferences {

    public static final String USE_LOCALS_AND_ATTRS_CAMELCASE = "USE_LOCALS_AND_ATTRS_CAMELCASE";
    public static final String USE_METHODS_FORMAT = "USE_METHODS_FORMAT";
    public static final boolean DEFAULT_USE_LOCALS_AND_ATTRS_CAMELCASE = false;
    public static final int METHODS_FORMAT_CAMELCASE_FIRST_LOWER = 0;
    public static final int METHODS_FORMAT_CAMELCASE_FIRST_UPPER = 1;
    public static final int METHODS_FORMAT_UNDERSCORE_SEPARATED = 2;
    public static final int DEFAULT_USE_METHODS_FORMAT = METHODS_FORMAT_UNDERSCORE_SEPARATED;

    public static final String[][] LABEL_AND_VALUE = new String[][] {
            { "underscore_separated", String.valueOf(METHODS_FORMAT_UNDERSCORE_SEPARATED) },
            { "CamelCase() with first upper", String.valueOf(METHODS_FORMAT_CAMELCASE_FIRST_UPPER) },
            { "camelCase() with first lower", String.valueOf(METHODS_FORMAT_CAMELCASE_FIRST_LOWER) }, };

    public static final String[][] LOCALS_LABEL_AND_VALUE = new String[][] {
            { "underscore_separated", String.valueOf(false) },
            { "camelCase with first lower", String.valueOf(true) }, };

    public static int TESTING_METHOD_FORMAT = DEFAULT_USE_METHODS_FORMAT;

    public static int useMethodsCamelCase() {
        if (SharedCorePlugin.inTestMode()) {
            return TESTING_METHOD_FORMAT;
        }
        return PydevPrefs.getEclipsePreferences().getInt(USE_METHODS_FORMAT, DEFAULT_USE_METHODS_FORMAT);
    }

    public static boolean TESTING_METHOD_LOCALS_AND_ATTRS_CAMEL_CASE = DEFAULT_USE_LOCALS_AND_ATTRS_CAMELCASE;

    public static boolean useLocalsAndAttrsCamelCase() {
        if (SharedCorePlugin.inTestMode()) {
            return TESTING_METHOD_LOCALS_AND_ATTRS_CAMEL_CASE;
        }
        return PydevPrefs.getEclipsePreferences().getBoolean(USE_LOCALS_AND_ATTRS_CAMELCASE,
                DEFAULT_USE_LOCALS_AND_ATTRS_CAMELCASE);
    }

}
