/*
 * Author: atotic
 * Created on Apr 28, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.LineBreakpoint;

/**
 * Represents python breakpoint.
 * 
 */
public class PyBreakpoint extends LineBreakpoint {

	static public final String PY_BREAK_MARKER = "org.python.pydev.debug.pyStopBreakpointMarker";
	
	static public final String FUNCTION_NAME_PROP = "pydev.function_name";
	
	public PyBreakpoint() {
	}

	public String getModelIdentifier() {
		return PyDebugModelPresentation.PY_DEBUG_MODEL_ID;
	}
	
	public String getFile() {
		IResource r = getMarker().getResource();
		return r.getLocation().toOSString();
	}
	
	public Object getLine() {
		try {
			return getMarker().getAttribute(IMarker.LINE_NUMBER);
		} catch (CoreException e) {
			return "";
		}
	}
}
