/*
 * Author: atotic
 * Created on Apr 28, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;

/**
 * Represents a python variable.
 * 
 * Eclipse gives you an option to separate implementation of variable
 * and its value. I've found it convenient to roll both of them into 1
 * class.
 * 
 */
public class PyVariable extends PlatformObject implements IVariable, IValue {
	
	protected String name;
	protected String type;
	protected String value;
	protected AbstractDebugTarget target;
	protected boolean isModified;
	
	//Only create one instance of an empty array to be returned
	private static final IVariable[] EMPTY_IVARIABLE_ARRAY = new IVariable[0]; 

	public PyVariable(AbstractDebugTarget target, String name, String type, String value) {
		this.value = value;
		this.name = name;
		this.type = type;
		this.target = target;
		isModified = false;
	}

	public String getDetailText() throws DebugException {
		return getValueString();
	}
	
	public IValue getValue() throws DebugException {
		return this;
	}
	
	public String getValueString() throws DebugException {
		if (value == null)
			return "";
		if ("StringType".equals(type) ||
			"UnicodeType".equals(type))	// quote the strings
			return "\"" + value + "\"";
		return value;
	}

	public String getName() throws DebugException {
		return name;
	}


	public String getModelIdentifier() {
		return target.getModelIdentifier();
	}

	public IDebugTarget getDebugTarget() {
		return target;
	}

	public ILaunch getLaunch() {
		return target.getLaunch();
	}

	/**
	 * LATER valueChanging nterface has not been implemented yet.
	 * When implemented, recently changed variables are shown in red.
	 */
	public boolean supportsValueModification() {
		return true;
	}

	public boolean hasValueChanged() throws DebugException {
		return isModified;
	}

	public void setModified( boolean mod ) {
		isModified = mod;
	}
	
	public void setValue(String expression) throws DebugException {
	}

	public void setValue(IValue value) throws DebugException {
	}
	
	public boolean verifyValue(String expression) throws DebugException {
		return false;
	}

	public boolean verifyValue(IValue value) throws DebugException {
		return false;
	}


	public Object getAdapter(Class adapter) {
		if (adapter.equals(ILaunch.class))
			return target.getAdapter(adapter);
		else if (adapter.equals(IPropertySource.class) ||
				adapter.equals(ITaskListResourceAdapter.class) ||
				adapter.equals(org.eclipse.ui.IContributorResourceAdapter.class) ||
				adapter.equals(org.eclipse.ui.IActionFilter.class) ||
				adapter.equals(org.eclipse.ui.model.IWorkbenchAdapter.class)
				|| adapter.equals(org.eclipse.debug.ui.actions.IToggleBreakpointsTarget.class)
				|| adapter.equals(org.eclipse.debug.ui.actions.IRunToLineTarget.class)
				||	adapter.equals(IResource.class)
				|| adapter.equals(org.eclipse.core.resources.IFile.class)
				)
			return  super.getAdapter(adapter);
		// ongoing, I do not fully understand all the interfaces they'd like me to support
		// so I print them out as errors
		System.err.println("PyVariable Need adapter " + adapter.toString());
		return super.getAdapter(adapter);
	}

	public boolean isAllocated() throws DebugException {
		return true;
	}

	public IVariable[] getVariables() throws DebugException {
		return EMPTY_IVARIABLE_ARRAY;
	}

	public boolean hasVariables() throws DebugException {
		return false;
	}
	
	public String getReferenceTypeName() throws DebugException {
		return type;
	}

}
