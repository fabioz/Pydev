package com.python.pydev.analysis.builder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderPrefPage;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.cache.LRUCache;
import org.python.pydev.core.log.Log;
import org.python.pydev.logging.DebugSettings;

public class AnalysisBuilderRunnableFactory {

    /**
     * Field that should know all the threads.
     */
    private volatile static Map<String, IAnalysisBuilderRunnable> availableThreads;
    
    /**
     * Cache to keep the last request time for a module. The key is the project name+module name and the value the documentTime
     * for the last analysis request for some module.
     */
    private volatile static LRUCache<String, Long> analysisTimeCache = new LRUCache<String, Long>(100);
    
    
    /**
     * @return Returns the availableThreads.
     */
    private static synchronized Map<String, IAnalysisBuilderRunnable> getAvailableThreads() {
        if(availableThreads == null){
            availableThreads = Collections.synchronizedMap(new HashMap<String, IAnalysisBuilderRunnable>());
        }
        return availableThreads;
    }
    
    /*Default*/ static synchronized void removeFromThreads(String moduleName, IAnalysisBuilderRunnable runnable) {
        Map<String, IAnalysisBuilderRunnable> available = getAvailableThreads();
        synchronized(available){
            IAnalysisBuilderRunnable analysisBuilderThread = available.get(moduleName);
            if(analysisBuilderThread == runnable){
                available.remove(moduleName);
            }
        }
    }
    
    
    // Logging Helpers -------------------------------


    private static void logCreate(String moduleName, IAnalysisBuilderRunnable analysisBuilderThread, String factory) {
        if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
            Log.toLogFile(analysisBuilderThread, "Created new builder: "+analysisBuilderThread+" for:"+moduleName+
                    " -- "+analysisBuilderThread.getAnalysisCauseStr()+ " -- "+factory);
        }
    }

    private static void logStop(IAnalysisBuilderRunnable oldAnalysisBuilderThread, String creation) {
        if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
            Log.toLogFile(oldAnalysisBuilderThread, "Stopping previous builder: "+oldAnalysisBuilderThread+
                    " ("+oldAnalysisBuilderThread.getModuleName()+" -- "+oldAnalysisBuilderThread.getAnalysisCauseStr()+
                    ") to create new. "+creation);
        }
    }
    
    private static void logExistingDocumentTimeHigher(IAnalysisBuilderRunnable oldAnalysisBuilderThread) {
        if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
            Log.toLogFile(oldAnalysisBuilderThread, "The document time from an existing is higher than a new one, so, leave it be...");
        }
    }

    // End Logging Helpers -------------------------------


    
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
     * 
     * @return The analysis key if all check were OK or null if some check failed.
     */
    private static String areNatureAndProjectAndTimeOK(IPythonNature nature, String moduleName, long documentTime, Map<String, IAnalysisBuilderRunnable> available) {
        if(nature == null){
            return null;
        }
        
        IProject project = nature.getProject();
        if(project == null || !project.isOpen()){
            return null;
        }
        
        String analysisKey = project.getName() + " - "+ moduleName;
        IAnalysisBuilderRunnable oldAnalysisBuilderThread = available.get(analysisKey);
        
        if(oldAnalysisBuilderThread != null && oldAnalysisBuilderThread.getDocumentTime() > documentTime){
            //If the document version of the new one is lower than the one already active, don't do the analysis
            logExistingDocumentTimeHigher(oldAnalysisBuilderThread);
            return null;
        }
        
        Long lastTime = analysisTimeCache.getObj(analysisKey);
        if(lastTime != null && lastTime > documentTime){
            //If the document version of the new one is lower than the one already active, don't do the analysis
            logExistingDocumentTimeHigher(oldAnalysisBuilderThread);
            return null;
        }
        analysisTimeCache.add(analysisKey, documentTime);

        
        return analysisKey;
    }
    
    
    
    // Factory creation methods -----------------------------------------
    
    /**
     * Creates a thread for analyzing some module (and stopping analysis of some other thread if there is one
     * already running).
     *  
     * @return The new runnable or null if there's one there already that has a higher document version.
     */
    /*Default*/ static synchronized IAnalysisBuilderRunnable createRunnable(IDocument document, IResource resource, 
            ICallback<IModule, Integer> module, boolean isFullBuild, 
            String moduleName, boolean forceAnalysis, int analysisCause, IPythonNature nature, long documentTime){
        
        
        Map<String, IAnalysisBuilderRunnable> available = getAvailableThreads();
        synchronized(available){
            String analysisKey = areNatureAndProjectAndTimeOK(nature, moduleName, documentTime, available);
            if(analysisKey == null){
                return null;
            }
            
            IAnalysisBuilderRunnable oldAnalysisBuilderThread = available.get(analysisKey);
            if(oldAnalysisBuilderThread != null){
                //there is some existing thread that we have to stop to create the new one
                oldAnalysisBuilderThread.stopAnalysis();
                logStop(oldAnalysisBuilderThread, "Factory: changed");
                
                if(!forceAnalysis){
                    forceAnalysis = oldAnalysisBuilderThread.getForceAnalysis();
                }
                if(!forceAnalysis){
                    if(PyDevBuilderPrefPage.getAnalyzeOnlyActiveEditor()){
                        if(analysisCause == IAnalysisBuilderRunnable.ANALYSIS_CAUSE_BUILDER && 
                                oldAnalysisBuilderThread.getAnalysisCause() != IAnalysisBuilderRunnable.ANALYSIS_CAUSE_BUILDER){
                            //we're stopping a previous analysis that would really happen, so, let's force this one
                            forceAnalysis = true;
                        }
                    }
                }
            }
            IAnalysisBuilderRunnable analysisBuilderThread = new AnalysisBuilderRunnable(document, resource, module, 
                    isFullBuild, moduleName, forceAnalysis, analysisCause, oldAnalysisBuilderThread, nature, documentTime);
            
            logCreate(moduleName, analysisBuilderThread, "Factory: changed");
            
            available.put(analysisKey, analysisBuilderThread);
            return analysisBuilderThread;
        }
    }



    /**
     * Creates a runnable for a module removal.
     * @return The new runnable or null if there's one there already that has a higher document version.
     */
    /*Default*/ static IAnalysisBuilderRunnable createRunnable(String moduleName, IPythonNature nature, boolean fullBuild,
            boolean forceAnalysis, int analysisCause, long documentTime) {
        

        Map<String, IAnalysisBuilderRunnable> available = getAvailableThreads();
        synchronized(available){
            String analysisKey = areNatureAndProjectAndTimeOK(nature, moduleName, documentTime, available);
            if(analysisKey == null){
                return null;
            }
            IAnalysisBuilderRunnable oldAnalysisBuilderThread = available.get(analysisKey);
            if(oldAnalysisBuilderThread != null){
                //there is some existing thread that we have to stop to create the new one
                oldAnalysisBuilderThread.stopAnalysis();
                logStop(oldAnalysisBuilderThread, "Factory: remove");
            }
            IAnalysisBuilderRunnable analysisBuilderThread = new AnalysisBuilderRunnableForRemove(moduleName, nature, 
                    fullBuild, oldAnalysisBuilderThread, forceAnalysis, analysisCause, documentTime);
            
            logCreate(moduleName, analysisBuilderThread, "Factory: remove");

            available.put(analysisKey, analysisBuilderThread);
            return analysisBuilderThread;
        }
    }
    
}
