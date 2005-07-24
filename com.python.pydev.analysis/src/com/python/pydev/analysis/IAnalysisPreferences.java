/*
 * Created on 24/07/2005
 */
package com.python.pydev.analysis;

public interface IAnalysisPreferences {
    
    public static final int TYPE_UNUSED_IMPORT = 0;
    public static final int TYPE_UNUSED_VARIABLE = 1;
    public static final int TYPE_UNDEFINED_VARIABLE = 2;
    public static final int TYPE_DUPLICATED_SIGNATURE = 3;
    
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
}
