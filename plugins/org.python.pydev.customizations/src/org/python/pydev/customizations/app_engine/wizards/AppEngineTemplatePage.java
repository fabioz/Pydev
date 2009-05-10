package org.python.pydev.customizations.app_engine.wizards;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.python.pydev.core.log.Log;

/**
 * This page is used to configure templates for google app engine.
 */
public class AppEngineTemplatePage extends WizardPage{
    
    /**
     * Constant for forcing the user to choose a template
     */
    protected static final String CHOOSE_ONE = "-- Choose One --";

    /**
     * Constant for creating an empty project
     */
    protected static final String EMPTY_PROJECT = "Empty Project";
    
    /**
     * The names of the templates and the related description to be shown to the user
     */
    protected Map<String, String> templateNamesAndDescriptions;
    
    /**
     * Combo-box with the template names for the user to choose. 
     */
    protected Combo comboTemplateNames;
    
    /**
     * A label to show the description for the selected template
     */
    protected Label templateDescription;
    
    /**
     * A string with the last choice the user has done in the combo
     */
    protected String lastChoice="";
    
    protected AppEngineTemplatePage(String pageName) {
        super(pageName);
        setChooseOneErrorMessage();
    }

    private void setChooseOneErrorMessage(){
        setErrorMessage("Please select how to create the project");
    }

    public void createControl(Composite parent){
        Font font = parent.getFont();

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        composite.setFont(font);

        
        Label templateLabel = new Label(composite, SWT.NONE);
        templateLabel.setText("From which template do you want to create your new Google App Project?");
        templateLabel.setFont(font);
        
        comboTemplateNames = new Combo(composite, SWT.BORDER);
        comboTemplateNames.setFont(font);
        templateNamesAndDescriptions = new HashMap<String, String>();
        templateNamesAndDescriptions.put("Hello World", "Creates a simple project that just prints 'Hello World'");
        
        ArrayList<String> keys = new ArrayList<String>(templateNamesAndDescriptions.keySet());
        Collections.sort(keys);
        
        keys.add(0, EMPTY_PROJECT);
        keys.add(0, CHOOSE_ONE);
        comboTemplateNames.setItems(keys.toArray(new String[0]));
        comboTemplateNames.setText(CHOOSE_ONE);
        GridData data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        data.grabExcessHorizontalSpace = true;
        comboTemplateNames.setLayoutData(data);
        
        comboTemplateNames.addSelectionListener(new SelectionListener(){
        
            public void widgetSelected(SelectionEvent e){
                handleComboSelectionChanged();
            }
        
            public void widgetDefaultSelected(SelectionEvent e){
            }
        });
        
        templateDescription = new Label(composite, SWT.NONE);
        templateDescription.setText("");
        data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        data.grabExcessHorizontalSpace = true;
        templateDescription.setLayoutData(data);
        templateDescription.setFont(font);
        setControl(composite);
    }

    /**
     * When the selection changes, we update the last choice, description and the error message.
     */
    protected void handleComboSelectionChanged(){
        String current = comboTemplateNames.getText();
        lastChoice = current;
        String description = templateNamesAndDescriptions.get(current);
        templateDescription.setText(description!=null?description:"");
        if(current.equals(CHOOSE_ONE)){
            setChooseOneErrorMessage();
        }else{
            setErrorMessage(null);
        }
    }

    
    /**
     * Called so that the initial structure is filled, given the source folder to fill.
     */
    public void fillSourceFolder(IFolder sourceFolder){
        String text = lastChoice;
        if(text.equals(CHOOSE_ONE) || text.equals(EMPTY_PROJECT)){
            //Do nothing
        }else if(text.equals("Hello World")){
            IFile helloWorld = sourceFolder.getFile("helloworld.py");
            String contents = "" +
            		"print 'Content-Type: text/plain'\n" +
            		"print ''\n" +
            		"print 'Hello, world!'\n" +
            		"";
            try{
                helloWorld.create(new ByteArrayInputStream(contents.getBytes()), true, null);
            }catch(CoreException e){
                Log.log(e);
            }
            
            IFile yaml = sourceFolder.getFile("app.yaml");
            contents = "" +
            		"application: helloworld\n" +
            		"version: 1\n" +
            		"runtime: python\n" +
            		"api_version: 1\n" +
            		"\n" +
            		"handlers:\n" +
            		"- url: /.*\n" +
            		"  script: helloworld.py\n" +
            		"";
            try{
                yaml.create(new ByteArrayInputStream(contents.getBytes()), true, null);
            }catch(CoreException e){
                Log.log(e);
            }
            
        }
    }
    
    
    
    

}
