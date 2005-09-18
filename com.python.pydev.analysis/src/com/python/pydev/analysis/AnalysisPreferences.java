/*
 * Created on 24/07/2005
 */
package com.python.pydev.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Preferences;

public class AnalysisPreferences implements IAnalysisPreferences{


    /**
     * singleton
     */
    private static IAnalysisPreferences analysisPreferences;
    
    /**
     * @return get the preferences for analysis based on the preferences
     */
    public static IAnalysisPreferences getAnalysisPreferences(){
        if(analysisPreferences == null){
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
    public static Object [][] completeSeverityMap = new Object[][]{
        {IAnalysisPreferences.TYPE_UNUSED_IMPORT       , AnalysisPreferenceInitializer.SEVERITY_UNUSED_IMPORT       , AnalysisPreferenceInitializer.DEFAULT_SEVERITY_UNUSED_IMPORT         },
        {IAnalysisPreferences.TYPE_UNUSED_VARIABLE     , AnalysisPreferenceInitializer.SEVERITY_UNUSED_VARIABLE     , AnalysisPreferenceInitializer.DEFAULT_SEVERITY_UNUSED_VARIABLE       },
        {IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE  , AnalysisPreferenceInitializer.SEVERITY_UNDEFINED_VARIABLE  , AnalysisPreferenceInitializer.DEFAULT_SEVERITY_UNDEFINED_VARIABLE    },
        {IAnalysisPreferences.TYPE_DUPLICATED_SIGNATURE, AnalysisPreferenceInitializer.SEVERITY_DUPLICATED_SIGNATURE, AnalysisPreferenceInitializer.DEFAULT_SEVERITY_DUPLICATED_SIGNATURE  },
        {IAnalysisPreferences.TYPE_REIMPORT            , AnalysisPreferenceInitializer.SEVERITY_REIMPORT            , AnalysisPreferenceInitializer.DEFAULT_SEVERITY_REIMPORT              },
        {IAnalysisPreferences.TYPE_UNRESOLVED_IMPORT   , AnalysisPreferenceInitializer.SEVERITY_UNRESOLVED_IMPORT   , AnalysisPreferenceInitializer.DEFAULT_SEVERITY_UNRESOLVED_IMPORT     },
        {IAnalysisPreferences.TYPE_NO_SELF             , AnalysisPreferenceInitializer.SEVERITY_NO_SELF             , AnalysisPreferenceInitializer.DEFAULT_SEVERITY_NO_SELF               },
    };
    


    public void clearCaches() {
        severityTypeMapCache = null;
    }
    
    HashMap<Integer, Integer> severityTypeMapCache = null;
    
    private Map<Integer, Integer> getSeverityTypeMap() {
        if(severityTypeMapCache == null){
            severityTypeMapCache = new HashMap<Integer, Integer>();
            Preferences pluginPreferences = AnalysisPlugin.getDefault().getPluginPreferences();
    
            for (int i = 0; i < completeSeverityMap.length; i++) {
                Object[] s = completeSeverityMap[i];
                severityTypeMapCache.put((Integer)s[0], pluginPreferences.getInt((String)s[1]));
            }
        }        
        return severityTypeMapCache;
    }
    
    /**
     * return the severity based on the user-set values
     *  
     * @see com.python.pydev.analysis.IAnalysisPreferences#getSeverityForType(int)
     */
    public int getSeverityForType(int type) {
        Map<Integer, Integer> severityTypeMap = getSeverityTypeMap();
        Integer sev = severityTypeMap.get(type);
        if(sev == null){
            throw new RuntimeException("Unable to get severity for: "+type);
        }
        return sev;
    }

    /**
     * yeah, we always do code analysis...
     *  
     * @see com.python.pydev.analysis.IAnalysisPreferences#makeCodeAnalysis()
     */
    public boolean makeCodeAnalysis() {
        return true;
    }

    /**
     * @see com.python.pydev.analysis.IAnalysisPreferences#getNamesIgnoredByUnusedVariable()
     */
    public List<String> getNamesIgnoredByUnusedVariable() {
        HashSet<String> names = new HashSet<String>();
        Preferences pluginPreferences = AnalysisPlugin.getDefault().getPluginPreferences();

        String string = pluginPreferences.getString(AnalysisPreferenceInitializer.NAMES_TO_IGNORE_UNUSED_VARIABLE);
        if(string != null){
            String[] strings = string.split(",");
            for (int i = 0; i < strings.length; i++) {
                names.add(strings[i].trim());
            }
        }
        
        return new ArrayList<String>(names);
    }

    /**
     * @see com.python.pydev.analysis.IAnalysisPreferences#getWhenAnalyze()
     */
    public int getWhenAnalyze() {
        Preferences pluginPreferences = AnalysisPlugin.getDefault().getPluginPreferences();
        return pluginPreferences.getInt(AnalysisPreferenceInitializer.WHEN_ANALYZE);
    }
}
