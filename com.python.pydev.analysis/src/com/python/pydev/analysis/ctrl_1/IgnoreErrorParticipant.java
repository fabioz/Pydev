/*
 * License: Common Public License v1.0
 * Created on Sep 20, 2005
 * 
 * @author Fabio Zadrozny
 */
package com.python.pydev.analysis.ctrl_1;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.builder.AnalysisRunner;

public class IgnoreErrorParticipant implements IAnalysisMarkersParticipant {

    private Image annotationImage;

    public IgnoreErrorParticipant() {
        ImageCache analysisImageCache = AnalysisPlugin.getDefault().getImageCache();
        annotationImage = analysisImageCache.get("icons/annotation_obj.gif");
    }

    /** 
     * @throws CoreException 
     * @see com.python.pydev.analysis.ctrl_1.IAnalysisMarkersParticipant#addProps(org.eclipse.core.resources.IMarker, com.python.pydev.analysis.IAnalysisPreferences, java.lang.String, org.python.pydev.core.docutils.PySelection, int, org.python.pydev.editor.PyEdit, java.util.List)
     */
    public void addProps(IMarker marker, IAnalysisPreferences analysisPreferences, String line, PySelection ps, int offset, PythonNature nature,
            PyEdit edit, List<ICompletionProposal> props) throws BadLocationException, CoreException {
        Integer id = (Integer) marker.getAttribute(AnalysisRunner.PYDEV_ANALYSIS_TYPE);
        String messageToIgnore = analysisPreferences.getRequiredMessageToIgnore(id);
        
        if(line.indexOf(messageToIgnore) != -1){
            //ok, move on...
            return ;
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
    }
}
