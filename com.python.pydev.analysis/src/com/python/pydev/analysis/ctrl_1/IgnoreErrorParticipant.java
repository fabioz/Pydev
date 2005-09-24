/*
 * License: Common Public License v1.0
 * Created on Sep 20, 2005
 * 
 * @author Fabio Zadrozny
 */
package com.python.pydev.analysis.ctrl_1;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.editor.correctionassist.heuristics.IAssistProps;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.ImageCache;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.builder.AnalysisRunner;

public class IgnoreErrorParticipant implements IAssistProps{

    private List<IMarker> markersAtLine;

    public IgnoreErrorParticipant() {
    }

    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, PythonNature nature, PyEdit edit, int offset) throws BadLocationException {
        ArrayList<ICompletionProposal> props = new ArrayList<ICompletionProposal>();
        IAnalysisPreferences analysisPreferences = AnalysisPreferences.getAnalysisPreferences();
        String line = ps.getLine();
        
        ImageCache analysisImageCache = AnalysisPlugin.getDefault().getImageCache();
        Image annotationImage = analysisImageCache.get("icons/annotation_obj.gif");
        
        for (IMarker marker : markersAtLine) {
            try {
                Integer type = (Integer) marker.getAttribute(AnalysisRunner.PYDEV_PROBLEM_ID_MARKER_INFO);
                String messageToIgnore = analysisPreferences.getRequiredMessageToIgnore(type);
                
                if(line.indexOf(messageToIgnore) != -1){
                    //ok, move on...
                    continue;
                }
                
                String strToAdd = messageToIgnore;
                char lastChar = line.charAt(line.length()-1);
                
                if(line.indexOf("#") == -1){
                    strToAdd = "#"+strToAdd;
                }

                if(!Character.isWhitespace(lastChar)){
                    strToAdd = " "+strToAdd;
                }

                
                IgnoreCompletionProposal proposal = new IgnoreCompletionProposal(
                        strToAdd,
                        ps.getEndLineOffset(), 
                        0,
                        offset,
                        annotationImage,
                        messageToIgnore.substring(1),
                        null,
                        null,
                        PyCompletionProposal.PRIORITY_DEFAULT,
                        edit
                        );
                props.add(proposal);
                
            } catch (CoreException e) {
                throw new RuntimeException(e);
            }
        }
        return props;
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
