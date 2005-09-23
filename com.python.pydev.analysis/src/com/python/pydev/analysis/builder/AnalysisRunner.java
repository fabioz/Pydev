/*
 * Created on 18/09/2005
 */
package com.python.pydev.analysis.builder;

import java.util.HashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PydevMarkerUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.PySelection;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.messages.IMessage;

public class AnalysisRunner {

    public static final String PYDEV_PROBLEM_ID_MARKER_INFO = "PYDEV_PROBLEM_ID";
    public static final String PYDEV_ANALYSIS_PROBLEM_MARKER = "com.python.pydev.analysis.pydev_analysis_problemmarker";

    /**
     * @param document the document we want to check
     * @return true if we can analyze it and false if there is some flag saying that we shouldn't
     */
    public boolean canDoAnalysis(IDocument document) {
        return document.get().indexOf("#@PydevCodeAnalysisIgnore") == -1;
    }


    /**
     * @param resource the resource that should have the markers deleted
     */
    public void deleteMarkers(IResource resource) {
        try {
            resource.deleteMarkers(PYDEV_ANALYSIS_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
        } catch (CoreException e3) {
            Log.log(e3);
        }
    }
    

    /**
     * @param resource the resource where we want to add the markers
     * @param document the document
     * @param messages the messages to add
     */
    public void addMarkers(IResource resource, IDocument document, IMessage[] messages) {
        try {
            IAnalysisPreferences analysisPreferences = AnalysisPreferences.getAnalysisPreferences();
            
            //add the markers... the id is put as additional info for it
            for (IMessage m : messages) {
                String messageToIgnore = analysisPreferences.getRequiredMessageToIgnore(m.getType());
                int startLine = m.getStartLine(document) - 1;
                String line = PySelection.getLine(document, startLine);
                if(line.indexOf(messageToIgnore) != -1){
                    //keep going... nothing to see here...
                    continue;
                }
                
                HashMap<String, Object> additionalInfo = new HashMap<String, Object>();
                additionalInfo.put(PYDEV_PROBLEM_ID_MARKER_INFO, m.getType());
                
                int startCol = m.getStartCol(document) - 1;
                int endLine = m.getEndLine(document) - 1;
                int endCol = m.getEndCol(document) - 1;
                
                
                String msg = m.getMessage();
                PydevMarkerUtils.createMarker(resource, 
                        document, 
                        msg, 
                        startLine, 
                        startCol,
                        endLine, 
                        endCol, 
                        AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER,
                        m.getSeverity(),
                        additionalInfo);
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }



}
