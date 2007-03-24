package org.python.pydev.debug.ui.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.python.copiedfromeclipsesrc.PydevFileEditorInput;
import org.python.pydev.core.REF;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.PyBreakpoint;
import org.python.pydev.debug.model.PyDebugModelPresentation;

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
					if (breakpointAtRulerLine(pyBreakpoint)) {
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

	protected boolean breakpointAtRulerLine(PyBreakpoint pyBreakpoint) throws CoreException {
        IDocument doc = getDocument();
		IMarker marker = pyBreakpoint.getMarker();
        Position position= getMarkerPosition(doc, marker);
		if (position != null) {
			try {
				int markerLineNumber= doc.getLineOfOffset(position.getOffset());
                if(getResource() instanceof IFile){
                    //workspace file
                    int rulerLine= getInfo().getLineOfLastMouseButtonActivity();
                    if (rulerLine == markerLineNumber) {
                        if (getTextEditor().isDirty()) {
                            return pyBreakpoint.getLineNumber() == markerLineNumber + 1;
                        }
                        return true;
                    }
                }else if(isInSameExternalEditor(marker)){
                    return true;
                }
			} catch (BadLocationException x) {
			}
		}
		
		return false;
	}

    /**
     * @return true if the given marker is from an external file, and the editor in which this action is being executed
     * is in this same editor.
     */
    protected boolean isInSameExternalEditor(IMarker marker) throws CoreException {
        String attribute = (String) marker.getAttribute(PyBreakpoint.PY_BREAK_EXTERNAL_PATH_ID);
        if(attribute != null){
            PydevFileEditorInput pydevFileEditorInput = getPydevFileEditorInput();
            if(attribute.equals(REF.getFileAbsolutePath(pydevFileEditorInput.getFile()))){
                return true;
            }
        }
        return false;
    }

    

    /**
     * @return the PydevFileEditorInput if we're dealing with an external file (or null otherwise)
     */
    protected PydevFileEditorInput getPydevFileEditorInput() {
        IEditorInput input = fTextEditor.getEditorInput();
        final PydevFileEditorInput pydevFileEditorInput;
        if (input instanceof PydevFileEditorInput) {
            pydevFileEditorInput = (PydevFileEditorInput) input;
        } else {
            pydevFileEditorInput = null;
        }
        return pydevFileEditorInput;
    }


    /**
     * @return the position for a marker.
     */
    protected Position getMarkerPosition(IDocument document, IMarker marker) {
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
	protected IResource getResource() {
		IEditorInput input= fTextEditor.getEditorInput();
		IResource resource= (IResource) input.getAdapter(IFile.class);
		if (resource == null) {
			resource= (IResource) input.getAdapter(IResource.class);
		}		
        if(resource == null){
            resource = ResourcesPlugin.getWorkspace().getRoot();
        }
		return resource;
	}

}
