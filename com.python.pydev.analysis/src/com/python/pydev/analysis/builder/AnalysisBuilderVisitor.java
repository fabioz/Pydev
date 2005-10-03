/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilder;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.OcurrencesAnalyzer;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
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
    public void visitingWillStart(IProgressMonitor monitor) {
        super.visitingWillStart(monitor);
        this.dependentModulesToAnalyze = new ArrayList<Tuple<String, PythonNature>>();
    }

    @Override
    public void visitingEnded(IProgressMonitor monitor) {
        super.visitingEnded(monitor);
        
        //ok, now, before we call the visit over, let's analyze the dependent modules
        for (Tuple<String, PythonNature> modNameAndNature : dependentModulesToAnalyze) {
            PythonNature nature = modNameAndNature.o2;
            String modName = modNameAndNature.o1;
            AbstractModule module = nature.getAstManager().getProjectModulesManager().getModule(modName, nature, false, false);
            
            monitor.setTaskName("Visiting dependencies...");
            if(module instanceof SourceModule){
                //if it is a source module, let's get the resource for it
                SourceModule mod = (SourceModule) module;
                
                this.memo = new HashMap<String, Object>();//clear the cache, just to be sure of it (because we are re-using the same instance to make new visits)
                setModuleInCache(mod);
                setModuleNameInCache(modName);
                
                IPath path = Path.fromOSString(REF.getFileAbsolutePath(mod.getFile()));
                if(DEBUG_DEPENDENCIES){
                    System.out.println("visiting dependent "+mod.getName());
                }
                
                monitor.setTaskName("Visiting dependencies..."+mod.getName());
                monitor.worked(1);
                
                IProject project = nature.getProject();
                
                //make the path relative to the project
                int i = path.matchingFirstSegments(project.getLocation());
                path = path.removeFirstSegments(i);
                IFile file = file = project.getFile(path);
                visitChangedResource(file, PyDevBuilder.getDocFromResource(file), module, false);
            }
        }
        
        
        this.dependentModulesToAnalyze.clear();
    }
    
    @Override
    public void visitChangedResource(IResource resource, IDocument document) {
        if(AnalysisPreferences.getAnalysisPreferences().getWhenAnalyze() == IAnalysisPreferences.ANALYZE_ON_SAVE){
            visitChangedResource(resource, document, null, true);
        }
    }
    
    /**
     * here we have to detect errors / warnings from the code analysis
     */
    public void visitChangedResource(IResource resource, IDocument document, AbstractModule module, boolean analyzeDependent) {
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
        }

        PythonNature nature = PythonNature.getPythonNature(resource.getProject());
        String moduleName = getModuleName(resource);
        
        //remove dependency information (and anything else that was already generated), but first, gather the modules dependent on this one.
        fillDependenciesAndRemoveInfo(moduleName, nature, analyzeDependent);

        
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
        IMessage[] messages = analyzer.analyzeDocument(nature, (SourceModule) module, analysisPreferences);
        runner.addMarkers(resource, document, messages, existing);
        
        for (IMarker marker : existing) {
            try {
                marker.delete();
            } catch (CoreException e) {
                PydevPlugin.log(e);
            }
        }
    }



    @Override
    public void visitRemovedResource(IResource resource, IDocument document) {
        String moduleName = getModuleName(resource);
        PythonNature nature = getPythonNature(resource);
        
        fillDependenciesAndRemoveInfo(moduleName, nature, true);
    }

    /**
     * @param moduleName
     * @param nature
     * @param analyzeDependent 
     */
    private void fillDependenciesAndRemoveInfo(String moduleName, PythonNature nature, boolean analyzeDependent) {
        AbstractAdditionalDependencyInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature.getProject());
        if(analyzeDependent){
            if(DEBUG_DEPENDENCIES){
                System.out.println("Getting modules dependent on: "+moduleName);
            }
            Set<String> dependenciesOn = info.getModulesThatHaveDependenciesOn(moduleName);
            for (String dependentOn : dependenciesOn) {
                if(DEBUG_DEPENDENCIES){
                    System.out.println("Adding dependent module to be analyzed later: "+dependentOn);
                }
                this.dependentModulesToAnalyze.add(new Tuple<String, PythonNature>(dependentOn, nature));
            }
        }
        info.removeInfoFromModule(moduleName);
    }

}
