/*
 * License: Common Public License v1.0
 * Created on Sep 20, 2005
 * 
 * @author Fabio Zadrozny
 */
package com.python.pydev.analysis.ctrl_1;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.editor.correctionassist.heuristics.IAssistProps;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.ImageCache;

import com.python.pydev.analysis.builder.AnalysisRunner;

public class IgnoreErrorParticipant implements IAssistProps{

    private List<IMarker> markersAtLine;

    public IgnoreErrorParticipant() {
    }

    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, PythonNature nature, PyEdit edit, int offset) throws BadLocationException {
        for (IMarker marker : markersAtLine) {
            try {
                marker.getAttribute(IMarker.MESSAGE);
            } catch (CoreException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

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
