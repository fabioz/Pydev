/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.Position;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.MarkerStub;
import com.python.pydev.analysis.builder.AnalysisRunner;
import com.python.pydev.analysis.ctrl_1.AbstractAnalysisMarkersParticipants;

public class TddCodeGenerationQuickFixParticipant extends AbstractAnalysisMarkersParticipants{


    protected void fillParticipants() {
        participants.add(new TddQuickFixParticipant());
    }
    
    
    /**
     * It is valid if any marker generated from the analysis is found
     *  
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.core.docutils.PySelection, java.lang.String, org.python.pydev.editor.PyEdit, int)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean isValid(PySelection ps, String sel, PyEdit edit, int offset) {
        if(super.isValid(ps, sel, edit, offset)){
            return true;
        }
        //Ok, we have no markers, but we might want to generate something at 'self.' if it's available
        int lineOfOffset = ps.getLineOfOffset(offset);
        String lineContents = ps.getLine(lineOfOffset);
        
        if(lineContents.indexOf("self.") != -1){
            HashSet<String> selfAttributeAccesses = PySelection.getSelfAttributeAccesses(lineContents);
            int lineOffset = ps.getLineOffset(lineOfOffset);
            List<MarkerAnnotationAndPosition> markersAtLine = new ArrayList<MarkerAnnotationAndPosition>();
            for (String string : selfAttributeAccesses) {
                Map attrs = new HashMap();
                attrs.put(AnalysisRunner.PYDEV_ANALYSIS_TYPE, IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE_IN_SELF);
                SimpleMarkerAnnotation markerAnnotation = new SimpleMarkerAnnotation(AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER, new MarkerStub(attrs));
                
                int indexOf = lineContents.indexOf("self."+string);
                if(indexOf >= 0){
                    Position position = new Position(lineOffset+indexOf+5, string.length());
                    markersAtLine.add(new MarkerAnnotationAndPosition(markerAnnotation, position));
                }
            }
            if(markersAtLine.size() > 0){
                this.markersAtLine = markersAtLine;
                return true;
            }
        }
        

        return false;
    }


}
