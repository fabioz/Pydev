/*
 * Author: atotic
 * Created on Mar 11, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.python.pydev.plugin.PydevPlugin;


/**
 * TODO just a stub for now. Volunteers needed to implement real include preferenecs
 */
public class ProjectProperties extends PropertyPage {

	Text pathText;
	public static QualifiedName SRC_PATH_PROP = new QualifiedName(PydevPlugin.getPluginID(), "SOURCE_PATH");
	
	/**
	 * Sample 
	 */
	protected Control createContents(Composite parent) {
		Composite topComp= new Composite(parent, SWT.NONE);
		GridLayout topLayout= new GridLayout();
		topLayout.numColumns= 2;
		topLayout.marginWidth= 0;
		topLayout.marginHeight= 0;
		topComp.setLayout(topLayout);

		Label pathLabel = new Label(topComp, SWT.NONE);
		pathLabel.setText("Include path");
		GridData data = new GridData ();
		data.verticalAlignment = GridData.BEGINNING;
		pathLabel.setLayoutData (data);

		pathText = new Text (topComp, SWT.BORDER);
		data = new GridData ();
		data.grabExcessHorizontalSpace = true;
		pathText.setLayoutData (data);			
		
		return topComp;
	}

	/**
	 * Saves values into the project
	 */
	public boolean performOk() {
		IProject project = (IProject)getElement().getAdapter(IProject.class);
		if (project != null) {
			try {
				project.setPersistentProperty(SRC_PATH_PROP, pathText.getText());
			} catch (CoreException e) {
				PydevPlugin.log(IStatus.ERROR, "Unexpected error setting project properties", e);
			}
		}
		return true;
	}

}
