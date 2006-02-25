/*
 * Created on 24/09/2005
 */
package com.python.pydev.analysis.ctrl_1;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.editor.correctionassist.heuristics.IAssistProps;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.ImageCache;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.builder.AnalysisRunner;

public class AnalysisMarkersParticipants implements IAssistProps{

    private List<IMarker> markersAtLine;
    private IAnalysisMarkersParticipant ignoreParticipant;
    private IAnalysisMarkersParticipant fixParticipant;
    private ArrayList<IAnalysisMarkersParticipant> participants;

    public AnalysisMarkersParticipants() {
        ignoreParticipant = new IgnoreErrorParticipant();
        fixParticipant = new UndefinedVariableFixParticipant();
        
        participants = new ArrayList<IAnalysisMarkersParticipant>();
        participants.add(ignoreParticipant);
        participants.add(fixParticipant);
    }

    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, PythonNature nature, PyEdit edit, int offset) throws BadLocationException {
        ArrayList<ICompletionProposal> props = new ArrayList<ICompletionProposal>();
        IAnalysisPreferences analysisPreferences = AnalysisPreferences.getAnalysisPreferences();
        String line = ps.getLine();
        
        for (IMarker marker : markersAtLine) {
            for (IAnalysisMarkersParticipant participant : participants) {
                try {
                    participant.addProps(marker, analysisPreferences, line, ps, offset, nature, edit, props);
                } catch (Exception e) {
                    PydevPlugin.log("Error when getting proposals.", e);
                }
            }
        }
        return props;
    }

    /**
     * It is valid if any marker generated from the analysis is found
     *  
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.core.docutils.PySelection, java.lang.String, org.python.pydev.editor.PyEdit, int)
     */
    public boolean isValid(PySelection ps, String sel, PyEdit edit, int offset) {
        PySourceViewer s = edit.getPySourceViewer();
        
        int line = ps.getLineOfOffset(offset);
        List<IMarker> markersAtLine = s.getMarkersAtLine(line, AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER);
        
        if(markersAtLine.size() > 0){
            //store it for later use
            this.markersAtLine = markersAtLine;
            return true;
        }
        
        return false;
    }

}
