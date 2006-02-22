/*
 * Created on 14/08/2005
 */
package org.python.pydev.debug.ui;

import java.io.File;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.dialogs.PythonModulePickerDialog;

/**
 * Tab where user chooses project and Python module for launch
 * 
 * <p>
 * Also show PYTHONPATH information
 * </p>
 * 
 * TODO: Fix project chooser. It allows to select folders.
 * 
 * TODO: Fix code completion job scheduling problem with this tab.
 * Show progress dialog when ASTManager and thus PYTHONPATH information
 * is not yet available.
 * 
 * @author Mikko Ohtamaa
 *
 */
public class MainModuleTab extends AbstractLaunchConfigurationTab {
    
    WidgetListener widgetListener = new WidgetListener();
    
    protected ModifyListener modifyListener = new ModifyListener() {
        public void modifyText(ModifyEvent e) {
            if(e.getSource() == locationField){
                File file = new File(locationField.getText());
                if(!file.exists()){
                    setErrorMessage("The file in the location does not exist.");
                }
                if(!file.isFile()){
                    setErrorMessage("The file in the location is not actually a file.");
                }
            }
                            
            updateLaunchConfigurationDialog();
        }
    };    

    
    private Text projectField, locationField;

    private Button browseProjectButton, browseModuleButton;

    private List pythonpath;
    

    private IInterpreterManager interpreterManager;
    
    public MainModuleTab(IInterpreterManager interpreterManager) {
        this.interpreterManager = interpreterManager;
    }

    /**
     * creates the widgets 
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        setControl(comp);
        GridLayout gridLayout = new GridLayout ();
        gridLayout.numColumns = 1;
        comp.setLayout (gridLayout);

        createProjectEditor(comp);
        Label label = new Label(comp, SWT.NONE);
        label.setText("PYTHONPATH that will be used in the run:");
        pythonpath = new List(comp, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);

        GridData gd = new GridData(GridData.FILL_BOTH);
        pythonpath.setLayoutData(gd);

    }

    /**
     * ok, choose project button just pressed
     */
    public void handleProjectButtonSelected() {
        IProject project = chooseProject();
        if (project == null) {
            return;
        }
        
        PythonNature pythonNature = PythonNature.getPythonNature(project);
        if(pythonNature == null){
            //the project does not have an associated python nature...
            String msg = "The selected project must have the python nature associated.";
            String title = "Invalid project (no python nature associated).";
            ErrorDialog.openError(getShell(), title, msg, 
                    PydevPlugin.makeStatus(IStatus.WARNING, title, null));
            return;
        }
        
        String projectName = project.getName();
        projectField.setText(projectName);     
               
    }
    
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        //no defaults to set
    }
    
    public void initializeFrom(ILaunchConfiguration configuration) {
        try {        	
        	String location = configuration.getAttribute(Constants.ATTR_LOCATION, "");
        	locationField.setText(location);
        } catch(CoreException e) {
        	locationField.setText("");
        }
        
        try {
            projectField.setText(configuration.getAttribute(Constants.ATTR_PROJECT, ""));            
        } catch (CoreException e) {
            projectField.setText("");            
        }
    }

    public void performApply(ILaunchConfigurationWorkingCopy conf) {
        String value;
        value = projectField.getText().trim();        
        setAttribute(conf, Constants.ATTR_PROJECT, value);
       
        value = locationField.getText().trim();
        setAttribute(conf, Constants.ATTR_LOCATION, value);
    }
    

    /**
     * the name for this tab 
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    public String getName() {
        return "Main";
    }
    
    @Override
    public Image getImage() {
        return PydevPlugin.getImageCache().get(Constants.MAIN_ICON);
    }
    
    /**
     * make a dialog and return a project (does no validation) and may return null.
     */
    private IProject chooseProject() {
        
        ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), null, false, "Choose the project for the run");
        dialog.open();
        Object[] objects = dialog.getResult();
        if(objects != null && objects.length == 1){
            if(objects[0] instanceof IPath){
                IPath p = (IPath) objects[0];
                if(p.segmentCount() > 0){
                    String string = p.segment(0);
                    IWorkspace w = ResourcesPlugin.getWorkspace();
                    return w.getRoot().getProject(string);
                }
            }
        }
        return null;
    }

    /**
     * Creates the widgets for specifying a main type.
     * 
     * @param parent the parent composite
     */
    private void createProjectEditor(final Composite parent) {
        Font font= parent.getFont();
        Group group= new Group(parent, SWT.NONE);
        group.setText("Project"); 
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        group.setLayoutData(gd);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        group.setLayout(layout);
        group.setFont(font);

        // Project chooser
        projectField = new Text(group, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        projectField.setLayoutData(gd);
        projectField.setFont(font);
        projectField.addModifyListener(widgetListener);
              
        browseProjectButton = createPushButton(group, "Browse...", null); //$NON-NLS-1$
        browseProjectButton.addSelectionListener(widgetListener);
                   
        
        // Main module group
        group= new Group(parent, SWT.NONE);
        group.setText("Main Module"); 
        gd = new GridData(GridData.FILL_HORIZONTAL);
        group.setLayoutData(gd);
        layout = new GridLayout();
        layout.numColumns = 2;
        group.setLayout(layout);
        group.setFont(font);
                
        // Main module chooser
        locationField = new Text (group,  SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        locationField.setLayoutData (gd);
        locationField.setFont(font);
        locationField.addModifyListener(modifyListener);        
                
        browseModuleButton = createPushButton(group, "Browse...", null);
        browseModuleButton.setText("Browse");        
        browseModuleButton.setEnabled(false);
        browseModuleButton.addSelectionListener(new SelectionAdapter() {        	
        	 public void widgetSelected(SelectionEvent e) {        		              
        		 IProject project = getProjectFromTextWidget();
                 PythonModulePickerDialog dialog = new PythonModulePickerDialog(
                		 parent.getShell(),
                		 "Main Module",
                		 "Choose Python module which starts execution",
                		 project);
                 int result = dialog.open();
                 
                 if(result == PythonModulePickerDialog.OK) {
                	 Object results[] = dialog.getResult();
                	 System.out.println("Results:" + results.length);
                	 if(results.length == 1) {
                		 IFile file = (IFile) results[0];
                		 locationField.setText(file.getRawLocation().toOSString());
                	 }
                 }
             }
        });
    }   


    private void updatePythonpath() {
        this.pythonpath.removeAll();
        IProject project = getProjectFromTextWidget();
        IPythonPathNature pythonPathNature = PythonNature.getPythonPathNature(project);
        if(pythonPathNature != null){
            java.util.List paths = pythonPathNature.getCompleteProjectPythonPath();
            if(paths == null) {
            	this.pythonpath.add("Code completion task is running, no PYTHONPATH information available");
            	// TODO: Make a progress dialog shown until code completition task 
            	// is finished and PYTHONPATH is available
            } else {
	            for (Iterator iter = paths.iterator(); iter.hasNext();) {
	                String element = (String) iter.next();
	                this.pythonpath.add(element);
	            }
            }
        }else{
            this.pythonpath.add("Unable to get the pythonpath associated with the current project for the run (no pydev nature associated).");
        }
    }

        /**
     * sets attributes in the working copy
     */
    private void setAttribute(ILaunchConfigurationWorkingCopy conf, String name, String value) {
        if (value == null || value.length() == 0){
            conf.setAttribute(name, (String)null);
        }else{
            conf.setAttribute(name, value);
        }
    }

    /**
     * @return
     */
    private IProject getProjectFromTextWidget() {
        IWorkspace w = ResourcesPlugin.getWorkspace();
        IResource project = w.getRoot().findMember(projectField.getText());
        
        if(project instanceof IProject){
            return (IProject) project;
        }
        return null;
    }
    
    private class WidgetListener implements ModifyListener, SelectionListener {
        public void modifyText(ModifyEvent e) {
            setErrorMessage(null);
            setMessage(null);
            browseModuleButton.setEnabled(false);

            Object source = e.getSource();
            if(source == projectField){
                IProject project = getProjectFromTextWidget();

                if(project == null){
                    setErrorMessage("invalid project");                    
                }else{
                	                	
                    PythonNature nature = PythonNature.getPythonNature((IProject)project);
                    if(nature == null){
                        setErrorMessage("Invalid project (no python nature associated).");
                    } 
                    
                    browseModuleButton.setEnabled(true);
                }
                updatePythonpath();
                
                updateLaunchConfigurationDialog();
            }else{
                updateLaunchConfigurationDialog();
            }
            
            
        }
        public void widgetSelected(SelectionEvent e) {
            Object source = e.getSource();
            if (source == browseProjectButton) {
                handleProjectButtonSelected();
            }else{
                updateLaunchConfigurationDialog();
            }
        }
        
        public void widgetDefaultSelected(SelectionEvent e) {
        }
    }    

}
