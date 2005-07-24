/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.builder;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.IMessage;
import com.python.pydev.analysis.OcurrencesAnalyzer;

public class AnalysisBuilderVisitor extends PyDevBuilderVisitor{

    private static final String PYDEV_PROBLEM_MARKER = "com.python.pydev.analysis.pydev_analysis_problemmarker";
    @Override
    public boolean visitChangedResource(IResource resource, IDocument document) {
        try {
            resource.deleteMarkers(PYDEV_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
        } catch (CoreException e3) {
            PydevPlugin.log(e3);
        }
        OcurrencesAnalyzer analyzer = new OcurrencesAnalyzer();
        PythonNature nature = PythonNature.getPythonNature(resource.getProject());
        AbstractModule module = AbstractModule.createModuleFromDoc("", null, document, nature, 0);
        IMessage[] messages = analyzer.analyzeDocument(nature, (SourceModule) module);
        try {
            for (IMessage m : messages) {
                String msg = "ID:" + m.getSubType() + " " + m.getMessage();
                createMarker(resource, document, msg, m.getStartLine() - 1, m.getStartCol() - 1, m.getEndLine() - 1, m.getEndCol() - 1, PYDEV_PROBLEM_MARKER, m.getSeverity());
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return true;
    }

    @Override
    public boolean visitRemovedResource(IResource resource, IDocument document) {
        return false;
    }

}
