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
    public static final int TYPE_NO_SELF = 6;
    
    /**
     * this severity indicates that the given message should be ignored
     */
    public static final int SEVERITY_IGNORE = -1;
    
    /**
     * Used to define if the analysis should happen only on save
     */
    public static final int ANALYZE_ON_SAVE = 1;
    
    /**
     * Used to define if the analysis should happen on any successful parse
     */
    public static final int ANALYZE_ON_SUCCESFUL_PARSE = 2;
    
    /**
     * @see #ANALYZE_ON_SAVE
     * @see #ANALYZE_ON_SUCCESFUL_PARSE
     * 
     * @return the even that should trigger the analysis 
     */
    int getWhenAnalyze();

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
     * if something changes (if user changes the preferences).
     */
    void clearCaches();
}
