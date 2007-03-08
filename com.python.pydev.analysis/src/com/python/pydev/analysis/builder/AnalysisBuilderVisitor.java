/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.builder;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.codecompletion.revisited.PyCodeCompletionVisitor;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;

public class AnalysisBuilderVisitor extends PyDevBuilderVisitor{

    /**
     * Do you want to gather information about dependencies?
     */
    public static final boolean DEBUG_DEPENDENCIES = false;

    @Override
    protected int getPriority() {
    	return PyCodeCompletionVisitor.PRIORITY_CODE_COMPLETION+1; //just after the code-completion priority
    }

    
    
    @Override
    public void visitChangedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
    	if(document == null){
    		return;
    	}
    	
    	//we may need to 'force' the analysis when a module is renamed, because the first message we receive is
    	//a 'delete' and after that an 'add' -- which is later mapped to this method, so, if we don't have info
    	//on the module we should analyze it because it is 'probably' a rename.
    	PythonNature nature = getPythonNature(resource);
    	if(nature == null){
    		return;
    	}
    	if(!nature.startRequests()){
    		return;
    	}
    	try{
	        String moduleName = getModuleName(resource, nature);
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
	            doVisitChangedResource(nature, resource, document, null, analyzeDependent, monitor);
        }
    	}finally{
    		nature.endRequests();
    	}
    }
    


    /**
     * here we have to detect errors / warnings from the code analysis
     */
    public void doVisitChangedResource(IPythonNature nature, IResource resource, IDocument document, IModule module, boolean analyzeDependent, IProgressMonitor monitor) {
        if(module == null){
            module = getSourceModule(resource, document, nature);
        }else{
            //this may happen if we are not in the regular visiting but in some parser changed (the module is passed, so we don't have to recreate it from the doc)
            setModuleInCache(module);
        }
        String moduleName = getModuleName(resource, nature);
        AnalysisBuilderRunnable runnable = AnalysisBuilderRunnable.createRunnable(document, resource, module, analyzeDependent, monitor, isFullBuild(), moduleName);
        if(isFullBuild()){
        	runnable.run();
        }else{
        	Thread thread = new Thread(runnable);
        	thread.setPriority(Thread.MIN_PRIORITY);
        	thread.setName("AnalysisBuilderThread :"+moduleName);
        	thread.start();
        }
    }




    @Override
    public void visitRemovedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
    	PythonNature nature = getPythonNature(resource);
    	if(nature == null){
    		return;
    	}
        String moduleName = getModuleName(resource, nature);
        fillDependenciesAndRemoveInfo(moduleName, nature, true, monitor, isFullBuild());
    }
    

    @Override
    public void visitingWillStart(IProgressMonitor monitor, boolean isFullBuild, IPythonNature nature) {
    	if(isFullBuild){
	    	AbstractAdditionalDependencyInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature.getProject());
	    	info.clearAllInfo();
    	}
    }
    
    /**
     * @param moduleName this is the module name
     * @param nature this is the nature
     * @param analyzeDependent determines if we should add dependent modules to be analyzed later
     */
    public static void fillDependenciesAndRemoveInfo(String moduleName, PythonNature nature, boolean analyzeDependent, IProgressMonitor monitor, boolean isFullBuild) {
        if(moduleName != null && nature != null){
            AbstractAdditionalDependencyInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature.getProject());
            boolean generateDelta;
            if(isFullBuild){
                generateDelta = false;
            }else{
                generateDelta = true;
            }
            info.removeInfoFromModule(moduleName, generateDelta);
        }
    }

}
