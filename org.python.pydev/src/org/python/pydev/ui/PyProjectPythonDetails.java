/*
 * Created on Jan 25, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public class PyProjectPythonDetails extends PropertyPage{

    public static class RadioController{
        public Button radioPy;
        public Button radioJy;
        public Combo comboGrammarVersion;
        public Label versionLabel;
        
        /**
         * @param p
         * @return
         */
        public Control doCreateContents(Composite p) {
            Composite topComp= new Composite(p, SWT.NONE);
            GridLayout innerLayout= new GridLayout();
            innerLayout.numColumns= 1;
            innerLayout.marginHeight= 0;
            innerLayout.marginWidth= 0;
            topComp.setLayout(innerLayout);
            GridData gd= new GridData(GridData.FILL_BOTH);
            topComp.setLayoutData(gd);

            
            
            //Project type
            Group group = new Group(topComp, SWT.NONE);
            group.setText("Choose the project type");
            GridLayout layout = new GridLayout();
            layout.horizontalSpacing = 8;
            layout.numColumns = 2;
            group.setLayout(layout);
            gd= new GridData(GridData.FILL_HORIZONTAL);
            group.setLayoutData(gd);

            radioPy = new Button(group, SWT.RADIO | SWT.LEFT);
            radioPy.setText("Python");
            
            radioJy = new Button(group, SWT.RADIO | SWT.LEFT);
            radioJy.setText("Jython");
            
            
            
            //Grammar version
            versionLabel = new Label(topComp, 0);
            versionLabel.setText("Grammar Version");
            gd= new GridData(GridData.FILL_HORIZONTAL);
            versionLabel.setLayoutData(gd);
            
            
            
            comboGrammarVersion = new Combo(topComp, SWT.READ_ONLY);
            comboGrammarVersion.add("2.1");
            comboGrammarVersion.add("2.2");
            comboGrammarVersion.add("2.3");
            comboGrammarVersion.add("2.4");
            comboGrammarVersion.add("2.5");
            comboGrammarVersion.add("2.6");
            comboGrammarVersion.add("3.0");
            
            gd= new GridData(GridData.FILL_HORIZONTAL);
            comboGrammarVersion.setLayoutData(gd);

            
            return topComp;
        }
        
        public String getSelected() {
            if(radioPy.getSelection()){
                return "python "+comboGrammarVersion.getText();
            }
            if(radioJy.getSelection()){
                return "jython "+comboGrammarVersion.getText();
            }
            throw new RuntimeException("Some radio must be selected");
        }

        public void setDefaultSelection() {
            radioPy.setSelection(true);
            comboGrammarVersion.setText("2.6");
        }

    }
    
    /**
     * The element.
     */
    public IAdaptable element;
    public RadioController radioController = new RadioController();

    /*
     *  (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPropertyPage#getElement()
     */
    public IAdaptable getElement() {
        return element;
    }
    /**
     * Sets the element that owns properties shown on this page.
     * 
     * @param element the element
     */
    public void setElement(IAdaptable element) {
        this.element = element;
    }
    
    public IProject getProject(){
        return (IProject)getElement().getAdapter(IProject.class);
    }

    @Override
    public Control createContents(Composite p) {
        Control contents = radioController.doCreateContents(p);
        setSelected();
        return contents;
    }
    
    private void setSelected() {
        PythonNature pythonNature = PythonNature.getPythonNature(getProject());
        try {
            //Set whether it's Python/Jython
            String version = pythonNature.getVersion();
            if(IPythonNature.Versions.ALL_PYTHON_VERSIONS.contains(version)){
                radioController.radioPy.setSelection(true);
                
            }else if(IPythonNature.Versions.ALL_JYTHON_VERSIONS.contains(version)){
                radioController.radioJy.setSelection(true);
            }
            
            //We must set the grammar version too (that's from a string in the format "Python 2.4" and we only want
            //the version).
            String v = StringUtils.split(version, ' ')[1];
            radioController.comboGrammarVersion.setText(v);
            
            
        } catch (CoreException e) {
            PydevPlugin.log(e);
        }
    }

    
    protected void performApply() {
        doIt();
    }
    
    public boolean performOk() {
        return doIt();
    }

    private boolean doIt() {
        IProject project = getProject();
        
        if (project!= null) {
            final String newVersion = radioController.getSelected();
            PythonNature pythonNature = PythonNature.getPythonNature(project);
            
            final IInterpreterManager interpreterManager;
            if(IPythonNature.Versions.ALL_JYTHON_VERSIONS.contains(newVersion)){
                interpreterManager = PydevPlugin.getJythonInterpreterManager();
            }else{
                interpreterManager = PydevPlugin.getPythonInterpreterManager();
            }
            
            try {
                interpreterManager.getDefaultInterpreter();
            } catch (NotConfiguredInterpreterException e){
                
                final Display display = Display.getDefault();
                display.syncExec(new Runnable(){

                    public void run() {
                        String msg = "You're currently trying to change your project to a %s project, " +
                                "but there is currently no interpreter configured for it, so, please, " +
                                "go to the menu: window > preferences > Pydev > Interpreter %s and configure " +
                                "it before changing your project type.";
                        final String managerRelatedName = interpreterManager.getManagerRelatedName();
                        MessageDialog.openError(display.getActiveShell(), "Invalid Interpreter", 
                                StringUtils.format(msg, managerRelatedName, managerRelatedName));
                    }
                    
                });
                return false;
            }
            
            try {
                pythonNature.setVersion(newVersion);
            } catch (CoreException e) {
                PydevPlugin.log(e);
            }
        }
        return true;
    }
}