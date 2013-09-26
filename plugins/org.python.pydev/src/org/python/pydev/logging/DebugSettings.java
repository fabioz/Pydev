/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
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

    /**
     * Debug the interpreter auto update?
     */
    public static boolean DEBUG_INTERPRETER_AUTO_UPDATE = PyLoggingPreferencesPage.isToDebugInterpreterAutoUpdate();
}
