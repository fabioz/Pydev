/**
 * Copyright (c) 2021 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.ctrl_1.flake8;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.CheckAnalysisErrors;
import org.python.pydev.core.IAnalysisMarkersParticipant;
import org.python.pydev.core.IAnalysisPreferences;
import org.python.pydev.core.IMarkerInfoForAnalysis;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.formatter.FormatStd;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_core.image.UIConstants;
import org.python.pydev.shared_ui.SharedUiPlugin;

public class Flake8IgnoreErrorParticipant implements IAnalysisMarkersParticipant {

    private Set<String> handled = new HashSet<>();

    private FormatStd format;

    public Flake8IgnoreErrorParticipant() {
        this(null);
    }

    /**
     * Only for tests.
     */
    Flake8IgnoreErrorParticipant(FormatStd format) {
        this.format = format;
    }

    @Override
    public void addProps(IMarkerInfoForAnalysis markerInfo, IAnalysisPreferences analysisPreferences,
            final String line, final PySelection ps, int offset, IPythonNature nature, final IPyEdit edit,
            List<ICompletionProposalHandle> props)
            throws BadLocationException, CoreException {
        Object attribute = markerInfo.getFlake8MessageId();
        Object message = markerInfo.getMessage();
        if (attribute == null || message == null) {
            return;
        }
        String messageId = attribute.toString();

        if (handled.contains(messageId)) {
            return;
        }
        handled.add(messageId);
        if (CheckAnalysisErrors.isFlake8ErrorHandledAtLine(line, messageId)) {
            return;
        }

        // Flake8 messages aren't really descriptive, so, let's try to show the description along.
        String displayString = "noqa: " + message;

        ICompletionProposalHandle proposal = CompletionProposalFactory.get()
                .createIgnoreFlake8CompletionProposalInSameLine(
                        messageId, ps.getEndLineOffset(), 0, offset,
                        SharedUiPlugin.getImageCache().get(UIConstants.ASSIST_ANNOTATION),
                        displayString, null,
                        null, IPyCompletionProposal.PRIORITY_DEFAULT, edit, line, ps, format, markerInfo);
        props.add(proposal);
    }
}
