/*
 * Created on May 23, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.editor.codecompletion.revisited.IASTManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PythonNature;
import org.python.pydev.ui.IInterpreterManager;

/**
 * @author Fabio Zadrozny
 */
public class InterpreterPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{

	private String initialInterpreterPath;
    private InterpreterEditor pathEditor;

    /**
	 * Initializer sets the preference store
	 */
	public InterpreterPreferencesPage() {
		super("Python Interpreters", GRID);
		setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
		initialInterpreterPath = getInterpreterPath();
	}

	
	private boolean hasChanged(){
		String currentInterpreterPath = getCurrentInterpreterPath();
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
    private String getCurrentInterpreterPath() {
        String s = pathEditor.createList(pathEditor.getExesList().getItems());
        return s;
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
		pathEditor = new InterpreterEditor ("Python interpreters (e.g.: python.exe)", p, PydevPlugin.getInterpreterManager());
		addField(pathEditor);
	}

	

    /**
     * Restores the modules.
     */
    private void restoreModules() {

        if(pathEditor.getExesList().getItemCount() <= 0){
            return;

        } else{
            final String item = pathEditor.getExesList().getItem(0);
        
	        ProgressMonitorDialog monitorDialog = new ProgressMonitorDialog(this.getShell());
	
	        monitorDialog.setBlockOnOpen(false);
	        try {
	            IRunnableWithProgress operation = new IRunnableWithProgress(){
	
	                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
	                    monitor.beginTask("Restoring PYTHONPATH", IProgressMonitor.UNKNOWN);
	                    IInterpreterManager iMan = PydevPlugin.getInterpreterManager();
	                    final InterpreterInfo info = iMan.getInterpreterInfo(item, monitor);
	                    info.restorePythonpath(monitor);
	                    
	                    //pass all project natures and restore the module manager
	                    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
	                    IProject[] projects = root.getProjects();
	                    for (int i = 0; i < projects.length; i++) {
                            IProject p = projects[i];
                            if(p.isOpen()){
                                PythonNature nature = PythonNature.getPythonNature(p);
                                if(nature != null){
                                    IASTManager astManager = nature.getAstManager();
                                    if(astManager != null){
                                        astManager.setSystemModuleManager(info.modulesManager, p);
                                    }
                                }
                            }
                        }
	                    monitor.done();
	                }};
	                
	            monitorDialog.run(true, true, operation);
	            
	        }catch (Exception e) {
	            PydevPlugin.log(e);
	        }            
        }
        
    }


    /**
     * @see org.eclipse.jface.preference.IPreferencePage#performOk()
     */
    public boolean performOk() {
        if(hasChanged()){
            restoreModules();
        }
        return super.performOk();
    }

}
