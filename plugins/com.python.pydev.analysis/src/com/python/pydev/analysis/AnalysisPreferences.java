/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
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
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Preferences;

public class AnalysisPreferences extends AbstractAnalysisPreferences {

    /**
     * singleton
     */
    private static IAnalysisPreferences analysisPreferences;

    /**
     * lock
     */
    public static final Object lock = new Object();

    /**
     * @return get the preferences for analysis based on the preferences
     */
    public static IAnalysisPreferences getAnalysisPreferences() {
        if (analysisPreferences == null) {
            analysisPreferences = new AnalysisPreferences();
        }
        return analysisPreferences;
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
                    AnalysisPreferenceInitializer.DEFAULT_SEVERITY_ARGUMENTS_MISMATCH }, };

    public void clearCaches() {
        synchronized (lock) {
            severityTypeMapCache = null;
        }
    }

    HashMap<Integer, Integer> severityTypeMapCache = null;

    private Map<Integer, Integer> getSeverityTypeMap() {
        synchronized (lock) {
            if (severityTypeMapCache == null) {
                severityTypeMapCache = new HashMap<Integer, Integer>();
                Preferences pluginPreferences = AnalysisPlugin.getDefault().getPluginPreferences();

                for (int i = 0; i < completeSeverityMap.length; i++) {
                    Object[] s = completeSeverityMap[i];
                    severityTypeMapCache.put((Integer) s[0], pluginPreferences.getInt((String) s[1]));
                }

                //TODO: Add ARGUMENTS_MISMATCH again later on
                severityTypeMapCache.put(IAnalysisPreferences.TYPE_ARGUMENTS_MISATCH, IMarker.SEVERITY_INFO); //Force it to be disabled for now!
            }
            return severityTypeMapCache;
        }
    }

    /**
     * return the severity based on the user-set values
     *  
     * @see com.python.pydev.analysis.IAnalysisPreferences#getSeverityForType(int)
     */
    public int getSeverityForType(int type) {
        synchronized (lock) {
            Map<Integer, Integer> severityTypeMap = getSeverityTypeMap();
            Integer sev = severityTypeMap.get(type);
            if (sev == null) {
                throw new RuntimeException("Unable to get severity for: " + type);
            }
            return sev;
        }
    }

    /**
     * yeah, we always do code analysis...
     *  
     * @see com.python.pydev.analysis.IAnalysisPreferences#makeCodeAnalysis()
     */
    public boolean makeCodeAnalysis() {
        synchronized (lock) {
            AnalysisPlugin plugin = AnalysisPlugin.getDefault();
            if (plugin == null) {
                return false;//in shutdown
            }
            Preferences pluginPreferences = plugin.getPluginPreferences();
            return pluginPreferences.getBoolean(AnalysisPreferenceInitializer.DO_CODE_ANALYSIS);
        }
    }

    /**
     * @see com.python.pydev.analysis.IAnalysisPreferences#getNamesIgnoredByUnusedVariable()
     */
    public Set<String> getNamesIgnoredByUnusedVariable() {
        return getSetOfNames(AnalysisPreferenceInitializer.NAMES_TO_IGNORE_UNUSED_VARIABLE);
    }

    public Set<String> getTokensAlwaysInGlobals() {
        return getSetOfNames(AnalysisPreferenceInitializer.NAMES_TO_CONSIDER_GLOBALS);
    }

    /**
     * @param preferencesName
     * @return
     */
    private Set<String> getSetOfNames(String preferencesName) {
        HashSet<String> names = new HashSet<String>();
        Preferences pluginPreferences = AnalysisPlugin.getDefault().getPluginPreferences();

        String string = pluginPreferences.getString(preferencesName);
        if (string != null) {
            String[] strings = string.split(",");
            for (int i = 0; i < strings.length; i++) {
                names.add(strings[i].trim());
            }
        }

        return names;
    }

    /**
     * @see com.python.pydev.analysis.IAnalysisPreferences#getModuleNamePatternsToBeIgnored()
     */
    public Set<String> getModuleNamePatternsToBeIgnored() {
        Set<String> setOfNames = getSetOfNames(AnalysisPreferenceInitializer.NAMES_TO_IGNORE_UNUSED_IMPORT);
        HashSet<String> ret = new HashSet<String>();
        for (String string : setOfNames) {
            //we have to make it a regular expression as java requires, so * is actually .*
            ret.add(string.replaceAll("\\*", ".*"));
        }
        return ret;
    }

    /**
     * @see com.python.pydev.analysis.IAnalysisPreferences#getWhenAnalyze()
     */
    public int getWhenAnalyze() {
        Preferences pluginPreferences = AnalysisPlugin.getDefault().getPluginPreferences();
        return pluginPreferences.getInt(AnalysisPreferenceInitializer.WHEN_ANALYZE);
    }

}
