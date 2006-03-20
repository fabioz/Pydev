/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.builder;

import java.util.ArrayList;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.PyCodeCompletionVisitor;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.OcurrencesAnalyzer;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.dependencies.PyStructuralChange;
import com.python.pydev.analysis.messages.IMessage;

public class AnalysisBuilderVisitor extends PyDevBuilderVisitor{
    

    /**
     * keeps the module name and the nature used in the visit
     */
    private ArrayList<Tuple<String, PythonNature>> dependentModulesToAnalyze;

    /**
     * Do you want to gather information about dependencies?
     */
    public static final boolean DEBUG_DEPENDENCIES = false;

    @Override
    protected int getPriority() {
    	return PyCodeCompletionVisitor.PRIORITY_CODE_COMPLETION+1; //just after the code-completion priority
    }

    @Override
    public void visitingWillStart(IProgressMonitor monitor) {
        super.visitingWillStart(monitor);
        if(DEBUG_DEPENDENCIES){
        	System.out.println("Visiting will start... creating modules to analyze.");
        }
        this.dependentModulesToAnalyze = new ArrayList<Tuple<String, PythonNature>>();
    }

    /**
     * When we finish visiting, the dependent modules found should be visited. When this analysis
     * is done, it does not get dependencies for later analysis (all the dependent modules should be already
     * set to be visited now).
     *  
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitingEnded(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void visitingEnded(IProgressMonitor monitor) {
        super.visitingEnded(monitor);
        
//        //ok, now, before we call the visit over, let's analyze the dependent modules
//        monitor.setTaskName("Visiting dependencies...");
//        if(DEBUG_DEPENDENCIES){
//        	System.out.println("There are "+dependentModulesToAnalyze.size()+" dependent modules to analyze:");
//        	for (Tuple<String, PythonNature> modNameAndNature : dependentModulesToAnalyze) {
//        		System.out.println("Module that will be analyzed: "+modNameAndNature.o1);
//        	}
//        }
//
//        for (Tuple<String, PythonNature> modNameAndNature : dependentModulesToAnalyze) {
//        	if(monitor.isCanceled()){
//        		break;
//        	}
//        	
//            PythonNature nature = modNameAndNature.o2;
//            String modName = modNameAndNature.o1;
//            AbstractModule module = nature.getAstManager().getProjectModulesManager().getModule(modName, nature, false, false);
//            
//            if(module instanceof SourceModule){
//                //if it is a source module, let's get the resource for it
//                SourceModule mod = (SourceModule) module;
//                
//                this.memo = new HashMap<String, Object>();//clear the cache, just to be sure of it (because we are re-using the same instance to make new visits)
//                setModuleInCache(mod);
//                setModuleNameInCache(modName);
//                
//                IPath path = Path.fromOSString(REF.getFileAbsolutePath(mod.getFile()));
//                if(DEBUG_DEPENDENCIES){
//                    System.out.println("visiting dependent "+mod.getName());
//                }
//                
//                monitor.setTaskName("Visiting dependency: "+mod.getName());
//                monitor.worked(1);
//                
//                IProject project = nature.getProject();
//                
//                //make the path relative to the project
//                int i = path.matchingFirstSegments(project.getLocation());
//                path = path.removeFirstSegments(i);
//                IFile file = file = project.getFile(path);
//                visitChangedResource(file, PyDevBuilder.getDocFromResource(file), module, false, monitor);
//            }
//        }
        
        
        this.dependentModulesToAnalyze.clear();
    }
    
    
    @Override
    public void visitChangedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
    	//we may need to 'force' the analysis when a module is renamed, because the first message we receive is
    	//a 'delete' and after that an 'add' -- which is later mapped to this method, so, if we don't have info
    	//on the module we should analyze it because it is 'probably' a rename.
        String moduleName = getModuleName(resource);
        PythonNature nature = getPythonNature(resource);
        boolean force = false;
        if(nature != null && moduleName != null){
        	AbstractAdditionalInterpreterInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature.getProject());
        	if(!info.hasInfoOn(moduleName)){
        		force = true;
        	}
        }

        boolean fullBuild = isFullBuild();
        if(fullBuild || force ||
           AnalysisPreferences.getAnalysisPreferences().getWhenAnalyze() == IAnalysisPreferences.ANALYZE_ON_SAVE){
            
            boolean analyzeDependent;
            if(fullBuild){
                analyzeDependent = false;
            }else{
                analyzeDependent = true;
            }
            doVisitChangedResource(resource, document, null, analyzeDependent, monitor);
        }
    }
    

    
    /**
     * @param resource the resource we want to know about
     * @return true if it is in the pythonpath
     */
    protected boolean isInPythonPath(IResource resource){
        IProject project = resource.getProject();
        PythonNature nature = PythonNature.getPythonNature(project);
        if(project != null && nature != null){
            ICodeCompletionASTManager astManager = nature.getAstManager();
            if(astManager == null){
            	//this is needed because it may not be restarted already...
            	//also, this will only happen when initializing eclipse with some editors already open
            	
            	for(int i=0; i<10 && astManager == null; i++){ //we will wait 10 seconds for it
            		try {
						Thread.sleep(1000);
					} catch (Exception e) {
						e.printStackTrace();
					}
					astManager = nature.getAstManager();
            	}
            }
            if(astManager != null){
                IModulesManager modulesManager = astManager.getModulesManager();
                return modulesManager.isInPythonPath(resource, project);
            }
        }

        return false;
    }

    /**
     * here we have to detect errors / warnings from the code analysis
     */
    public void doVisitChangedResource(IResource resource, IDocument document, IModule module, boolean analyzeDependent, IProgressMonitor monitor) {
        AnalysisRunner runner = new AnalysisRunner();
        
        IAnalysisPreferences analysisPreferences = AnalysisPreferences.getAnalysisPreferences();
        analysisPreferences.clearCaches();

        if(!runner.canDoAnalysis(document) ||
           !isInPythonPath(resource) || //just get problems in resources that are in the pythonpath
           analysisPreferences.makeCodeAnalysis() == false //let's see if we should do code analysis
            ){ 
            runner.deleteMarkers(resource);
            return;
        }
        
        if(module == null){
            module = getSourceModule(resource, document);
        }else{
        	//this may happen if we are not in the regular visiting but in some parser changed (the module is passed, so we don't have to recreate it from the doc)
        	setModuleInCache(module);
        }

        PythonNature nature = PythonNature.getPythonNature(resource.getProject());
        String moduleName = getModuleName(resource);
        
        //remove dependency information (and anything else that was already generated), but first, gather the modules dependent on this one.
        fillDependenciesAndRemoveInfo(moduleName, nature, analyzeDependent, monitor);
        recreateCtxInsensitiveInfo(resource, document);
        
    	monitor.setTaskName("Analyzing module: "+moduleName);
    	monitor.worked(1);
        
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
        IMessage[] messages = analyzer.analyzeDocument(nature, (SourceModule) module, analysisPreferences, document);
        monitor.setTaskName("Adding markers for module: "+moduleName);
        monitor.worked(1);
        runner.addMarkers(resource, document, messages, existing);
        
        for (IMarker marker : existing) {
            try {
                marker.delete();
            } catch (CoreException e) {
                PydevPlugin.log(e);
            }
        }
    }



    private void recreateCtxInsensitiveInfo(IResource resource, IDocument document) {
        IModule sourceModule = getSourceModule(resource, document);
        PythonNature nature = getPythonNature(resource);

        AbstractAdditionalInterpreterInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature.getProject());
        
        //info.removeInfoFromModule(sourceModule.getName()); -- does not remove info from the module because this should be already
        //done once it gets here (the AnalysisBuilder, that also makes dependency info should take care of this).
        boolean generateDelta;
        if(isFullBuild()){
            generateDelta = false;
        }else{
            generateDelta = true;
        }
        
        if (sourceModule instanceof SourceModule) {
            SourceModule m = (SourceModule) sourceModule;
            info.addSourceModuleInfo(m, nature, generateDelta);
        }
    }

    @Override
    public void visitRemovedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
        String moduleName = getModuleName(resource);
        PythonNature nature = getPythonNature(resource);
        
        fillDependenciesAndRemoveInfo(moduleName, nature, true, monitor);
    }

    /**
     * @param moduleName this is the module name
     * @param nature this is the nature
     * @param analyzeDependent determines if we should add dependent modules to be analyzed later
     */
    private void fillDependenciesAndRemoveInfo(String moduleName, PythonNature nature, boolean analyzeDependent, IProgressMonitor monitor) {
        AbstractAdditionalDependencyInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature.getProject());
        if(analyzeDependent){
        	String progressMsg = "Getting modules dependent on: "+moduleName;
			monitor.setTaskName(progressMsg);
        	monitor.worked(1);
            if(DEBUG_DEPENDENCIES){ System.out.println(progressMsg);}
            
            Set<String> dependenciesOn = info.calculateDependencies(new PyStructuralChange());
            for (String dependentOn : dependenciesOn) {
                if(DEBUG_DEPENDENCIES){
                    System.out.println("Adding dependent module to be analyzed later: "+dependentOn);
                }
                this.dependentModulesToAnalyze.add(new Tuple<String, PythonNature>(dependentOn, nature));
            }
        }
        boolean generateDelta;
        if(isFullBuild()){
            generateDelta = false;
        }else{
            generateDelta = true;
        }
        info.removeInfoFromModule(moduleName, generateDelta);
    }

}
