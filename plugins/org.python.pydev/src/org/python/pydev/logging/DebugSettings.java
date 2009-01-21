package org.python.pydev.logging;


/**
 * This class is used to give debug settings for this plugin. 
 * 
 * Note: constants are updated from the preferences page when it changes (it's not final)
 * 
 * @author Fabio
 */
public class DebugSettings {

    /**
     * Should we debug requests for analysis (syntax check, etc.)
     */
    public static boolean DEBUG_ANALYSIS_REQUESTS = PyLoggingPreferencesPage.isToDebugAnalysisRequests();
    
    
    /**
     * This constant is used to debug the code-completion process on a production environment,
     * so that we gather enough information about what's happening and the possible reasons
     * for some bug (at this moment this is being specifically added because of a halting bug
     * for pydev in linux: https://sourceforge.net/tracker/index.php?func=detail&aid=1509582&group_id=85796&atid=577329)
     * 
     * It is kept updated from the Preferences Page
     */
    public static volatile boolean DEBUG_CODE_COMPLETION = PyLoggingPreferencesPage.isToDebugCodeCompletion();
    
}
