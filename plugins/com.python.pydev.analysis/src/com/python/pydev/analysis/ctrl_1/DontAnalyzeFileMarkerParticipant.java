/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
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
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.editor.correctionassist.heuristics.IAssistProps;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

import com.python.pydev.analysis.builder.AnalysisRunner;

public class DontAnalyzeFileMarkerParticipant implements IAssistProps {

    private Image annotationImage;

    public DontAnalyzeFileMarkerParticipant() {
        ImageCache analysisImageCache = PydevPlugin.getImageCache();
        annotationImage = analysisImageCache.get(UIConstants.ASSIST_ANNOTATION);
    }

    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, IPythonNature nature,
            PyEdit edit, int offset) throws BadLocationException {
        List<ICompletionProposal> props = new ArrayList<ICompletionProposal>();
        if (ps.getCursorLine() == 0) {
            IgnoreCompletionProposal proposal = new IgnoreCompletionProposal(AnalysisRunner.PYDEV_CODE_ANALYSIS_IGNORE
                    + ps.getEndLineDelim(), 0, 0, AnalysisRunner.PYDEV_CODE_ANALYSIS_IGNORE.length()
                    + ps.getEndLineDelim().length(), annotationImage,
                    AnalysisRunner.PYDEV_CODE_ANALYSIS_IGNORE.substring(1), null, null,
                    PyCompletionProposal.PRIORITY_DEFAULT, edit);
            props.add(proposal);

        }
        return props;
    }

    public boolean isValid(PySelection ps, String sel, PyEdit edit, int offset) {
        return ps.getCursorLine() == 0
                && ps.getCursorLineContents().indexOf(AnalysisRunner.PYDEV_CODE_ANALYSIS_IGNORE) == -1;
    }

}
