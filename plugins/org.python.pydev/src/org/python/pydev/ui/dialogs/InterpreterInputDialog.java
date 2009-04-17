/**
 * 
 */
package org.python.pydev.ui.dialogs;

import java.io.File;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.core.Tuple;
import org.python.pydev.ui.pythonpathconf.AbstractInterpreterEditor;

/**
 * @author raul
 *
 */
public class InterpreterInputDialog extends Dialog {

	/**
     * Button id for an "Browser" button (value 64).
     */
    public int BROWSER_ID = 64;
    
	/**
     * Button browser label
     */
    public String BROWSER_LABEL = "Brows&e...";
	
	/**
     * The title of the dialog.
     */
    protected String title;

    /**
     * The message to display, or <code>null</code> if none.
     */
    protected String message;
    
    /**
     * The interpreter name input value; the empty string by default.
     */
    protected String interpreterValue = "";
    
    /**
     * The interpreter executable input value; the empty string by default.
     */
    protected String interpreterExecutableValue = "";
    
    /**
     * Ok button widget.
     */
    protected Button okButton;
    
    /**
     * Browser button widget.
     */
    protected Button browserButton;

    /**
     * Input text widget.
     */
    protected Text interpreterNameField;
    
    /**
     * Input text widget.
     */
    protected Text interpreterExecutableField;
    
    /**
     * Error message label widget.
     */
    private Text errorMessageText;
    
    private AbstractInterpreterEditor editor;
    
    /**
     * Construtor.
     * @param shell the shell.
     * @param dialogTitle the title of the dialog.
     * @param dialogMessage the message of the dialog.
     */
	public InterpreterInputDialog(Shell shell, String dialogTitle, String dialogMessage, AbstractInterpreterEditor editor) {
		super(shell);		
		this.title = dialogTitle;		
        this.message = dialogMessage;
        this.editor = editor;
	}
	
	 /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        if (title != null)
            shell.setText(title);
    }
	
	 /*
     * (non-Javadoc) Method declared on Dialog.
     */
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.OK_ID) {
            interpreterValue = interpreterNameField.getText().trim();
            interpreterExecutableValue = interpreterExecutableField.getText().trim();
        } else {
            interpreterValue = null;
            interpreterExecutableValue = null;
        }
        super.buttonPressed(buttonId);
    }
   
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
     */
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        okButton = createButton(parent, IDialogConstants.OK_ID,
                IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);
        setErrorMessage("Please, inform the name and executable of your intepreter");
    }
    
    /*
     * (non-Javadoc) Method declared on Dialog.
     */
    protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		main.setLayout(layout);
		main.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        
        // create message
        if (message != null) {
        	Label messageLabel = new Label(main, SWT.WRAP);
			messageLabel.setText(message);
			data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
			data.horizontalSpan = 3;
			data.widthHint = 150;
			messageLabel.setLayoutData(data);
        }       
                
        Label separator = new Label(main,SWT.NONE);
        data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		data.horizontalSpan = 4;
        separator.setText("");
        separator.setLayoutData(data);
        
		createFields(main);    
		
		errorMessageText = new Text(main, SWT.READ_ONLY);
        errorMessageText.setBackground(errorMessageText.getDisplay()
                .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        errorMessageText.setForeground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_RED));
        data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		data.horizontalSpan = 3;
		data.widthHint = 150;
		errorMessageText.setLayoutData(data);

        applyDialogFont(main);
        
        return main;
    }

    /**
     * Override to create any extra content between the prompt and the password fields.
     * @param main
     */
    protected void createExtraContent(Composite main) {
		
	}

	/**
	 * Creates the three widgets that represent the password entry area. 
	 * @param parent  the parent of the widgets, which has two columns
	 */
	protected void createFields(Composite parent) {
		final Listener interpreterFieldsListener = new Listener() {
			public void handleEvent(Event event) {
			    
				String errorMessage = null;
				
				String interpreterName = interpreterNameField.getText().trim();
                if (interpreterName.equals("")){
					errorMessage = "The interpreter name must be specified";
				}
				
				String executableOrJar = interpreterExecutableField.getText().trim();
                if (errorMessage == null && executableOrJar.equals("")){
					errorMessage = "The interpreter location must be specified";
				}
				if(errorMessage == null){
    				File file = new File(executableOrJar);
    				if (!file.exists() || file.isDirectory()){
    					errorMessage = "Invalid interpreter";
    				}
				}
				if(errorMessage == null){
				    errorMessage = editor.getDuplicatedMessageError(interpreterName, executableOrJar);
				}
				setErrorMessage(errorMessage);
			}
		};
		
		new Label(parent, SWT.NONE).setText("Interpreter Name: "); //$NON-NLS-1$
		
		interpreterNameField = new Text(parent, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);		
		data.horizontalSpan = 3;
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
		interpreterNameField.setLayoutData(data);
		interpreterNameField.addListener(SWT.SELECTED, interpreterFieldsListener);
		interpreterNameField.addListener(SWT.KeyDown, interpreterFieldsListener);
		interpreterNameField.addListener(SWT.KeyUp, interpreterFieldsListener);
		
		new Label(parent, SWT.NONE).setText("Interpreter Executable: "); //$NON-NLS-1$
		
		interpreterExecutableField = new Text(parent, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);		
		data.horizontalSpan = 2;
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
		interpreterExecutableField.setLayoutData(data);
		interpreterExecutableField.addListener(SWT.SELECTED, interpreterFieldsListener);
		interpreterExecutableField.addListener(SWT.KeyDown, interpreterFieldsListener);
		interpreterExecutableField.addListener(SWT.KeyUp, interpreterFieldsListener);
		
		
		browserButton = createButton(parent, BROWSER_ID, BROWSER_LABEL, true);
		data = new GridData(GridData.FILL_HORIZONTAL);		
		data.horizontalSpan = 1;
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		browserButton.setLayoutData(data);
		browserButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent evt) {
				FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);

	            String[] filterExtensions = editor.getInterpreterFilterExtensions();
	            if (filterExtensions != null) {
	                dialog.setFilterExtensions(filterExtensions);
	            }

	            String file = dialog.open();
	            if (file != null){
	                file = file.trim();
	                interpreterExecutableField.setText(file);
	            }
                interpreterFieldsListener.handleEvent(null); //Make it update the error message
	            
			}
		});

	
	}	

    /**
     * Return the password inserted by the user.
     * @return the password inserted by the user or <code>null</code> if user canceled
     */
    public Tuple<String, String> getInterpreterNameAndExecutable() {
        if(interpreterValue == null || interpreterExecutableValue == null){
            return null;
        }
    	return new Tuple<String, String>(interpreterValue.trim(), interpreterExecutableValue);
    }
    
    /**
     * Sets or clears the error message.
     * If not <code>null</code>, the OK button is disabled.
     * 
     * @param errorMessage
     *            the error message, or <code>null</code> to clear
     */
    public void setErrorMessage(String errorMessage) {
        errorMessageText.setText(errorMessage == null ? "" : errorMessage); //$NON-NLS-1$
        okButton.setEnabled(errorMessage == null);
        errorMessageText.getParent().update();
    }
    
    
	
}