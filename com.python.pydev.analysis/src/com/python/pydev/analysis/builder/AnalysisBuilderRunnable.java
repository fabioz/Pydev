/*
 * Created on Apr 6, 2006
 */
package com.python.pydev.analysis.builder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderPrefPage;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.IModule;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.DebugSettings;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.OccurrencesAnalyzer;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.messages.IMessage;

/**
 * This class is used to do analysis on a thread, so that if an analysis is asked for some analysis that
 * is already in progress, that analysis will be stopped and this one will begin.
 * 
 * @author Fabio
 */
public class AnalysisBuilderRunnable implements Runnable{
    
    /**
     * Field that should know all the threads.
     */
    private volatile static Map<String, AnalysisBuilderRunnable> availableThreads;
    
    
    /**
     * These are the callbacks called whenever there's a run to be done in this class.
     */
    public static final List<ICallback<Object, IResource>> analysisBuilderListeners = 
        new ArrayList<ICallback<Object,IResource>>();
    
    
    /**
     * @return Returns the availableThreads.
     */
    private static synchronized Map<String, AnalysisBuilderRunnable> getAvailableThreads() {
        if(availableThreads == null){
            availableThreads = Collections.synchronizedMap(new HashMap<String, AnalysisBuilderRunnable>());
        }
        return availableThreads;
    }
    
    private synchronized void removeFromThreads() {
        Map<String, AnalysisBuilderRunnable> available = getAvailableThreads();
        synchronized(available){
            AnalysisBuilderRunnable analysisBuilderThread = available.get(moduleName);
            if(analysisBuilderThread == this){
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
    public static synchronized AnalysisBuilderRunnable createRunnable(IDocument document, IResource resource, 
    		ICallback<IModule, Integer> module, boolean analyzeDependent, IProgressMonitor monitor, boolean isFullBuild, 
            String moduleName, boolean forceAnalysis, int analysisCause){
        
        Map<String, AnalysisBuilderRunnable> available = getAvailableThreads();
        synchronized(available){
            AnalysisBuilderRunnable analysisBuilderThread = available.get(moduleName);
            if(analysisBuilderThread != null){
                //there is some existing thread that we have to stop to create the new one
                analysisBuilderThread.stopAnalysis();
                if(!forceAnalysis){
                	forceAnalysis = analysisBuilderThread.forceAnalysis;
                }
                if(!forceAnalysis){
                	if(PyDevBuilderPrefPage.getAnalyzeOnlyActiveEditor()){
	                	if(analysisCause == ANALYSIS_CAUSE_BUILDER && analysisBuilderThread.analysisCause != ANALYSIS_CAUSE_BUILDER){
	                		//we're stopping a previous analysis that would really happen, so, let's force this one
	                		forceAnalysis = true;
	                	}
                	}
                }
            }
            analysisBuilderThread = new AnalysisBuilderRunnable(document, resource, module, analyzeDependent, 
                    monitor, isFullBuild, moduleName, forceAnalysis, analysisCause);
            
            available.put(moduleName, analysisBuilderThread);
            return analysisBuilderThread;
        }
    }
    
    // -------------------------------------------------------------------------------------------- ATTRIBUTES

    private IDocument document;
    private WeakReference<IResource> resource;
    private ICallback<IModule, Integer> module;
    private boolean analyzeDependent;
    private IProgressMonitor monitor;
    private IProgressMonitor internalCancelMonitor;
    private boolean isFullBuild;
    private String moduleName;
    private boolean forceAnalysis;
    
    public static final int ANALYSIS_CAUSE_BUILDER = 1;
    public static final int ANALYSIS_CAUSE_PARSER = 2;
    private int analysisCause;
    private Object lock = new Object();
    
    public static final int FULL_MODULE = 1;
    public static final int DEFINITIONS_MODULE = 2;
    // ---------------------------------------------------------------------------------------- END ATTRIBUTES
    
    
    /**
     * @param module: this is a callback that'll be called with a boolean that should return the IModule to be used in the
     * analysis.
     * The parameter is FULL_MODULE or DEFINITIONS_MODULE
     */
    private AnalysisBuilderRunnable(IDocument document, IResource resource, ICallback<IModule, Integer> module, boolean analyzeDependent, 
            IProgressMonitor monitor, boolean isFullBuild, String moduleName, boolean forceAnalysis, int analysisCause) {
        this.document = document;
        this.resource = new WeakReference<IResource>(resource);
        this.module = module;
        this.analyzeDependent = analyzeDependent;
        this.monitor = monitor;
        this.isFullBuild = isFullBuild;
        this.moduleName = moduleName;
        this.internalCancelMonitor = new NullProgressMonitor();
        this.forceAnalysis = forceAnalysis;
        this.analysisCause = analysisCause;
    }

    private void dispose() {
        this.document = null;
        this.resource = null;
        this.module = null;
        this.monitor = null;
        this.moduleName = null;
        this.internalCancelMonitor = null;
    }
    
    public void stopAnalysis() {
        this.internalCancelMonitor.setCanceled(true);
    }
    
    private void checkStop(){
        if(this.internalCancelMonitor.isCanceled() || monitor.isCanceled()){
            throw new OperationCanceledException();
        }
    }
    
    public void run() {
        try{
            doAnalysis();
        }catch(NoClassDefFoundError e){
            //ignore, plugin finished and thread still active
        }
    }
    
    public void doAnalysis(){
        try {
            if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
                Log.toLogFile(this, "doAnalysis()");
            }
            //if the resource is not open, there's not much we can do...
            IResource r = resource.get();
            if(r == null || !r.getProject().isOpen()){
                return;
            }
            
            AnalysisRunner runner = new AnalysisRunner();
            checkStop();
            
            IAnalysisPreferences analysisPreferences = AnalysisPreferences.getAnalysisPreferences();
            //update the severities, etc.
            analysisPreferences.clearCaches();

            boolean makeAnalysis = runner.canDoAnalysis(document) && 
                PyDevBuilderVisitor.isInPythonPath(r) && //just get problems in resources that are in the pythonpath
                analysisPreferences.makeCodeAnalysis();
            
            
            if (!makeAnalysis) {
                //let's see if we should do code analysis
                synchronized(lock){
                    AnalysisRunner.deleteMarkers(r);
                }
            }

            checkStop();
            PythonNature nature = PythonNature.getPythonNature(r);

            
            if(r == null){
                return;
            }
            AbstractAdditionalInterpreterInfo info = AdditionalProjectInterpreterInfo.
                getAdditionalInfoForProject(nature);
            
            if(info == null){
                return;
            }
            
            //Note that if this becomes something slow, we could use: http://www.twmacinta.com/myjava/fast_md5.php
            //as an option.
//            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
//            digest.update(document.get().getBytes());
//            byte[] hash = digest.digest();
            
            //remove dependency information (and anything else that was already generated), but first, gather 
            //the modules dependent on this one.
            if(!isFullBuild){

//                if(!forceAnalysis){
//                    //if the analysis is not forced, we can decide to stop the process of analyzing it if the hash 
//                    //is still the same
//                    if(Arrays.equals(hash, info.getLastModificationHash(moduleName))){
//                        if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
//                            Log.toLogFile(this, "Skipping: hash is still the same for: "+moduleName);
//                        }
//                        return; //nothing changed
//                    }
//                }

                
                //if it is a full build, that info is already removed -- as well as the time
                AnalysisBuilderVisitor.fillDependenciesAndRemoveInfo(moduleName, nature, analyzeDependent, 
                        monitor, isFullBuild);
            }

            
            boolean onlyRecreateCtxInsensitiveInfo = !forceAnalysis && analysisCause == ANALYSIS_CAUSE_BUILDER && PyDevBuilderPrefPage.getAnalyzeOnlyActiveEditor();
            
            int moduleRequest;
            if(onlyRecreateCtxInsensitiveInfo){
            	moduleRequest = DEFINITIONS_MODULE;
            }else{
            	moduleRequest = FULL_MODULE;
            }
            
			SourceModule module = (SourceModule) this.module.call(moduleRequest);
            //recreate the ctx insensitive info
            recreateCtxInsensitiveInfo(info, module, nature, r);
            
            if(onlyRecreateCtxInsensitiveInfo){
                if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
                    Log.toLogFile(this, "Skipping: analysisCause == ANALYSIS_CAUSE_BUILDER && " +
                    		"PyDevBuilderPrefPage.getAnalyzeOnlyActiveEditor()");
                }
                return;
            }
            
            //let's see if we should continue with the process
            if(!makeAnalysis){
                if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
                    Log.toLogFile(this, "Skipping: !makeAnalysis");
                }
                return;
            }
            
            if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
                String analysisCauseStr;
                if(analysisCause == ANALYSIS_CAUSE_BUILDER){
                    analysisCauseStr = "Builder";
                }else if(analysisCause == ANALYSIS_CAUSE_PARSER){
                    analysisCauseStr = "Parser";
                }else{
                    analysisCauseStr = "Unknown?";
                }
                
                Log.toLogFile(this, "makeAnalysis:"+makeAnalysis+" " +
                		"analysisCause: "+analysisCauseStr);
            }
            
            //if there are callbacks registered, call them (mostly for tests)
            for(ICallback<Object, IResource> callback:analysisBuilderListeners){
                callback.call(r);
            }

            //monitor.setTaskName("Analyzing module: " + moduleName);
            monitor.worked(1);

            checkStop();
            OccurrencesAnalyzer analyzer = new OccurrencesAnalyzer();

            //ok, let's do it
            checkStop();
            IMessage[] messages = analyzer.analyzeDocument(nature, module, analysisPreferences, 
                    document, this.internalCancelMonitor);
            
            if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
                Log.toLogFile(this, "Adding markers for module: "+moduleName);
                for (IMessage message : messages) {
                    Log.toLogFile(this, message.toString());
                }
            }
            
            monitor.worked(1);
            
            //last chance to stop...
            checkStop();
            
            //don't stop after setting to add / remove the markers
            r = resource.get();
            if(r != null){
                runner.setMarkers(r, document, messages);
            }
            
            //set the new time only after the analysis is finished
//            info.setLastModificationHash(moduleName, hash);

        } catch (OperationCanceledException e) {
            //ok, ignore it
            Log.toLogFile(this, "OperationCanceledException: cancelled by new runnable");
        } catch (Exception e){
            PydevPlugin.log(e);
        } finally{
            try{
                removeFromThreads();
            }catch (Throwable e){
                PydevPlugin.log(e);
            }
            dispose();
        }
    }

    
    /**
     * @return false if there's no modification among the current version of the file and the last version analyzed.
     */
    private void recreateCtxInsensitiveInfo(AbstractAdditionalInterpreterInfo info, IModule sourceModule, 
            PythonNature nature, IResource r) {
        
        //info.removeInfoFromModule(sourceModule.getName()); -- does not remove info from the module because this
        //should be already done once it gets here (the AnalysisBuilder, that also makes dependency info 
        //should take care of this).
        boolean generateDelta;
        if(isFullBuild){
            generateDelta = false;
        }else{
            generateDelta = true;
        }
        
        if (sourceModule instanceof SourceModule) {
            SourceModule m = (SourceModule) sourceModule;
            info.addSourceModuleInfo(m, nature, generateDelta);
        }
    }


}
