/*
 * Author: atotic
 * Created on Apr 30, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;
import org.python.copiedfromeclipsesrc.PydevFileEditorInput;
import org.python.pydev.core.REF;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.PyBreakpoint;
import org.python.pydev.debug.model.PyDebugModelPresentation;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * Setting/removing breakpoints in the ruler
 * 
 * Inspired by:
 * 
 * @see org.eclipse.jdt.internal.debug.ui.actions.ManageBreakpointRulerAction
 */

public class BreakpointRulerAction extends AbstractBreakpointRulerAction {

    private List fMarkers;

    private String fAddLabel;

    private String fRemoveLabel;

    public BreakpointRulerAction(ITextEditor editor, IVerticalRulerInfo ruler) {
        setInfo(ruler);
        setTextEditor(editor);
        setText("Breakpoint &Properties...");
        fAddLabel = "Add Breakpoint";
        fRemoveLabel = "Remove Breakpoint";
    }

    /**
     * Checks whether a position includes the ruler's line of activity.
     * 
     * @param position the position to be checked
     * @param document the document the position refers to
     * @return <code>true</code> if the line is included by the given position
     */
    protected boolean includesRulerLine(Position position, IDocument document) {
        if (position != null) {
            try {
                int markerLine = document.getLineOfOffset(position.getOffset());
                int line = getInfo().getLineOfLastMouseButtonActivity();
                if (line == markerLine) {
                    return true;
                }
            } catch (BadLocationException x) {
            }
        }
        return false;
    }

    /**
     * @see IUpdate#update()
     */
    public void update() {
        fMarkers = getMarkersFromCurrentFile();
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
     * @return the markers that correspond to the markers from the current editor.
     */
    protected List<IMarker> getMarkersFromCurrentFile() {

        List<IMarker> breakpoints = new ArrayList<IMarker>();

        IResource resource = getResource();
        IDocument document = getDocument();

        try {
            List<IMarker> markers = new ArrayList<IMarker>();
            boolean isExternalFile = false;
            
            markers.addAll(Arrays.asList(resource.findMarkers(PyBreakpoint.PY_BREAK_MARKER, true, IResource.DEPTH_INFINITE)));
            markers.addAll(Arrays.asList(resource.findMarkers(PyBreakpoint.PY_CONDITIONAL_BREAK_MARKER, true, IResource.DEPTH_INFINITE)));
            
            if (!(resource instanceof IFile)) {
                //it was created from an external file
                isExternalFile = true;
            } 

            IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
            for (IMarker marker : markers) {
                IBreakpoint breakpoint = breakpointManager.getBreakpoint(marker);
                if (breakpoint != null && breakpointManager.isRegistered(breakpoint)) {
                    Position pos = getMarkerPosition(document, marker);
                    
                    if(!isExternalFile){
                        if (includesRulerLine(pos, document)) {
                            breakpoints.add(marker);
                        }
                    }else{
                        
                        if(isInSameExternalEditor(marker)){
                            breakpoints.add(marker);
                        }
                    }
                }
            }
        } catch (CoreException x) {
            PydevDebugPlugin.log(IStatus.ERROR, "Unexpected getMarkers error", x);
        }
        return breakpoints;
    }

    
    /**
     * This is the function that actually adds the marker to the Eclipse
     * structure.
     */
    protected void addMarker() {
        try {
            IDocument document = getDocument();
            int rulerLine = getInfo().getLineOfLastMouseButtonActivity();

            int lineNumber = rulerLine + 1;
            if (lineNumber < 0)
                return;

            // just to validate it
            document.getLineInformation(lineNumber - 1);
            final IResource resource = getResource();

            // The map containing the marker attributes
            final Map<String, Object> map = new HashMap<String, Object>();

            // if not null, we're dealing with an external file.
            final PydevFileEditorInput pydevFileEditorInput = getPydevFileEditorInput();

            // the location of the breakpoint
            String functionName = null;
            if (fTextEditor instanceof PyEdit) {
                SimpleNode ast = ((PyEdit) fTextEditor).getAST();
                if (ast != null) {
                    functionName = NodeUtils.getContextName(lineNumber - 1, ast);
                }
            }

            map.put(IMarker.MESSAGE, "what's the message");
            map.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
            map.put(IBreakpoint.ENABLED, new Boolean(true));
            map.put(IBreakpoint.ID, PyDebugModelPresentation.PY_DEBUG_MODEL_ID);
            if (functionName != null) {
                map.put(PyBreakpoint.FUNCTION_NAME_PROP, functionName);
            }
            if (pydevFileEditorInput != null) {
                map.put(PyBreakpoint.PY_BREAK_EXTERNAL_PATH_ID, REF.getFileAbsolutePath(pydevFileEditorInput.getFile()));
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
        } catch (BadLocationException e) {
            e.printStackTrace();
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param markers the markers that will be removed in this function (they may be in any editor, not only in the current one)
     */
    protected void removeMarkers(List markers) {
        IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
        try {
            Iterator e = markers.iterator();
            while (e.hasNext()) {
                IBreakpoint breakpoint = breakpointManager.getBreakpoint((IMarker) e.next());
                breakpointManager.removeBreakpoint(breakpoint, true);
            }
        } catch (CoreException e) {
            PydevDebugPlugin.log(IStatus.ERROR, "error removing markers", e);
        }
    }
}
