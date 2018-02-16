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
import org.python.pydev.ast.analysis.IAnalysisPreferences;
import org.python.pydev.core.CheckAnalysisErrors;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.formatter.FormatStd;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_core.image.IImageCache;
import org.python.pydev.shared_core.image.UIConstants;
import org.python.pydev.shared_ui.SharedUiPlugin;

import com.python.pydev.analysis.additionalinfo.builders.AnalysisRunner;

public class IgnoreErrorParticipant implements IAnalysisMarkersParticipant {

    private Set<Integer> handled = new HashSet<>();

    private FormatStd format;

    public IgnoreErrorParticipant() {
        this(null);
    }

    /**
     * Only for tests.
     */
    /*default*/ IgnoreErrorParticipant(FormatStd format) {
        this.format = format;
    }

    /**
     * @throws CoreException
     * @see org.python.pydev.ast.analysis.ctrl_1.IAnalysisMarkersParticipant#addProps(org.eclipse.core.resources.IMarker, org.python.pydev.ast.analysis.IAnalysisPreferences, java.lang.String, org.python.pydev.core.docutils.PySelection, int, org.python.pydev.editor.PyEdit, java.util.List)
     */
    @Override
    public void addProps(MarkerAnnotationAndPosition marker, IAnalysisPreferences analysisPreferences,
            final String line, final PySelection ps, int offset, IPythonNature nature, final PyEdit edit,
            List<ICompletionProposalHandle> props)
            throws BadLocationException, CoreException {
        Integer id = (Integer) marker.markerAnnotation.getMarker().getAttribute(AnalysisRunner.PYDEV_ANALYSIS_TYPE);
        if (handled.contains(id)) {
            return;
        }
        handled.add(id);
        final String messageToIgnore = analysisPreferences.getRequiredMessageToIgnore(id);
        if (CheckAnalysisErrors.isCodeAnalysisErrorHandled(line, messageToIgnore)) {
            return;
        }

        IImageCache imageCache = SharedUiPlugin.getImageCache();
        ICompletionProposalHandle proposal = CompletionProposalFactory.get().createIgnoreCompletionProposalInSameLine(
                messageToIgnore, ps.getEndLineOffset(), 0, offset,
                imageCache != null ? imageCache.get(UIConstants.ASSIST_ANNOTATION) : null,
                messageToIgnore.substring(1), null, null, IPyCompletionProposal.PRIORITY_DEFAULT, edit, line, ps,
                format);
        props.add(proposal);
    }
}
