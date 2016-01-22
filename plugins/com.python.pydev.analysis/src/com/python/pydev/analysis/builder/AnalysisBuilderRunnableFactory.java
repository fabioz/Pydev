/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.builder;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderPrefPage;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.logging.DebugSettings;
import org.python.pydev.shared_core.cache.LRUCache;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.structure.Tuple;

public class AnalysisBuilderRunnableFactory {

    /**
     * Field that should know all the threads.
     *
     * Key is tuple with project name and module name.
     */
    private volatile static Map<KeyForAnalysisRunnable, IAnalysisBuilderRunnable> availableThreads;

    /**
     * Cache to keep the last request time for a module. The key is the project name+module name and the value the documentTime
     * for the last analysis request for some module.
     */
    private volatile static LRUCache<KeyForAnalysisRunnable, Tuple<Long, Long>> analysisTimeCache = new LRUCache<KeyForAnalysisRunnable, Tuple<Long, Long>>(
            100);

    private static final Object lock = new Object();

    /**
     * @return Returns the availableThreads.
     */
    private static Map<KeyForAnalysisRunnable, IAnalysisBuilderRunnable> getAvailableThreads() {
        synchronized (lock) {
            if (availableThreads == null) {
                availableThreads = new HashMap<KeyForAnalysisRunnable, IAnalysisBuilderRunnable>();
            }
            return availableThreads;
        }
    }

    /*Default*/static void removeFromThreads(KeyForAnalysisRunnable key, IAnalysisBuilderRunnable runnable) {
        synchronized (lock) {
            Map<KeyForAnalysisRunnable, IAnalysisBuilderRunnable> available = getAvailableThreads();
            IAnalysisBuilderRunnable analysisBuilderThread = available.get(key);
            if (analysisBuilderThread == runnable) {
                available.remove(key);
            }
        }
    }

    // Logging Helpers -------------------------------

    private static void logCreate(String moduleName, IAnalysisBuilderRunnable analysisBuilderThread, String factory) {
        if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
            Log.toLogFile(analysisBuilderThread, "Created new builder: " + analysisBuilderThread + " for:" + moduleName
                    + " -- " + analysisBuilderThread.getAnalysisCauseStr() + " -- " + factory);
        }
    }

    private static void logStop(IAnalysisBuilderRunnable oldAnalysisBuilderThread, String creation) {
        if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
            Log.toLogFile(
                    oldAnalysisBuilderThread,
                    "Stopping previous builder: " + oldAnalysisBuilderThread + " ("
                            + oldAnalysisBuilderThread.getModuleName() + " -- "
                            + oldAnalysisBuilderThread.getAnalysisCauseStr() + ") to create new. " + creation);
        }
    }

    // End Logging Helpers -------------------------------

    /**
     * We use this delta because when saving we may get one analysis request from the builder and one from the parser
     * which are almost the same (but with a small difference), so, this is used so that they become closer.
     */
    private static final int DELTA_TO_CONSIDER_SAME = 500;

    /**
     * This will check if the nature is not null, related project is open and if the documentTime for the new request
     * is lower than one already in place (this can happen if we have a notification from a successful parse, but
     * it's only acknowledged after a build request, because the parse to finish can take some time, while the build
     * is 'automatic').
     *
     * @param nature the related nature
     * @param moduleName the name of the module we'll analyze
     * @param documentTime the time of the creation of the document we're about to analyze.
     * @param available the existing threads.
     * @param resourceModificationStamp
     * @param analysisCause
     *
     * @return The analysis key if all check were OK or null if some check failed.
     */
    private static KeyForAnalysisRunnable areNatureAndProjectAndTimeOK(IPythonNature nature,
            String moduleName, long documentTime, Map<KeyForAnalysisRunnable, IAnalysisBuilderRunnable> available,
            long resourceModificationStamp, int analysisCause) {
        synchronized (lock) {
            if (nature == null) {
                return null;
            }

            IProject project = nature.getProject();
            if (project == null || !project.isOpen()) {
                return null;
            }

            KeyForAnalysisRunnable analysisKey = new KeyForAnalysisRunnable(project.getName(), moduleName,
                    analysisCause);
            IAnalysisBuilderRunnable oldAnalysisBuilderThread = available.get(analysisKey);

            if (oldAnalysisBuilderThread != null) {
                if (!checkTimesOk(oldAnalysisBuilderThread, oldAnalysisBuilderThread.getDocumentTime(), documentTime,
                        oldAnalysisBuilderThread.getResourceModificationStamp(), resourceModificationStamp)) {
                    return null;
                }
            }

            Tuple<Long, Long> lastTime = analysisTimeCache.getObj(analysisKey);
            if (lastTime != null) {
                long oldDocTime = lastTime.o1;
                long oldResourceTime = lastTime.o2;
                if (!checkTimesOk(oldAnalysisBuilderThread, oldDocTime, documentTime, oldResourceTime,
                        resourceModificationStamp)) {
                    return null;
                }
            }
            analysisTimeCache.add(analysisKey, new Tuple<Long, Long>(documentTime, resourceModificationStamp));

            return analysisKey;
        }
    }

    private static boolean checkTimesOk(IAnalysisBuilderRunnable oldAnalysisBuilderThread, long oldDocTime,
            long documentTime, long oldResourceStamp, long resourceStamp) {
        if (oldAnalysisBuilderThread != null && oldDocTime > documentTime - DELTA_TO_CONSIDER_SAME) {
            //If the document version of the new one is lower than the one already active, don't do the analysis
            if (oldResourceStamp != resourceStamp) {
                if (oldResourceStamp == IResource.NULL_STAMP || resourceStamp == IResource.NULL_STAMP) {
                    return true; //one of them is null, so, it's ok to keep going.
                }
                if (resourceStamp > oldResourceStamp) {
                    return true; //it actually changed in the meantime, so, just keep going
                }
            }
            if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
                Log.toLogFile(oldAnalysisBuilderThread,
                        createExistinTimeHigherMessage(oldDocTime, documentTime, oldResourceStamp, resourceStamp));
            }

            return false;
        }
        return true;
    }

    private static String createExistinTimeHigherMessage(long oldTime, long documentTime, long oldResourceTime,
            long resourceTime) {
        return "The document time from an existing is higher than a new one, so, leave it be... " + oldTime +
                " > " + documentTime +
                " - " + DELTA_TO_CONSIDER_SAME +
                " (delta to consider equal) -- resource stamp (old, new): " + oldResourceTime +
                ", " + resourceTime;
    }

    // Factory creation methods -----------------------------------------

    /**
     * Creates a thread for analyzing some module (and stopping analysis of some other thread if there is one
     * already running).
     *
     * @return The new runnable or null if there's one there already that has a higher document version.
     */
    /*Default*/static IAnalysisBuilderRunnable createRunnable(IDocument document, IResource resource,
            ICallback<IModule, Integer> module, boolean isFullBuild, String moduleName, boolean forceAnalysis,
            int analysisCause, IPythonNature nature, long documentTime, long resourceModificationStamp) {

        synchronized (lock) {
            Map<KeyForAnalysisRunnable, IAnalysisBuilderRunnable> available = getAvailableThreads();
            KeyForAnalysisRunnable analysisKey = areNatureAndProjectAndTimeOK(nature, moduleName, documentTime,
                    available, resourceModificationStamp, analysisCause);
            if (analysisKey == null) {
                return null;
            }

            IAnalysisBuilderRunnable oldAnalysisBuilderThread = available.get(analysisKey);
            if (oldAnalysisBuilderThread != null) {
                //there is some existing thread that we have to stop to create the new one
                oldAnalysisBuilderThread.stopAnalysis();
                logStop(oldAnalysisBuilderThread, "Factory: changed");

                if (!forceAnalysis) {
                    forceAnalysis = oldAnalysisBuilderThread.getForceAnalysis();
                    if (forceAnalysis) {
                        if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
                            Log.toLogFile(oldAnalysisBuilderThread,
                                    "Now forcing analysis because old one, which didn't finish was forced!");
                        }
                    }
                }
                if (!forceAnalysis) {
                    if (PyDevBuilderPrefPage.getAnalyzeOnlyActiveEditor()) {
                        if (analysisCause == IAnalysisBuilderRunnable.ANALYSIS_CAUSE_BUILDER
                                && oldAnalysisBuilderThread
                                        .getAnalysisCause() != IAnalysisBuilderRunnable.ANALYSIS_CAUSE_BUILDER) {
                            //we're stopping a previous analysis that would really happen, so, let's force this one
                            forceAnalysis = true;
                        }
                    }
                }
            }
            IAnalysisBuilderRunnable analysisBuilderThread = new AnalysisBuilderRunnable(document, resource, module,
                    isFullBuild, moduleName, forceAnalysis, analysisCause, oldAnalysisBuilderThread, nature,
                    documentTime, analysisKey, resourceModificationStamp);

            logCreate(moduleName, analysisBuilderThread, "Factory: changed");

            available.put(analysisKey, analysisBuilderThread);
            return analysisBuilderThread;
        }
    }

    /**
     * Creates a runnable for a module removal.
     * @return The new runnable or null if there's one there already that has a higher document version.
     */
    /*Default*/static IAnalysisBuilderRunnable createRunnable(String moduleName, IPythonNature nature,
            boolean fullBuild, boolean forceAnalysis, int analysisCause, long documentTime,
            long resourceModificationStamp) {

        synchronized (lock) {
            Map<KeyForAnalysisRunnable, IAnalysisBuilderRunnable> available = getAvailableThreads();
            KeyForAnalysisRunnable analysisKey = areNatureAndProjectAndTimeOK(nature, moduleName, documentTime,
                    available, resourceModificationStamp, analysisCause);
            if (analysisKey == null) {
                return null;
            }
            IAnalysisBuilderRunnable oldAnalysisBuilderThread = available.get(analysisKey);
            if (oldAnalysisBuilderThread != null) {
                //there is some existing thread that we have to stop to create the new one
                oldAnalysisBuilderThread.stopAnalysis();
                logStop(oldAnalysisBuilderThread, "Factory: remove");
            }
            IAnalysisBuilderRunnable analysisBuilderThread = new AnalysisBuilderRunnableForRemove(moduleName, nature,
                    fullBuild, oldAnalysisBuilderThread, forceAnalysis, analysisCause, documentTime, analysisKey,
                    resourceModificationStamp);

            logCreate(moduleName, analysisBuilderThread, "Factory: remove");

            available.put(analysisKey, analysisBuilderThread);
            return analysisBuilderThread;
        }
    }

}
