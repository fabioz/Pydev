/*
 * Author: Fabio Zadrozny
 * Created on Mar 11, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.dialogs.PropertyPage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PythonNature;
import org.python.pydev.ui.editors.TreeWithAddRemove;


/**
 * 
 * This page is specified to appear in the plugin.xml
 */
public class PyProjectProperties extends PropertyPage {

    /**
     * This is the property that has the python path - associated with the project.
     */
	public static QualifiedName PROJECT_SOURCE_PATH = new QualifiedName(PydevPlugin.getPluginID(), "PROJECT_SOURCE_PATH");
	public static QualifiedName PROJECT_EXTERNAL_SOURCE_PATH = new QualifiedName(PydevPlugin.getPluginID(), "PROJECT_EXTERNAL_SOURCE_PATH");
	
    /**
     * @return
     */
    public static List getProjectPythonPath(IProject project) {
        if(project == null){
            return null;
        }
        
        List paths;
        try {
            String persistentProperty = getProjectPythonPathStr(project);

            if(persistentProperty != null){
                String[] strings = persistentProperty.split("\\|");
                
                paths = new ArrayList();
                for (int i = 0; i < strings.length; i++) {
                    if(strings[i].trim().length() > 0){
                        paths.add(strings[i]);
                    }
                }
            }else{
                paths = new ArrayList();
            }
        } catch (Exception e) {

            PydevPlugin.log(e);
            paths = new ArrayList();
            
        }
        return paths;
    }

    /**
     * @param project
     * @return
     * @throws CoreException
     */
    public static String getProjectPythonPathStr(IProject project) throws CoreException {
        String source = project.getPersistentProperty(PROJECT_SOURCE_PATH);
        String external = project.getPersistentProperty(PROJECT_EXTERNAL_SOURCE_PATH);
        if(source == null){
            source = "";
        }
        String[] strings = source.split("\\|");
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < strings.length; i++) {
            if(strings[i].trim().length()>0){
                IPath p = new Path(strings[i]);
                p = PydevPlugin.getLocation(p);
                File file = p.toFile().getAbsoluteFile();
                buf.append(file.getAbsolutePath());
                buf.append("|");
            }
        }
        if(external == null){
            external = "";
        }
        return buf.toString()+"|"+external;
    }

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
                String sourcePath = project.getPersistentProperty(PyProjectProperties.PROJECT_SOURCE_PATH);
                String externalSourcePath = project.getPersistentProperty(PyProjectProperties.PROJECT_EXTERNAL_SOURCE_PATH);
                treeSourceFolders = new TreeWithAddRemove(topComp, 0, project, sourcePath);
                data = new GridData(GridData.FILL_BOTH);
                data.grabExcessHorizontalSpace = true;
                data.grabExcessVerticalSpace = true;
                treeSourceFolders.setLayoutData(data);

                treeExternalLibs = new TreeWithAddRemove(topComp, 0, project, externalSourcePath) {
                    protected String getImageConstant() {
                        return UIConstants.LIB_SYSTEM;
                    }

                    protected Object getSelectionDialog() {
                        return new DirectoryDialog(getShell());
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
			    String sourcePath = project.getPersistentProperty(PyProjectProperties.PROJECT_SOURCE_PATH);
			    String externalSourcePath = project.getPersistentProperty(PyProjectProperties.PROJECT_EXTERNAL_SOURCE_PATH);
			    
			    String newSourcePath = treeSourceFolders.getTreeItemsAsStr();
			    String newExternalSourcePath = treeExternalLibs.getTreeItemsAsStr();
			    
			    
			    if(sourcePath ==  null || sourcePath.equals(newSourcePath) == false){
					project.setPersistentProperty(PROJECT_SOURCE_PATH, newSourcePath);
					changed = true;
			    }				

			    if(externalSourcePath ==  null || externalSourcePath.equals(newExternalSourcePath) == false){
					project.setPersistentProperty(PROJECT_EXTERNAL_SOURCE_PATH, newExternalSourcePath);
					changed = true;
			    }				

			    if(changed){
				    IProjectNature nature = project.getNature(PythonNature.PYTHON_NATURE_ID);
					
					if(nature instanceof PythonNature){
					    ((PythonNature)nature).rebuildPath(PyProjectProperties.getProjectPythonPathStr(project));
					}
			    }
				
			} catch (Exception e) {
				PydevPlugin.log(IStatus.ERROR, "Unexpected error setting project properties", e);
			}
		}
		return true;
    }

	
}
