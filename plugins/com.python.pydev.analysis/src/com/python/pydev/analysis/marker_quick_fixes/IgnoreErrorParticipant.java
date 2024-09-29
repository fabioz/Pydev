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
package com.python.pydev.analysis.marker_quick_fixes;

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
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_core.image.IImageCache;
import org.python.pydev.shared_core.image.UIConstants;

public class IgnoreErrorParticipant implements IAnalysisMarkersParticipant {

    private Set<Integer> handled = new HashSet<>();

    private FormatStd format;

    public IgnoreErrorParticipant() {
        this(null);
    }

    private IgnoreErrorParticipant(FormatStd format) {
        this.format = format;
    }

    /**
     * Only for tests.
     */
    public static IgnoreErrorParticipant createForTests(FormatStd format) {
        return new IgnoreErrorParticipant(format);
    }

    @Override
    public void addProps(IMarkerInfoForAnalysis markerInfo, IAnalysisPreferences analysisPreferences,
            final String line, final PySelection ps, int offset, IPythonNature nature, final IPyEdit edit,
            List<ICompletionProposalHandle> props)
            throws BadLocationException, CoreException {
        Integer id = markerInfo.getPyDevAnalisysType();
        if (handled.contains(id)) {
            return;
        }
        handled.add(id);
        final String messageToIgnore = analysisPreferences.getRequiredMessageToIgnore(id);
        if (CheckAnalysisErrors.isCodeAnalysisErrorHandled(line, messageToIgnore)) {
            return;
        }

        IImageCache imageCache = SharedCorePlugin.getImageCache();
        ICompletionProposalHandle proposal = CompletionProposalFactory.get().createIgnoreCompletionProposalInSameLine(
                messageToIgnore, ps.getEndLineOffset(), 0, offset,
                imageCache != null ? imageCache.get(UIConstants.ASSIST_ANNOTATION) : null,
                messageToIgnore.substring(1), null, null, IPyCompletionProposal.PRIORITY_DEFAULT, edit, line, ps,
                format);
        props.add(proposal);
    }
}
