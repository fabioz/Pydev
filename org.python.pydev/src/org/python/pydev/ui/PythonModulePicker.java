package org.python.pydev.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;

/**
 * Dialog to choose a Python module from a project
 * 
 * @author Mikko Ohtamaa
 */
public class PythonModulePicker extends ElementTreeSelectionDialog {
	
	
	public PythonModulePicker(Shell parent, IProject project) {
		super(parent, null, null);
	}

	public PythonModulePicker(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider) {
		super(parent, labelProvider, contentProvider);
		// TODO Auto-generated constructor stub
	}
	
	
}
