package com.python.pydev.analysis.builder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderPrefPage;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.logging.DebugSettings;

public class AnalysisBuilderRunnableFactory {

    /**
     * Field that should know all the threads.
     */
    private volatile static Map<String, IAnalysisBuilderRunnable> availableThreads;
    
    
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


    /**
     * Creates a thread for analyzing some module (and stopping analysis of some other thread if there is one
     * already running).
     *  
     * @param moduleName the name of the module
     * @return a builder thread.
     */
    /*Default*/ static synchronized IAnalysisBuilderRunnable createRunnable(IDocument document, IResource resource, 
            ICallback<IModule, Integer> module, boolean isFullBuild, 
            String moduleName, boolean forceAnalysis, int analysisCause, IPythonNature nature){
        
        Map<String, IAnalysisBuilderRunnable> available = getAvailableThreads();
        synchronized(available){
            IAnalysisBuilderRunnable oldAnalysisBuilderThread = available.get(moduleName);
            if(oldAnalysisBuilderThread != null){
                if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
                    Log.toLogFile(oldAnalysisBuilderThread, "Stopping previous builder: "+oldAnalysisBuilderThread+
                            " ("+oldAnalysisBuilderThread.getModuleName()+") to create new.");
                }
                
                //there is some existing thread that we have to stop to create the new one
                oldAnalysisBuilderThread.stopAnalysis();
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
                    isFullBuild, moduleName, forceAnalysis, analysisCause, oldAnalysisBuilderThread, nature);
            if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
                Log.toLogFile(analysisBuilderThread, "Created new builder: "+analysisBuilderThread+" for:"+moduleName);
            }
            
            available.put(moduleName, analysisBuilderThread);
            return analysisBuilderThread;
        }
    }

    public static IAnalysisBuilderRunnable createRunnable(String moduleName, IPythonNature nature, boolean fullBuild,
            boolean forceAnalysis, int analysisCause) {
        Map<String, IAnalysisBuilderRunnable> available = getAvailableThreads();
        synchronized(available){
            IAnalysisBuilderRunnable oldAnalysisBuilderThread = available.get(moduleName);
            if(oldAnalysisBuilderThread != null){
                //there is some existing thread that we have to stop to create the new one
                oldAnalysisBuilderThread.stopAnalysis();
                if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
                    Log.toLogFile(oldAnalysisBuilderThread, "Stopping previous builder: "+oldAnalysisBuilderThread+
                            " ("+oldAnalysisBuilderThread.getModuleName()+") to create new.");
                }
            }
            IAnalysisBuilderRunnable analysisBuilderThread = new AnalysisBuilderRunnableForRemove(moduleName, nature, 
                    fullBuild, oldAnalysisBuilderThread, forceAnalysis, analysisCause);
            
            if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
                Log.toLogFile(analysisBuilderThread, "Created new builder: "+analysisBuilderThread+" for:"+moduleName);
            }

            available.put(moduleName, analysisBuilderThread);
            return analysisBuilderThread;
        }
    }
    
}
