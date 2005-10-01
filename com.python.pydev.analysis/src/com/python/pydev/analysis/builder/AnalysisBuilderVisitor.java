/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.builder;

import java.util.ArrayList;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.OcurrencesAnalyzer;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.messages.IMessage;

public class AnalysisBuilderVisitor extends PyDevBuilderVisitor{

    @Override
    public void visitChangedResource(IResource resource, IDocument document) {
        if(AnalysisPreferences.getAnalysisPreferences().getWhenAnalyze() == IAnalysisPreferences.ANALYZE_ON_SAVE){
            visitChangedResource(resource, document, null);
        }
    }
    
    /**
     * here we have to detect errors / warnings from the code analysis
     */
    public void visitChangedResource(IResource resource, IDocument document, AbstractModule module) {
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
        
        AbstractAdditionalInterpreterInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature.getProject());
        info.removeInfoFromModule(moduleName);
    }

}
