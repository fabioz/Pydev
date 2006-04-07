/*
 * Created on Apr 6, 2006
 */
package com.python.pydev.analysis.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.IModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.OcurrencesAnalyzer;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.messages.IMessage;

/**
 * This class is used to do analysis on a thread, so that if an analysis is asked for some analysis that
 * is already in progress, that analysis will be stopped and this one will begin.
 * 
 * @author Fabio
 */
public class AnalysisBuilderThread extends Thread{
    
    /**
     * Field that should know all the threads.
     */
    private static Map<String, AnalysisBuilderThread> availableThreads;
    
    /**
     * @return Returns the availableThreads.
     */
    private static Map<String, AnalysisBuilderThread> getAvailableThreads() {
        if(availableThreads == null){
            availableThreads = Collections.synchronizedMap(new HashMap<String, AnalysisBuilderThread>());
        }
        return availableThreads;
    }
    
    /**
     * Creates a thread for analyzing some module (and stopping analysis of some other thread if there is one
     * already running).
     *  
     * @param moduleName the name of the module
     * @return a builder thread.
     */
    public static synchronized AnalysisBuilderThread createThread(IDocument document, IResource resource, IModule module, boolean analyzeDependent, IProgressMonitor monitor, boolean isFullBuild, String moduleName){
        Map<String, AnalysisBuilderThread> available = getAvailableThreads();
        synchronized(available){
            AnalysisBuilderThread analysisBuilderThread = available.get(moduleName);
            if(analysisBuilderThread != null){
                //there is some existing thread that we have to stop to create the new one
                analysisBuilderThread.stopAnalysis();
            }
            analysisBuilderThread = new AnalysisBuilderThread(document, resource, module, analyzeDependent, monitor, isFullBuild, moduleName);
            available.put(moduleName, analysisBuilderThread);
            return analysisBuilderThread;
        }
    }

    private IDocument document;
    private IResource resource;
    private IModule module;
    private boolean analyzeDependent;
    private IProgressMonitor monitor;
    private IProgressMonitor internalCancelMonitor;
    private boolean isFullBuild;
    private String moduleName;
    
    public AnalysisBuilderThread(IDocument document, IResource resource, IModule module, boolean analyzeDependent, IProgressMonitor monitor, boolean isFullBuild, String moduleName) {
        this.document = document;
        this.resource = resource;
        this.module = module;
        this.analyzeDependent = analyzeDependent;
        this.monitor = monitor;
        this.isFullBuild = isFullBuild;
        this.moduleName = moduleName;
        this.internalCancelMonitor = new NullProgressMonitor();
    }

    public void stopAnalysis() {
        this.internalCancelMonitor.setCanceled(true);
    }
    
    private void checkStop(){
        if(this.internalCancelMonitor.isCanceled() || monitor.isCanceled()){
            throw new CancelledException();
        }
    }
    
    @Override
    public void run() {
        doAnalysis();
    }
    
    public void doAnalysis(){
        try {
            AnalysisRunner runner = new AnalysisRunner();
            checkStop();
            
            IAnalysisPreferences analysisPreferences = AnalysisPreferences.getAnalysisPreferences();
            analysisPreferences.clearCaches();

            if (!runner.canDoAnalysis(document) || !PyDevBuilderVisitor.isInPythonPath(resource) || //just get problems in resources that are in the pythonpath
                    analysisPreferences.makeCodeAnalysis() == false //let's see if we should do code analysis
            ) {
                runner.deleteMarkers(resource);
                return;
            }

            checkStop();
            PythonNature nature = PythonNature.getPythonNature(resource.getProject());

            //remove dependency information (and anything else that was already generated), but first, gather the modules dependent on this one.
            AnalysisBuilderVisitor.fillDependenciesAndRemoveInfo(moduleName, nature, analyzeDependent, monitor, isFullBuild);
            recreateCtxInsensitiveInfo(resource, document, module, nature);

            monitor.setTaskName("Analyzing module: " + moduleName);
            monitor.worked(1);

            checkStop();
            OcurrencesAnalyzer analyzer = new OcurrencesAnalyzer();

            ArrayList<IMarker> existing = new ArrayList<IMarker>();
            try {
                IMarker[] found = resource.findMarkers(AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
                for (IMarker marker : found) {
                    existing.add(marker);
                }
            } catch (CoreException e) {
                //ignore it
                PydevPlugin.log(e);
            } 
            
            //ok, let's do it
            checkStop();
            IMessage[] messages = analyzer.analyzeDocument(nature, (SourceModule) module, analysisPreferences, document, this.internalCancelMonitor);
            monitor.setTaskName("Adding markers for module: "+moduleName);
            monitor.worked(1);
            runner.addMarkers(resource, document, messages, existing);
            
            checkStop();
            for (IMarker marker : existing) {
                try {
                    marker.delete();
                } catch (CoreException e) {
                    PydevPlugin.log(e);
                }
            }
        } catch (CancelledException e) {
            //ok, ignore it
            //System.out.println("Ok, canceled previous");
        } catch (Exception e){
            PydevPlugin.log(e);
        }
        
    }
    
    private void recreateCtxInsensitiveInfo(IResource resource, IDocument document, IModule sourceModule, PythonNature nature) {
        AbstractAdditionalInterpreterInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature.getProject());
        
        //info.removeInfoFromModule(sourceModule.getName()); -- does not remove info from the module because this should be already
        //done once it gets here (the AnalysisBuilder, that also makes dependency info should take care of this).
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
