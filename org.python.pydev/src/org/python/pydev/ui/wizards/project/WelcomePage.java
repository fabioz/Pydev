package org.python.pydev.ui.wizards.project;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * The first page in Project generation wizard
 * 
 * Checks that Python interpreter is configured
 */
public class WelcomePage extends WizardPage {

    
    Composite mainContainer;
    Composite mainControl;
                    
    public WelcomePage(String pageName) {
        super(pageName);
    }
    
    public WelcomePage(String pageName, String title,
            ImageDescriptor titleImage) {
        super(pageName, title, titleImage); 
    }
    
    public void createControl(Composite parent) {
    	            
        setDescription("Python project creation requirements");
    	
        mainContainer=new Composite(parent, SWT.NONE);
        GridData data=new GridData(GridData.FILL_BOTH);
        mainContainer.setLayoutData(data);
        GridLayout layout=new GridLayout(1, false);
        mainContainer.setLayout(layout);
        setControl(mainContainer);
                                        
        mainControl=getDefaultMain(mainContainer);
        
        // Never finish this page because there is no interpreter
        setPageComplete(false);   
    }
    
    protected Composite getDefaultMain(Composite parent)
    {
        Composite panel=new Composite(parent, SWT.NONE);
        GridLayout layout=new GridLayout(1, false);
        panel.setLayout(layout);
        
        Text interpreterNoteText = new Text(panel, SWT.MULTI);
        interpreterNoteText.setEditable(false);
        interpreterNoteText.setText("Please configure a Python interpreter in Window -> Preferences -> PyDev \nbefore creating a new Python project");
                                            
        setErrorMessage(null);        
        return panel;        
    }    
    
}