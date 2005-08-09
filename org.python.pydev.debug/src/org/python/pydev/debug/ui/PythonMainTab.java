/*
 * Author: atotic
 * Created: Aug 20, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui;

import java.io.IOException;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;
import org.python.pydev.ui.StreamConsumer;

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
		interpreterField.setItems (PydevPrefs.getInterpreters());
		interpreterField.select(0);
		data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		interpreterField .setLayoutData (data);
		interpreterField.addModifyListener(modifyListener);
	}

	
	
	/**
	 * returns true if interpreter was launched successfully
	 */
	static String cachedExecutable = null;
	static boolean cachedExecutableValid = false;
	public static boolean validateInterpreterPath(String executable) throws Exception{
		// we cache the last query because this gets called a lot
		// i do not want to launch
	    // may throw exception with the error received...
		if (cachedExecutable != null && cachedExecutable.equals(executable) && cachedExecutableValid == true)
			return cachedExecutableValid;
		
		try {
			String versionOption = " -V";
			
			String complete = executable + versionOption;
            Process pr = Runtime.getRuntime().exec(complete);
			StreamConsumer outputs = new StreamConsumer(pr.getInputStream());
			outputs.start();
			StreamConsumer errors = new StreamConsumer(pr.getErrorStream());
			errors.start();
			pr.waitFor();
			
			int ret = pr.exitValue();
			if (ret == 0){
				if(errorsInOutput(executable, outputs, errors)){
				    throw new Exception("Unable to find interpreter: "+executable);
				}
			}else{
			    throw new Exception("Unable to execute: "+complete+" (returned value "+ret+" when executed).");
			}
			
		} catch (InterruptedException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		}
		
		//only cache if suceeded (otherwise, exception has already been thrown)...
		cachedExecutable = executable;
		cachedExecutableValid = true;
		return true;
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
	
	private boolean isValid(String interpreter) {
	    boolean b;
		try {
            b = validateInterpreterPath(interpreter);
        } catch (Exception e) {
			setErrorMessage("Python interpreter '" + interpreter +"'not valid.\n" +
					"Additional interpreters may be set in PyDev/Python Interpreters.\n" +
					"Error received: "+e.getMessage());
			return false;
        }
		return b;
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
						PydevPlugin.getDefault().getBundle().getEntry("/"),
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
//		System.out.println("setDefaults");
	}

	/** The original AbstractLaunchConfigurationTab does
	 * refresh in reverse (updateMessage, then buttons)
	 * This does not work well (Message uses ol
	 */	
	protected void updateLaunchConfigurationDialog() {
		if (getLaunchConfigurationDialog() != null) {
			getLaunchConfigurationDialog().updateButtons();
			getLaunchConfigurationDialog().updateMessage();
//			getLaunchConfigurationDialog().updateButtons();
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
