/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.ctrl_1;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.editor.correctionassist.heuristics.IAssistProps;
import org.python.pydev.shared_core.structure.OrderedSet;
import org.python.pydev.shared_ui.ImageCache;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.builder.AnalysisRunner;

public abstract class AbstractAnalysisMarkersParticipants implements IAssistProps {

    protected ArrayList<IAnalysisMarkersParticipant> participants;

    public AbstractAnalysisMarkersParticipants() {
        participants = new ArrayList<IAnalysisMarkersParticipant>();
    }

    protected abstract void fillParticipants();

    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, IPythonNature nature,
            PyEdit edit, int offset) throws BadLocationException {
        fillParticipants();

        PySourceViewer s = edit.getPySourceViewer();

        int line = ps.getLineOfOffset(offset);
        OrderedSet<MarkerAnnotationAndPosition> markersAtLine = new OrderedSet<MarkerAnnotationAndPosition>();

        //Add it to a set to make sure that the entries are unique.
        //-- i.e.: the code analysis seems to be creating 2 markers in the following case (when sys is undefined):
        //sys.call1().call2()
        //So, we add it to a set to make sure we'll only analyze unique markers.
        //Note that it'll check equality by the marker type and text (not by position), so, if a given error
        //appears twice in the same line being correct, we'll only show the options once here (which is what
        //we want).
        List<MarkerAnnotationAndPosition> markersAtLine2 = s.getMarkersAtLine(line,
                AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER);
        markersAtLine.addAll(markersAtLine2);

        ArrayList<ICompletionProposal> props = new ArrayList<ICompletionProposal>();

        if (markersAtLine != null) {
            IAnalysisPreferences analysisPreferences = new AnalysisPreferences(edit);
            String currLine = ps.getLine();
            for (MarkerAnnotationAndPosition marker : markersAtLine) {
                for (IAnalysisMarkersParticipant participant : participants) {
                    try {
                        participant.addProps(marker, analysisPreferences, currLine, ps, offset, nature, edit, props);
                    } catch (Exception e) {
                        Log.log("Error when getting proposals.", e);
                    }
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
        return ps.getSelLength() == 0;
    }

}
