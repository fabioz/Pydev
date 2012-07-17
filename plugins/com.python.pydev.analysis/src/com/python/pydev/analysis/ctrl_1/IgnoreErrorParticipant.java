/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
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
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.builder.AnalysisRunner;

public class IgnoreErrorParticipant implements IAnalysisMarkersParticipant {

    private Image annotationImage;

    private Set<Integer> handled = new HashSet<Integer>();

    public IgnoreErrorParticipant() {
        ImageCache analysisImageCache = PydevPlugin.getImageCache();
        annotationImage = analysisImageCache.get(UIConstants.ASSIST_ANNOTATION);

    }

    /** 
     * @throws CoreException 
     * @see com.python.pydev.analysis.ctrl_1.IAnalysisMarkersParticipant#addProps(org.eclipse.core.resources.IMarker, com.python.pydev.analysis.IAnalysisPreferences, java.lang.String, org.python.pydev.core.docutils.PySelection, int, org.python.pydev.editor.PyEdit, java.util.List)
     */
    public void addProps(MarkerAnnotationAndPosition marker, IAnalysisPreferences analysisPreferences, String line,
            PySelection ps, int offset, IPythonNature nature, PyEdit edit, List<ICompletionProposal> props)
            throws BadLocationException, CoreException {
        Integer id = (Integer) marker.markerAnnotation.getMarker().getAttribute(AnalysisRunner.PYDEV_ANALYSIS_TYPE);
        if (handled.contains(id)) {
            return;
        }
        handled.add(id);
        String messageToIgnore = analysisPreferences.getRequiredMessageToIgnore(id);

        if (line.indexOf(messageToIgnore) != -1) {
            //ok, move on...
            return;
        }

        String strToAdd = messageToIgnore;
        int lineLen = line.length();
        char lastChar = lineLen == 0 ? ' ' : line.charAt(lineLen - 1);

        if (line.indexOf("#") == -1) {
            strToAdd = "#" + strToAdd;
        }

        if (!Character.isWhitespace(lastChar)) {
            strToAdd = " " + strToAdd;
        }

        IgnoreCompletionProposal proposal = new IgnoreCompletionProposal(strToAdd, ps.getEndLineOffset(), 0, offset,
                annotationImage, messageToIgnore.substring(1), null, null, PyCompletionProposal.PRIORITY_DEFAULT, edit);
        props.add(proposal);
    }
}
