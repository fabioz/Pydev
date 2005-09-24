/*
 * Created on 24/09/2005
 */
package com.python.pydev.analysis;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;

public final class AnalysisPreferencesStub implements IAnalysisPreferences {
    public int severityForUnusedImport;
    public int severityForUnusedVariable;
    public int severityForUndefinedVariable;
    public int severityForDuplicatedSignature;
    public int severityForReimport;
    public int severityForUnresolvedImport;
    public int severityForNoSelf;
    
    public AnalysisPreferencesStub(){
        severityForUnusedImport = IMarker.SEVERITY_WARNING;
        severityForUnusedVariable = IMarker.SEVERITY_WARNING;
        severityForUndefinedVariable = IMarker.SEVERITY_ERROR;
        severityForDuplicatedSignature = IMarker.SEVERITY_ERROR;
        severityForReimport = IMarker.SEVERITY_WARNING;
        severityForUnresolvedImport = IMarker.SEVERITY_ERROR;
        severityForNoSelf = IMarker.SEVERITY_ERROR;
    }
    
    public int getSeverityForType(int type) {
        if (type == TYPE_UNUSED_IMPORT){
            return severityForUnusedImport;
        }
        if (type == TYPE_UNUSED_VARIABLE){
            return severityForUnusedVariable;
        }
        if (type == TYPE_UNDEFINED_VARIABLE){
            return severityForUndefinedVariable;
        }
        if (type == TYPE_DUPLICATED_SIGNATURE){
            return severityForDuplicatedSignature;
        }
        if (type == TYPE_REIMPORT){
            return severityForReimport;
        }
        if (type == TYPE_UNRESOLVED_IMPORT){
            return severityForUnresolvedImport;
        }
        if (type == TYPE_NO_SELF){
            return severityForNoSelf;
        }
        throw new RuntimeException("unable to get severity for type "+type);
    }

    public boolean makeCodeAnalysis() {
        return true;
    }

    /**
     * @see com.python.pydev.analysis.IAnalysisPreferences#getNamesIgnoredByUnusedVariable()
     */
    public List<String> getNamesIgnoredByUnusedVariable() {
        List<String> names = new ArrayList<String>();
        names.add("dummy");
        return names;
    }

    public void clearCaches() {
        //no caches here
    }

    public int getWhenAnalyze() {
        return IAnalysisPreferences.ANALYZE_ON_SUCCESFUL_PARSE;
    }

    public String getRequiredMessageToIgnore(int type) {
        throw new RuntimeException("unimplemented");
    }
}