/*
 * Author: atotic
 * Author: fabioz
 * Created: Aug 20, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui;

import java.io.File;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
 * The main Python debug setup tab.
 * 
 * <p>Interesting functionality: InterpreterEditor will try the verify the choice of an interpreter.
 * <p>Getting the ModifyListener to display the proper error message was tricky
 */
public class ArgumentsTab extends AbstractLaunchConfigurationTab {

    // Widgets
    Text baseDirectoryField;
    Text programArgumentField;
    Combo interpreterComboField;
    Text vmArgumentsField;
    
    // View constant
    static final String DEFAULT_INTERPRETER_NAME = "Default Interpreter";

    protected ModifyListener modifyListener = new ModifyListener() {
        public void modifyText(ModifyEvent e) {       
            if(e.getSource() == baseDirectoryField){
            	
                File file = new File(baseDirectoryField.getText());
                if(!file.exists()){
                    setErrorMessage("The directory in the Base Directory does not exist.");
                }
                if(!file.isDirectory()){
                    setErrorMessage("The directory in the location is not actually a directory.");
                }
                
                
            }else if(e.getSource() == programArgumentField){
                
            }
            updateLaunchConfigurationDialog();
        }
    };    
    
    private IInterpreterManager interpreterManager;
    private Button button;
    private ILaunchConfigurationWorkingCopy workingCopyForCommandLineGeneration;
    private Text text;

    private SelectionListener listener = new SelectionListener(){

        public void widgetSelected(SelectionEvent e) {
            if(e.getSource() == button){
                try {
                    //ok, show the command-line to the user
                    ILaunchConfigurationDialog launchConfigurationDialog = getLaunchConfigurationDialog();
                    ILaunchConfigurationTab[] tabs = launchConfigurationDialog.getTabs();
                    for (int i = 0; i < tabs.length; i++) {
                        tabs[i].performApply(workingCopyForCommandLineGeneration);
                    }
                    String run;
                    if(interpreterManager.isJython()){
                        run = PythonRunnerConfig.RUN_JYTHON;
                    }else if(interpreterManager.isPython()){
                        run = PythonRunnerConfig.RUN_REGULAR;
                    }else{
                        throw new RuntimeException("Should be python or jython interpreter (found unknown).");
                    }
                    
                    PythonRunnerConfig config = new PythonRunnerConfig(workingCopyForCommandLineGeneration, launchConfigurationDialog.getMode(), run);
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
        }};
    

    public ArgumentsTab(IInterpreterManager interpreterManager) {
        this.interpreterManager = interpreterManager;
    }


    public void createControl(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        setControl(comp);
        GridLayout gridLayout = new GridLayout ();        
        comp.setLayout (gridLayout);
        
        GridData data = new GridData (GridData.FILL_HORIZONTAL);
         
        Label label2 = new Label (comp, SWT.NONE);
        label2.setText ("Base directory:");
        label2.setLayoutData (data);

        baseDirectoryField = new Text (comp, SWT.BORDER);
        data = new GridData ();
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.FILL;
        data.horizontalSpan = 1;
        data.verticalSpan = 2;
        baseDirectoryField.setLayoutData (data);
        baseDirectoryField.addModifyListener(modifyListener);
        baseDirectoryField.setLayoutData(data);

        Label label4 = new Label (comp, SWT.NONE);
        data = new GridData (GridData.FILL_HORIZONTAL);
        label4.setText ("Program Arguments:");
        label4.setLayoutData (data);

        programArgumentField = new Text (comp, SWT.BORDER | SWT.MULTI);
        data = new GridData ();
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.FILL;
        data.horizontalSpan = 1;
        data.verticalSpan = 2;
        programArgumentField.setLayoutData (data);
        programArgumentField.addModifyListener(modifyListener);

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
            interpreters[0] = ArgumentsTab.DEFAULT_INTERPRETER_NAME;
            
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
        interpreterComboField.addModifyListener(modifyListener);
        
    	//label
    	Label l3 = new Label(comp,SWT.None);
    	l3.setText("VM arguments (for python.exe or java.exe): ");
    	data = new GridData(GridData.FILL_HORIZONTAL);
    	data.horizontalSpan = 2;
    	l3.setLayoutData(data);
    	
    	//Text of the Arguments        
    	vmArgumentsField = new Text(comp, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
    	data = new GridData();
    	data.grabExcessHorizontalSpace = true;
    	data.grabExcessVerticalSpace = true;
        data.horizontalAlignment = SWT.FILL;
        data.verticalAlignment = SWT.FILL;
        data.horizontalSpan = 2;
        data.verticalSpan = 5;
        vmArgumentsField.setLayoutData(data);
        vmArgumentsField.addModifyListener(modifyListener);

        button = new Button (comp, SWT.NONE);
        button.setText ("See resulting command-line for the given parameters");
        data = new GridData ();
        data.horizontalSpan = 2;
        data.horizontalAlignment = GridData.FILL;
        button.setLayoutData (data);
        button.addSelectionListener(this.listener);
        
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


    
    /**
     * checks if some launch is valid 
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
     */
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

    /**
     * this is the name that will appear to the user 
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    public String getName() {
        return "Arguments";
    }

    /**
     * this is the image that will appear to the user 
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
     */
    public Image getImage() {
        return PydevPlugin.getImageCache().get(Constants.MAIN_ICON);
    }
    
    public void setDefaults(ILaunchConfigurationWorkingCopy arg0) {
        //no defaults to set
    }

    /**
     * Initializes widgets from ILaunchConfiguration
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    public void initializeFrom(ILaunchConfiguration conf) {
        try {
            workingCopyForCommandLineGeneration = conf.getWorkingCopy();
        } catch (CoreException e1) {
            throw new RuntimeException(e1);
        }
                
        String baseDirectory = "";
        String interpreter = "";
        String arguments = "";
        String vmArguments = "";
        try {
            baseDirectory = conf.getAttribute(Constants.ATTR_WORKING_DIRECTORY, "");
            interpreter = conf.getAttribute(Constants.ATTR_INTERPRETER, Constants.ATTR_INTERPRETER_DEFAULT);
            vmArguments = conf.getAttribute(Constants.ATTR_VM_ARGUMENTS,"");
            arguments = conf.getAttribute(Constants.ATTR_PROGRAM_ARGUMENTS, "");
        }
        catch (CoreException e) {
        }
        vmArgumentsField.setText(vmArguments);
        baseDirectoryField.setText(baseDirectory);
        programArgumentField.setText(arguments);
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
    
    /**
     * save the values 
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void performApply(ILaunchConfigurationWorkingCopy conf) {
        String value;        

        value = baseDirectoryField.getText().trim();
        setAttribute(conf, Constants.ATTR_WORKING_DIRECTORY, value);
        
        value = programArgumentField.getText().trim();
        setAttribute(conf, Constants.ATTR_PROGRAM_ARGUMENTS, value);
        
        value = vmArgumentsField.getText().trim();
        setAttribute(conf, Constants.ATTR_VM_ARGUMENTS, value);
        
        if (interpreterComboField.getSelectionIndex() == 0){
        	// The default was selected
        	value = Constants.ATTR_INTERPRETER_DEFAULT;
        	
        }else{
        	value = interpreterComboField.getText();
        }
        setAttribute(conf, Constants.ATTR_INTERPRETER, value);
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
        
}
