/*
 * Author: atotic
 * Created: Sep 8, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.List;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * Field editor for a list of python interpreter with executable verifier.
 * 
 * <p>heavily inspired by org.eclipse.jface.preference.PathEditor
 * <p>Tries to run python binary to make sure it exists
 * 
 * Subclasses must implement :
 * <code>parseString</code>,
 * <code>createList</code>, 
 * <code>getNewInputObject</code>
 */

public class InterpreterEditor extends ListEditor {

	/**
	 * The last path, or <code>null</code> if none.
	 */
	private String lastPath;
	
	private Map execToLibs = new HashMap();

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
	
	/**
     * @see org.eclipse.jface.preference.FieldEditor#createControl(org.eclipse.swt.widgets.Composite)
     */
    protected void createControl(Composite parent) {
        super.createControl(parent);
        final List listControl = getListControl(parent);
        listControl.addSelectionListener(new SelectionListener(){

            public void widgetSelected(SelectionEvent e) {
                System.out.println("widgetSelected "+e);
                if(listControl.getSelectionCount()==1){
                    String s = listControl.getSelection()[0];
                    System.out.println(s);
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                System.out.println("widgetDefaultSelected "+e);
            }
            
        });
    }
	
	/**
	 * Method declared on ListEditor.
	 * Creates a new path element by means of a file dialog.
	 */
	protected String getNewInputObject() {
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		
		if (System.getProperty("os.name").startsWith("Win")){
			dialog.setFilterExtensions(new String[] {"*.exe", "*.*"});
		}else{
			// right file dialog executable filters for unix/mac?
		}
		
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
		
		//ok, now that we got the file, let's see if it is valid and get the library info.
		InterpreterInfo info = PydevPrefs.getInfoFromExecutable(file);
		return file;
	}


	
	/**
	 * true if executable is jython. A hack, 
	 */
	static public boolean isJython(String executable) {
		return executable.toLowerCase().indexOf("jython") != -1;
	}
	
	
	/** 
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


	public static String[] getInterpreterList(String stringList) {
		if (stringList == null) {
			PydevPlugin.log(IStatus.WARNING, "No python interpreters specified", (Throwable)null);
			return new String[] {};
		}
		StringTokenizer st = new StringTokenizer(stringList, File.pathSeparator + "\n\r"); //$NON-NLS-1$
		ArrayList v = new ArrayList();
		while (st.hasMoreElements()) {
			v.add(st.nextElement());
		}
		return (String[])v.toArray(new String[v.size()]);
	}

	/**
	 */
	protected String[] parseString(String stringList) {
		return getInterpreterList(stringList);
	}
} 
