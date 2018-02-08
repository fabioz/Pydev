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
import org.python.pydev.ast.analysis.IAnalysisPreferences;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.editor.correctionassist.IAssistProps;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.image.IImageCache;
import org.python.pydev.shared_core.structure.OrderedSet;

import com.python.pydev.analysis.AnalysisPreferences;

public abstract class AbstractAnalysisMarkersParticipants implements IAssistProps {

    protected ArrayList<IAnalysisMarkersParticipant> participants;

    public AbstractAnalysisMarkersParticipants() {
        participants = new ArrayList<IAnalysisMarkersParticipant>();
    }

    protected abstract String getMarkerType();

    protected abstract void fillParticipants();

    @Override
    public List<ICompletionProposalHandle> getProps(PySelection ps, IImageCache imageCache, File f,
            IPythonNature nature,
            IPyEdit edit, int offset) throws BadLocationException {
        fillParticipants();

        PySourceViewer s = ((PyEdit) edit).getPySourceViewer();

        int line = ps.getLineOfOffset(offset);
        OrderedSet<MarkerAnnotationAndPosition> markersAtLine = new OrderedSet<MarkerAnnotationAndPosition>();

        //Add it to a set to make sure that the entries are unique.
        //-- i.e.: the code analysis seems to be creating 2 markers in the following case (when sys is undefined):
        //sys.call1().call2()
        //So, we add it to a set to make sure we'll only analyze unique markers.
        //Note that it'll check equality by the marker type and text (not by position), so, if a given error
        //appears twice in the same line being correct, we'll only show the options once here (which is what
        //we want).
        List<MarkerAnnotationAndPosition> markersAtLine2 = s.getMarkersAtLine(line, getMarkerType());
        markersAtLine.addAll(markersAtLine2);

        ArrayList<ICompletionProposalHandle> props = new ArrayList<ICompletionProposalHandle>();

        if (markersAtLine != null) {
            IAnalysisPreferences analysisPreferences = new AnalysisPreferences(edit);
            String currLine = ps.getLine();
            for (MarkerAnnotationAndPosition marker : markersAtLine) {
                for (IAnalysisMarkersParticipant participant : participants) {
                    try {
                        participant.addProps(marker, analysisPreferences, currLine, ps, offset, nature, (PyEdit) edit,
                                props);
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
     * @see org.python.pydev.editor.correctionassist.IAssistProps#isValid(org.python.pydev.core.docutils.PySelection, java.lang.String, org.python.pydev.editor.PyEdit, int)
     */
    @Override
    public boolean isValid(PySelection ps, String sel, IPyEdit edit, int offset) {
        return ps.getSelLength() == 0;
    }

}
