/*
 * Created on 14/08/2005
 */
package org.python.pydev.debug.ui;

import java.util.Iterator;

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
import org.python.pydev.debug.core.Constants;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.IPythonPathNature;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.interpreters.IInterpreterManager;

public class PythonProjectRelatedTab extends AbstractLaunchConfigurationTab {

    private class WidgetListener implements ModifyListener, SelectionListener {
        public void modifyText(ModifyEvent e) {
            setErrorMessage(null);
            setMessage(null);

            Object source = e.getSource();
            if(source == fProjText){
                IProject project = getProjectFromTextWidget();

                if(project == null){
                    setErrorMessage("invalid project");
                }else{
                    PythonNature nature = PythonNature.getPythonNature((IProject)project);
                    if(nature == null){
                        setErrorMessage("Invalid project (no python nature associated).");
                    }
                }
                updatePythonpath();
                
                updateLaunchConfigurationDialog();
            }else{
                updateLaunchConfigurationDialog();
            }
        }
        public void widgetSelected(SelectionEvent e) {
            Object source = e.getSource();
            if (source == fProjButton) {
                handleProjectButtonSelected();
            }else{
                updateLaunchConfigurationDialog();
            }
        }
        
        public void widgetDefaultSelected(SelectionEvent e) {
        }
    }
    
    WidgetListener widgetListener = new WidgetListener();

    
    private Text fProjText;

    private Button fProjButton;

    private List pythonpath;


    private IInterpreterManager interpreterManager;
    
    public PythonProjectRelatedTab(IInterpreterManager interpreterManager) {
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
        fProjText.setText(projectName);     

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
    private void createProjectEditor(Composite parent) {
        Font font= parent.getFont();
        Group group= new Group(parent, SWT.NONE);
        group.setText("Project:"); 
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        group.setLayoutData(gd);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        group.setLayout(layout);
        group.setFont(font);

        fProjText = new Text(group, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        fProjText.setLayoutData(gd);
        fProjText.setFont(font);
        fProjText.addModifyListener(widgetListener);
        
        fProjButton = createPushButton(group, "Browse", null); //$NON-NLS-1$
        fProjButton.addSelectionListener(widgetListener);
    }   


    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        //no defaults to set
    }

    private void updatePythonpath() {
        this.pythonpath.removeAll();
        IProject project = getProjectFromTextWidget();
        IPythonPathNature pythonPathNature = PythonNature.getPythonPathNature(project);
        java.util.List paths = pythonPathNature.getCompleteProjectPythonPath();
        for (Iterator iter = paths.iterator(); iter.hasNext();) {
            String element = (String) iter.next();
            this.pythonpath.add(element);
        }
    }

    public void initializeFrom(ILaunchConfiguration configuration) {
        try {
            fProjText.setText(configuration.getAttribute(Constants.ATTR_PROJECT, ""));
        } catch (CoreException e) {
            fProjText.setText("");
        }
    }

    public void performApply(ILaunchConfigurationWorkingCopy conf) {
        String value;
        value = fProjText.getText().trim();
        
        setAttribute(conf, Constants.ATTR_PROJECT, value);
    }
    

    /**
     * the name for this tab 
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    public String getName() {
        return "Project";
    }
    
    @Override
    public Image getImage() {
        return PydevPlugin.getImageCache().get(Constants.MAIN_ICON);
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
        IResource project = w.getRoot().findMember(fProjText.getText());
        
        if(project instanceof IProject){
            return (IProject) project;
        }
        return null;
    }

}
