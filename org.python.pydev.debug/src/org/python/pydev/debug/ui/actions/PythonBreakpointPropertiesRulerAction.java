package org.python.pydev.debug.ui.actions;

import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.PyBreakpoint;

public class PythonBreakpointPropertiesRulerAction extends
		AbstractBreakpointRulerAction implements IAction {

	public PythonBreakpointPropertiesRulerAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
		setInfo(rulerInfo);
		setTextEditor(editor);
		setText("Breakpoint &Properties..."); 
	}

	/**
	 * @throws CoreException 
	 * @see Action#run()
	 */
	public void run() {
		IBreakpoint breakPoint = getBreakpoint();
		if (breakPoint != null) {
			PropertyDialogAction action= 
				new PropertyDialogAction(getTextEditor().getEditorSite().getShell(), new ISelectionProvider() {
					public void addSelectionChangedListener(ISelectionChangedListener listener) {
					}
					public ISelection getSelection() {
						return new StructuredSelection(getBreakpoint());
					}
					public void removeSelectionChangedListener(ISelectionChangedListener listener) {
					}
					public void setSelection(ISelection selection) {
					}
				});
			action.run();	
			
			/*
			Map map = null;
			try {
				map = breakPoint.getMarker().getAttributes();
			} catch (CoreException e) {
				PydevDebugPlugin.log(IStatus.ERROR, e.getLocalizedMessage(), e);
			}
			
			IEditorInput editorInput= getTextEditor().getEditorInput();		
			final IResource resource = (IResource)editorInput.getAdapter(IResource.class);
			
			if (resource == null) {
				PydevDebugPlugin.log(IStatus.ERROR, "Could not find resource to create marker on", null);
				return;
			}
				
			
			try {
				if (map != null) {
					//resource.deleteMarkers(breakPoint.getMarker().getType(), false, 0);
					IMarker marker = breakPoint.getMarker();
					marker.delete();
					
					marker= resource.createMarker(PyBreakpoint.PY_CONDITIONAL_BREAK_MARKER);
					marker.setAttributes(map);
					breakPoint.setMarker(marker);
					
					IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
					breakpointManager.addBreakpoint(breakPoint);
				}
			} catch (CoreException e) {
				PydevDebugPlugin.log(IStatus.ERROR, e.getLocalizedMessage(), e);
			}*/
		}
	}
	
	public void update() {
		setBreakpoint(determineBreakpoint());
		if (getBreakpoint() == null || !(getBreakpoint() instanceof PyBreakpoint)) {
			setBreakpoint(null);
			setEnabled(false);
			return;
		}
		setEnabled(true);
		
//		IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
//		breakpointManager.fireBreakpointChanged(getBreakpoint());
	}

}
