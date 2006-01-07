/*
 * License: Common Public License v1.0
 * Created on Sep 14, 2005
 * 
 * @author Fabio Zadrozny
 */
package com.python.pydev.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.docutils.WordUtils;
import org.python.pydev.utils.MultiStringFieldEditor;

import com.python.pydev.PydevExtensionInitializer;
import com.python.pydev.PydevPlugin;



public class MainExtensionsPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	
	//--------------------------------------------------------------------------------------------------------
	private class GetInfoButtonListener implements SelectionListener{

		public void widgetSelected(SelectionEvent e) {
            //ok, let's ask the user about its info and send it to the e-mail
            Dialog dialog = new GetInfoDialog(getShell());
            dialog.setBlockOnOpen(true);
            int ret = dialog.open();
            System.out.println(ret);
		}

		public void widgetDefaultSelected(SelectionEvent e) {
		}
		
	}

	
	
	
	//--------------------------------------------------------------------------------------------------------
	private class ValidateButtonListener implements SelectionListener{
		
		public void widgetSelected(SelectionEvent e) {				
			performApply();				
			doValidate();
		}
		
		private void doValidate() {
			String userName = getFieldValue(PydevExtensionInitializer.USER_NAME_VALIDATE_EXTENSION);
			String license = getFieldValue(PydevExtensionInitializer.LICENSE_NUMBER_VALIDATE_EXTENSION);
			if( !fieldsValid( userName, license ) )
				return;
			
			PydevPlugin.getDefault().saveLicense( license.trim() );
			setErrorMessage(null);
			String txt = "Pydev extension";
			String msg = "License validated";

			if(!PydevPlugin.getDefault().checkValid()){
				msg = "License not validated";
			}
			
			MessageBox message = new MessageBox( getShell(), SWT.OK | SWT.ICON_INFORMATION );
			message.setText(txt);
			message.setMessage(msg);									
			message.open();
		}

		private boolean fieldsValid(String userName, String license) {
			if( userName == null || userName.trim()=="" ) {
				setErrorMessage( "User e-mail is missing" );
				return false;
				
			} else if( license == null || license.trim()=="" ) {
				setErrorMessage( "License number is missing" );
				return false;
				
			}
			return true;
		}
		
		public void widgetDefaultSelected(SelectionEvent e) {
		}    		
	}

	
	//--------------------------------------------------------------------------------------------------------
	public MainExtensionsPreferencesPage() {
        super(GRID);
        //Set the preference store for the preference page.
    	setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());   	
    }    

	
	@Override
    protected void createFieldEditors() {
	    GridData data = null;
        
    	Composite composite = getFieldEditorParent();
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new GridLayout());

        Label label = new Label(composite, SWT.NONE);
        setLabelBold(composite, label);
        String msg1 = "If you alredy received your license, please fill in your e-mail, paste the license you received and press 'Validate'.";
        String msg2 = "If you still haven't requested your license, press 'Request license' and follow the instructions.";
        label.setText(WordUtils.wrap(msg1, 80)+"\n\n"+WordUtils.wrap(msg2, 80));
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        data.grabExcessHorizontalSpace = true;
        label.setLayoutData(data);
        
    	addField(new StringFieldEditor(PydevExtensionInitializer.USER_NAME_VALIDATE_EXTENSION, "User e-mail:", composite));
    	addField(new MultiStringFieldEditor(PydevExtensionInitializer.LICENSE_NUMBER_VALIDATE_EXTENSION, "License:", composite));
    	
    	Button btValidate = new Button(composite, SWT.PUSH);
    	btValidate.setText("Validate");
    	btValidate.setFont( composite.getFont() );
    	btValidate.addSelectionListener(new ValidateButtonListener());    	
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        data.grabExcessHorizontalSpace = true;
        btValidate.setLayoutData(data);
    	
    	Button btGetInfo = new Button(composite, SWT.PUSH);
    	btGetInfo.setText("Request license");
    	btGetInfo.setFont( composite.getFont() );
    	btGetInfo.addSelectionListener(new GetInfoButtonListener());    	
    	data = new GridData(GridData.FILL_HORIZONTAL);
    	data.horizontalSpan = 2;
    	data.grabExcessHorizontalSpace = true;
        btGetInfo.setLayoutData(data);
    }


    /**
     * @param composite
     * @param label
     */
    public static void setLabelBold(Composite composite, Label label) {
        try {
            FontData labelFontData = new FontData("Arial", 10, SWT.BOLD);
            label.setFont(new Font(composite.getDisplay(), labelFontData));
        } catch (Throwable e) {
            //ignore
        }
    }    

    public void init(IWorkbench workbench) {
    }
    
    private String getFieldValue( String id ) {
    	return getPreferenceStore().getString(id);
    }
    
}
