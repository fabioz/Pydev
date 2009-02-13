/*
 * Created on Apr 6, 2006
 */
package com.python.pydev.analysis.builder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderPrefPage;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.logging.DebugSettings;
import org.python.pydev.plugin.PydevPlugin;

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
public class AnalysisBuilderRunnable extends AbstractAnalysisBuilderRunnable{
    
    /**
     * These are the callbacks called whenever there's a run to be done in this class.
     */
    public static final List<ICallback<Object, IResource>> analysisBuilderListeners = 
        new ArrayList<ICallback<Object,IResource>>();


    // -------------------------------------------------------------------------------------------- ATTRIBUTES

    private IDocument document;
    private WeakReference<IResource> resource;
    private ICallback<IModule, Integer> module;
    
    // ---------------------------------------------------------------------------------------- END ATTRIBUTES
    

    
    /**
     * @param oldAnalysisBuilderThread This is an existing runnable that was already analyzing things... we must wait for it
     * to finish to start it again.
     * 
     * @param module: this is a callback that'll be called with a boolean that should return the IModule to be used in the
     * analysis.
     * The parameter is FULL_MODULE or DEFINITIONS_MODULE
     */
    /*Default*/ AnalysisBuilderRunnable(IDocument document, IResource resource, ICallback<IModule, Integer> module, 
            boolean isFullBuild, String moduleName, boolean forceAnalysis, int analysisCause, 
            IAnalysisBuilderRunnable oldAnalysisBuilderThread, IPythonNature nature) {
        super(isFullBuild, moduleName, forceAnalysis, analysisCause, oldAnalysisBuilderThread, nature);
        
        this.document = document;
        this.resource = new WeakReference<IResource>(resource);
        this.module = module;
    }

    protected void dispose() {
        this.document = null;
        this.resource = null;
        this.module = null;
    }
    
    
    protected void doAnalysis(){
        try {
            if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
                Log.toLogFile(this, "doAnalysis() - "+moduleName);
            }
            //if the resource is not open, there's not much we can do...
            IResource r = resource.get();
            if(r == null || !r.getProject().isOpen()){
                Log.toLogFile(this, "Finished analysis -- resource null or project closed -- "+moduleName);
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
                AnalysisRunner.deleteMarkers(r);
            }

            if(nature == null){
                Log.log("Finished analysis: null nature -- "+moduleName);
                return;
            }
            AbstractAdditionalInterpreterInfo info = AdditionalProjectInterpreterInfo.
                getAdditionalInfoForProject(nature);
            
            if(info == null){
                Log.log("Unable to get additional info for: "+r+" -- "+moduleName);
                return;
            }
            
            checkStop();
            //remove dependency information (and anything else that was already generated), but first, gather 
            //the modules dependent on this one.
            if(!isFullBuild){

                //if it is a full build, that info is already removed
                AnalysisBuilderRunnableForRemove.removeInfoForModule(moduleName, nature, isFullBuild);
            }

            
            boolean onlyRecreateCtxInsensitiveInfo = !forceAnalysis && 
                analysisCause == ANALYSIS_CAUSE_BUILDER && 
                PyDevBuilderPrefPage.getAnalyzeOnlyActiveEditor();
            
            int moduleRequest;
            if(onlyRecreateCtxInsensitiveInfo){
            	moduleRequest = DEFINITIONS_MODULE;
            }else{
            	moduleRequest = FULL_MODULE;
            }
            
            
            //get the module for the analysis
            checkStop();
			SourceModule module = (SourceModule) this.module.call(moduleRequest);
			
			
			
			checkStop();
			//recreate the ctx insensitive info
            recreateCtxInsensitiveInfo(info, module, nature, r);
            
            if(onlyRecreateCtxInsensitiveInfo){
                if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
                    Log.toLogFile(this, "Skipping: !forceAnalysis && analysisCause == ANALYSIS_CAUSE_BUILDER && " +
                    		"PyDevBuilderPrefPage.getAnalyzeOnlyActiveEditor() -- "+moduleName);
                }
                return;
            }
            
            //let's see if we should continue with the process
            if(!makeAnalysis){
                if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
                    Log.toLogFile(this, "Skipping: !makeAnalysis -- "+moduleName);
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
                		"analysisCause: "+analysisCauseStr+" -- "+moduleName);
            }
            
            checkStop();
            OccurrencesAnalyzer analyzer = new OccurrencesAnalyzer();

            //ok, let's do it
            checkStop();
            IMessage[] messages = analyzer.analyzeDocument(nature, module, analysisPreferences, 
                    document, this.internalCancelMonitor);
            
            checkStop();
            if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
                Log.toLogFile(this, "Adding markers for module: "+moduleName);
                for (IMessage message : messages) {
                    Log.toLogFile(this, message.toString());
                }
            }
            
            //last chance to stop...
            checkStop();
            
            //don't stop after setting to add / remove the markers
            r = resource.get();
            if(r != null){
                runner.setMarkers(r, document, messages, this.internalCancelMonitor);
            }
            
            //if there are callbacks registered, call them if we still didn't return (mostly for tests)
            for(ICallback<Object, IResource> callback:analysisBuilderListeners){
                callback.call(resource.get());
            }

        } catch (OperationCanceledException e) {
            //ok, ignore it
            if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
                Log.toLogFile(this, "OperationCanceledException: cancelled by new runnable -- "+moduleName);
            }
        } catch (Exception e){
            PydevPlugin.log(e);
        } finally{
            try{
                AnalysisBuilderRunnableFactory.removeFromThreads(moduleName, this);
            }catch (Throwable e){
                PydevPlugin.log(e);
            }
            
            dispose();
        }
        

    }

    
    /**
     * @return false if there's no modification among the current version of the file and the last version analyzed.
     */
    private void recreateCtxInsensitiveInfo(AbstractAdditionalInterpreterInfo info, SourceModule sourceModule, 
            IPythonNature nature, IResource r) {
        
        //info.removeInfoFromModule(sourceModule.getName()); -- does not remove info from the module because this
        //should be already done once it gets here (the AnalysisBuilder, that also makes dependency info 
        //should take care of this).
        boolean generateDelta;
        if(isFullBuild){
            generateDelta = false;
        }else{
            generateDelta = true;
        }
        info.addSourceModuleInfo(sourceModule, nature, generateDelta);
    }


}
