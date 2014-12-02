/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/07/2005
 */
package com.python.pydev.analysis;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;

import com.python.pydev.analysis.ui.AnalysisPreferencesPage;

public class AnalysisPreferenceInitializer extends AbstractPreferenceInitializer {

    public static final String WHEN_ANALYZE = "WHEN_ANALYZE";
    public static final int DEFAULT_WHEN_ANALYZE = IAnalysisPreferences.ANALYZE_ON_SUCCESFUL_PARSE;

    public static final String DEFAULT_SCOPE = "com.python.pydev.analysis";

    public static final String SEVERITY_UNUSED_PARAMETER = "SEVERITY_UNUSED_PARAMETER";
    public static final int DEFAULT_SEVERITY_UNUSED_PARAMETER = IMarker.SEVERITY_INFO;

    public static final String SEVERITY_UNUSED_VARIABLE = "SEVERITY_UNUSED_VARIABLE";
    public static final int DEFAULT_SEVERITY_UNUSED_VARIABLE = IMarker.SEVERITY_WARNING;

    public static final String NAMES_TO_IGNORE_UNUSED_VARIABLE = "NAMES_TO_IGNORE_UNUSED_VARIABLE";
    public static final String DEFAULT_NAMES_TO_IGNORE_UNUSED_VARIABLE = "dummy, _, unused";

    public static final String NAMES_TO_IGNORE_UNUSED_IMPORT = "NAMES_TO_IGNORE_UNUSED_IMPORT";
    public static final String DEFAULT_NAMES_TO_IGNORE_UNUSED_IMPORT = "__init__, *QT";

    public static final String SEVERITY_UNUSED_IMPORT = "SEVERITY_UNUSED_IMPORT";
    public static final int DEFAULT_SEVERITY_UNUSED_IMPORT = IMarker.SEVERITY_WARNING;

    public static final String SEVERITY_UNDEFINED_VARIABLE = "SEVERITY_UNDEFINED_VARIABLE";
    public static final int DEFAULT_SEVERITY_UNDEFINED_VARIABLE = IMarker.SEVERITY_ERROR;

    public static final String SEVERITY_DUPLICATED_SIGNATURE = "SEVERITY_DUPLICATED_SIGNATURE";
    public static final int DEFAULT_SEVERITY_DUPLICATED_SIGNATURE = IMarker.SEVERITY_ERROR;

    public static final String SEVERITY_REIMPORT = "SEVERITY_REIMPORT";
    public static final int DEFAULT_SEVERITY_REIMPORT = IMarker.SEVERITY_WARNING;

    public static final String SEVERITY_UNRESOLVED_IMPORT = "SEVERITY_UNRESOLVED_IMPORT";
    public static final int DEFAULT_SEVERITY_UNRESOLVED_IMPORT = IMarker.SEVERITY_ERROR;

    public static final String SEVERITY_NO_SELF = "SEVERITY_NO_SELF";
    public static final int DEFAULT_SEVERITY_NO_SELF = IMarker.SEVERITY_ERROR;

    public static final String SEVERITY_UNUSED_WILD_IMPORT = "SEVERITY_UNUSED_WILD_IMPORT";
    public static final int DEFAULT_SEVERITY_UNUSED_WILD_IMPORT = IMarker.SEVERITY_WARNING;

    public static final String SEVERITY_UNDEFINED_IMPORT_VARIABLE = "SEVERITY_UNDEFINED_IMPORT_VARIABLE";
    public static final int DEFAULT_SEVERITY_UNDEFINED_IMPORT_VARIABLE = IMarker.SEVERITY_ERROR;

    public static final String DO_CODE_ANALYSIS = "DO_CODE_ANALYSIS";
    public static final boolean DEFAULT_DO_CODE_ANALYSIS = true;

    public static final String NAMES_TO_CONSIDER_GLOBALS = "NAMES_TO_CONSIDER_GLOBALS";
    public static final String DEFAULT_NAMES_TO_CONSIDER_GLOBALS = "_,tr";

    public static final String SEVERITY_NO_EFFECT_STMT = "SEVERITY_NO_EFFECT_STMT";
    public static final int DEFAULT_SEVERITY_NO_EFFECT_STMT = IMarker.SEVERITY_WARNING;

    public static final String SEVERITY_INDENTATION_PROBLEM = "SEVERITY_INDENTATION_PROBLEM";
    public static final int DEFAULT_SEVERITY_INDENTATION_PROBLEM = IMarker.SEVERITY_WARNING;

    public static final String SEVERITY_ASSIGNMENT_TO_BUILT_IN_SYMBOL = "SEVERITY_ASSIGNMENT_TO_BUILT_IN_SYMBOL";
    public static final int DEFAULT_SEVERITY_ASSIGNMENT_TO_BUILT_IN_SYMBOL = IMarker.SEVERITY_WARNING;

    public static final String SEVERITY_PEP8 = "SEVERITY_PEP8";
    public static final int DEFAULT_SEVERITY_PEP8 = IMarker.SEVERITY_INFO;

    public static final String DO_AUTO_IMPORT = "DO_AUTO_IMPORT";
    public static final boolean DEFAULT_DO_AUT_IMPORT = true;

    public static final String DO_AUTO_IMPORT_ON_ORGANIZE_IMPORTS = "DO_AUTO_IMPORT_ON_ORGANIZE_IMPORTS";
    public static final boolean DEFAULT_DO_AUTO_IMPORT_ON_ORGANIZE_IMPORTS = true;

    public static final String DO_IGNORE_IMPORTS_STARTING_WITH_UNDER = "DO_IGNORE_FIELDS_WITH_UNDER";
    public static final boolean DEFAULT_DO_IGNORE_FIELDS_WITH_UNDER = false;

    public static final String SEVERITY_ARGUMENTS_MISMATCH = "SEVERITY_ARGUMENTS_MISMATCH";
    public static final int DEFAULT_SEVERITY_ARGUMENTS_MISMATCH = IMarker.SEVERITY_INFO; //Currently does not run by default!

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = DefaultScope.INSTANCE.getNode(DEFAULT_SCOPE);

        for (int i = 0; i < AnalysisPreferences.completeSeverityMap.length; i++) {
            Object[] s = AnalysisPreferences.completeSeverityMap[i];
            node.putInt((String) s[1], (Integer) s[2]);

        }
        node.put(NAMES_TO_IGNORE_UNUSED_VARIABLE, DEFAULT_NAMES_TO_IGNORE_UNUSED_VARIABLE);
        node.put(NAMES_TO_IGNORE_UNUSED_IMPORT, DEFAULT_NAMES_TO_IGNORE_UNUSED_IMPORT);
        node.put(NAMES_TO_CONSIDER_GLOBALS, DEFAULT_NAMES_TO_CONSIDER_GLOBALS);
        node.putInt(WHEN_ANALYZE, DEFAULT_WHEN_ANALYZE);
        node.putBoolean(DO_CODE_ANALYSIS, DEFAULT_DO_CODE_ANALYSIS);
        node.putBoolean(DO_AUTO_IMPORT, DEFAULT_DO_AUT_IMPORT);
        node.putBoolean(DO_AUTO_IMPORT_ON_ORGANIZE_IMPORTS, DEFAULT_DO_AUTO_IMPORT_ON_ORGANIZE_IMPORTS);
        node.putBoolean(DO_IGNORE_IMPORTS_STARTING_WITH_UNDER, DEFAULT_DO_IGNORE_FIELDS_WITH_UNDER);

        //pep8 related.
        node.putBoolean(AnalysisPreferencesPage.USE_PEP8_CONSOLE, AnalysisPreferencesPage.DEFAULT_USE_PEP8_CONSOLE);
        node.putBoolean(AnalysisPreferencesPage.PEP8_USE_SYSTEM, AnalysisPreferencesPage.DEFAULT_PEP8_USE_SYSTEM);
    }

}
