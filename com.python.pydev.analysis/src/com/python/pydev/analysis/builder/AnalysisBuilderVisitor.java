/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.builder;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.OcurrencesAnalyzer;
import com.python.pydev.analysis.messages.IMessage;

public class AnalysisBuilderVisitor extends PyDevBuilderVisitor{

    private static final String PYDEV_ANALYSIS_PROBLEM_MARKER = "com.python.pydev.analysis.pydev_analysis_problemmarker";
    
    /**
     * here we have to detect errors / warnings from the code analysis
     *  
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitChangedResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    @Override
    public boolean visitChangedResource(IResource resource, IDocument document) {
        try {
            resource.deleteMarkers(PYDEV_ANALYSIS_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
        } catch (CoreException e3) {
            Log.log(e3);
        }
        
        if(isInPythonPath(resource)){ //just get problems in resources that are in the pythonpath
            IAnalysisPreferences analysisPreferences = AnalysisPreferences.getAnalysisPreferences();
            analysisPreferences.clearCaches();
            
            //let's see if we should do code analysis
            if(analysisPreferences.makeCodeAnalysis() == false){
                return true;
            }
    
            OcurrencesAnalyzer analyzer = new OcurrencesAnalyzer();
            PythonNature nature = PythonNature.getPythonNature(resource.getProject());
            IFile f = (IFile) resource;
            String file = f.getRawLocation().toOSString();
            
            String moduleName = nature.getAstManager().getProjectModulesManager().resolveModule(file);
            
            AbstractModule module = AbstractModule.createModuleFromDoc(moduleName, new File(file), document, nature, 0);
            
            //ok, let's do it
            IMessage[] messages = analyzer.analyzeDocument(nature, (SourceModule) module, analysisPreferences);
            try {
                
                //add the markers
                for (IMessage m : messages) {
                    String msg = "ID:" + m.getType() + " " + m.getMessage();
                    createMarker(resource, document, msg, 
                            m.getStartLine(document) - 1, m.getStartCol(document) - 1, m.getEndLine(document) - 1, m.getEndCol(document) - 1, 
                            PYDEV_ANALYSIS_PROBLEM_MARKER, m.getSeverity());
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
        return true;
    }

    @Override
    public boolean visitRemovedResource(IResource resource, IDocument document) {
        return false;
    }

}
