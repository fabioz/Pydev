/*
 * Author: atotic
 * Created on Apr 22, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;
import org.python.pydev.debug.model.remote.AbstractRemoteDebugger;
import org.python.pydev.debug.model.remote.GetFrameCommand;
import org.python.pydev.debug.model.remote.GetVariableCommand;

/**
 * Represents a stack entry.
 * 
 * Needs to integrate with the source locator
 */
public class PyStackFrame extends PlatformObject implements IStackFrame, IVariableLocator {

	private String name;
	private PyThread thread;
	private String id;
	private IPath path;
	private int line;
	private IVariable[] variables;
	private IVariableLocator localsLocator;
	private IVariableLocator globalsLocator;
	private IVariableLocator frameLocator;
	private AbstractDebugTarget target;
	private static final IVariable[] EMPTY_IVARIABLE_ARRAY = new IVariable[0]; 

	public PyStackFrame(PyThread in_thread, String in_id, String name, IPath file, int line, AbstractDebugTarget target) {
		this.id = in_id;
		this.name = name;
		this.path = file;
		this.line = line;
		this.thread = in_thread;
		localsLocator = new IVariableLocator() {
			public String getPyDBLocation() {
				return thread.getId() + "\t" + id + "\tLOCAL"; 
			}
		};
		frameLocator = new IVariableLocator() {
			public String getPyDBLocation() {
				return thread.getId() + "\t" + id + "\tFRAME"; 
			}
		};
		globalsLocator = new IVariableLocator() {
			public String getPyDBLocation() {
				return thread.getId() + "\t" + id + "\tGLOBAL"; 
			}
		};
		this.target = target;
	}

	public String getId() {
		return id;
	}
	
	public IVariableLocator getLocalsLocator() {
		return localsLocator;
	}
	
	public IVariableLocator getGlobalLocator() {
		return globalsLocator;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public void setPath(IPath path) {
		this.path = path;
	}
	
	public void setLine(int line) {
		this.line = line;
	}

	public IPath getPath() {
		return path;
	}
	
	public IThread getThread() {
		return thread;
	}
	
	public void setVariables(IVariable[] locals) {
		this.variables = locals;
	}
	
	public IVariable[] getVariables() throws DebugException {
		return EMPTY_IVARIABLE_ARRAY;
	}

    /**
     * create a map with the variables, such that the name of the variable points to the IVariable
     * 
     * @return the map
     */
	public Map<String, IVariable> getVariablesAsMap() throws DebugException {
        HashMap<String, IVariable> map = new HashMap<String, IVariable>();
        for (IVariable var : variables) {
            map.put(var.getName(), var);
        }
	    return map;
	}
	
	public boolean hasVariables() throws DebugException {
		return true;
	}

	public int getLineNumber() throws DebugException {
		return line;
	}

	public int getCharStart() throws DebugException {
		return -1;
	}

	public int getCharEnd() throws DebugException {
		return -1;
	}

	public String getName() throws DebugException {
		return name + " [" + path.lastSegment() + ":" + Integer.toString(line) + "]";
	}

	public IRegisterGroup[] getRegisterGroups() throws DebugException {
		return null;
	}

	public boolean hasRegisterGroups() throws DebugException {
		return false;
	}

	public String getModelIdentifier() {
		return thread.getModelIdentifier();
	}

	public IDebugTarget getDebugTarget() {
		return thread.getDebugTarget();
	}

	public ILaunch getLaunch() {
		return thread.getLaunch();
	}

	public boolean canStepInto() {
		return thread.canStepInto();
	}

	public boolean canStepOver() {
		return thread.canStepOver();
	}

	public boolean canStepReturn() {
		return thread.canStepReturn();
	}

	public boolean isStepping() {
		return thread.isStepping();
	}

	public void stepInto() throws DebugException {
		thread.stepInto();
	}

	public void stepOver() throws DebugException {
		thread.stepOver();
	}

	public void stepReturn() throws DebugException {
		thread.stepReturn();
	}

	public boolean canResume() {
		return thread.canResume();
	}

	public boolean canSuspend() {
		return thread.canSuspend();
	}

	public boolean isSuspended() {
		return thread.isSuspended();
	}

	public void resume() throws DebugException {
		thread.resume();
	}

	public void suspend() throws DebugException {
		thread.suspend();
	}

	public boolean canTerminate() {
		return thread.canTerminate();
	}

	public boolean isTerminated() {
		return thread.isTerminated();
	}

	public void terminate() throws DebugException {
		thread.terminate();
	}

	public Object getAdapter(Class adapter) {
		if (adapter.equals(ILaunch.class) ||
			adapter.equals(IResource.class)){
			return thread.getAdapter(adapter);
		}	
		
		if (adapter.equals(ITaskListResourceAdapter.class)){
			return null;
		}
		
		if (adapter.equals(IPropertySource.class) 
			|| adapter.equals(ITaskListResourceAdapter.class)
			|| adapter.equals(org.eclipse.debug.ui.actions.IToggleBreakpointsTarget.class)
			|| adapter.equals(org.eclipse.debug.ui.actions.IRunToLineTarget.class)
			){
			return  super.getAdapter(adapter);
		}
		
		if (adapter.equals(IDeferredWorkbenchAdapter.class)){
			return new DeferredWorkbenchAdapter(this);
		}
		
		// ongoing, I do not fully understand all the interfaces they'd like me to support
//		System.err.println("PyStackFrame Need adapter " + adapter.toString());
		return super.getAdapter(adapter);
	}

	/**
	 * fixed - this was bug http://sourceforge.net/tracker/index.php?func=detail&aid=1174821&group_id=85796&atid=577329
	 * in the forum (unable to get stack correctly when recursing)
	 */
	public int hashCode() {
		return id.hashCode();
	}
    
	/**
     * fixed - this was bug http://sourceforge.net/tracker/index.php?func=detail&aid=1174821&group_id=85796&atid=577329
     * in the forum (unable to get stack correctly when recursing)
	 */
	public boolean equals(Object obj) {
        if (obj instanceof PyStackFrame) {
            PyStackFrame sf = (PyStackFrame) obj;
            return this.id.equals(sf.id) && this.path.toString().equals(sf.path.toString())
                    && this.line == sf.line;
        }
        return false;
    }
	
	public GetVariableCommand getFrameCommand(AbstractRemoteDebugger dbg) {
		return new GetFrameCommand(dbg, frameLocator.getPyDBLocation());
	}

	public String getPyDBLocation() {
		return this.frameLocator.getPyDBLocation();
	}

	public AbstractRemoteDebugger getDebugger() {
		return target.getDebugger();
	}

}
