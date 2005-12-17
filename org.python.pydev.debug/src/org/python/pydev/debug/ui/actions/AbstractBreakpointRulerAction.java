package org.python.pydev.debug.ui.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;
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

	protected boolean breakpointAtRulerLine(PyBreakpoint pyBreakpoint) throws CoreException {
		AbstractMarkerAnnotationModel model = getAnnotationModel();
		if (model != null) {
			Position position= model.getMarkerPosition(pyBreakpoint.getMarker());
			if (position != null) {
				IDocumentProvider provider= getTextEditor().getDocumentProvider();
				IDocument doc=  provider.getDocument(getTextEditor().getEditorInput());
				try {
					int markerLineNumber= doc.getLineOfOffset(position.getOffset());
					int rulerLine= getInfo().getLineOfLastMouseButtonActivity();
					if (rulerLine == markerLineNumber) {
						if (getTextEditor().isDirty()) {
							return pyBreakpoint.getLineNumber() == markerLineNumber + 1;
						}
						return true;
					}
				} catch (BadLocationException x) {
				}
			}
		}
		
		return false;
	}

	/**
	 * Returns the <code>AbstractMarkerAnnotationModel</code> of the editor's input.
	 *
	 * @return the marker annotation model
	 */
	protected AbstractMarkerAnnotationModel getAnnotationModel() {
		IDocumentProvider provider= fTextEditor.getDocumentProvider();
		IAnnotationModel model= provider.getAnnotationModel(getTextEditor().getEditorInput());
		if (model instanceof AbstractMarkerAnnotationModel) {
			return (AbstractMarkerAnnotationModel) model;
		}
		return null;
	}
	
	/** 
	 * @return the resource for which to create the marker or <code>null</code>
	 */
	protected IResource getResource() {
		IEditorInput input= fTextEditor.getEditorInput();
		IResource resource= (IResource) input.getAdapter(IFile.class);
		if (resource == null) {
			resource= (IResource) input.getAdapter(IResource.class);
		}		
		return resource;
	}

}
