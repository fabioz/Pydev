package org.python.pydev.debug.model;



public class PyExceptionBreakPointManager {

	private static PyExceptionBreakPointManager pyExceptionBreakPointManager;
	private PyDebugTarget pyDebugTarget;
	
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
	
	public PyDebugTarget getPyDebugTarget() {
		return pyDebugTarget;
	}

	public void setPyDebugTarget(PyDebugTarget pyDebugTarget) {
		this.pyDebugTarget = pyDebugTarget;
	}
	
	public void removePyDebugTarget() {
		this.pyDebugTarget = null;
	}
}
