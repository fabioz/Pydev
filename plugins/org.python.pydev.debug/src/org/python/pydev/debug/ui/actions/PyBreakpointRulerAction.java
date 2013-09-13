/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic, fabioz
 * Created on Apr 30, 2004
 */
package org.python.pydev.debug.ui.actions;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.PyBreakpoint;
import org.python.pydev.debug.model.PyDebugModelPresentation;
import org.python.pydev.editorinput.PydevFileEditorInput;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_ui.utils.PyMarkerUtils;

/**
 * Setting/removing breakpoints in the ruler
 * 
 * Inspired by:
 * 
 * @see org.eclipse.jdt.internal.debug.ui.actions.ManageBreakpointRulerAction
 */

public class PyBreakpointRulerAction extends AbstractBreakpointRulerAction {

    public static final String PYDEV_BREAKPOINT = "PyDev breakpoint";

    private List<IMarker> fMarkers;

    private String fAddLabel;

    private String fRemoveLabel;

    public PyBreakpointRulerAction(ITextEditor editor, IVerticalRulerInfo ruler) {
        setInfo(ruler);
        setTextEditor(editor);
        setText("Breakpoint &Properties...");
        fAddLabel = "Add Breakpoint";
        fRemoveLabel = "Remove Breakpoint";
    }

    /**
     * @see IUpdate#update()
     */
    public void update() {
        fMarkers = getMarkersFromCurrentFile(true);
        setText(fMarkers.isEmpty() ? fAddLabel : fRemoveLabel);
    }

    /**
     * @see Action#run()
     */
    public void run() {
        if (fMarkers.isEmpty()) {
            addMarker();
        } else {
            removeMarkers(fMarkers);
        }
    }

    /**
     * This is the function that actually adds the marker to the Eclipse
     * structure.
     */
    protected void addMarker() {
        IDocument document = getDocument();
        int rulerLine = getInfo().getLineOfLastMouseButtonActivity();
        addBreakpointMarker(document, rulerLine + 1, fTextEditor);
    }

    public static void addBreakpointMarker(IDocument document, int lineNumber, ITextEditor textEditor) {
        try {
            if (lineNumber < 0)
                return;

            // just to validate it
            try {
                document.getLineInformation(lineNumber - 1);
            } catch (Exception e) {
                return; //ignore
            }
            final IResource resource = PyMarkerUtils.getResourceForTextEditor(textEditor);

            // The map containing the marker attributes
            final Map<String, Object> map = new HashMap<String, Object>();

            // if not null, we're dealing with an external file.
            final IEditorInput externalFileEditorInput = getExternalFileEditorInput(textEditor);

            //TODO: that happens when we're trying to set a breakpoint in a file that's within a zip file.
            if (externalFileEditorInput == null && resource instanceof IWorkspaceRoot) {
                return;
            }

            map.put(IMarker.MESSAGE, PYDEV_BREAKPOINT);
            map.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
            map.put(IBreakpoint.ENABLED, new Boolean(true));
            map.put(IBreakpoint.ID, PyDebugModelPresentation.PY_DEBUG_MODEL_ID);
            if (externalFileEditorInput != null) {
                File file = PydevFileEditorInput.getFile(externalFileEditorInput);
                if (file != null) {
                    map.put(PyBreakpoint.PY_BREAK_EXTERNAL_PATH_ID, FileUtils.getFileAbsolutePath(file));
                }
            }

            IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
                public void run(IProgressMonitor monitor) throws CoreException {
                    IMarker marker = resource.createMarker(PyBreakpoint.PY_BREAK_MARKER);
                    marker.setAttributes(map);
                    PyBreakpoint br = new PyBreakpoint();
                    br.setMarker(marker);
                    IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
                    breakpointManager.addBreakpoint(br);
                }
            };

            resource.getWorkspace().run(runnable, null);
        } catch (Exception e) {
            Log.log(e);
        }
    }

    /**
     * @param markers the markers that will be removed in this function (they may be in any editor, not only in the current one)
     */
    public static void removeMarkers(List<IMarker> markers) {
        IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
        try {
            Iterator<IMarker> e = markers.iterator();
            while (e.hasNext()) {
                IBreakpoint breakpoint = breakpointManager.getBreakpoint(e.next());
                breakpointManager.removeBreakpoint(breakpoint, true);
            }
        } catch (CoreException e) {
            PydevDebugPlugin.log(IStatus.ERROR, "error removing markers", e);
        }
    }
}
