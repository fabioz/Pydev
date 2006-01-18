/*
 * Created on Jan 17, 2006
 */
package org.python.pydev.ui.wizards.files;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;


/**
 * The default creation page may be found at org.eclipse.ui.dialogs.WizardNewFileCreationPage
 */
public class PythonAbstractPathPage extends WizardPage{

    private IStructuredSelection selection;
    private Text textSourceFolder;
    private Button btBrowseSourceFolder;
    private Text textPackage;
    private Button btBrowsePackage;
    private Text textName;

    protected PythonAbstractPathPage(String pageName, IStructuredSelection selection) {
        super(pageName);
        setPageComplete(false);
        this.selection = selection;
    }

    public void createControl(Composite parent) {
        // top level group
        Composite topLevel = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 3;
        topLevel.setLayout(gridLayout);
        topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
        topLevel.setFont(parent.getFont());

        Label label;

        label = new Label(topLevel, SWT.NONE);
        label.setText("Source Folder");
        textSourceFolder = new Text(topLevel, SWT.BORDER);
        btBrowseSourceFolder = new Button(topLevel, SWT.NONE);
        setLayout(label, textSourceFolder, btBrowseSourceFolder);
        
        label = new Label(topLevel, SWT.NONE);
        label.setText("Package");
        textPackage = new Text(topLevel, SWT.BORDER);
        btBrowsePackage = new Button(topLevel, SWT.NONE);
        setLayout(label, textPackage, btBrowsePackage);
        
        label = new Label(topLevel, SWT.NONE);
        label.setText("Name");
        textName = new Text(topLevel, SWT.BORDER);
        setLayout(label, textName, null);
        
        // Show description on opening
        setErrorMessage(null);
        setMessage(null);
        setControl(topLevel);
    }

    private void setLayout(Label label, Text text, Button bt) {
        GridData data;
        
        data = new GridData();
        data.grabExcessHorizontalSpace = false;
        label.setLayoutData(data);
        
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        text.setLayoutData(data);
        
        if(bt != null){
            data = new GridData();
            bt.setLayoutData(data);
            bt.setText("Browse...");
        }
    }

    public IFile createNewFile() {
        return null;
    }
    
}
