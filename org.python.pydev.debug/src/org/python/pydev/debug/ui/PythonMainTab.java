/*
 * Author: atotic
 * Created: Aug 20, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.python.pydev.debug.core.*;

/**
 * The main Python debug setup tab.
 * 
 * <p>Interesting functionality: InterpreterEditor will try the verify the choice of an interpreter.
 * <p>Getting the ModifyListener to display the proper error message was tricky
 */
public class PythonMainTab extends AbstractLaunchConfigurationTab {

	Image tabIcon;
	// Widgets
	Text locationField;
	Text baseDirectoryField;
	Text programArgumentField;
	Combo interpreterField;
	protected ModifyListener modifyListener = new ModifyListener() {
		
		public void modifyText(ModifyEvent e) {
			updateLaunchConfigurationDialog();
		}
	};
	
	public void dispose() {
		super.dispose();
		if (tabIcon != null)
			tabIcon.dispose();
	}

	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		GridLayout gridLayout = new GridLayout ();
		gridLayout.numColumns = 2;
		comp.setLayout (gridLayout);

		Label label0 = new Label (comp, SWT.NONE);
		label0.setText ("Location");
		GridData data = new GridData ();
		data.horizontalSpan = 2;
		data.grabExcessHorizontalSpace = true;
		label0.setLayoutData (data);
		locationField = new Text (comp, SWT.BORDER);
		locationField.addModifyListener(modifyListener);
		data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		locationField.setLayoutData (data);

		Label label2 = new Label (comp, SWT.NONE);
		label2.setText ("Base directory");
		data = new GridData ();
		data.horizontalSpan = 2;
		label2.setLayoutData (data);

		baseDirectoryField = new Text (comp, SWT.BORDER);
		data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		baseDirectoryField.setLayoutData (data);
		baseDirectoryField.addModifyListener(modifyListener);
		Label label4 = new Label (comp, SWT.NONE);
		label4.setText ("Program arguments");
		data = new GridData ();
		data.horizontalSpan = 2;
		label4.setLayoutData (data);

		programArgumentField = new Text (comp, SWT.BORDER | SWT.MULTI);
		data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		data.verticalSpan = 2;
		programArgumentField.setLayoutData (data);
		programArgumentField.addModifyListener(modifyListener);

		Label label6 = new Label (comp, SWT.NONE);
		label6.setText ("Interpreter");
		data = new GridData ();
		data.horizontalSpan = 2;
		label6.setLayoutData (data);


		interpreterField = new Combo (comp, SWT.DROP_DOWN);
		interpreterField.setItems (PydevDebugPlugin.getDefault().getInterpreters());
		interpreterField.select(0);
		data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		interpreterField .setLayoutData (data);
		interpreterField.addModifyListener(modifyListener);
	}


	
	private boolean isValid(String interpreter) {
		if (!InterpreterEditor.validateInterpreterPath(interpreter)) {
			setErrorMessage("Can't find Python interpreter '" + interpreter +"'"+
			"\nUse the Pydev/Debug preferences to specify additional editors.");
			return false;
		}
		return true;
	}
	public boolean isValid(ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		setMessage(null);
		String interpreter;
		try {
			interpreter = launchConfig.getAttribute(Constants.ATTR_INTERPRETER, "" );
		} catch (CoreException e) {
			setErrorMessage("No interpreter? " + e.getMessage());
			return false;
		}
		return isValid(interpreter);
	}

	private void setAttribute(ILaunchConfigurationWorkingCopy conf, String name, String value) {
		if (value.length() == 0)
			conf.setAttribute(name, (String)null);
		else
			conf.setAttribute(name, value);
	}
	
	public String getName() {
		return "Main";
	}

	public Image getImage() {
		if (tabIcon == null) {			
			ImageDescriptor desc;
			try {
				URL url = new URL(
						PydevDebugPlugin.getDefault().getDescriptor().getInstallURL(),
						Constants.MAIN_ICON);
				desc = ImageDescriptor.createFromURL(url);
			} catch (MalformedURLException e) {
				desc = ImageDescriptor.getMissingImageDescriptor();
				e.printStackTrace();
			}
			tabIcon = desc.createImage();
		}
		return tabIcon;
	}
	

	public void setDefaults(ILaunchConfigurationWorkingCopy arg0) {
		// As far as I know, this has never been called
		System.out.println("setDefaults");
	}

	/** The original AbstractLaunchConfigurationTab does
	 * refresh in reverse (updateMessage, then buttons)
	 * This does not work well (Message uses ol
	 * E3 fixes this problem
	 */	
	protected void updateLaunchConfigurationDialog() {
		if (getLaunchConfigurationDialog() != null) {
			getLaunchConfigurationDialog().updateButtons();
			getLaunchConfigurationDialog().updateMessage();
			getLaunchConfigurationDialog().updateButtons();
		}
	}

	/**
	 * Initializes widgets from ILaunchConfiguration
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration conf) {
		String location = "";
		String baseDirectory = "";
		String interpreter = "";
		String arguments = "";
		try {
			location = conf.getAttribute(Constants.ATTR_LOCATION, "");
			baseDirectory = conf.getAttribute(Constants.ATTR_WORKING_DIRECTORY, "");
			interpreter = conf.getAttribute(Constants.ATTR_INTERPRETER, "");
			arguments = conf.getAttribute(Constants.ATTR_PROGRAM_ARGUMENTS, "");
		}
		catch (CoreException e) {
		}
		locationField.setText(location);
		baseDirectoryField.setText(baseDirectory);
		programArgumentField.setText(arguments);
		String[] interpreters = interpreterField.getItems();
		int selectThis = -1;
		for (int i=0; i< interpreters.length; i++)
			if (interpreter.equals(interpreters[i]))
				selectThis = i;
		if (selectThis == -1) {
			PydevDebugPlugin.log(IStatus.INFO, "Obsolete interpreter selected", null);
			interpreterField.add(interpreter);
			interpreterField.select(interpreterField.getItemCount()-1);
		}
		else
			interpreterField.select(selectThis);
		setDirty(false);
	}
	
	public void performApply(ILaunchConfigurationWorkingCopy conf) {
		String value;
		value = locationField.getText().trim();
		setAttribute(conf, Constants.ATTR_LOCATION, value);
		value = baseDirectoryField.getText().trim();
		setAttribute(conf, Constants.ATTR_WORKING_DIRECTORY, value);
		value = programArgumentField.getText().trim();
		setAttribute(conf, Constants.ATTR_PROGRAM_ARGUMENTS, value);
		value = interpreterField.getText();
		setAttribute(conf, Constants.ATTR_INTERPRETER, value);
	}
	
}
