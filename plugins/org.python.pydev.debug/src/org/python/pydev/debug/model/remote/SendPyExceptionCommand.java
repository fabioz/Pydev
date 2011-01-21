package org.python.pydev.debug.model.remote;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.XMLUtils;

public class SendPyExceptionCommand extends AbstractDebuggerCommand {

	int commandId;

	public SendPyExceptionCommand(AbstractDebugTarget debugger, int commandId) {
		super(debugger);
		this.commandId = commandId;
	}

	@Override
	public String getOutgoing() {
		String pyExceptions = "";
		try {
			pyExceptions = readExceptionXML();
		} catch (CoreException e) {
			String msg = "Unexpected XML SAX error";
			PydevDebugPlugin.log(IStatus.ERROR, msg, new RuntimeException(msg));
		}
		return makeCommand(commandId, sequence, pyExceptions);
	}
	
	/**
	 * Read and Parse xml file from
	 * workspace/.metadata/.plugins/org.python.pydev/python_exceptions.xml
	 * 
	 * @return
	 * @throws CoreException 
	 */
	private String readExceptionXML() throws CoreException {
		String pyExceptionsToBreak = "";
		IPath path = PydevDebugPlugin.getWorkspace().getRoot().getLocation();
		String filePath = path.toString() + "/" + Constants.FILE_PATH;
		String fileName = filePath + "/" + Constants.FILE_NAME;
		if (isFileExists(fileName)) {
			pyExceptionsToBreak = XMLUtils.getPyException(fileName);
		} else {
			XMLUtils.createXMLFile(filePath, Constants.FILE_NAME);
			return (String) StringUtils.EMPTY;
		}
		return pyExceptionsToBreak;
	}

	/**
	 * Check whether the file exists at given path
	 * 
	 * @param filePath
	 * @return
	 */
	private boolean isFileExists(String filePath) {
		File file = new File(filePath);
		return file.exists();
	}
}
