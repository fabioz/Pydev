/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.PyBreakpoint;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor_input.EditorInputUtils;
import org.python.pydev.shared_ui.utils.PyMarkerUtils;

/**
 * Some things similar to: org.eclipse.ui.texteditor.MarkerRulerAction
 */
public abstract class AbstractBreakpointRulerAction extends Action implements IUpdate {

    protected IVerticalRulerInfo fInfo;
    protected ITextEditor fTextEditor;
    private IBreakpoint fBreakpoint;

    protected IBreakpoint getBreakpoint() {
        return fBreakpoint;
    }

    protected void setBreakpoint(IBreakpoint breakpoint) {
        fBreakpoint = breakpoint;
    }

    protected ITextEditor getTextEditor() {
        return fTextEditor;
    }

    protected void setTextEditor(ITextEditor textEditor) {
        fTextEditor = textEditor;
    }

    protected IVerticalRulerInfo getInfo() {
        return fInfo;
    }

    protected void setInfo(IVerticalRulerInfo info) {
        fInfo = info;
    }

    /**
     * @return the breakpoint in the line the user clicked last or null if there is no such breakpoint.
     */
    protected IBreakpoint getBreakpointFromLastLineOfActivityInCurrentEditor() {
        List<IBreakpoint> breakpoints = getBreakpointsFromCurrentFile(true);
        int size = breakpoints.size();
        if (size == 0) {
            return null;

        } else if (size == 1) {
            return breakpoints.get(0);

        } else if (size > 1) {
            Log.log("Did not expect more than one breakpoint in the current line. Returning first.");
            return breakpoints.get(0);

        } else {
            Log.log("Unexpected condition!");
            return null;
        }
    }

    /**
     * @return the document of the editor's input
     */
    protected IDocument getDocument() {
        IDocumentProvider provider = fTextEditor.getDocumentProvider();
        return provider.getDocument(fTextEditor.getEditorInput());
    }

    protected IResource getResourceForDebugMarkers() {
        return PyMarkerUtils.getResourceForTextEditor(fTextEditor);
    }

    public boolean isExternalFileEditor() {
        return isExternalFileEditor(fTextEditor);
    }

    public IEditorInput getExternalFileEditorInput() {
        return getExternalFileEditorInput(fTextEditor);
    }

    // utilities -------------------------------------------------------------------------------------------------------
    // utilities -------------------------------------------------------------------------------------------------------
    // utilities -------------------------------------------------------------------------------------------------------
    // utilities -------------------------------------------------------------------------------------------------------
    // utilities -------------------------------------------------------------------------------------------------------

    /**
     * @return whether we're in an external editor or not.
     */
    public static boolean isExternalFileEditor(ITextEditor editor) {
        IEditorInput externalFileEditorInput = getExternalFileEditorInput(editor);
        if (externalFileEditorInput != null) {
            return true;
        }
        return false;
    }

    /**
     * @return the IEditorInput if we're dealing with an external file (or null otherwise)
     */
    public static IEditorInput getExternalFileEditorInput(ITextEditor editor) {
        IEditorInput input = editor.getEditorInput();

        //only return not null if it's an external file (IFileEditorInput marks a workspace file, not external file)
        if (input instanceof IFileEditorInput) {
            return null;
        }

        if (input instanceof IPathEditorInput) { //PydevFileEditorInput would enter here
            return input;
        }

        try {
            if (input instanceof IURIEditorInput) {
                return input;
            }
        } catch (Throwable e) {
            //IURIEditorInput not added until eclipse 3.3
        }

        //Note that IStorageEditorInput is not handled for external files (files from zip)

        return input;
    }

    /**
     * @return true if the given marker is from an external file, and the editor in which this action is being executed
     * is in this same editor.
     */
    protected static boolean isInSameExternalEditor(IMarker marker, IEditorInput externalFileEditorInput)
            throws CoreException {
        if (marker == null || externalFileEditorInput == null) {
            return false;
        }

        String attribute = (String) marker.getAttribute(PyBreakpoint.PY_BREAK_EXTERNAL_PATH_ID);
        if (attribute != null) {
            File file = EditorInputUtils.getFile(externalFileEditorInput);
            if (file == null) {
                return false;
            }
            if (attribute.equals(FileUtils.getFileAbsolutePath(file))) {
                return true;
            }
        }
        return false;
    }

    public static List<IMarker> getMarkersFromCurrentFile(BaseEditor edit, int line) {
        return getMarkersFromEditorResource(PyMarkerUtils.getResourceForTextEditor(edit), edit.getDocument(),
                getExternalFileEditorInput(edit), line, true, edit.getAnnotationModel());

    }

    protected List<IBreakpoint> getBreakpointsFromCurrentFile(boolean onlyIncludeLastLineActivity) {
        List<Tuple<IMarker, IBreakpoint>> markersAndBreakpointsFromEditorResource = getMarkersAndBreakpointsFromEditorResource(
                getResourceForDebugMarkers(), getDocument(), getExternalFileEditorInput(), getInfo()
                        .getLineOfLastMouseButtonActivity(), onlyIncludeLastLineActivity, getAnnotationModel());

        int size = markersAndBreakpointsFromEditorResource.size();
        ArrayList<IBreakpoint> r = new ArrayList<IBreakpoint>(size);
        for (int i = 0; i < size; i++) {
            r.add(markersAndBreakpointsFromEditorResource.get(i).o2);
        }
        return r;
    }

    private IAnnotationModel getAnnotationModel() {
        if (fTextEditor == null) {
            return null;
        }
        final IDocumentProvider documentProvider = fTextEditor.getDocumentProvider();
        if (documentProvider == null) {
            return null;
        }
        return documentProvider.getAnnotationModel(fTextEditor.getEditorInput());
    }

    /**
     * @return all the breakpoint markers from the current file or only the ones which match the last line of
     * activity (i.e.: the one the user clicked).
     */
    protected List<IMarker> getMarkersFromCurrentFile(boolean onlyIncludeLastLineActivity) {
        return getMarkersFromEditorResource(getResourceForDebugMarkers(), getDocument(), getExternalFileEditorInput(),
                getInfo().getLineOfLastMouseButtonActivity(), onlyIncludeLastLineActivity, getAnnotationModel());
    }

    public static List<IMarker> getMarkersFromEditorResource(IResource resource, IDocument document,
            IEditorInput externalFileEditorInput, int lastLineActivity, boolean onlyIncludeLastLineActivity,
            IAnnotationModel annotationModel) {
        List<Tuple<IMarker, IBreakpoint>> markersAndBreakpointsFromEditorResource = getMarkersAndBreakpointsFromEditorResource(
                resource, document, externalFileEditorInput, lastLineActivity, onlyIncludeLastLineActivity,
                annotationModel);

        int size = markersAndBreakpointsFromEditorResource.size();
        ArrayList<IMarker> r = new ArrayList<IMarker>(size);
        for (int i = 0; i < size; i++) {
            r.add(markersAndBreakpointsFromEditorResource.get(i).o1);
        }
        return r;
    }

    protected static boolean includesRulerLine(Position position, IDocument document, int line) {

        if (position != null) {
            try {
                int markerLine = document.getLineOfOffset(position.getOffset());
                if (line == markerLine) {
                    return true;
                }
            } catch (BadLocationException x) {
            }
        }

        return false;
    }

    /**
     * @param resource may be the file open in the editor or the workspace root (if it is an external file)
     * @param document is the document opened in the editor
     * @param externalFileEditorInput is not-null if this is an external file
     * @param info is the vertical ruler info (only used if this is not an external file)
     * @param onlyIncludeLastLineActivity if only the markers that are in the last mouse-click should be included
     *
     * @return the markers that correspond to the markers from the current editor.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static List<Tuple<IMarker, IBreakpoint>> getMarkersAndBreakpointsFromEditorResource(IResource resource,
            IDocument document, IEditorInput externalFileEditorInput, int lastLineActivity,
            boolean onlyIncludeLastLineActivity, IAnnotationModel annotationModel) {
        List<Tuple<IMarker, IBreakpoint>> breakpoints = new ArrayList<Tuple<IMarker, IBreakpoint>>();

        try {
            List<IMarker> markers = new ArrayList<IMarker>();
            boolean isExternalFile = false;

            markers.addAll(Arrays.asList(resource.findMarkers(PyBreakpoint.PY_BREAK_MARKER, true,
                    IResource.DEPTH_INFINITE)));
            markers.addAll(Arrays.asList(resource.findMarkers(PyBreakpoint.PY_CONDITIONAL_BREAK_MARKER, true,
                    IResource.DEPTH_INFINITE)));
            markers.addAll(Arrays.asList(resource.findMarkers(PyBreakpoint.DJANGO_BREAK_MARKER, true,
                    IResource.DEPTH_INFINITE)));

            if (!(resource instanceof IFile)) {
                //it was created from an external file
                isExternalFile = true;
            }

            IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
            for (IMarker marker : markers) {
                if (marker == null) {
                    continue;
                }
                IBreakpoint breakpoint = breakpointManager.getBreakpoint(marker);
                if (breakpoint != null && breakpointManager.isRegistered(breakpoint)) {
                    Position pos = PyMarkerUtils.getMarkerPosition(document, marker, annotationModel);

                    if (!isExternalFile) {
                        if (!onlyIncludeLastLineActivity) {
                            breakpoints.add(new Tuple(marker, breakpoint));
                        } else if (includesRulerLine(pos, document, lastLineActivity)) {
                            breakpoints.add(new Tuple(marker, breakpoint));
                        }
                    } else {

                        if (isInSameExternalEditor(marker, externalFileEditorInput)) {
                            if (!onlyIncludeLastLineActivity) {
                                breakpoints.add(new Tuple(marker, breakpoint));
                            } else if (includesRulerLine(pos, document, lastLineActivity)) {
                                breakpoints.add(new Tuple(marker, breakpoint));
                            }
                        }
                    }
                }
            }
        } catch (CoreException x) {
            PydevDebugPlugin.log(IStatus.ERROR, "Unexpected getMarkers error (recovered properly)", x);
        }
        return breakpoints;
    }

}
