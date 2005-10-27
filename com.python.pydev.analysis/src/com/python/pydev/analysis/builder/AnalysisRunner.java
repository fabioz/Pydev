/*
 * Created on 18/09/2005
 */
package com.python.pydev.analysis.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PydevMarkerUtils;
import org.python.pydev.core.log.Log;

import com.python.pydev.analysis.messages.IMessage;

public class AnalysisRunner {

    /**
     * Indicates the type of the message given the constants in com.python.pydev.analysis.IAnalysisPreferences (unused import, 
     * undefined variable...)
     */
    public static final String PYDEV_ANALYSIS_TYPE = "PYDEV_TYPE";
    
    /**
     * Indicates the additional info for the marker (depends on its type) - may be null
     */
    public static final String PYDEV_ANALYSIS_ADDITIONAL_INFO = "PYDEV_INFO";
    
    /**
     * this is the type of the marker
     */
    public static final String PYDEV_ANALYSIS_PROBLEM_MARKER = "com.python.pydev.analysis.pydev_analysis_problemmarker";

    /**
     * do we want to debug this class?
     */
	private static final boolean DEBUG_ANALYSIS_RUNNER = false;

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
     * @param existing these are the existing markers. After this method, the list will contain only the ones that
     * should be removed.
     */
    public void addMarkers(IResource resource, IDocument document, IMessage[] messages, ArrayList<IMarker> existing) {
        try {
            
            //add the markers... the id is put as additional info for it
            for (IMessage m : messages) {
                
                HashMap<String, Object> additionalInfo = new HashMap<String, Object>();
                additionalInfo.put(PYDEV_ANALYSIS_TYPE, m.getType());
                
                //not all messages have additional info
                List<String> infoForType = m.getAdditionalInfo();
                if(infoForType != null){
                    additionalInfo.put(PYDEV_ANALYSIS_ADDITIONAL_INFO, infoForType);
                }
                
                int startLine = m.getStartLine(document) - 1;
                int startCol = m.getStartCol(document) - 1;
                int endLine = m.getEndLine(document) - 1;
                int endCol = m.getEndCol(document) - 1;
                
                
                String msg = m.getMessage();
                if(DEBUG_ANALYSIS_RUNNER){
                	System.out.printf("\nAdding at start:%s end:%s line:%s message:%s " , startCol, endCol, startLine, msg);
                }
                PydevMarkerUtils.createMarker(resource, 
                        document, 
                        msg, 
                        startLine, 
                        startCol,
                        endLine, 
                        endCol, 
                        AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER,
                        m.getSeverity(),
                        additionalInfo,
                        existing);
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }



}
