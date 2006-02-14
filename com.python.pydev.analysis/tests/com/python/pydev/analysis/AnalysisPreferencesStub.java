/*
 * Created on 24/09/2005
 */
package com.python.pydev.analysis;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IMarker;

public final class AnalysisPreferencesStub extends AbstractAnalysisPreferences {
    public int severityForUnusedImport;
    public int severityForUnusedVariable;
    public int severityForUndefinedVariable;
    public int severityForDuplicatedSignature;
    public int severityForReimport;
    public int severityForUnresolvedImport;
    public int severityForNoSelf;
    public int severityForUnusedWildImport;
    public int severityForUndefinedImportVariable;
    
    public AnalysisPreferencesStub(){
        severityForUnusedImport = IMarker.SEVERITY_WARNING;
        severityForUnusedVariable = IMarker.SEVERITY_WARNING;
        severityForUndefinedVariable = IMarker.SEVERITY_ERROR;
        severityForDuplicatedSignature = IMarker.SEVERITY_ERROR;
        severityForReimport = IMarker.SEVERITY_WARNING;
        severityForUnresolvedImport = IMarker.SEVERITY_ERROR;
        severityForNoSelf = IMarker.SEVERITY_ERROR;
        severityForUnusedWildImport = IMarker.SEVERITY_WARNING;
        severityForUndefinedImportVariable = IMarker.SEVERITY_WARNING;
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
        if (type == TYPE_UNUSED_WILD_IMPORT){
            return severityForUnusedWildImport;
        }
        if (type == TYPE_UNDEFINED_IMPORT_VARIABLE){
        	return severityForUndefinedImportVariable;
        }
        throw new RuntimeException("unable to get severity for type "+type);
    }

    public boolean makeCodeAnalysis() {
        return true;
    }

    /**
     * @see com.python.pydev.analysis.IAnalysisPreferences#getNamesIgnoredByUnusedVariable()
     */
    public Set<String> getNamesIgnoredByUnusedVariable() {
        Set<String> names = new HashSet<String>();
        names.add("dummy");
        return names;
    }
    
    public Set<String> getModuleNamePatternsToBeIgnored() {
        Set<String> names = new HashSet<String>();
        names.add("__init__");
        return names;
    }

    public Set<String> getTokensAlwaysInGlobals() {
    	Set<String> names = new HashSet<String>();
    	names.add("considerGlobal");
    	return names;
    }
    
    public void clearCaches() {
        //no caches here
    }

    public int getWhenAnalyze() {
        return IAnalysisPreferences.ANALYZE_ON_SUCCESFUL_PARSE;
    }

}