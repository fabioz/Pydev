/*
 * Author: atotic
 * Created on Apr 28, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.LineBreakpoint;

/**
 * Represents python breakpoint.
 * 
 */
public class PyBreakpoint extends LineBreakpoint {
    /**
     * Marker attribute storing the path id of some external file
     */
    public static final String PY_BREAK_EXTERNAL_PATH_ID = "org.python.pydev.debug.PYDEV_EXTERNAL_PATH_ID";

	static public final String PY_BREAK_MARKER = "org.python.pydev.debug.pyStopBreakpointMarker";
	
	static public final String PY_CONDITIONAL_BREAK_MARKER = "org.python.pydev.debug.pyConditionalStopBreakpointMarker";
	
	static public final String FUNCTION_NAME_PROP = "pydev.function_name";

	/**
	 * Breakpoint attribute storing a breakpoint's conditional expression
	 * (value <code>"org.eclipse.jdt.debug.core.condition"</code>). This attribute is stored as a
	 * <code>String</code>.
	 */
	protected static final String CONDITION= "org.python.pydev.debug.condition"; //$NON-NLS-1$
	/**
	 * Breakpoint attribute storing a breakpoint's condition enablement
	 * (value <code>"org.eclipse.jdt.debug.core.conditionEnabled"</code>). This attribute is stored as an
	 * <code>boolean</code>.
	 */
	protected static final String CONDITION_ENABLED= "org.python.pydev.debug.conditionEnabled";

	public PyBreakpoint() {
	}

	public String getModelIdentifier() {
		return PyDebugModelPresentation.PY_DEBUG_MODEL_ID;
	}
	
	public String getFile() {
		IMarker marker = getMarker();
        IResource r = marker.getResource();
        if(r instanceof IFile){
            return r.getLocation().toOSString();
        }else{
            //it's an external file...
            try {
                return (String) marker.getAttribute(PyBreakpoint.PY_BREAK_EXTERNAL_PATH_ID);
            } catch (CoreException e) {
                throw new RuntimeException(e);
            }
        }
	}
	
	public Object getLine() {
		try {
			return getMarker().getAttribute(IMarker.LINE_NUMBER);
		} catch (CoreException e) {
			return "";
		}
	}

	public boolean supportsCondition() {
		return true;
	}

	public String getCondition() throws DebugException {
		return ensureMarker().getAttribute(CONDITION, null);
	}

	public boolean isConditionEnabled() throws DebugException {
		return ensureMarker().getAttribute(CONDITION_ENABLED, false);
	}

	public void setConditionEnabled(boolean conditionEnabled) throws CoreException {
		setAttributes(new String[]{CONDITION_ENABLED}, new Object[]{new Boolean(conditionEnabled)});
	}

	public void setCondition(String condition) throws CoreException {
		if (condition != null && condition.trim().length() == 0) {
			condition = null;
		}
		setAttributes(new String []{CONDITION}, new Object[]{condition});
	}
	
	
	/**
	 * Returns the marker associated with this breakpoint.
	 * 
	 * @return breakpoint marker
	 * @exception DebugException if no marker is associated with 
	 *  this breakpoint or the associated marker does not exist
	 */
	protected IMarker ensureMarker() throws DebugException {
		IMarker m = getMarker();
		if (m == null || !m.exists()) {
			throw new DebugException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugException.REQUEST_FAILED,
				"Breakpoint_no_associated_marker", null));
		}
		return m;
	}
}
