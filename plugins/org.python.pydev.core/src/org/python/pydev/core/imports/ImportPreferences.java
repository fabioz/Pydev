package org.python.pydev.core.imports;

import java.util.Optional;

import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.preferences.PyScopedPreferences;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.process.ProcessUtils;

public class ImportPreferences {

    public static final String IMPORT_ENGINE = "IMPORT_ENGINE";
    public static final String IMPORT_ENGINE_REGULAR_SORT = "IMPORT_ENGINE_REGULAR_SORT";
    public static final String IMPORT_ENGINE_PEP_8 = "IMPORT_ENGINE_PEP_8";
    public static final String IMPORT_ENGINE_ISORT = "IMPORT_ENGINE_ISORT";

    public final static String DEFAULT_IMPORT_ENGINE = IMPORT_ENGINE_PEP_8;
    public static final String GROUP_IMPORTS = "GROUP_IMPORTS";

    public final static boolean DEFAULT_GROUP_IMPORTS = true;
    public static final String MULTILINE_IMPORTS = "MULTILINE_IMPORTS";

    public final static boolean DEFAULT_MULTILINE_IMPORTS = true;
    public static final String FROM_IMPORTS_FIRST = "FROM_IMPORTS_FIRST";

    public final static boolean DEFAULT_FROM_IMPORTS_FIRST = false;
    public static final String SORT_NAMES_GROUPED = "SORT_NAMES_GROUPED";

    public final static boolean DEFAULT_SORT_NAMES_GROUPED = false;
    public static final String DELETE_UNUSED_IMPORTS = "DELETE_UNUSED_IMPORTS";

    //Left default as false because it can be a destructive operation (i.e.: many imports
    //may have a reason even without being used -- and in this case it must be marked as @UnusedImport,
    //so, making it so that the user has to enable this option and know what he is doing).
    public final static boolean DEFAULT_DELETE_UNUSED_IMPORTS = false;

    public static final String BREAK_IMPORTS_MODE = "BREAK_IMPORTS_MODE";
    public static final String BREAK_IMPORTS_MODE_ESCAPE = "ESCAPE";
    public static final String BREAK_IMPORTS_MODE_PARENTHESIS = "PARENTHESIS";

    public final static String DEFAULT_BREAK_IMPORTS_MODE = BREAK_IMPORTS_MODE_PARENTHESIS;

    public static final String LOCATION_SEARCH = "LOCATION_SEARCH";
    public static final String LOCATION_SPECIFY = "LOCATION_SPECIFY";
    public static final String ISORT_LOCATION_OPTION = "ISORT_LOCATION_OPTION";

    public static final String DEFAULT_ISORT_LOCATION_OPTION = LOCATION_SEARCH;
    public static final String ISORT_FILE_LOCATION = "ISORT_FILE_LOCATION";
    public static final String ISORT_PARAMETERS = "ISORT_PARAMETERS";

    /**
     * May be changed for testing purposes.
     */
    public static boolean pep8ImportsForTests = true;

    /**
     * @return whether to format imports according to pep8
     */
    public static String getImportEngine(IAdaptable projectAdaptable) {
        if (SharedCorePlugin.inTestMode()) {
            if (pep8ImportsForTests) {
                return IMPORT_ENGINE_PEP_8;
            } else {
                return IMPORT_ENGINE_REGULAR_SORT;
            }
        }
        String importEngine = PyScopedPreferences.getString(IMPORT_ENGINE, projectAdaptable);
        if (importEngine == null) {
            importEngine = IMPORT_ENGINE_PEP_8;
        }
        switch (importEngine) {
            case IMPORT_ENGINE_PEP_8:
            case IMPORT_ENGINE_ISORT:
            case IMPORT_ENGINE_REGULAR_SORT:
                return importEngine;

            default:
                // Wrong value: use PEP 8 engine.
                return IMPORT_ENGINE_PEP_8;
        }
    }

    public static Optional<String> getISortExecutable(IAdaptable projectAdaptable) {
        String locationOption = PyScopedPreferences.getString(ISORT_LOCATION_OPTION, projectAdaptable);
        if (LOCATION_SPECIFY.equals(locationOption)) {
            String isortFileLocation = PyScopedPreferences.getString(ISORT_FILE_LOCATION, projectAdaptable);
            if (isortFileLocation != null && isortFileLocation.length() > 0) {
                return Optional.of(isortFileLocation);
            }
        }
        return Optional.empty();
    }

    public static String[] getISortArguments(IAdaptable projectAdaptable) {
        String parameters = PyScopedPreferences.getString(ISORT_PARAMETERS, projectAdaptable);
        if (parameters != null && parameters.length() > 0) {
            return ProcessUtils.parseArguments(parameters);
        }
        return new String[0];
    }

    /**
     * May be changed for testing purposes.
     */
    public static boolean deleteUnusedImportsForTests = true;

    /**
     * @return whether to delete unused imports
     */
    public static boolean getDeleteUnusedImports(IAdaptable projectAdaptable) {
        if (SharedCorePlugin.inTestMode()) {
            return deleteUnusedImportsForTests;
        }
        return PyScopedPreferences.getBoolean(DELETE_UNUSED_IMPORTS, projectAdaptable);
    }

    /**
     * May be changed for testing purposes.
     */
    public static String breakImportModeForTests = BREAK_IMPORTS_MODE_PARENTHESIS;

    /**
     * @return the way to break imports as the constants specified
     * @see BREAK_IMPORTS_MODE_ESCAPE
     * @see BREAK_IMPORTS_MODE_PARENTHESIS
     */
    public static String getBreakImportMode(IAdaptable projectAdaptable) {
        if (SharedCorePlugin.inTestMode()) {
            return breakImportModeForTests;
        }
        return PyScopedPreferences.getString(BREAK_IMPORTS_MODE, projectAdaptable);
    }

    /**
     * May be changed for testing purposes.
     */
    public static boolean sortNamesGroupedForTests = false;

    public static boolean getSortNamesGrouped(IAdaptable projectAdaptable) {
        if (SharedCorePlugin.inTestMode()) {
            return sortNamesGroupedForTests;
        }
        return PyScopedPreferences.getBoolean(SORT_NAMES_GROUPED, projectAdaptable);
    }

    /**
     * May be changed for testing purposes.
     */
    public static boolean multilineImportsForTests = true;

    /**
     * @return true if imports should be wrapped when they exceed the print margin.
     */
    public static boolean getMultilineImports(IAdaptable projectAdaptable) {
        if (SharedCorePlugin.inTestMode()) {
            return multilineImportsForTests;
        }
        return PyScopedPreferences.getBoolean(MULTILINE_IMPORTS, projectAdaptable);
    }

    /**
     * May be changed for testing purposes.
     */
    public static boolean sortFromImportsFirstForTests = true;

    /**
     * @return true if 'from ... import ...' statements should be sorted before 'import ...' statements.
     * E.g, a set of imports would be organized like the following:
     *   from a_module import b, c, d
     *   from c_module import e, f
     *   import b_module
     *   import d_module
     */
    public static boolean getSortFromImportsFirst(IAdaptable projectAdaptable) {
        if (CorePlugin.getDefault() == null) {
            return sortFromImportsFirstForTests;
        }
        return PyScopedPreferences.getBoolean(FROM_IMPORTS_FIRST, projectAdaptable);
    }

    /**
     * May be changed for testing purposes.
     */
    public static boolean groupImportsForTests = true;

    /**
     * @return true if imports should be grouped when possible. E.g.: If from aaa import b and from aaa import c
     * exist, they should be grouped as from aaa import b, c
     */
    public static boolean getGroupImports(IAdaptable projectAdaptable) {
        if (SharedCorePlugin.inTestMode()) {
            return groupImportsForTests;
        }
        return PyScopedPreferences.getBoolean(GROUP_IMPORTS, projectAdaptable);
    }

}
