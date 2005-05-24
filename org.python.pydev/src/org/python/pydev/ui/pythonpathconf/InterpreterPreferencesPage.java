/*
 * Created on May 23, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.IInterpreterManager;

/**
 * @author Fabio Zadrozny
 */
public class InterpreterPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{

	private String initialInterpreterPath;

    /**
	 * Initializer sets the preference store
	 */
	public InterpreterPreferencesPage() {
		super("Python Interpreters", GRID);
		setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
		initialInterpreterPath = getInterpreterPath();
	}

	
	private boolean hasChanged(){
		String currentInterpreterPath = getInterpreterPath();
		if(initialInterpreterPath.equals(currentInterpreterPath)){
		    return false;
		}else{
		    initialInterpreterPath = currentInterpreterPath;
		    return true;
		}
	}
	
	/**
     * @return
     */
    private String getInterpreterPath() {
        return getPreferenceStore().getString(IInterpreterManager.INTERPRETER_PATH);
    }


    public void init(IWorkbench workbench) {
	}
	
	/**
	 * Creates the editors
	 */
	protected void createFieldEditors() {
		Composite p = getFieldEditorParent();
		InterpreterEditor pathEditor = new InterpreterEditor ("Python interpreters (e.g.: python.exe)", p, PydevPlugin.getInterpreterManager());
		addField(pathEditor);
	}

	

	/**
     * @see org.eclipse.jface.preference.PreferencePage#performApply()
     */
    protected void performApply() {
        super.performApply();
        if(hasChanged()){
            restoreModules();
        }
    }
    
    /**
     * 
     */
    private void restoreModules() {
        IInterpreterManager iMan = PydevPlugin.getInterpreterManager();
        String interpreter = iMan.getDefaultInterpreter();
        iMan.getInterpreterInfo(interpreter, new NullProgressMonitor());
        
    }


    /**
     * @see org.eclipse.jface.preference.IPreferencePage#performOk()
     */
    public boolean performOk() {
        boolean ok = super.performOk();
        if(hasChanged()){
            restoreModules();
        }
        return ok;
    }

}
