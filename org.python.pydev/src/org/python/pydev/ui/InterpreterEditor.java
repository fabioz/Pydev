/*
 * Author: atotic
 * Created: Sep 8, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Field editor for a list of python interpreter with executable verifier.
 * 
 * <p>heavily inspired by org.eclipse.jface.preference.PathEditor
 * <p>Tries to run python binary to make sure it exists
 */

public class InterpreterEditor extends ListEditor {

	/**
	 * The last path, or <code>null</code> if none.
	 */
	private String lastPath;

	/**
	 * Creates a path field editor.
	 * 
	 * @param name the name of the preference this field editor works on
	 * @param labelText the label text of the field editor
	 * @param parent the parent of the field editor's control
	 */
	public InterpreterEditor(
		String name,
		String labelText,
		Composite parent) {
		init(name, labelText);
		createControl(parent);
	}
	
	/* 
	 * Creates a single string of paths from a list of items
	 */
	protected String createList(String[] items) {
		StringBuffer path = new StringBuffer("");

		for (int i = 0; i < items.length; i++) {
			path.append(items[i]);
			path.append(File.pathSeparator);
		}
		return path.toString();
	}

	/*
	 * Method declared on ListEditor.
	 * Creates a new path element by means of a file dialog.
	 */
	protected String getNewInputObject() {
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		if (System.getProperty("os.name").startsWith("Win"))
			dialog.setFilterExtensions(new String[] {"*.exe", "*.*"});
		else
			; // TODO right file dialog executable filters for unix/mac?
		if (lastPath != null) {
			if (new File(lastPath).exists())
				dialog.setFilterPath(lastPath);
		}
		String file = dialog.open();
		if (file != null) {
			file = file.trim();
			if (file.length() == 0)
				return null;
			lastPath = file;
		}
		return file;
	}

	public static String[] getInterpreterList(String stringList) {
		if (stringList == null) {
			PydevPlugin.log(IStatus.WARNING, "No python interpreters specified", (Throwable)null);
			return new String[] {"python"};
		}
		StringTokenizer st = new StringTokenizer(stringList, File.pathSeparator + "\n\r"); //$NON-NLS-1$
		ArrayList v = new ArrayList();
		while (st.hasMoreElements()) {
			v.add(st.nextElement());
		}
		if (v.size() == 0)
			v.add("python");
		return (String[])v.toArray(new String[v.size()]);
	}

	/**
	 * Lifted from org.eclipse.help.internal.browser.MozillaFactory
	 * On some OSes 0 is always returned by "which" command
	 * it is necessary to examine ouput to find out failure.
	 * @param outputs
	 * @param errors
	 * @return true if there are errors
	 * @throws InterruptedException
	 */
	static private boolean errorsInOutput(
		String executable,
		StreamConsumer outputs,
		StreamConsumer errors) {
		try {
			outputs.join(1000);
			if (outputs.getLastLine() != null
				&& outputs.getLastLine().indexOf("no " + executable + " in")
					>= 0) {
				return true;
			}
			errors.join(1000);
			if (errors.getLastLine() != null
				&& errors.getLastLine().indexOf("no " + executable + " in")
					>= 0) {
				return true;
			}
		} catch (InterruptedException ie) {
			// ignore
		}
		return false;
	}
	
	/**
	 * true if executable is jython. A hack, 
	 */
	static public boolean isJython(String executable) {
		return executable.toLowerCase().indexOf("jython") != -1;
	}

	/**
	 * returns true if interpreter was launched successfully
	 */
	public static boolean validateInterpreterPath(String executable) {
		boolean retVal = true;
		
		try {
			String versionOption = " -V";
			// Jython command line option is --version, not -V
			if (isJython(executable))
				versionOption = " --version";
			Process pr = Runtime.getRuntime().exec(executable + versionOption);
			StreamConsumer outputs = new StreamConsumer(pr.getInputStream());
			outputs.start();
			StreamConsumer errors = new StreamConsumer(pr.getErrorStream());
			errors.start();
			pr.waitFor();
			int ret = pr.exitValue();
			if (ret == 0)
				retVal = !errorsInOutput(executable, outputs, errors);
			else
				retVal = false;
		} catch (InterruptedException e) {
			retVal = false;
		} catch (IOException e) {
			// launching which failed, assume browser executable is present
			retVal = false;
		}
		return retVal;
	}
	
	/* (non-Javadoc)
	 * Method declared on ListEditor.
	 */
	protected String[] parseString(String stringList) {
		return getInterpreterList(stringList);
	}
}
