/*
 * License: Common Public License v1.0
 * Created on Mar 11, 2004
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.IPythonPathNature;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.editors.TreeWithAddRemove;


/**
 * 
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
        GridData gd= new GridData(GridData.FILL_BOTH);
        topComp.setLayoutData(gd);
        
        
		GridData data = new GridData ();

		if(project != null){
		    try {
		    	String sourcePath = PythonNature.getPythonPathNature(project).getProjectSourcePath();
                String externalSourcePath = PythonNature.getPythonPathNature(project).getProjectExternalSourcePath();

                Label l2 = new Label(topComp, SWT.None);
		    	l2.setText("Project Source Folders and jars.");
		    	gd = new GridData();
		    	gd.grabExcessHorizontalSpace = true;
		    	gd.grabExcessVerticalSpace = false;
		    	l2.setLayoutData(gd);

		    	
                treeSourceFolders = new TreeWithAddRemove(topComp, 0, project, sourcePath);
                data = new GridData(GridData.FILL_BOTH);
                data.grabExcessHorizontalSpace = true;
                data.grabExcessVerticalSpace = true;
                treeSourceFolders.setLayoutData(data);


                
                
		    	l2 = new Label(topComp, SWT.None);
		    	l2.setText("External Source Folders and jars.");
		    	gd = new GridData();
		    	gd.grabExcessHorizontalSpace = true;
		    	gd.grabExcessVerticalSpace = false;
		    	l2.setLayoutData(gd);

                treeExternalLibs = new TreeWithAddRemove(topComp, 0, project, externalSourcePath) {
                    protected String getImageConstant() {
                        return UIConstants.LIB_SYSTEM;
                    }

                    protected Object getSelectionDialogAddSourceFolder() {
                        return new DirectoryDialog(getShell());
                    }
                    
                    @Override
                    protected Object getSelectionDialogAddJar() {
                        return new FileDialog(getShell());
                    }
                };
                data = new GridData(GridData.FILL_BOTH);
                data.grabExcessHorizontalSpace = true;
                data.grabExcessVerticalSpace = true;
                treeExternalLibs.setLayoutData(data);
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
		    
		}
		return topComp;
	}


    /**
     * Apply only saves the new value. does not do code completion update.
     * 
     * @see org.eclipse.jface.preference.PreferencePage#performApply()
     */
    protected void performApply() {
		doIt();
    }
    
    /**
	 * Saves values into the project and updates the code completion. 
	 */
	public boolean performOk() {
		return doIt();
	}

    /**
     * Save the pythonpath - only updates model if asked to.
     * @return
     */
    private boolean doIt() {
        if (project != null) {
			try {
			    boolean changed = false;
			    IPythonPathNature pythonPathNature = PythonNature.getPythonPathNature(project);
			    
                String sourcePath = pythonPathNature.getProjectSourcePath();
			    String externalSourcePath = pythonPathNature.getProjectExternalSourcePath();
			    
			    String newSourcePath = treeSourceFolders.getTreeItemsAsStr();
			    String newExternalSourcePath = treeExternalLibs.getTreeItemsAsStr();
			    
			    
			    if(sourcePath ==  null || sourcePath.equals(newSourcePath) == false){
			        pythonPathNature.setProjectSourcePath(newSourcePath);
					changed = true;
			    }				

			    if(externalSourcePath ==  null || externalSourcePath.equals(newExternalSourcePath) == false){
			        pythonPathNature.setProjectExternalSourcePath(newExternalSourcePath);
					changed = true;
			    }				

			    PythonNature pythonNature = PythonNature.getPythonNature(project);
			    if(pythonNature != null && (changed || pythonNature.getAstManager() == null)){
			        pythonNature.rebuildPath(pythonPathNature.getOnlyProjectPythonPathStr());
			    }
				
			} catch (Exception e) {
				PydevPlugin.log(IStatus.ERROR, "Unexpected error setting project properties", e);
			}
		}
		return true;
    }
}
