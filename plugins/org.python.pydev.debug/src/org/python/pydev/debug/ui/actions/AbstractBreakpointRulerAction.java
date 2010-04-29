package org.python.pydev.debug.ui.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.python.pydev.core.REF;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.PyBreakpoint;
import org.python.pydev.debug.model.PyDebugModelPresentation;
import org.python.pydev.editorinput.PydevFileEditorInput;

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

    protected IBreakpoint determineBreakpoint() {
        IBreakpoint[] breakpoints= DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(PyDebugModelPresentation.PY_DEBUG_MODEL_ID);
        for (int i= 0; i < breakpoints.length; i++) {
            IBreakpoint breakpoint= breakpoints[i];
            if (breakpoint instanceof PyBreakpoint ) {
                PyBreakpoint pyBreakpoint= (PyBreakpoint)breakpoint;
                try {
                    if (breakpointAtRulerLine(pyBreakpoint, getExternalFileEditorInput())) {
                        return pyBreakpoint;
                    }
                } catch (CoreException ce) {
                    PydevDebugPlugin.log(IStatus.ERROR,ce.getLocalizedMessage(),ce);
                    continue;
                }
            }
        }
        return null;
    }

    /**
     * @return the document of the editor's input
     */
    protected IDocument getDocument() {
        IDocumentProvider provider = fTextEditor.getDocumentProvider();
        return provider.getDocument(fTextEditor.getEditorInput());
    }

    protected boolean breakpointAtRulerLine(PyBreakpoint pyBreakpoint, IEditorInput externalFileEditorInput) throws CoreException {
        IDocument doc = getDocument();
        IMarker marker = pyBreakpoint.getMarker();
        Position position= getMarkerPosition(doc, marker);
        if (position != null) {
            try {
                int markerLineNumber= doc.getLineOfOffset(position.getOffset());
                if(getResourceForDebugMarkers() instanceof IFile){
                    //workspace file
                    int rulerLine= getInfo().getLineOfLastMouseButtonActivity();
                    if (rulerLine == markerLineNumber) {
                        if (getTextEditor().isDirty()) {
                            return pyBreakpoint.getLineNumber() == markerLineNumber + 1;
                        }
                        return true;
                    }
                }else if(isInSameExternalEditor(marker, externalFileEditorInput)){
                    return true;
                }
            } catch (BadLocationException x) {
            }
        }
        
        return false;
    }
    
    protected IResource getResourceForDebugMarkers() {
        return getResourceForDebugMarkers(fTextEditor);
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
        if(externalFileEditorInput != null){
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
        if(input instanceof IFileEditorInput){
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
    protected static boolean isInSameExternalEditor(IMarker marker, IEditorInput externalFileEditorInput) throws CoreException {
        if(marker == null || externalFileEditorInput == null){
            return false;
        }
        
        String attribute = (String) marker.getAttribute(PyBreakpoint.PY_BREAK_EXTERNAL_PATH_ID);
        if(attribute != null){
        	File file = PydevFileEditorInput.getFile(externalFileEditorInput);
            if(file == null){
                return false;
            }
            if(attribute.equals(REF.getFileAbsolutePath(file))){
                return true;
            }
        }
        return false;
    }


    /**
     * @return the position for a marker.
     */
    public static Position getMarkerPosition(IDocument document, IMarker marker) {
        int start = MarkerUtilities.getCharStart(marker);
        int end = MarkerUtilities.getCharEnd(marker);

        if (start > end) {
            end = start + end;
            start = end - start;
            end = end - start;
        }

        if (start == -1 && end == -1) {
            // marker line number is 1-based
            int line = MarkerUtilities.getLineNumber(marker);
            if (line > 0 && document != null) {
                try {
                    start = document.getLineOffset(line - 1);
                    end = start;
                } catch (BadLocationException x) {
                }
            }
        }

        if (start > -1 && end > -1)
            return new Position(start, end - start);

        return null;
    }

    
    /** 
     * @return the resource for which to create the marker or <code>null</code>
     * 
     * If the editor maps to a workspace file, it will return that file. Otherwise, it will return the 
     * workspace root (so, markers from external files will be created in the workspace root).
     */
    public static IResource getResourceForDebugMarkers(ITextEditor textEditor) {
        IEditorInput input= textEditor.getEditorInput();
        IResource resource= (IResource) input.getAdapter(IFile.class);
        if (resource == null) {
            resource= (IResource) input.getAdapter(IResource.class);
        }        
        if(resource == null){
            resource = ResourcesPlugin.getWorkspace().getRoot();
        }
        return resource;
    }

    

    /**
     * Checks whether a position includes the ruler's line of activity.
     * 
     * @param position the position to be checked
     * @param document the document the position refers to
     * @return <code>true</code> if the line is included by the given position
     */
    public static boolean includesRulerLine(Position position, IDocument document, IVerticalRulerInfo info) {
        if (position != null && info != null && document != null) {
            try {
                int markerLine = document.getLineOfOffset(position.getOffset());
                int line = info.getLineOfLastMouseButtonActivity();
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
    public static List<IMarker> getMarkersFromEditorResource(IResource resource, IDocument document, 
            IEditorInput externalFileEditorInput, IVerticalRulerInfo info,
            boolean onlyIncludeLastLineActivity) {

        if(onlyIncludeLastLineActivity){
            Assert.isNotNull(info);
        }
        
        List<IMarker> breakpoints = new ArrayList<IMarker>();

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
                if(marker == null){
                    continue;
                }
                IBreakpoint breakpoint = breakpointManager.getBreakpoint(marker);
                if (breakpoint != null && breakpointManager.isRegistered(breakpoint)) {
                    Position pos = getMarkerPosition(document, marker);
                    
                    if(!isExternalFile){
                        if(!onlyIncludeLastLineActivity){
                            breakpoints.add(marker);
                        }else if (includesRulerLine(pos, document, info)) {
                            breakpoints.add(marker);
                        }
                    }else{
                        
                        if(isInSameExternalEditor(marker, externalFileEditorInput)){
                            if(!onlyIncludeLastLineActivity){
                                breakpoints.add(marker);
                            }else if (includesRulerLine(pos, document, info)) {
                                breakpoints.add(marker);
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
