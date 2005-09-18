/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.builder;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.OcurrencesAnalyzer;
import com.python.pydev.analysis.messages.IMessage;

public class AnalysisBuilderVisitor extends PyDevBuilderVisitor{

    @Override
    public boolean visitChangedResource(IResource resource, IDocument document) {
        if(AnalysisPreferences.getAnalysisPreferences().getWhenAnalyze() == IAnalysisPreferences.ANALYZE_ON_SAVE){
            return visitChangedResource(resource, document, null);
        }
        return true;
    }
    
    /**
     * here we have to detect errors / warnings from the code analysis
     *  
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitChangedResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    public boolean visitChangedResource(IResource resource, IDocument document, AbstractModule module) {
        AnalysisRunner runner = new AnalysisRunner();
        
        runner.deleteMarkers(resource);
        if(!runner.canDoAnalysis(document)){
            return true;
        }
        
        if(isInPythonPath(resource)){ //just get problems in resources that are in the pythonpath
            IAnalysisPreferences analysisPreferences = AnalysisPreferences.getAnalysisPreferences();
            analysisPreferences.clearCaches();
            
            //let's see if we should do code analysis
            if(analysisPreferences.makeCodeAnalysis() == false){
                return true;
            }
    
            if(module == null){
                module = getSourceModule(resource, document);
            }
            
            OcurrencesAnalyzer analyzer = new OcurrencesAnalyzer();
            PythonNature nature = PythonNature.getPythonNature(resource.getProject());
            //ok, let's do it
            IMessage[] messages = analyzer.analyzeDocument(nature, (SourceModule) module, analysisPreferences);
            runner.addMarkers(resource, document, messages);
        }
        return true;
    }




    @Override
    public boolean visitRemovedResource(IResource resource, IDocument document) {
        return true;
    }

}
