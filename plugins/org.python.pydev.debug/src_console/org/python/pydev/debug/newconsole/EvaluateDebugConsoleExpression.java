package org.python.pydev.debug.newconsole;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.ui.DebugUITools;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.EvaluateConsoleExpressionCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;

/**
 * Class to exectute console command in the debugging context
 * 
 * @author hussain.bohra
 * 
 */
public class EvaluateDebugConsoleExpression implements ICommandResponseListener {

	String EMPTY = (String) StringUtils.EMPTY;
	private String payload;
	
	/**
	 * Last selected frame in the debug console
	 */
	private PyStackFrame lastSelectedFrame;

	/**
	 * By default, debug console will be linked with the selected frame
	 */
	private boolean isLinkedWithDebug = true;

	
	/**
     * @return the currently selected / suspended frame.
     */
    public static PyStackFrame getCurrentSuspendedPyStackFrame(){
        IAdaptable context = DebugUITools.getDebugContext();
        
        if(context instanceof PyStackFrame){
            PyStackFrame stackFrame = (PyStackFrame) context;
            if(!stackFrame.isTerminated() && stackFrame.isSuspended()){
                return stackFrame;
            }
        }
        return null;
    }

	/**
	 * If debug console is linked with the selected frame in debug window, then
	 * it returns the current suspended frame. Otherwise it returns the frame
	 * that was selected on the last line of execution.
	 * 
	 * @return selectedFrame in debug view
	 */
	public PyStackFrame getLastSelectedFrame() {
		if (lastSelectedFrame == null) {
			lastSelectedFrame = getCurrentSuspendedPyStackFrame();
		}
		
		if (isLinkedWithDebug){
			lastSelectedFrame = getCurrentSuspendedPyStackFrame();
			return lastSelectedFrame;
		} else { // Console is not linked with debug selection
			if (lastSelectedFrame == null) { 
				return null;
			} else {
				if (lastSelectedFrame.getThread().isSuspended()){
					// Debugger is currently paused
					return lastSelectedFrame;
				} else { // return null if debugger is not paused
					return null;
				}
			}
		}
	}
    
    /**
     * This method will get called from AbstractDebugTarget when 
     * output arrives for the posted command 
     */
    public void commandComplete(AbstractDebuggerCommand cmd) {
		try {
			this.payload = ((EvaluateConsoleExpressionCommand) cmd)
					.getResponse();
		} catch (CoreException e) {
			this.payload = e.getMessage();
		}
	}

    /**
     * Reset the payload
     */
	public void resetPayload() {
		this.payload = null;
	}

	/**
	 * Initialize the console
	 * 
	 * @param consoleId
	 */
	public void initializeConsole() {
		PyStackFrame frame = getLastSelectedFrame();
		if (frame != null) {
			AbstractDebugTarget target = frame.getTarget();
			String locator = getLocator(frame.getThreadId(),
					frame.getId(), "INITIALIZE");
			AbstractDebuggerCommand cmd = new EvaluateConsoleExpressionCommand(
					target, locator, this);
			target.postCommand(cmd);
		}
	}

	/**
	 * Execute the line in selected frame context
	 * 
	 * @param consoleId
	 * @param command
	 */
	public void executeCommand(String command) {
		PyStackFrame frame = getLastSelectedFrame();
		if (frame != null) {
			AbstractDebugTarget target = frame.getTarget();
			String locator = getLocator(frame.getThreadId(),
					frame.getId(), "EVALUATE", command);
			AbstractDebuggerCommand cmd = new EvaluateConsoleExpressionCommand(
					target, locator, this);
			target.postCommand(cmd);
		}
	}

	/**
	 * Post the completions command
	 * 
	 * @param consoleId
	 * @param actTok
	 * @param offset
	 */
	public String getCompletions(String actTok, int offset){
		String result = EMPTY;
		PyStackFrame frame = getLastSelectedFrame();
		if (frame != null) {
			AbstractDebugTarget target = frame.getTarget();
			String locator = getLocator(frame.getThreadId(),
					frame.getId(), "GET_COMPLETIONS", actTok);
			AbstractDebuggerCommand cmd = new EvaluateConsoleExpressionCommand(
					target, locator, this);
			target.postCommand(cmd);
			result = waitForCommand();
		}
		return result;
	}

	/**
	 * Create and send the command to debugger to close the console
	 * 
	 * @param consoleId
	 */
	public void close(){
		if (lastSelectedFrame != null) {
			AbstractDebugTarget target = lastSelectedFrame.getTarget();
			String locator = getLocator(lastSelectedFrame.getThreadId(),
					lastSelectedFrame.getId(), "CLOSE");
			AbstractDebuggerCommand cmd = new EvaluateConsoleExpressionCommand(
					target, locator, this);
			target.postCommand(cmd);
		}
	}

	/**
	 * Keeps in a loop for 3 seconds or until the completions are found. If no
	 * completions are found in that time, returns an empty array.
	 */
	public String waitForCommand() {
		int timeout = PydevConsoleConstants.CONSOLE_TIMEOUT; // wait up to 3 seconds
		while (--timeout > 0 && payload == null) {
			try {
				Thread.sleep(10); // 10 millis
			} catch (InterruptedException e) {
				// ignore
			}
		}

		String temp = this.payload;
		this.payload = null;
		if (temp == null) {
			Log.logInfo("Timeout for waiting for debug completions elapsed (3 seconds).");
			return EMPTY;
		}
		return temp;
	}

	/**
	 * join and return all locators with '\t' 
	 * 
	 * @param locators
	 * @return
	 */
	private String getLocator(String... locators){
		return StringUtils.join("\t", locators);
	}
	
	/**
	 * Enable/Disable linking of the debug console with the suspended frame.
	 * 
	 * @param isLinkedWithDebug
	 */
	public void linkWithDebugSelection(boolean isLinkedWithDebug){
		this.isLinkedWithDebug = isLinkedWithDebug;
	}


	/**
	 * This class represent the console message to be displayed in the debug console. 
	 * 
	 * @author hussain.bohra
	 *
	 */
	public static class PydevDebugConsoleMessage {

		private boolean more;
		private StringBuilder outputMessage = new StringBuilder();
		private StringBuilder errorMessage = new StringBuilder();

		public boolean isMore() {
			return more;
		}

		public void setMore(boolean more) {
			this.more = more;
		}

		public void appendMessage(String output, boolean isError) {
			if (!isError) {
				outputMessage.append(output);
				outputMessage.append("\n");
			} else {
				errorMessage.append(output);
				errorMessage.append("\n");
			}
		}

		public StringBuilder getOutputMessage() {
			return outputMessage;
		}

		public StringBuilder getErrorMessage() {
			return errorMessage;
		}
	}
}
