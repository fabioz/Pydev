/*
 * Created on 24/07/2005
 */
package com.python.pydev.analysis;

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
     * return the severity based on the user-set values
     *  
     * @see com.python.pydev.analysis.IAnalysisPreferences#getSeverityForType(int)
     */
    public int getSeverityForType(int type) {
        Preferences pluginPreferences = AnalysisPlugin.getDefault().getPluginPreferences();
        switch(type){
        
            case IAnalysisPreferences.TYPE_UNUSED_IMPORT:
                return pluginPreferences.getInt(AnalysisPreferenceInitializer.SEVERITY_UNUSED_IMPORT);
        

            case IAnalysisPreferences.TYPE_UNUSED_VARIABLE:
                return pluginPreferences.getInt(AnalysisPreferenceInitializer.SEVERITY_UNUSED_VARIABLE);
                
            case IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE:
                return pluginPreferences.getInt(AnalysisPreferenceInitializer.SEVERITY_UNDEFINED_VARIABLE);
                
            case IAnalysisPreferences.TYPE_DUPLICATED_SIGNATURE:
                return pluginPreferences.getInt(AnalysisPreferenceInitializer.SEVERITY_DUPLICATED_SIGNATURE);
                
            case IAnalysisPreferences.TYPE_REIMPORT:
                return pluginPreferences.getInt(AnalysisPreferenceInitializer.SEVERITY_REIMPORT);
                
            default: 
                throw new RuntimeException("Unable to get severity for: "+type);
        }
    }

    /**
     * yeah, we always do code analysis...
     *  
     * @see com.python.pydev.analysis.IAnalysisPreferences#makeCodeAnalysis()
     */
    public boolean makeCodeAnalysis() {
        return true;
    }


}
