/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 20, 2005
 * 
 * @author Fabio Zadrozny
 */
package com.python.pydev.analysis.ctrl_1;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyFormatStd.FormatStd;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.editor.correctionassist.IgnoreCompletionProposal;
import org.python.pydev.editor.correctionassist.IgnoreCompletionProposalInSameLine;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.proposals.PyCompletionProposal;

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.builder.AnalysisRunner;

public class IgnoreErrorParticipant implements IAnalysisMarkersParticipant {

    private Set<Integer> handled = new HashSet<Integer>();

    private FormatStd format;

    public IgnoreErrorParticipant() {
        this(null);
    }

    /**
     * Only for tests.
     */
    /*default*/IgnoreErrorParticipant(FormatStd format) {
        this.format = format;
    }

    /** 
     * @throws CoreException 
     * @see com.python.pydev.analysis.ctrl_1.IAnalysisMarkersParticipant#addProps(org.eclipse.core.resources.IMarker, com.python.pydev.analysis.IAnalysisPreferences, java.lang.String, org.python.pydev.core.docutils.PySelection, int, org.python.pydev.editor.PyEdit, java.util.List)
     */
    public void addProps(MarkerAnnotationAndPosition marker, IAnalysisPreferences analysisPreferences,
            final String line, final PySelection ps, int offset, IPythonNature nature, final PyEdit edit,
            List<ICompletionProposal> props)
            throws BadLocationException, CoreException {
        Integer id = (Integer) marker.markerAnnotation.getMarker().getAttribute(AnalysisRunner.PYDEV_ANALYSIS_TYPE);
        if (handled.contains(id)) {
            return;
        }
        handled.add(id);
        final String messageToIgnore = analysisPreferences.getRequiredMessageToIgnore(id);

        if (line.indexOf(messageToIgnore) != -1) {
            //ok, move on...
            return;
        }

        IgnoreCompletionProposal proposal = new IgnoreCompletionProposalInSameLine(messageToIgnore,
                ps.getEndLineOffset(), 0,
                offset, //note: the cursor position is unchanged!
                PydevPlugin.getImageCache().get(UIConstants.ASSIST_ANNOTATION),
                messageToIgnore.substring(1), null, null,
                PyCompletionProposal.PRIORITY_DEFAULT, edit, line, ps, format);
        props.add(proposal);
    }
}
