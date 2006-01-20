/*
 * Created on Jan 17, 2006
 */
package org.python.pydev.ui.wizards.files;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.dialogs.PythonSrcFolderSelectionDialog;


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

        createSourceFolderSelect(topLevel);
        createPackageSelect(topLevel);
        createNameSelect(topLevel);
        
        // Show description on opening
        setErrorMessage(null);
        setMessage(null);
        setControl(topLevel);
    }

    /**
     * @param topLevel
     */
    private void createNameSelect(Composite topLevel) {
        Label label;
        label = new Label(topLevel, SWT.NONE);
        label.setText("Name");
        textName = new Text(topLevel, SWT.BORDER);
        setLayout(label, textName, null);
    }

    /**
     * @param topLevel
     */
    private void createPackageSelect(Composite topLevel) {
        Label label;
        label = new Label(topLevel, SWT.NONE);
        label.setText("Package");
        textPackage = new Text(topLevel, SWT.BORDER);
        btBrowsePackage = new Button(topLevel, SWT.NONE);
        setLayout(label, textPackage, btBrowsePackage);
    }

    /**
     * @param topLevel
     */
    private void createSourceFolderSelect(Composite topLevel) {
        Label label;
        label = new Label(topLevel, SWT.NONE);
        label.setText("Source Folder");
        textSourceFolder = new Text(topLevel, SWT.BORDER);
        btBrowseSourceFolder = new Button(topLevel, SWT.NONE);
        setLayout(label, textSourceFolder, btBrowseSourceFolder);
        
        Object element = selection.getFirstElement();
        String srcPath = null;
        IProject project = null;
        
        try {
            if (element instanceof IFolder) {
                IFolder f = (IFolder) element;
                project = f.getProject();
                IPythonPathNature nature = PythonNature.getPythonPathNature(project);
                String[] srcPaths = PythonNature.getStrAsStrItems(nature.getProjectSourcePath());
                String relFolder = f.getFullPath().toString();
                for (String src : srcPaths) {
                    if(relFolder.startsWith(src)){
                        srcPath = src;
                        break;
                    }
                }
                textSourceFolder.setText(srcPath);
            }
            
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
        
        btBrowseSourceFolder.addSelectionListener(new SelectionListener(){

            public void widgetSelected(SelectionEvent e) {
                try {
                    PythonSrcFolderSelectionDialog dialog = new PythonSrcFolderSelectionDialog(getShell());
                    dialog.open();
                    Object firstResult = dialog.getFirstResult();
                    if(firstResult instanceof IFolder){
                        IFolder f = (IFolder) firstResult;
                        textSourceFolder.setText(f.getFullPath().toString());
                    }
                } catch (Exception e1) {
                    PydevPlugin.log(e1);
                }
                
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
            
        });
        System.out.println(element.getClass());
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
