/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.codecoverage;

import org.eclipse.core.resources.IContainer;

/**
 * @author Fabio Zadrozny
 */
public class PyCoveragePreferences {

    private static boolean internalAllRunsDoCoverage = false;
    private static boolean clearCoverageInfoOnNextLaunch = true;
    private static boolean refreshAfterNextLaunch = true;
    private static IContainer lastChosenDir;



    public static boolean getAllRunsDoCoverage() {
        return getInternalAllRunsDoCoverage() && lastChosenDir != null && lastChosenDir.exists();
    }

    /*default*/ static boolean getInternalAllRunsDoCoverage() {
        return internalAllRunsDoCoverage;
    }
    /*default*/ static void setInternalAllRunsDoCoverage(boolean selection) {
        internalAllRunsDoCoverage = selection;
    }

    
    
    /*default*/ static void setRefreshAfterNextLaunch(boolean selection) {
        refreshAfterNextLaunch = selection;
    }
    
    public static boolean getRefreshAfterNextLaunch() {
        return refreshAfterNextLaunch;
    }
    
    
    /*default*/ static void setClearCoverageInfoOnNextLaunch(boolean selection) {
        clearCoverageInfoOnNextLaunch = selection;
    }
    
    public static boolean getClearCoverageInfoOnNextLaunch() {
        return clearCoverageInfoOnNextLaunch;
    }
    
    
    

    /*default*/ static void setLastChosenDir(IContainer container) {
        lastChosenDir = container;
    }
    public static IContainer getLastChosenDir() {
        return lastChosenDir;
    }


}
