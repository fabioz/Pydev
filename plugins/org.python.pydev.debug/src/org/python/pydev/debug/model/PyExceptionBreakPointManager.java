package org.python.pydev.debug.model;



public class PyExceptionBreakPointManager {

	private static PyExceptionBreakPointManager pyExceptionBreakPointManager;
	private AbstractDebugTarget pyDebugTarget;
	
	private PyExceptionBreakPointManager(){
		
	}
	
	public static PyExceptionBreakPointManager getInstance(){
		if(pyExceptionBreakPointManager == null){
			synchronized (PyExceptionBreakPointManager.class) {
				if(pyExceptionBreakPointManager == null){
					pyExceptionBreakPointManager = new PyExceptionBreakPointManager();
				}
			}
		}
		return pyExceptionBreakPointManager;
	}
	
	public AbstractDebugTarget getPyDebugTarget() {
		return pyDebugTarget;
	}

	public void setPyDebugTarget(AbstractDebugTarget pyDebugTarget) {
		this.pyDebugTarget = pyDebugTarget;
	}

}
