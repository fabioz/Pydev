/*
 * Author: atotic
 * Created on May 6, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.python.pydev.debug.core.PydevDebugPlugin;

/**
 * Provides decoration for model elements in the debugger interface.
 */
public class PyDebugModelPresentation implements IDebugModelPresentation {

	static public String PY_DEBUG_MODEL_ID = "org.python.pydev.debug";

	protected ListenerList fListeners= new ListenerList();		


	public Image getImage(Object element) {
		if (element instanceof PyBreakpoint) {
			try {
				if (((PyBreakpoint)element).isEnabled())
					return PydevDebugPlugin.getImageCache().get("icons/breakmarker.gif");
				else
					return PydevDebugPlugin.getImageCache().get("icons/breakmarker_gray.gif");
			} catch (CoreException e) {
				PydevDebugPlugin.log(IStatus.ERROR, "getImage error", e);
			}
		}
		else if (element instanceof PyDebugTarget ||
				element instanceof PyThread ||
				element instanceof PyStackFrame)
			return null;
		return null;
	}

	public String getText(Object element) {
		if (element instanceof PyBreakpoint) {
			IMarker marker = ((PyBreakpoint)element).getMarker();
			try {
				Map attrs = marker.getAttributes();
	//				Set set = attrs.keySet();
	//				for (Iterator i = set.iterator(); i.hasNext();)
	//					System.out.println(i.next().toString());
				IResource resource = marker.getResource();
				String file = resource.getFullPath().lastSegment();
				Object lineNumber = attrs.get(IMarker.LINE_NUMBER);
				String functionName = (String)attrs.get(PyBreakpoint.FUNCTION_NAME_PROP);
				if (file == null)
					file = "unknown";
				if (lineNumber == null)
					lineNumber = "unknown";
				String location = file + ":" + lineNumber.toString();
				if (functionName == null)
					return location;
				else
					return functionName + " [" + location + "]";
			} catch (CoreException e) {
				PydevDebugPlugin.log(IStatus.ERROR, "error retreiving marker attributes", e);
				return "error";
			}
		}
		else if (element instanceof PyDebugTarget 
				|| element instanceof PyStackFrame 	
				|| element instanceof PyThread) {
			return null;
		}
		PydevDebugPlugin.log(IStatus.ERROR, "unknown debug type", null);
		return null;
	}

	/**
	 * override
	 * TODO comment this method
	 */
	public void computeDetail(IValue value, IValueDetailListener listener) {
		// TODO Auto-generated method stub
		System.out.println("in detail");
	}

	/**
	 * override
	 * TODO comment this method
	 */
	public IEditorInput getEditorInput(Object element) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * override
	 * TODO comment this method
	 */
	public String getEditorId(IEditorInput input, Object element) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setAttribute(String attribute, Object value) {
		// TODO Auto-generated method stub
		System.out.println("setattribute");
	}

	public void addListener(ILabelProviderListener listener) {
		fListeners.add(listener);
	}

	public void removeListener(ILabelProviderListener listener) {
		fListeners.remove(listener);
	}

	public void dispose() {
	}


	public boolean isLabelProperty(Object element, String property) {
		// Not really sure what this does. see IBaseLabelProvider:isLabelProperty
		return false;
	}


}
