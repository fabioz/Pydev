/*
 * Created on Jan 25, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.dialogs.PropertyPage;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public class PyProjectPythonDetails extends PropertyPage{


	/**
	 * The element.
	 */
	private IAdaptable element;
    private Button radioPy24;
    private Button radioPy23;
    private Button radioJy21;

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
    protected Control createContents(Composite p) {
        Composite topComp= new Composite(p, SWT.NONE);
        GridLayout innerLayout= new GridLayout();
        innerLayout.numColumns= 1;
        innerLayout.marginHeight= 0;
        innerLayout.marginWidth= 0;
        topComp.setLayout(innerLayout);
        GridData gd= new GridData(GridData.FILL_BOTH);
        topComp.setLayoutData(gd);

        
        
        Group group = new Group(topComp, SWT.NONE);
        group.setText("Choose the project type");
        GridLayout layout = new GridLayout();
        layout.horizontalSpacing = 8;
        layout.numColumns = 3;
        group.setLayout(layout);

        
        
        radioPy23 = new Button(group, SWT.RADIO | SWT.LEFT);
        radioPy23.setText(IPythonNature.PYTHON_VERSION_2_3);

        radioPy24 = new Button(group, SWT.RADIO | SWT.LEFT);
        radioPy24.setText(IPythonNature.PYTHON_VERSION_2_4);
        
        radioJy21 = new Button(group, SWT.RADIO | SWT.LEFT);
        radioJy21.setText(IPythonNature.JYTHON_VERSION_2_1);
        
        setSelected();
        return topComp;
    }
    
    private void setSelected() {
        PythonNature pythonNature = PythonNature.getPythonNature(getProject());
        try {
            String version = pythonNature.getVersion();
            if(version.equals(IPythonNature.PYTHON_VERSION_2_3)){
                radioPy23.setSelection(true);
            }
            else if(version.equals(IPythonNature.PYTHON_VERSION_2_4)){
                radioPy24.setSelection(true);
            }
            else if(version.equals(IPythonNature.JYTHON_VERSION_2_1)){
                radioJy21.setSelection(true);
            }
        } catch (CoreException e) {
            PydevPlugin.log(e);
        }
    }

    private String getSelected() {
        if(radioPy23.getSelection()){
            return IPythonNature.PYTHON_VERSION_2_3;
        }
        if(radioPy24.getSelection()){
            return IPythonNature.PYTHON_VERSION_2_4;
        }
        if(radioJy21.getSelection()){
            return IPythonNature.JYTHON_VERSION_2_1;
        }
        throw new RuntimeException("Some radio must be selected");
    }
    
    protected void performApply() {
        doIt();
    }
    
    public boolean performOk() {
        return doIt();
    }

    private boolean doIt() {
        if (getProject()!= null) {
            PythonNature pythonNature = PythonNature.getPythonNature(getProject());
            try {
                pythonNature.setVersion(getSelected());
            } catch (CoreException e) {
                PydevPlugin.log(e);
            }
        }
        return true;
    }
}