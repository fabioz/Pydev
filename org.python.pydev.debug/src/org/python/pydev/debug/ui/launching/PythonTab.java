/*
 * Author: wrwright
 * Created on Feb 7, 2004
 * License: Common Public License v1.0
 */ 
package org.python.pydev.debug.ui.launching;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.externaltools.internal.model.*;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
/**
 * @author wwright
 *
 * Configuration data for python launch.
 */
public class PythonTab implements ILaunchConfigurationTab {

	protected static final String interpreterAttribute = IExternalToolConstants.ATTR_LOCATION;
	protected static final String workingDirectoryAttribute = IExternalToolConstants.ATTR_WORKING_DIRECTORY;
	protected static final String argumentsAttribute = "python.arguments";
	protected static final String mainScriptAttribute = "python.mainScript";
	protected static final String stopInMainAttribute = "python.stopInMain";

    private String interpreter = "";
	private String mainScript = "";
	private String workingDir = "";
	private String arguments = "";
	private Text fMainText;
	private Text fWorkingDirText;
	private Text fInterpreterText;
	private Text fArgumentsText;
	private ILaunchConfigurationDialog myLaunchDialog;
	private Image tabImage;
    
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Button fStopInMainCheckButton;
		Button fSearchExternalJarsCheckButton;
		Button fSearchButton;
		Label fMainLabel;
		Button fInterpreterButton;
		Label fInterpreterLabel;
		
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		WorkbenchHelp.setHelp(getControl(), "SomeContext");
		GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);		
		GridData gd;
				
		// ---------  Interpreter  -------------
		String buttonText = "Browse...";
		String labelText = "Interpreter";
		SelectionAdapter listener = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent evt) {
			handleInterpreterButtonSelected();
		}};
		
		fInterpreterText = makeEntryField(comp, buttonText, labelText, listener);


		// ---------  Main Script  -------------
		buttonText = "Browse...";
		labelText = "Main Script";
		listener = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent evt) {
			handleMainButtonSelected();
		}};
		fMainText = makeEntryField(comp, buttonText, labelText, listener);

		// ---------  Arguments  -------------
		labelText = "Program Arguments";
		fArgumentsText = makeEntryField(comp, null, labelText, null);

		// ---------  Working Directory  -------------
		buttonText = "Browse...";
		labelText = "Working Directory";
		listener = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent evt) {
			handleWorkingDirButtonSelected();
		}};
		fWorkingDirText = makeEntryField(comp, buttonText, labelText, listener);


		fStopInMainCheckButton = new Button(comp, SWT.CHECK);
		fStopInMainCheckButton.setText("Stop in Main Script"); //$NON-NLS-1$
		fStopInMainCheckButton.setFont(comp.getParent().getFont());
		fStopInMainCheckButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});		
		

	}

	private Text makeEntryField(
		Composite parent,
		String buttonText,
		String labelText,
		SelectionAdapter listener) {

		Button fSearchExternalJarsCheckButton;
		Button fSearchButton;
		Label fMainLabel;
		GridData gd;
		createVerticalSpacer(parent);
		
		Composite mainComp = new Composite(parent, SWT.NONE);
		GridLayout mainLayout = new GridLayout();
		mainLayout.numColumns = 2;
		mainLayout.marginHeight = 0;
		mainLayout.marginWidth = 0;
		mainComp.setLayout(mainLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		
		mainComp.setLayoutData(gd);
		mainComp.setFont(parent.getParent().getFont());
		
		fMainLabel = new Label(mainComp, SWT.NONE);
		fMainLabel.setText(labelText);
		gd = new GridData();
		gd.horizontalSpan = 2;
		fMainLabel.setLayoutData(gd);
		fMainLabel.setFont(parent.getParent().getFont());
		
		Text retText = new Text(mainComp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL|GridData.HORIZONTAL_ALIGN_BEGINNING);
		retText.setLayoutData(gd);
		retText.setFont(parent.getParent().getFont());
		retText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		
		if (buttonText != null) {
  		  fSearchButton = createPushButton(mainComp,buttonText, null);
		  fSearchButton.addSelectionListener(listener);
		}

        return retText;
	}
	/**
	 * Raise a file dialog to look for an interpreter
	 */
	private void handleInterpreterButtonSelected() {
		FileDialog dialog = new FileDialog(getControl().getShell());
		String interp = dialog.open();
		if (interp != null)
		  fInterpreterText.setText(interp);
		
	}
	/**
	 * Raise a file dialog to look for the main script
	 */
	private void handleMainButtonSelected() {
		FileDialog dialog = new FileDialog(getControl().getShell());
		String [] exts = {"*.py"}; 
		
		dialog.setFilterExtensions(exts);
		String main = dialog.open();
		if (main != null)
		  fMainText.setText(main);
	}
	
	/**
	 * Raise a directory dialog to look for a CWD to run in
	 */
	private void handleWorkingDirButtonSelected() {
		DirectoryDialog dialog = new DirectoryDialog(getControl().getShell());
		String dir = dialog.open();
		if (dir != null)
		  fWorkingDirText.setText(dir);
		
		
	}
	/**
	 * sync the instance data with the widgets
	 */
	private void updateLaunchConfigurationDialog() {
		interpreter = fInterpreterText.getText();
		mainScript = fMainText.getText();
		workingDir = fWorkingDirText.getText();
		arguments = fArgumentsText.getText();
		validCheck(interpreter, mainScript, workingDir);
		myLaunchDialog.updateButtons();
		myLaunchDialog.updateMessage();
	}
	/**
	 * Create some empty space 
	 */
	private Label createVerticalSpacer(Composite comp) {
		return new Label(comp, SWT.NONE);
	}

    Control myControl;
	/**
	 * @param comp
	 */
	private void setControl(Composite comp) {
		myControl = comp;
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getControl()
	 */
	public Control getControl() {
		
		return myControl;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		try {
			interpreter = configuration.getAttribute(interpreterAttribute, interpreter);
			mainScript = configuration.getAttribute(mainScriptAttribute, mainScript);
			workingDir = configuration.getAttribute(workingDirectoryAttribute, workingDir);
			arguments = configuration.getAttribute(argumentsAttribute, arguments);
			configuration.getAttribute(stopInMainAttribute, false);
		} catch (CoreException e) {
			
			e.printStackTrace();
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			fInterpreterText.setText(configuration.getAttribute(interpreterAttribute, interpreter));
			fMainText.setText(configuration.getAttribute(mainScriptAttribute, mainScript));
			fWorkingDirText.setText(configuration.getAttribute(workingDirectoryAttribute, workingDir));
			fArgumentsText.setText(configuration.getAttribute(argumentsAttribute, arguments));
			configuration.getAttribute(stopInMainAttribute, false);
		} catch (CoreException e) {
			
			e.printStackTrace();
		}

	}
	protected Button createPushButton(Composite parent, String label, FontMetrics fontMetrics) {
		Button button = new Button(parent, SWT.PUSH);
		button.setFont(parent.getFont());
		button.setText(label);
		GridData gd= getButtonGridData(button, fontMetrics);
		button.setLayoutData(gd);
		return button;	
	}
	
	private GridData getButtonGridData(Button button, FontMetrics fontMetrics) {
		GridData gd= new GridData( GridData.VERTICAL_ALIGN_BEGINNING);

		int widthHint= 0;//Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.BUTTON_WIDTH);
		gd.widthHint= Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
	
		gd.heightHint= button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).y;//Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.BUTTON_HEIGHT);
		return gd;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
		if (tabImage != null) {
		  tabImage.dispose();
		  tabImage = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(interpreterAttribute, interpreter);
		configuration.setAttribute(mainScriptAttribute, mainScript);
		configuration.setAttribute(workingDirectoryAttribute, workingDir);
		configuration.setAttribute(argumentsAttribute, arguments);
		configuration.setAttribute(stopInMainAttribute, false);
		
		// real arguments are main script name + arguments
		configuration.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, "-u " + mainScript + " " + arguments);
	}

    private String error_msg;
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getErrorMessage()
	 */
	public String getErrorMessage() {
		return error_msg;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getMessage()
	 */
	public String getMessage() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration launchConfig) {
		boolean ret = false;

		try {
			String interpName = launchConfig.getAttribute(interpreterAttribute, "");
			String scriptName = launchConfig.getAttribute(mainScriptAttribute, "");
			String workingDirName = launchConfig.getAttribute(workingDirectoryAttribute, "");
			ret = validCheck(interpName, scriptName, workingDirName);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	/*
	 *  Check for invalid entry field data and set the error_msg string accordingly.
	 */
	private boolean validCheck(String interpName, String scriptName, String workingDirName) {
		boolean ret;
		error_msg = null;
		File executable = new File(interpName);
		File script = new File(scriptName);
		File dir = new File(workingDirName);
		ret = executable.exists() && script.exists() && dir.isDirectory();
		if (!executable.exists()) {
			error_msg = "Interpreter '"+interpName+"' does not exist.";
		} else if(!script.exists()) {
			error_msg = "Main script '"+scriptName+"' does not exist.";
  	    } else if(!dir.isDirectory()) {
		    error_msg = "Working directory '"+workingDirName+"' does not exist.";
   	    }
		return ret;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#canSave()
	 */
	public boolean canSave() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setLaunchConfigurationDialog(org.eclipse.debug.ui.ILaunchConfigurationDialog)
	 */
	public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
		myLaunchDialog = dialog;

	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#launched(org.eclipse.debug.core.ILaunch)
	 */
	public void launched(ILaunch launch) {
		// TODO: do I care that this was launched?

	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return "Python";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		if (tabImage == null) {

			ImageDescriptor desc;
			try {
				URL url =
					new URL(
						PydevDebugPlugin
							.getDefault()
							.getDescriptor()
							.getInstallURL(),
						Constants.MAIN_ICON);
				desc = ImageDescriptor.createFromURL(url);
			} catch (MalformedURLException e) {
				desc = ImageDescriptor.getMissingImageDescriptor();
				e.printStackTrace();
			}
			tabImage = desc.createImage();
		}
		return tabImage;
	}
}


