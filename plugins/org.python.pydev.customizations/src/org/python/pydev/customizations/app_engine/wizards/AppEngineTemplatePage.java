package org.python.pydev.customizations.app_engine.wizards;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
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
import org.python.pydev.core.ICallback;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.customizations.CustomizationsPlugin;

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
    protected Map<String, Tuple<String, File>> templateNamesAndDescriptions;
    
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
        templateNamesAndDescriptions = new HashMap<String, Tuple<String, File>>();
        
        
        try{
            loadTemplates();
        }catch(CoreException e1){
            Log.log(e1);
        }
        
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
                handleComboSelectionChanged(comboTemplateNames.getText());
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
     * Loads the templates from the filesystem.
     */
    private void loadTemplates() throws CoreException{
        File relativePath = CustomizationsPlugin.getBundleInfo().getRelativePath(new Path("templates/google_app_engine"));
        File[] files = relativePath.listFiles();
        for(File dir:files){
            if(dir.isDirectory()){
                File[] secondLevelFiles = dir.listFiles();
                for(File file2:secondLevelFiles){
                    if(file2.getName().equals("description.txt")){
                        String fileContents = REF.getFileContents(file2).trim();
                        Tuple<String, String> nameAndDesc = StringUtils.splitOnFirst(fileContents, ':');
                        templateNamesAndDescriptions.put(nameAndDesc.o1, new Tuple<String, File>(nameAndDesc.o2, dir));
                        break;
                    }
                }
            }
        }
    }

    /**
     * When the selection changes, we update the last choice, description and the error message.
     */
    protected void handleComboSelectionChanged(String text){
        lastChoice = text;
        Tuple<String, File> description = templateNamesAndDescriptions.get(lastChoice);
        templateDescription.setText(description!=null?description.o1:"");
        if(lastChoice.equals(CHOOSE_ONE)){
            setChooseOneErrorMessage();
        }else{
            setErrorMessage(null);
        }
    }

    
    /**
     * Called so that the initial structure is filled, given the source folder to fill.
     */
    public void fillSourceFolder(IFolder sourceFolder){
        if(lastChoice == null || lastChoice.equals(CHOOSE_ONE) || lastChoice.equals(EMPTY_PROJECT)){
            //Do nothing
        }else{
            Tuple<String, File> tuple = templateNamesAndDescriptions.get(lastChoice);
            if(tuple != null && tuple.o2.isDirectory()){
                try{
                    //copy all but the description.txt file.
                    REF.copyDirectory(tuple.o2, sourceFolder.getLocation().toFile(), new ICallback<Boolean, File>(){
                    
                        public Boolean call(File arg){
                            if(arg.getName().equals("description.txt")){
                                return true;
                            }
                            return false;
                        }
                    });
                }catch(IOException e){
                    Log.log(e);
                }
                try{
                    sourceFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
                }catch(CoreException e){
                    Log.log(e);
                }
            }
        }
    }
    
    
    
    

}
