/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.builder;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderPrefPage;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.codecompletion.revisited.PyCodeCompletionVisitor;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.fastparser.FastDefinitionsParser;
import org.python.pydev.plugin.DebugSettings;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
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
    		
    		
    		//For now, always analyze the files, because otherwise we could end up with files not analyzed
    		//when it was edited outside and refreshed... A different approach must be considered so that we
    		//don't analyze the file twice when editing/saving it.
    		
    		//change: always analyze the file, being only on save or not
            boolean analyzeDependent;
            if(isFullBuild()){
                analyzeDependent = false;
            }else{
                analyzeDependent = true;
            }
            IModule module;
            if(PyDevBuilderPrefPage.getAnalyzeOnlyActiveEditor()){
            	if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
            		System.out.println("AnalysisBuilderVisitor: PyDevBuilderPrefPage.getAnalyzeOnlyActiveEditor()");
            	}
            	IFile f = (IFile) resource;
                String file = f.getRawLocation().toOSString();
            	String moduleName = getModuleName(resource, nature);
				module = new SourceModule(moduleName, new File(file), 
            			FastDefinitionsParser.parse(document.get(), moduleName), null);
            }else{
            	module = getSourceModule(resource, document, nature);
            }
			doVisitChangedResource(nature, resource, document, module, analyzeDependent, monitor, false, 
            		AnalysisBuilderRunnable.ANALYSIS_CAUSE_BUILDER);
            
    	}finally{
    		nature.endRequests();
    	}
    }
    


    /**
     * here we have to detect errors / warnings from the code analysis
     */
    public void doVisitChangedResource(IPythonNature nature, IResource resource, IDocument document, 
    		IModule module, boolean analyzeDependent, IProgressMonitor monitor, boolean forceAnalysis,
    		int analysisCause) {
    	
        Assert.isNotNull(module);
        setModuleInCache(module);
        
        final String moduleName = getModuleName(resource, nature);
        final AnalysisBuilderRunnable runnable = AnalysisBuilderRunnable.createRunnable(
        		document, resource, module, analyzeDependent, monitor, isFullBuild(), moduleName, forceAnalysis, analysisCause);
        
        if(isFullBuild()){
        	runnable.run();
        }else{
            Job workbenchJob = new Job("") {
            
                @Override
                public IStatus run(IProgressMonitor monitor) {
                    this.getThread().setName("AnalysisBuilderThread :"+moduleName);
                    runnable.run();
                    return Status.OK_STATUS;
                }
            
            };
            workbenchJob.setSystem(true);
            workbenchJob.setPriority(Job.BUILD);
            workbenchJob.schedule();
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
	    	AbstractAdditionalDependencyInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature);
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
            AbstractAdditionalDependencyInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature);
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
