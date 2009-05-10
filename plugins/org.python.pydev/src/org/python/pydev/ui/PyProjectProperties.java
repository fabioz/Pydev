/*
 * License: Common Public License v1.0
 * Created on Mar 11, 2004
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;


import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.dialogs.MapOfStringsInputDialog;
import org.python.pydev.ui.dialogs.ProjectFolderSelectionDialog;
import org.python.pydev.ui.editors.TreeWithAddRemove;

/**
 * This page is specified to appear in the plugin.xml
 */
public class PyProjectProperties extends PropertyPage {

    /**
     * This is the project we are editing
     */
    private IProject project;
    
    /**
     * Tree with source folders
     */
    private TreeWithAddRemove treeSourceFolders;
    
    /**
     * Tree with external folders
     */
    private TreeWithAddRemove treeExternalLibs;

    /**
     * Variables are edited here 
     */
    private TreeWithAddRemove treeVariables;
    
    /**
     * Yes: things are tab-separated
     */
    private TabFolder tabFolder;
    
    /**
     * Creates contents given its parent.
     */
    protected Control createContents(Composite p) {
        project = (IProject)getElement().getAdapter(IProject.class);
        
        
        Composite topComp= new Composite(p, SWT.NONE);
        GridLayout innerLayout= new GridLayout();
        innerLayout.numColumns= 1;
        innerLayout.marginHeight= 0;
        innerLayout.marginWidth= 0;
        topComp.setLayout(innerLayout);
        GridData gd = new GridData(GridData.FILL_BOTH);
        topComp.setLayoutData(gd);
        
        
        tabFolder = new TabFolder(topComp, SWT.None);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessVerticalSpace = true;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan  = 1;
        tabFolder.setLayoutData(gd);

        
        if(project != null){
            try {
                IPythonPathNature nature = PythonNature.getPythonPathNature(project);
                
                createTabProjectSourceFolders(nature.getProjectSourcePath(false));
                createTabExternalSourceFolders(nature.getProjectExternalSourcePath(false));
                createTabVariables(nature.getVariableSubstitution());
                
                createRestoreButton(topComp);
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
            
        }
        return topComp;
    }


    private void createRestoreButton(Composite topComp){
        Button button = new Button(topComp, SWT.NONE);
        button.setText("Force restore internal info");
        button.addSelectionListener(new SelectionListener(){

            public void widgetSelected(SelectionEvent e) {
                doIt(true);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
            
        });
    }

    
    private void createTabVariables(Map<String, String> variables){
        if(variables == null){
            variables = new HashMap<String, String>();
        }
        TabItem tabItem = new TabItem(tabFolder, SWT.None);
        tabItem.setText("Variables");
        tabItem.setImage(PydevPlugin.getImageCache().get(UIConstants.VARIABLE_ICON));
        Composite topComp = new Composite(tabFolder, SWT.None);
        topComp.setLayout(new GridLayout(1, false));
        
        
        GridData gd;
        GridData data;
        Label l2;
        l2 = new Label(topComp, SWT.None);
        l2.setText("Variables used to resolve:\n" +
        		"  - source folders\n" +
        		"  - external libraries\n" +
        		"  - main module in launch configuration"
        );
        
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = false;
        l2.setLayoutData(gd);
        
        final Map<String, String> vars = variables; 
        
        treeVariables = new TreeWithAddRemove(topComp, 0, vars) {
            
            @Override
            protected void createSecondAddButton(Composite buttonsSourceFolders){
                //We do not want to create the 2nd add button!
            }
            
            @Override
            protected String getSecondAddButtonLabel(){
                throw new AssertionError("Should not be called: We didn't add the second button.");
            }

            
            @Override
            protected String getImageConstant() {
                return UIConstants.VARIABLE_ICON;
            }
            
            @Override
            protected void handleAddButtonSelected(int nButton){
                if(nButton == 1){
                    addItemWithDialog(new MapOfStringsInputDialog(getShell(), "Variable", "Enter the variable name/value.", vars));
                    
                }else{
                    throw new AssertionError("Unexpected (only 1st should be available)");
                }
            }

            
            @Override
            protected String getFirstAddButtonLabel() {
                return "Add variable";
            }
        };
        
        data = new GridData(GridData.FILL_BOTH);
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        treeVariables.setLayoutData(data);
        
        tabItem.setControl(topComp);
    }
    

    private void createTabExternalSourceFolders(String externalSourcePath){
        TabItem tabItem = new TabItem(tabFolder, SWT.None);
        tabItem.setText("External Libraries");
        Composite topComp = new Composite(tabFolder, SWT.None);
        tabItem.setImage(PydevPlugin.getImageCache().get(UIConstants.LIB_SYSTEM));
        topComp.setLayout(new GridLayout(1, false));
        
        
        GridData gd;
        GridData data;
        Label l2;
        l2 = new Label(topComp, SWT.None);
        l2.setText("External Source Folders (and zips/jars/eggs).");
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = false;
        l2.setLayoutData(gd);

        treeExternalLibs = new TreeWithAddRemove(topComp, 0, PythonNature.getStrAsStrItems(externalSourcePath)) {
            
            @Override
            protected String getImageConstant() {
                return UIConstants.LIB_SYSTEM;
            }
            
            @Override
            protected String getSecondAddButtonLabel(){
                return "Add zip/jar/egg";
            }

            @Override
            protected String getFirstAddButtonLabel() {
                return "Add source folder";
            }

            @Override
            protected void handleAddButtonSelected(int nButton){
                if(nButton == 1){
                    addItemWithDialog(new DirectoryDialog(getShell()));
                    
                }else if(nButton == 2){
                    addItemWithDialog(new FileDialog(getShell(), SWT.MULTI));
                    
                }else{
                    throw new AssertionError("Unexpected");
                }
            }
        };
        data = new GridData(GridData.FILL_BOTH);
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        treeExternalLibs.setLayoutData(data);
        
        tabItem.setControl(topComp);
    }


    private void createTabProjectSourceFolders(String sourcePath){
        TabItem tabItem = new TabItem(tabFolder, SWT.None);
        tabItem.setText("Source Folders");
        tabItem.setImage(PydevPlugin.getImageCache().get(UIConstants.SOURCE_FOLDER_ICON));
        Composite topComp = new Composite(tabFolder, SWT.None);
        topComp.setLayout(new GridLayout(1, false));

        GridData gd;
        GridData data;
        Label l2 = new Label(topComp, SWT.None);
        l2.setText(
                "Project Source Folders (and zips/jars/eggs).\n" +
        		"Note that even when using variables, the final paths resolved must be workspace-relative.");
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = false;
        l2.setLayoutData(gd);

        
        treeSourceFolders = new TreeWithAddRemove(topComp, 0, PythonNature.getStrAsStrItems(sourcePath)){

            @Override
            protected String getSecondAddButtonLabel(){
                return "Add zip/jar/egg";
            }
          
            @Override
            protected String getFirstAddButtonLabel() {
                return "Add source folder";
            }

            @Override
            protected void handleAddButtonSelected(int nButton){
                if(nButton == 1){
                    addItemWithDialog(new ProjectFolderSelectionDialog(getShell(), project, true, "Choose source folders to add to PYTHONPATH"));
                    
                }else if(nButton == 2){
                    addItemWithDialog(new ResourceSelectionDialog(getShell(), project, "Choose zip/jar/egg to add to PYTHONPATH"));
                    
                }else{
                    throw new AssertionError("Unexpected");
                }
            }
            
            @Override
            protected String getImageConstant() {
                return UIConstants.SOURCE_FOLDER_ICON;
            }

        };
        data = new GridData(GridData.FILL_BOTH);
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        treeSourceFolders.setLayoutData(data);
        
        tabItem.setControl(topComp);
    }


    /**
     * Apply only saves the new value. does not do code completion update.
     * 
     * @see org.eclipse.jface.preference.PreferencePage#performApply()
     */
    protected void performApply() {
        doIt(false);
    }
    
    /**
     * Saves values into the project and updates the code completion. 
     */
    public boolean performOk() {
        return doIt(false);
    }

    /**
     * Save the pythonpath - only updates model if asked to.
     * @return
     */
    private boolean doIt(boolean force) {
        if (project != null) {
            try {
                boolean changed = false;
                IPythonPathNature pythonPathNature = PythonNature.getPythonPathNature(project);
                
                String sourcePath = pythonPathNature.getProjectSourcePath(false);
                String externalSourcePath = pythonPathNature.getProjectExternalSourcePath(false);
                Map<String, String> variableSubstitution = pythonPathNature.getVariableSubstitution();
                
                String newSourcePath = StringUtils.leftAndRightTrim(treeSourceFolders.getTreeItemsAsStr(), '|');
                String newExternalSourcePath = StringUtils.leftAndRightTrim(treeExternalLibs.getTreeItemsAsStr(), '|');
                Map<String, String> newVariableSubstitution = treeVariables.getTreeItemsAsMap();
                
                
                
                if(checkIfShouldBeSet(sourcePath, newSourcePath)){
                    pythonPathNature.setProjectSourcePath(newSourcePath);
                    changed = true;
                }                

                if(checkIfShouldBeSet(externalSourcePath, newExternalSourcePath)){
                    pythonPathNature.setProjectExternalSourcePath(newExternalSourcePath);
                    changed = true;
                }                
                
                if(checkIfShouldBeSet(variableSubstitution, newVariableSubstitution)){
                    pythonPathNature.setVariableSubstitution(newVariableSubstitution);
                    changed = true;
                }                

                PythonNature pythonNature = PythonNature.getPythonNature(project);
                if(pythonNature != null && (changed || force || pythonNature.getAstManager() == null)){
                    pythonNature.rebuildPath();
                }
                
            } catch (Exception e) {
                PydevPlugin.log(IStatus.ERROR, "Unexpected error setting project properties", e);
            }
        }
        return true;
    }


    @SuppressWarnings("unchecked")
    private boolean checkIfShouldBeSet(Object oldVal, Object newVal){
        if(oldVal == null){
            if(newVal == null){
                return false;//both null
            }
            if(newVal instanceof String){
                
                String string = (String) newVal;
                if(string.trim().length() == 0){
                    return false; //both are empty
                }
                
            }else if(newVal instanceof Map){
                Map map = (Map) newVal;
                if(map.size() == 0){
                    return false; //both are empty
                }
            }else{
                throw new AssertionError("Unexpected: "+newVal);
            }
            
            return true;
        }
        
        if(!oldVal.equals(newVal)){
            return true;
        }
        return false;
    }
}
