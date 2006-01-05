/*
 * License: Common Public License v1.0
 * Created on Sep 14, 2005
 * 
 * @author Fabio Zadrozny
 */
package com.python.pydev.ui;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.utils.MultiStringFieldEditor;

import com.python.pydev.PydevExtensionInitializer;
import com.python.pydev.PydevPlugin;



public class MainExtensionsPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	
	//--------------------------------------------------------------------------------------------------------
	private class GetInfoButtonListener implements SelectionListener{

		public void widgetSelected(SelectionEvent e) {
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
			if( !isValidFields( userName, license ) )
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

		private boolean isValidFields(String userName, String license) {
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
    	IPreferenceStore store =
    		PydevPlugin.getDefault().getPreferenceStore();
    	setPreferenceStore(store);   	
    }    

	
	@Override
    protected void createFieldEditors() {
    	Composite composite = getFieldEditorParent();
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new GridLayout());

    	addField(new StringFieldEditor(PydevExtensionInitializer.USER_NAME_VALIDATE_EXTENSION, "User e-mail:", composite));
    	addField(new MultiStringFieldEditor(PydevExtensionInitializer.LICENSE_NUMBER_VALIDATE_EXTENSION, "License:", composite));
    	
    	Button btValidate = new Button(composite, SWT.PUSH);
    	btValidate.setText("Validate");
    	btValidate.setFont( composite.getFont() );
    	btValidate.addSelectionListener(new ValidateButtonListener());    	
    	
    	Button btGetInfo = new Button(composite, SWT.PUSH);
    	btGetInfo.setText("Get info to request license.");
    	btGetInfo.setFont( composite.getFont() );
    	btGetInfo.addSelectionListener(new GetInfoButtonListener());    	
    }    

    public void init(IWorkbench workbench) {
    }
    
    private String getFieldValue( String id ) {
    	return getPreferenceStore().getString(id);
    }
    
}
