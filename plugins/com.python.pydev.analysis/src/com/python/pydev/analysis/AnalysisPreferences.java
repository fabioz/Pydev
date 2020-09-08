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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.python.pydev.ast.analysis.IAnalysisPreferences;
import org.python.pydev.core.preferences.PydevPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.preferences.IScopedPreferences;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

public class AnalysisPreferences extends AbstractAnalysisPreferences {

    private final IAdaptable projectAdaptable;

    public AnalysisPreferences(IAdaptable projectAdaptable) {
        this.projectAdaptable = projectAdaptable;
    }

    @Override
    public IAdaptable getProjectAdaptable() {
        return projectAdaptable;
    }

    /**
     * when adding a new type, it must be specified:
     * here
     * AnalysisPreferenceInitializer
     * IAnalysisPreferences
     * AnalysisPreferencesPage
     */
    final static Object[][] completeSeverityMap = new Object[][] {
            { IAnalysisPreferences.TYPE_UNUSED_IMPORT, AnalysisPreferenceInitializer.SEVERITY_UNUSED_IMPORT,
                    AnalysisPreferenceInitializer.DEFAULT_SEVERITY_UNUSED_IMPORT },
            { IAnalysisPreferences.TYPE_UNUSED_VARIABLE, AnalysisPreferenceInitializer.SEVERITY_UNUSED_VARIABLE,
                    AnalysisPreferenceInitializer.DEFAULT_SEVERITY_UNUSED_VARIABLE },
            { IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE, AnalysisPreferenceInitializer.SEVERITY_UNDEFINED_VARIABLE,
                    AnalysisPreferenceInitializer.DEFAULT_SEVERITY_UNDEFINED_VARIABLE },
            { IAnalysisPreferences.TYPE_DUPLICATED_SIGNATURE,
                    AnalysisPreferenceInitializer.SEVERITY_DUPLICATED_SIGNATURE,
                    AnalysisPreferenceInitializer.DEFAULT_SEVERITY_DUPLICATED_SIGNATURE },
            { IAnalysisPreferences.TYPE_REIMPORT, AnalysisPreferenceInitializer.SEVERITY_REIMPORT,
                    AnalysisPreferenceInitializer.DEFAULT_SEVERITY_REIMPORT },
            { IAnalysisPreferences.TYPE_UNRESOLVED_IMPORT, AnalysisPreferenceInitializer.SEVERITY_UNRESOLVED_IMPORT,
                    AnalysisPreferenceInitializer.DEFAULT_SEVERITY_UNRESOLVED_IMPORT },
            { IAnalysisPreferences.TYPE_NO_SELF, AnalysisPreferenceInitializer.SEVERITY_NO_SELF,
                    AnalysisPreferenceInitializer.DEFAULT_SEVERITY_NO_SELF },
            { IAnalysisPreferences.TYPE_UNUSED_WILD_IMPORT, AnalysisPreferenceInitializer.SEVERITY_UNUSED_WILD_IMPORT,
                    AnalysisPreferenceInitializer.DEFAULT_SEVERITY_UNUSED_WILD_IMPORT },
            { IAnalysisPreferences.TYPE_UNDEFINED_IMPORT_VARIABLE,
                    AnalysisPreferenceInitializer.SEVERITY_UNDEFINED_IMPORT_VARIABLE,
                    AnalysisPreferenceInitializer.DEFAULT_SEVERITY_UNDEFINED_IMPORT_VARIABLE },
            { IAnalysisPreferences.TYPE_UNUSED_PARAMETER, AnalysisPreferenceInitializer.SEVERITY_UNUSED_PARAMETER,
                    AnalysisPreferenceInitializer.DEFAULT_SEVERITY_UNUSED_PARAMETER },
            { IAnalysisPreferences.TYPE_NO_EFFECT_STMT, AnalysisPreferenceInitializer.SEVERITY_NO_EFFECT_STMT,
                    AnalysisPreferenceInitializer.DEFAULT_SEVERITY_NO_EFFECT_STMT },
            { IAnalysisPreferences.TYPE_INDENTATION_PROBLEM,
                    AnalysisPreferenceInitializer.SEVERITY_INDENTATION_PROBLEM,
                    AnalysisPreferenceInitializer.DEFAULT_SEVERITY_INDENTATION_PROBLEM },
            { IAnalysisPreferences.TYPE_ASSIGNMENT_TO_BUILT_IN_SYMBOL,
                    AnalysisPreferenceInitializer.SEVERITY_ASSIGNMENT_TO_BUILT_IN_SYMBOL,
                    AnalysisPreferenceInitializer.DEFAULT_SEVERITY_ASSIGNMENT_TO_BUILT_IN_SYMBOL },
            { IAnalysisPreferences.TYPE_PEP8, AnalysisPreferenceInitializer.SEVERITY_PEP8,
                    AnalysisPreferenceInitializer.DEFAULT_SEVERITY_PEP8 },
            { IAnalysisPreferences.TYPE_ARGUMENTS_MISATCH, AnalysisPreferenceInitializer.SEVERITY_ARGUMENTS_MISMATCH,
                    AnalysisPreferenceInitializer.DEFAULT_SEVERITY_ARGUMENTS_MISMATCH },
            { IAnalysisPreferences.TYPE_FSTRING_SYNTAX_ERROR, AnalysisPreferenceInitializer.SEVERITY_FSTRING_ERROR,
                    AnalysisPreferenceInitializer.DEFAULT_SEVERITY_FSTRING_ERROR },
            { IAnalysisPreferences.TYPE_INVALID_ENCODING, AnalysisPreferenceInitializer.SEVERITY_INVALID_ENCODING,
                    AnalysisPreferenceInitializer.DEFAULT_SEVERITY_INVALID_ENCODING },
    };

    private HashMap<Integer, Integer> severityTypeMapCache;
    private final Object lock = new Object();

    private Map<Integer, Integer> getSeverityTypeMap() {
        if (severityTypeMapCache == null) {
            synchronized (lock) {
                if (severityTypeMapCache == null) {
                    //Do it lazily as it's possible we don't need it...
                    HashMap<Integer, Integer> temp = new HashMap<Integer, Integer>();
                    IEclipsePreferences analysisEclipsePreferences = PydevPrefs.getAnalysisEclipsePreferences();
                    IEclipsePreferences defaultAnalysisEclipsePreferences = PydevPrefs
                            .getDefaultAnalysisEclipsePreferences();
                    IScopedPreferences iScopedPreferences = PyAnalysisScopedPreferences.get();

                    for (int i = 0; i < completeSeverityMap.length; i++) {
                        Object[] s = completeSeverityMap[i];
                        int v = iScopedPreferences.getInt(analysisEclipsePreferences, defaultAnalysisEclipsePreferences,
                                (String) s[1], projectAdaptable);
                        temp.put((Integer) s[0], v);
                    }

                    //TODO: Add ARGUMENTS_MISMATCH again later on
                    temp.put(IAnalysisPreferences.TYPE_ARGUMENTS_MISATCH, -1); //Force it to be disabled for now!
                    severityTypeMapCache = temp;
                }
            }
        }
        return severityTypeMapCache;
    }

    /**
     * return the severity based on the user-set values
     *
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#getSeverityForType(int)
     */
    @Override
    public int getSeverityForType(int type) {
        Map<Integer, Integer> severityTypeMap = getSeverityTypeMap();
        Integer sev = severityTypeMap.get(type);
        if (sev == null) {
            throw new RuntimeException("Unable to get severity for: " + type);
        }
        return sev;
    }

    /**
     * yeah, we always do code analysis...
     *
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#makeCodeAnalysis()
     */
    @Override
    public boolean makeCodeAnalysis() {
        AnalysisPlugin plugin = AnalysisPlugin.getDefault();
        if (plugin == null) {
            return false;//in shutdown
        }
        return PyAnalysisScopedPreferences.getBoolean(AnalysisPreferenceInitializer.DO_CODE_ANALYSIS,
                projectAdaptable);
    }

    /**
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#getNamesIgnoredByUnusedVariable()
     */
    @Override
    public Set<String> getNamesIgnoredByUnusedVariable() {
        return getSetOfNames(AnalysisPreferenceInitializer.NAMES_TO_IGNORE_UNUSED_VARIABLE);
    }

    @Override
    public Set<String> getTokensAlwaysInGlobals() {
        return getSetOfNames(AnalysisPreferenceInitializer.NAMES_TO_CONSIDER_GLOBALS);
    }

    /**
     * @param preferencesName
     * @return
     */
    private Set<String> getSetOfNames(String preferencesName) {
        HashSet<String> names = new HashSet<String>();
        String string = PyAnalysisScopedPreferences.getString(preferencesName, projectAdaptable);
        if (string != null) {
            String[] strings = string.split(",");
            for (int i = 0; i < strings.length; i++) {
                names.add(strings[i].trim());
            }
        }

        return names;
    }

    /**
     * @see org.python.pydev.ast.analysis.IAnalysisPreferences#getModuleNamePatternsToBeIgnored()
     */
    @Override
    public Set<String> getModuleNamePatternsToBeIgnored() {
        Set<String> setOfNames = getSetOfNames(AnalysisPreferenceInitializer.NAMES_TO_IGNORE_UNUSED_IMPORT);
        HashSet<String> ret = new HashSet<String>();
        for (String string : setOfNames) {
            //we have to make it a regular expression as java requires, so * is actually .*
            ret.add(string.replaceAll("\\*", ".*"));
        }
        return ret;
    }

    public static String[] getPep8CommandLine(IAdaptable projectAdaptable) {
        return ProcessUtils.parseArguments(getPep8CommandLineAsStr(projectAdaptable));
    }

    public static String getPep8CommandLineAsStr(IAdaptable projectAdaptable) {
        return PyAnalysisScopedPreferences.getString(AnalysisPreferenceInitializer.PEP8_COMMAND_LINE, projectAdaptable);
    }

    public static boolean useConsole(IAdaptable projectAdaptable) {
        if (AnalysisPreferenceInitializer.SHOW_IN_PEP8_FEATURE_ENABLED) {
            return PyAnalysisScopedPreferences.getBoolean(AnalysisPreferenceInitializer.USE_PEP8_CONSOLE,
                    projectAdaptable);
        }
        return false;
    }

    public static boolean useSystemInterpreter(IAdaptable projectAdaptable) {
        return PyAnalysisScopedPreferences.getBoolean(AnalysisPreferenceInitializer.PEP8_USE_SYSTEM, projectAdaptable);
    }

    public static boolean TESTS_DO_AUTO_IMPORT = true;

    public static boolean doAutoImport(IAdaptable projectAdaptable) {
        if (SharedCorePlugin.inTestMode()) {
            return TESTS_DO_AUTO_IMPORT;
        }

        return PyAnalysisScopedPreferences.getBoolean(AnalysisPreferenceInitializer.DO_AUTO_IMPORT, projectAdaptable);
    }

    public static boolean TESTS_DO_AUTO_IMPORT_ON_ORGANIZE_IMPORTS = true;

    public static boolean doAutoImportOnOrganizeImports(IAdaptable projectAdaptable) {
        if (SharedCorePlugin.inTestMode()) {
            return TESTS_DO_AUTO_IMPORT_ON_ORGANIZE_IMPORTS;
        }
        return PyAnalysisScopedPreferences.getBoolean(AnalysisPreferenceInitializer.DO_AUTO_IMPORT_ON_ORGANIZE_IMPORTS,
                projectAdaptable);
    }

    public static boolean TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER = false;

    public static boolean doIgnoreImportsStartingWithUnder(IAdaptable projectAdaptable) {
        if (SharedCorePlugin.inTestMode()) {
            return TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER;
        }

        return PyAnalysisScopedPreferences.getBoolean(
                AnalysisPreferenceInitializer.DO_IGNORE_IMPORTS_STARTING_WITH_UNDER, projectAdaptable);
    }

    /**
    *
    * @param doIgnoreImportsStartingWithUnder: result from the doIgnoreImportsStartingWithUnder() method
    * (but should be called before so that it does not get into a loop which call this method as that method
    * may be slow).
    */
    public static String removeImportsStartingWithUnderIfNeeded(String declPackageWithoutInit, FastStringBuffer buf,
            boolean doIgnoreImportsStartingWithUnder) {
        if (doIgnoreImportsStartingWithUnder) {
            List<String> splitted = StringUtils.dotSplit(declPackageWithoutInit);

            boolean foundStartingWithoutUnder = false;
            buf.clear();
            int len = splitted.size();
            for (int i = len - 1; i >= 0; i--) {
                String s = splitted.get(i);
                if (!foundStartingWithoutUnder) {
                    if (s.charAt(0) == '_') {
                        continue;
                    }
                    foundStartingWithoutUnder = true;
                }
                buf.insert(0, s);
                if (i != 0) {
                    buf.insert(0, '.');
                }
            }
            declPackageWithoutInit = buf.toString();
        }
        return declPackageWithoutInit;
    }
}
