/*
 * Author: atotic
 * Created on Apr 30, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.internal.resources.MarkerAttributeMap;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
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
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.editor.model.FunctionNode;
import org.python.pydev.editor.model.Location;
import org.python.pydev.editor.model.ModelUtils;

/**
 * Setting/removing breakpoints in the ruler
 * 
 * Inspired by:
 * @see org.eclipse.jdt.internal.debug.ui.actions.ManageBreakpointRulerAction
 */

public class BreakpointRulerAction extends Action implements IUpdate {

	private IVerticalRulerInfo fRuler;
	private ITextEditor fTextEditor;
	private List fMarkers;

	private String fAddLabel;
	private String fRemoveLabel;
	
	public BreakpointRulerAction(ITextEditor editor, IVerticalRulerInfo ruler) {
		fRuler= ruler;
		fTextEditor= editor;
		fAddLabel= "Add Breakpoint"; 
		fRemoveLabel= "Remove Breakpoint";
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
				int markerLine= document.getLineOfOffset(position.getOffset());
				int line= fRuler.getLineOfLastMouseButtonActivity();
				if (line == markerLine) {
					return true;
				}
			} catch (BadLocationException x) {
			}
		}
		return false;
	}
	
	/**
	 * @return this action's vertical ruler
	 */
	protected IVerticalRulerInfo getVerticalRulerInfo() {
		return fRuler;
	}
	
	/**
	 * @return this action's editor
	 */
	protected ITextEditor getTextEditor() {
		return fTextEditor;
	}
	
	/**
	 * @return the marker annotation model of the editor's input.
	 */
	protected AbstractMarkerAnnotationModel getAnnotationModel() {
		IDocumentProvider provider= fTextEditor.getDocumentProvider();
		IAnnotationModel model= provider.getAnnotationModel(fTextEditor.getEditorInput());
		if (model instanceof AbstractMarkerAnnotationModel) {
			return (AbstractMarkerAnnotationModel) model;
		}
		return null;
	}

	/**
	 * @return the document of the editor's input
	 */
	protected IDocument getDocument() {
		IDocumentProvider provider= fTextEditor.getDocumentProvider();
		return provider.getDocument(fTextEditor.getEditorInput());
	}
	
	/**
	 * @see IUpdate#update()
	 */
	public void update() {
		fMarkers= getMarkers();
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
	
	protected List getMarkers() {
		
		List breakpoints= new ArrayList();
		
		IResource resource= getResource();
		IDocument document= getDocument();
		AbstractMarkerAnnotationModel model= getAnnotationModel();
		
		if (model != null) {
			try {
				
				IMarker[] markers= null;
				if (resource instanceof IFile)
					markers= resource.findMarkers(PyBreakpoint.PY_BREAK_MARKER, true, IResource.DEPTH_INFINITE);
				else {
					IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
					markers= root.findMarkers(PyBreakpoint.PY_BREAK_MARKER, true, IResource.DEPTH_INFINITE);
				}
				
				if (markers != null) {
					IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
					for (int i= 0; i < markers.length; i++) {
						IBreakpoint breakpoint= breakpointManager.getBreakpoint(markers[i]);
						if (breakpoint != null && breakpointManager.isRegistered(breakpoint)) {
							Position pos = model.getMarkerPosition(markers[i]); 
							if (includesRulerLine(pos, document))
								breakpoints.add(markers[i]);
						}
					}
				}
			} catch (CoreException x) {
				PydevDebugPlugin.log(IStatus.ERROR, "Unexpected getMarkers error", x);
			}
		}
		return breakpoints;
	}

	protected void addMarker() {		
		try {
			IDocument document= getDocument();
			int rulerLine= getVerticalRulerInfo().getLineOfLastMouseButtonActivity();
			
			int lineNumber = rulerLine + 1;
			if (lineNumber < 0)
				return;
			IRegion line= document.getLineInformation(lineNumber - 1);
			IEditorInput editorInput= getTextEditor().getEditorInput();			
			final IResource resource = (IResource)editorInput.getAdapter(IResource.class);
			
			if (resource == null)
				throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Could not find resource to create marker on", null));

			final MarkerAttributeMap map = new MarkerAttributeMap();
			
			String functionName = getFunctionAboveLine(document, lineNumber-1);
			
			map.put(IMarker.MESSAGE, "what's the message");
			map.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
			map.put(IBreakpoint.ENABLED, new Boolean(true));
			map.put(IBreakpoint.ID, PyDebugModelPresentation.PY_DEBUG_MODEL_ID);
			if (functionName != null)
				map.put(PyBreakpoint.FUNCTION_NAME_PROP, functionName);

			IWorkspaceRunnable r= new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					IMarker marker= resource.createMarker(PyBreakpoint.PY_BREAK_MARKER);
					marker.setAttributes(map);
					PyBreakpoint br = new PyBreakpoint();
					br.setMarker(marker);
					IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
					breakpointManager.addBreakpoint(br);
				}
			};
			
			resource.getWorkspace().run(r, null);
		} catch (BadLocationException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param document
	 * @param lineNumber
	 * @return
	 */
	private String getFunctionAboveLine(IDocument document, int lineNumber) {
		if (!(fTextEditor instanceof PyEdit))
			return null;
		AbstractNode root = ((PyEdit)fTextEditor).getPythonModel();
		AbstractNode closest = ModelUtils.getLessOrEqualNode(root, new Location(lineNumber+1, 0));
		while (closest != null &&
			!(closest instanceof FunctionNode))
			closest = closest.getParent();
		if (closest != null)
			return closest.getName();
		return null;
	}

	protected void removeMarkers(List markers) {
		IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
		try {
			Iterator e= markers.iterator();
			while (e.hasNext()) {
				IBreakpoint breakpoint= breakpointManager.getBreakpoint((IMarker) e.next());
				breakpointManager.removeBreakpoint(breakpoint, true);
			}
		} catch (CoreException e) {
			PydevDebugPlugin.log(IStatus.ERROR, "error removing markers", e);
		}
	}
}
