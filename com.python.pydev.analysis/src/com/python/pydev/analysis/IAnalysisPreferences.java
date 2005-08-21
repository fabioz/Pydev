/*
 * Created on 24/07/2005
 */
package com.python.pydev.analysis;

import java.util.List;

public interface IAnalysisPreferences {
    
    public static final int TYPE_UNUSED_IMPORT = 0;
    public static final int TYPE_UNUSED_VARIABLE = 1;
    public static final int TYPE_UNDEFINED_VARIABLE = 2;
    public static final int TYPE_DUPLICATED_SIGNATURE = 3;
    public static final int TYPE_REIMPORT = 4;
    public static final int TYPE_UNRESOLVED_IMPORT = 5;
    
    /**
     * this severity indicates that the given message should be ignored
     */
    public static final int SEVERITY_IGNORE = -1;

    /**
     * @see org.eclipse.core.resources.IMarker#SEVERITY_ERROR
     * @see org.eclipse.core.resources.IMarker#SEVERITY_WARNING
     * @see org.eclipse.core.resources.IMarker#SEVERITY_INFO
     * 
     * @see IAnalysisPreferences#SEVERITY_IGNORE
     * 
     * @return this message severity.
     */
    int getSeverityForType(int type);
    
    /**
     * @return whether we should do code analysis
     */
    boolean makeCodeAnalysis();
    
    /**
     * @return a set with the names that should be ignored when reporting unused variables
     * (this are names 'starting with' that should be ignored)
     * 
     * e.g.: if dummy is in the set, ignore names starting with 'dummy' that will be reported as unused variables
     */
    List<String> getNamesIgnoredByUnusedVariable();
    
    /**
     * The analysis preferences may have caches, so that we don't get all from the cache, but we must be able to clear them
     * if something changes.
     */
    void clearCaches();
}
