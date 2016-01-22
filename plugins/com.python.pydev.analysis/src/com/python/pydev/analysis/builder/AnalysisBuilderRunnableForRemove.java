/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.builder;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.logging.DebugSettings;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;

/**
 * This class is used to do analysis on a thread, just to remove the actual info.
 *
 * @author Fabio
 */
public class AnalysisBuilderRunnableForRemove extends AbstractAnalysisBuilderRunnable {

    /**
     * @param oldAnalysisBuilderThread This is an existing runnable that was already analyzing things... we must wait for it
     * to finish to start it again.
     *
     * @param module: this is a callback that'll be called with a boolean that should return the IModule to be used in the
     * analysis.
     * The parameter is FULL_MODULE or DEFINITIONS_MODULE
     */
    /*Default*/ AnalysisBuilderRunnableForRemove(String moduleName, IPythonNature nature, boolean isFullBuild,
            IAnalysisBuilderRunnable oldAnalysisBuilderThread, boolean forceAnalysis, int analysisCause,
            long documentTime, KeyForAnalysisRunnable key, long resourceModificationStamp) {
        super(isFullBuild, moduleName, forceAnalysis, analysisCause, oldAnalysisBuilderThread, nature, documentTime,
                key, resourceModificationStamp);

        if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
            Log.toLogFile(this, "Removing additional info from: " + moduleName);
        }
        removeInfoForModule(moduleName, nature, isFullBuild);
    }

    @Override
    public void doAnalysis() {
        // Do nothing (we let it be scheduled just to stop executing an existing analysis).
    }

    /**
     * @param moduleName this is the module name
     * @param nature this is the nature
     */
    public static void removeInfoForModule(String moduleName, IPythonNature nature, boolean isFullBuild) {
        if (moduleName != null && nature != null) {
            AbstractAdditionalDependencyInfo info;
            try {
                info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature);
            } catch (MisconfigurationException e) {
                Log.log(e);
                return;
            }

            boolean generateDelta;
            if (isFullBuild) {
                generateDelta = false;
            } else {
                generateDelta = true;
            }
            info.removeInfoFromModule(moduleName, generateDelta);
        } else {
            if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
                Log.toLogFile("Unable to remove info. name: " + moduleName + " or nature:" + nature + " is null.",
                        AnalysisBuilderRunnableForRemove.class);
            }
        }
    }
}
