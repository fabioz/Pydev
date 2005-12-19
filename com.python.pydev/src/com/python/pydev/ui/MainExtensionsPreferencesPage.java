/*
 * License: Common Public License v1.0
 * Created on Sep 14, 2005
 * 
 * @author Fabio Zadrozny
 */
package com.python.pydev.ui;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.licensemanager.encryption.ClientEncryption;
import org.python.pydev.licensemanager.persistence.businessrules.LicenseDefaults;
import org.python.pydev.licensemanager.persistence.dao.ValueObject;
import org.python.pydev.licensemanager.rmi.ILicenseManager;

import com.python.pydev.PydevExtensionInitializer;
import com.python.pydev.PydevPlugin;
import com.python.pydev.properties.RMIProperties;
import com.python.pydev.util.EnvGetter;

public class MainExtensionsPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
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
    	addField(new StringFieldEditor(PydevExtensionInitializer.USER_NAME_VALIDATE_EXTENSION, "User name", composite));
    	addField(new StringFieldEditor(PydevExtensionInitializer.LICENSE_NUMBER_VALIDATE_EXTENSION, "License number", composite));
    	
    	Button btValidate = new Button(composite, SWT.PUSH);
    	btValidate.setText("Validate");
    	btValidate.setFont( composite.getFont() );
    	btValidate.addSelectionListener(new SelectionListener(){
			public void widgetSelected(SelectionEvent e) {				
				performApply();				
				doValidate();
			}
			private void doValidate() {
				String host = RMIProperties.getInstance().get( RMIProperties.IP ) + ":" + RMIProperties.getInstance().get( RMIProperties.PORT );;
				String name = "//" + host + "/" + "LicenseManager";
				
				String userName = getFieldValue(PydevExtensionInitializer.USER_NAME_VALIDATE_EXTENSION);
				String license = getFieldValue(PydevExtensionInitializer.LICENSE_NUMBER_VALIDATE_EXTENSION);
				if( !isValidFields( userName, license ) )
					return;
				
				ValueObject vo = new ValueObject();
				vo.put( LicenseDefaults.USER, ClientEncryption.getInstance().encrypt( userName ) );
				vo.put( LicenseDefaults.LICENSE_NUMBER, ClientEncryption.getInstance().encrypt( license ) );
				vo.put( LicenseDefaults.DATA, ClientEncryption.getInstance().encrypt( EnvGetter.getEnvVariables() ) );
				
				try {
					ILicenseManager licenseManager = (ILicenseManager)Naming.lookup( name );
					String data = licenseManager.validateLicense( vo );
					if( data==null ){
						setErrorMessage("Invalid license number");
					} else {					
						PydevPlugin.getDefault().saveLicense( data );
						PydevPlugin.getDefault().checkValid();
						setErrorMessage(null);
						MessageBox message = new MessageBox( getShell(), SWT.OK | SWT.ICON_INFORMATION );
						message.setText("Pydev extension");
						message.setMessage("License validated");									
						message.open();
					}					
				} catch (MalformedURLException e1) {					
					setErrorMessage("MalformedURLException");
					org.python.pydev.plugin.PydevPlugin.log( e1 );
					e1.printStackTrace();
				} catch (RemoteException e1) {					
					setErrorMessage("RemoteException");
					org.python.pydev.plugin.PydevPlugin.log( e1 );
					e1.printStackTrace();
				} catch (NotBoundException e1) {					
					setErrorMessage("NotBoundException");
					org.python.pydev.plugin.PydevPlugin.log( e1 );
					e1.printStackTrace();
				}
			}
			private boolean isValidFields(String userName, String license) {
				if( userName=="" ) {
					setErrorMessage( "User name is missing" );
					return false;
				} else if( license=="" ) {
					setErrorMessage( "License number is missing" );
					return false;
				}
				return true;
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}    		
    	});    	
    }    

    public void init(IWorkbench workbench) {
    	System.out.println("init of MainExtension");
    }
    
    private String getFieldValue( String id ) {
    	return getPreferenceStore().getString(id);
    }
    
    @Override
    public void dispose() {
    	getPreferenceStore().setValue(PydevExtensionInitializer.USER_NAME_VALIDATE_EXTENSION,"");
		getPreferenceStore().setValue(PydevExtensionInitializer.LICENSE_NUMBER_VALIDATE_EXTENSION,"");
    	super.dispose();   	
    }       
}
