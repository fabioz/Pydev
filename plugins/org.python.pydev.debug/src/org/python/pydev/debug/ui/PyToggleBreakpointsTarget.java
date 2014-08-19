/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTargetExtension;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.debug.model.PyBreakpoint;
import org.python.pydev.debug.ui.actions.PyBreakpointRulerAction;
import org.python.pydev.editor.PyEdit;

public class PyToggleBreakpointsTarget implements IToggleBreakpointsTarget, IToggleBreakpointsTargetExtension,
        IPyToggleBreakpointsTarget {

    PyToggleBreakpointsTarget() {
    }

    // --------------- All others point to this 2 methods!
    public void toggleBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
        if (part instanceof PyEdit && selection instanceof TextSelection) {
            TextSelection textSelection = (TextSelection) selection;
            PyEdit pyEdit = (PyEdit) part;
            int startLine = textSelection.getStartLine();

            List<IMarker> markersFromCurrentFile = PyBreakpointRulerAction.getMarkersFromCurrentFile(pyEdit, startLine);
            if (markersFromCurrentFile.size() > 0) {
                PyBreakpointRulerAction.removeMarkers(markersFromCurrentFile);
            } else {
                PyBreakpointRulerAction.addBreakpointMarker(pyEdit.getDocument(), startLine + 1, pyEdit,
                        PyBreakpoint.PY_BREAK_TYPE_PYTHON);
            }

        }

    }

    public boolean canToggleBreakpoints(IWorkbenchPart part, ISelection selection) {
        return selection instanceof TextSelection && part instanceof PyEdit;
    }

    public void toggleLineBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
        toggleBreakpoints(part, selection);
    }

    public boolean canToggleLineBreakpoints(IWorkbenchPart part, ISelection selection) {
        return canToggleBreakpoints(part, selection);
    }

    public void toggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
        toggleBreakpoints(part, selection);
    }

    public boolean canToggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) {
        return canToggleBreakpoints(part, selection);
    }

    public void toggleWatchpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
        toggleBreakpoints(part, selection);
    }

    public boolean canToggleWatchpoints(IWorkbenchPart part, ISelection selection) {
        return canToggleBreakpoints(part, selection);
    }

    @Override
    public void addBreakpointMarker(IDocument document, int line, ITextEditor fTextEditor) {
        PyBreakpointRulerAction.addBreakpointMarker(document, line, fTextEditor, PyBreakpoint.PY_BREAK_TYPE_PYTHON);
    }

}
