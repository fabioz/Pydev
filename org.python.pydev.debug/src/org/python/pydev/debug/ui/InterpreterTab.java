/*
 * Author: fabioz
 * Author: ldore
 * Created: Feb 20, 2008
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.docutils.WordUtils;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.ui.launching.PythonRunnerConfig;
import org.python.pydev.plugin.PydevPlugin;

/**
 * The Python interpreter setup tab.
 *
 * The contents of this tab was formerly in the ArgumentsTab, until 
 * controls took too much space to fit.
 * 
 * As benefit of the split, this separates the program launch parameters 
 * from the Interpreter configuration. 
 * 
 * <p>Interesting functionality: InterpreterEditor will try the verify the choice of an interpreter.
 * <p>Getting the ModifyListener to display the proper error message was tricky
 */
public class InterpreterTab extends AbstractLaunchConfigurationTab {

    // Widgets
    Combo interpreterComboField;
    
    // View constant
    public static final String DEFAULT_INTERPRETER_NAME = "Default Interpreter";

    private IInterpreterManager interpreterManager;
    private Button buttonSeeResultingCommandLine;
    private ILaunchConfigurationWorkingCopy workingCopyForCommandLineGeneration;
    private Text text;

    private SelectionListener listener = new SelectionListener(){

        public void widgetSelected(SelectionEvent e) {
            if(e.getSource() == buttonSeeResultingCommandLine){
                try {
                    //ok, show the command-line to the user
                    ILaunchConfigurationDialog launchConfigurationDialog = getLaunchConfigurationDialog();
                    ILaunchConfigurationTab[] tabs = launchConfigurationDialog.getTabs();
                    for (int i = 0; i < tabs.length; i++) {
                        tabs[i].performApply(workingCopyForCommandLineGeneration);
                    }
                    PythonRunnerConfig config = getConfig(workingCopyForCommandLineGeneration, launchConfigurationDialog);
                    String commandLineAsString = config.getCommandLineAsString();
                    commandLineAsString = WordUtils.wrap(commandLineAsString, 80);
                    commandLineAsString += "\n\nThe PYTHONPATH that will be used is:\n\n";
                    commandLineAsString += config.pythonpathUsed;
                    text.setText(commandLineAsString);
                } catch (Exception e1) {
                    text.setText("Unable to make the command-line. \n\nReason:\n\n"+e1.getMessage());
                }
            }
        }


        public void widgetDefaultSelected(SelectionEvent e) {
        }
    };

    /**
     * @param conf the launch configuration to be used
     * @param launchConfigurationDialog the dialog for the launch configuration
     * @return a PythonRunnerConfig configured with the given launch configuration
     * @throws CoreException
     */
    private PythonRunnerConfig getConfig(ILaunchConfiguration conf, ILaunchConfigurationDialog launchConfigurationDialog) throws CoreException {
        String run;
        if(interpreterManager.isJython()){
            run = PythonRunnerConfig.RUN_JYTHON;
        }else if(interpreterManager.isPython()){
            run = PythonRunnerConfig.RUN_REGULAR;
        }else{
            throw new RuntimeException("Should be python or jython interpreter (found unknown).");
        }
        
        boolean makeArgumentsVariableSubstitution = false;
        //we don't want to make the arguments substitution (because it could end opening up a dialog for the user
        //requesting something).
        PythonRunnerConfig config = new PythonRunnerConfig(conf, launchConfigurationDialog.getMode(), run, makeArgumentsVariableSubstitution);
        return config;
    }
    
    public InterpreterTab(IInterpreterManager interpreterManager) {
        this.interpreterManager = interpreterManager;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        setControl(comp);
        GridLayout gridLayout = new GridLayout ();        
        comp.setLayout (gridLayout);
        
        GridData data = new GridData (GridData.FILL_HORIZONTAL);
        
        Label label6 = new Label (comp, SWT.NONE);
        label6.setText ("Interpreter:");
        data = new GridData (GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        label6.setLayoutData (data);

        interpreterComboField = new Combo (comp, SWT.DROP_DOWN);
        String[] interpreters = this.interpreterManager.getInterpreters();
        if (interpreters.length > 0){
        	// There is at least one interpreter defined, add the default interpreter option at the beginning.
            String[] interpreterNames = interpreters;
            interpreters = new String[interpreterNames.length+1];
            interpreters[0] = InterpreterTab.DEFAULT_INTERPRETER_NAME;
            
            for (int i = 0; i < interpreterNames.length; i ++){
            	interpreters[i+1] = interpreterNames[i];
            }
        }
        interpreterComboField.setItems (interpreters);
        interpreterComboField.select(0);
        data = new GridData ();
        data.horizontalAlignment = GridData.FILL;
        data.horizontalSpan = 2;
        interpreterComboField.setLayoutData (data);
        // interpreterComboField.addModifyListener(modifyListener);
        
        buttonSeeResultingCommandLine = new Button (comp, SWT.NONE);
        buttonSeeResultingCommandLine.setText ("See resulting command-line for the given parameters");
        data = new GridData ();
        data.horizontalSpan = 2;
        data.horizontalAlignment = GridData.FILL;
        buttonSeeResultingCommandLine.setLayoutData (data);
        buttonSeeResultingCommandLine.addSelectionListener(this.listener);
        
        text = new Text (comp, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        text.setText ("In case you are in doubt how will the run happen, click the button to \n" +
                      "see the command-line that will be executed with the current parameters\n" +
                      "(and the PYTHONPATH / CLASSPATH used for the run).");
        data = new GridData ();
        data.grabExcessVerticalSpace = true;
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.FILL;
        data.horizontalSpan = 1;
        data.verticalSpan = 5;
        text.setLayoutData (data);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
     */
    public boolean isValid(ILaunchConfiguration launchConfig) {
        setErrorMessage(null);
        setMessage(null);
        String interpreter;
        try {
            interpreter = launchConfig.getAttribute(Constants.ATTR_INTERPRETER, Constants.ATTR_INTERPRETER_DEFAULT);
        } catch (CoreException e) {
            setErrorMessage("No interpreter? " + e.getMessage());
            return false;
        }
        if(interpreter == null){
            return false;
        }
        
        try {
            return checkIfInterpreterExists(interpreter);
            
        } catch (Exception e) {
            //we might have problems getting the configured interpreters
            setMessage(e.getMessage());
            setErrorMessage(e.getMessage());
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    public String getName() {
        return "Interpreter";
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getImage()
     */
    public Image getImage() {
        return PydevPlugin.getImageCache().get(Constants.PYTHON_ORG_ICON);
    }
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void setDefaults(ILaunchConfigurationWorkingCopy arg0) {
        //no defaults to set
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    public void initializeFrom(ILaunchConfiguration configuration) {
        try {
            workingCopyForCommandLineGeneration = configuration.getWorkingCopy();
        } catch (CoreException e1) {
            throw new RuntimeException(e1);
        }
                
        String interpreter = "";
        try {
            interpreter = configuration.getAttribute(Constants.ATTR_INTERPRETER, Constants.ATTR_INTERPRETER_DEFAULT);
        }
        catch (CoreException e) {
        }

        String[] interpreters = interpreterComboField.getItems();
        
        if(interpreters.length == 0){
            setErrorMessage("No interpreter is configured, please, go to window > preferences > interpreters and add the interpreter you want to use.");
        }else{
        
            int selectThis = -1;
            
            if (interpreter.equals(Constants.ATTR_INTERPRETER_DEFAULT))
            {
            	selectThis = 0;
            }
            else {
            	for (int i=1; i< interpreters.length; i++){
            		if (interpreter.equals(interpreters[i])){
            			selectThis = i;
            			break;
            		}
            	}
            }
            
            if (selectThis == -1) {
            	if (interpreter.startsWith("${")) {
            		interpreterComboField.setText(interpreter);
            	}else{
            		setErrorMessage("Obsolete interpreter is selected. Choose a new one.");
            	}
            }else{
                interpreterComboField.select(selectThis);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        String value;        

        if (interpreterComboField.getSelectionIndex() == 0){
        	// The default was selected
        	value = Constants.ATTR_INTERPRETER_DEFAULT;
        	
        }else{
        	value = interpreterComboField.getText();
        }
        setAttribute(configuration, Constants.ATTR_INTERPRETER, value);
    }
    
    /**
     * @param interpreter the interpreter to validate
     * @return true if the interpreter is configured in pydev
     */
    protected boolean checkIfInterpreterExists(String interpreter) {
    	if (interpreter.equals(Constants.ATTR_INTERPRETER_DEFAULT))	{
    	    if(this.interpreterManager.getDefaultInterpreter() != null){
    			// The default interpreter is selected, and we have a default interpreter
	    		return true;
	    	}
	    	//otherwise, the default is selected, but we have no default
	    	return false;
    	}
    	
        String[] interpreters = this.interpreterManager.getInterpreters();
        for (int i = 0; i < interpreters.length; i++) {
            if (interpreters[i] != null && interpreters[i].equals(interpreter)) {
                return true;
            }
        }
        if(interpreter.startsWith("${")){
        	return true;
        }
        return false;
    }
    
    /**
     * sets attributes in the working copy
     */
    private void setAttribute(ILaunchConfigurationWorkingCopy conf, String name, String value) {
        if (value == null || value.length() == 0){
            conf.setAttribute(name, (String)null);
        }else{
            conf.setAttribute(name, value);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#setLaunchConfigurationDialog(org.eclipse.debug.ui.ILaunchConfigurationDialog)
     */
	public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
		super.setLaunchConfigurationDialog(dialog);
	}	    
}
