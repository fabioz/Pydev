package org.python.pydev.debug.ui.actions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.plugin.PydevPlugin;

public class PyConfigureException extends PyAction {

	private static String DELIMITER = "|";
	private static String SEPERATOR = "/";
	
	public void run(IAction action) {

		IStructuredContentProvider contentProvider = new ExceptionProvider();
		ListSelectionDialog dialog = new ListSelectionDialog(getShell(), "",
				contentProvider, new LabelProvider(), "");

		dialog.setInitialElementSelections(readExceptionsFromFile());
		dialog.setTitle("Add Python Exception Breakpoint");
		dialog.setMessage("Choose an Exception (* = any string, ? = any char):");
		dialog.open();

		Object[] selectedItems = dialog.getResult();
		if (selectedItems != null) {
			String[] exceptionArray = Arrays.copyOf(selectedItems,
					selectedItems.length, String[].class);
			writeExceptionsToFile(exceptionArray);
		}
	}

	private void writeExceptionsToFile(String[] exceptionArray) {
		String pyExceptionsToBreak = StringUtils.join(DELIMITER, exceptionArray);
		IPath path = PydevDebugPlugin.getWorkspace().getRoot().getLocation();
		String filePath = path.toString() + SEPERATOR + Constants.FILE_PATH;
		String fileName = filePath + SEPERATOR + Constants.FILE_NAME;
		try {
			FileWriter fstream = new FileWriter(fileName);
			BufferedWriter bufferedWriter = new BufferedWriter(fstream);
			bufferedWriter.write(pyExceptionsToBreak);
			bufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<String> readExceptionsFromFile() {
		
		String[] lastSelectedItems = {} ;
		IPath path = PydevDebugPlugin.getWorkspace().getRoot().getLocation();
		String filePath = path.toString() + SEPERATOR + Constants.FILE_PATH;
		String fileName = filePath + SEPERATOR + Constants.FILE_NAME;
		try {
		    BufferedReader bReader = new BufferedReader(new FileReader(fileName));
		    String exceptionString;
		    while ((exceptionString = bReader.readLine()) != null) {
		    	lastSelectedItems = exceptionString.split("\\" + DELIMITER);
		    }
		    bReader.close();
		} catch (IOException e) {
		}


		return Arrays.asList(lastSelectedItems);
	}

	private class ExceptionProvider implements IStructuredContentProvider {

		public void dispose() {

		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public Object[] getElements(Object inputElement) {
			IInterpreterManager manager = PydevPlugin
					.getPythonInterpreterManager();
			IPythonNature pythonNature;
			try {
				pythonNature = getPyEdit().getPythonNature();
			} catch (MisconfigurationException e1) {
				return null;
			}

			IModule builtInPythonMod = pythonNature.getBuiltinMod();
			pythonNature.getAstManager();

			/*
			 * TODO: Exceptions are hardcoded in an enum here for the time being
			 * Had to merge with the Fabio's latest development branch
			 */
			Collection<String> elements = new ArrayList<String>();
			for (PyExceptions exception : PyExceptions.values()) {
				elements.add(exception.toString());
			}
			return elements.toArray();
		}

	}

}
