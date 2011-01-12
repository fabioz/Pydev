package com.python.pydev.analysis.ctrl_1;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.editor.correctionassist.heuristics.IAssistProps;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.builder.AnalysisRunner;

public abstract class AbstractAnalysisMarkersParticipants implements IAssistProps{
    
    protected ArrayList<IAnalysisMarkersParticipant> participants;
    protected List<MarkerAnnotationAndPosition> markersAtLine;

    public AbstractAnalysisMarkersParticipants() {
        participants = new ArrayList<IAnalysisMarkersParticipant>();
        fillParticipants();
    }

    protected abstract void fillParticipants();


    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, IPythonNature nature, PyEdit edit, int offset) throws BadLocationException {
        ArrayList<ICompletionProposal> props = new ArrayList<ICompletionProposal>();
        IAnalysisPreferences analysisPreferences = AnalysisPreferences.getAnalysisPreferences();
        String line = ps.getLine();
        
        
        for (MarkerAnnotationAndPosition marker : markersAtLine) {
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
        List<MarkerAnnotationAndPosition> markersAtLine = s.getMarkersAtLine(line, AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER);
        
        if(markersAtLine.size() > 0){
            //store it for later use
            this.markersAtLine = markersAtLine;
            return true;
        }
        
        return false;
    }

}
