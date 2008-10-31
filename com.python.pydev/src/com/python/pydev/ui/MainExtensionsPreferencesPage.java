/*
 * License: Common Public License v1.0
 * Created on Sep 14, 2005
 * 
 * @author Fabio Zadrozny
 */
package com.python.pydev.ui;

import java.util.Calendar;

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
    private class ValidateButtonListener implements SelectionListener{
        
        public void widgetSelected(SelectionEvent e) {                
            performApply();                
            doValidate();
        }
        
        private void doValidate() {
            updateLicInfo();
            String license = getFieldValue(PydevExtensionInitializer.LICENSE).replaceAll("\n", "").replaceAll("\r", "").replaceAll(" ", "");
            String email = getFieldValue(PydevExtensionInitializer.USER_EMAIL);
            String licenseProvider = getFieldValue(PydevExtensionInitializer.LIC_PROVIDER);
            
            PydevPlugin.getDefault().saveLicense( email, license.trim(), licenseProvider );
            
            setErrorMessage(null);
            String txt = "Pydev extensions";
            String msg = "License validated";

            String validStr = PydevPlugin.getDefault().checkValidStr();
            if(!PydevPlugin.getDefault().isValidated()){
                msg = "License not validated.\nReason:\n\n"+validStr;
            }
            
            MessageBox message = new MessageBox( getShell(), SWT.OK | SWT.ICON_INFORMATION );
            message.setText(txt);
            message.setMessage(msg);                                    
            message.open();
            updateLicInfo();

            eMailFieldEditor.setStringValue(email);
            licenseFieldEditor.setStringValue(getFieldValue(PydevExtensionInitializer.LICENSE));
        }
        
        public void widgetDefaultSelected(SelectionEvent e) {
        }            
    }

    private Label labelUser;
    private Label labelExp;
    private Label labelType;
    private StringFieldEditor eMailFieldEditor;
    private MultiStringFieldEditor licenseFieldEditor;

    
    //--------------------------------------------------------------------------------------------------------
    public MainExtensionsPreferencesPage() {
        super(GRID);
        //Set the preference store for the preference page.
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());       
    }    

    
    @Override
    protected void createFieldEditors() {
        GridData data = null;
        Label label = null;
        
        Composite composite = getFieldEditorParent();
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new GridLayout());

//        Label label = new Label(composite, SWT.NONE);
//        String msg2 = "If you still haven't requested your license, press 'Request license' and follow the instructions.";
//        label.setText(WordUtils.wrap(msg2, 80));
//        data = new GridData(GridData.FILL_HORIZONTAL);
//        data.horizontalSpan = 2;
//        data.grabExcessHorizontalSpace = true;
//        label.setLayoutData(data);
//        
//        
//        Button btGetInfo = new Button(composite, SWT.PUSH);
//        btGetInfo.setText("Request license");
//        btGetInfo.setFont( composite.getFont() );
//        btGetInfo.addSelectionListener(new GetInfoButtonListener());        
//        data = new GridData(GridData.FILL_HORIZONTAL);
//        data.horizontalSpan = 2;
//        data.grabExcessHorizontalSpace = true;
//        btGetInfo.setLayoutData(data);

        String msg1 = "If you alredy received your license, please fill in your e-mail, paste the license you received and press 'Validate'.";
        label = new Label(composite, SWT.NONE);
        label.setText("\n\n"+WordUtils.wrap(msg1, 80));
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        data.grabExcessHorizontalSpace = true;
        label.setLayoutData(data);
        
        eMailFieldEditor = new StringFieldEditor(PydevExtensionInitializer.USER_EMAIL, "E-mail (or username):", composite);
        licenseFieldEditor = new MultiStringFieldEditor(PydevExtensionInitializer.LICENSE, "License Key:", composite);
        addField(eMailFieldEditor);
        addField(licenseFieldEditor);
        
        Button btValidate = new Button(composite, SWT.PUSH);
        btValidate.setText("Validate");
        btValidate.setFont( composite.getFont() );
        btValidate.addSelectionListener(new ValidateButtonListener());        
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        data.grabExcessHorizontalSpace = true;
        btValidate.setLayoutData(data);
        
        
        labelUser = new Label(composite, SWT.NONE);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        data.grabExcessHorizontalSpace = true;
        labelUser.setLayoutData(data);
        
        labelExp = new Label(composite, SWT.NONE);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        data.grabExcessHorizontalSpace = true;
        labelExp.setLayoutData(data);
        
        labelType = new Label(composite, SWT.NONE);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        data.grabExcessHorizontalSpace = true;
        labelType.setLayoutData(data);

        updateLicInfo();
    }


    /**
     * 
     */
    private void updateLicInfo() {
        try {
            String licenseProvider = getFieldValue(PydevExtensionInitializer.LIC_PROVIDER);
            String expirationTimeString = getFieldValue(PydevExtensionInitializer.LIC_TIME);
            
            Calendar expTime;
            if(PydevPlugin.isPydevLicenseProvider(licenseProvider)){
                //OK, it's a Pydev license, so, the expiration date must be calculated...
                expTime = PydevPlugin.getExpTime(expirationTimeString);
            }else{
                //Aptana license: it's OK, the expiration date is already correct
                expTime = Calendar.getInstance();
                expTime.setTimeInMillis(Long.parseLong(expirationTimeString));
            }
            
            String formattedDate = PydevPlugin.formatDate(expTime);
            labelExp.setText("Expires at: " + formattedDate);
        } catch (Exception e) {
            labelExp.setText("Expires at: ");
            
        }
        
        try{
            final String MULTI = "Multi Developer";

            String type = getFieldValue(PydevExtensionInitializer.LIC_TYPE);
            String devs = getFieldValue(PydevExtensionInitializer.LIC_DEVS);
            
            if(type.equals(MULTI)){
                labelType.setText("License: "+type+ " ("+devs+")");
            }else{
                labelType.setText("License: "+type);
            }
        } catch (Exception e) {
            labelType.setText("License: ");
        }
        labelUser.setText("Registered to: "+getFieldValue(PydevExtensionInitializer.USER_NAME));
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
