/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/09/2005
 */
package com.python.pydev.analysis.ctrl_1;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.python.pydev.ast.analysis.IAnalysisPreferences;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;

import com.python.pydev.analysis.additionalinfo.builders.AnalysisRunner;
import com.python.pydev.analysis.refactoring.quick_fixes.UndefinedVariableQuickFixCreator;

/**
 * Class that'll create proposals for fixing an undefined variable found.
 *
 * @author Fabio
 */
public class UndefinedVariableFixParticipant implements IAnalysisMarkersParticipant {

    /**
     * Defines whether a reparse should be forced after applying the completion.
     */
    private boolean forceReparseOnApply;

    public UndefinedVariableFixParticipant() {
        this(true);
    }

    public UndefinedVariableFixParticipant(boolean forceReparseOnApply) {
        this.forceReparseOnApply = forceReparseOnApply;
    }

    /**
     * @see IAnalysisMarkersParticipant#addProps(MarkerAnnotation, IAnalysisPreferences, String, PySelection, int, IPythonNature,
     * PyEdit, List)
     *
     */
    @Override
    public void addProps(MarkerAnnotationAndPosition markerAnnotation, IAnalysisPreferences analysisPreferences,
            String line, PySelection ps, int offset, IPythonNature initialNature, PyEdit edit,
            List<ICompletionProposalHandle> props)
            throws BadLocationException, CoreException {
        IMarker marker = markerAnnotation.markerAnnotation.getMarker();
        Integer id = (Integer) marker.getAttribute(AnalysisRunner.PYDEV_ANALYSIS_TYPE);
        if (id != IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE) {
            return;
        }
        if (initialNature == null) {
            return;
        }
        ICodeCompletionASTManager astManager = initialNature.getAstManager();
        if (astManager == null) {
            return;
        }

        if (markerAnnotation.position == null) {
            return;
        }
        int start = markerAnnotation.position.offset;
        int end = start + markerAnnotation.position.length;
        UndefinedVariableQuickFixCreator.createImportQuickProposalsFromMarkerSelectedText(edit, ps, offset,
                initialNature,
                props, astManager, start, end, forceReparseOnApply);
    }

}
