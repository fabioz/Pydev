/**
 * Copyright (c) 20017 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.ctrl_1.pylint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyFormatStd.FormatStd;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.editor.correctionassist.CheckAnalysisErrors;
import org.python.pydev.editor.correctionassist.IgnoreCompletionProposal;
import org.python.pydev.editor.correctionassist.IgnorePyLintCompletionProposalInSameLine;
import org.python.pydev.shared_core.IMiscConstants;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal;

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.ctrl_1.IAnalysisMarkersParticipant;

public class PyLintIgnoreErrorParticipant implements IAnalysisMarkersParticipant {

    private Set<String> handled = new HashSet<>();

    private FormatStd format;

    public PyLintIgnoreErrorParticipant() {
        this(null);
    }

    /**
     * Only for tests.
     */
    /*default*/ PyLintIgnoreErrorParticipant(FormatStd format) {
        this.format = format;
    }

    /**
     * @throws CoreException
     * @see com.python.pydev.analysis.ctrl_1.IAnalysisMarkersParticipant#addProps(org.eclipse.core.resources.IMarker, com.python.pydev.analysis.IAnalysisPreferences, java.lang.String, org.python.pydev.core.docutils.PySelection, int, org.python.pydev.editor.PyEdit, java.util.List)
     */
    @Override
    public void addProps(MarkerAnnotationAndPosition marker, IAnalysisPreferences analysisPreferences,
            final String line, final PySelection ps, int offset, IPythonNature nature, final PyEdit edit,
            List<ICompletionProposalHandle> props)
            throws BadLocationException, CoreException {
        IMarker m = marker.markerAnnotation.getMarker();
        Object attribute = m.getAttribute(IMiscConstants.PYLINT_MESSAGE_ID);
        if (attribute == null) {
            return;
        }
        String messageId = attribute.toString();

        if (handled.contains(messageId)) {
            return;
        }
        handled.add(messageId);
        if (CheckAnalysisErrors.isPyLintErrorHandledAtLine(line, messageId)) {
            return;
        }

        IgnoreCompletionProposal proposal = new IgnorePyLintCompletionProposalInSameLine(messageId,
                ps.getEndLineOffset(), 0,
                offset, //note: the cursor position is unchanged!
                SharedUiPlugin.getImageCache().get(UIConstants.ASSIST_ANNOTATION),
                "pylint: disable=" + messageId, null, null,
                IPyCompletionProposal.PRIORITY_DEFAULT, edit, line, ps, format, m);
        props.add(proposal);
    }
}
