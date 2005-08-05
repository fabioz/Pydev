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
import org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.IInterpreterManager;

/**
 * @author Fabio Zadrozny
 */
public class InterpreterPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{

    private InterpreterEditor pathEditor;
    private boolean changed = false;

    /**
	 * Initializer sets the preference store
	 */
	public InterpreterPreferencesPage() {
		super(GRID);
		setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        changed = false;
	}
    
    @Override
    public String getTitle() {
        return "Python Interpreters";
    }

    /**
     * @return the title that should be used above the interpreters editor.
     */
    protected String getInterpretersTitle() {
        return "Python interpreters (e.g.: python.exe)";
    }
    
    /**
     * @param p this is the composite that should be the interpreter parent
     * @return an interpreter editor (used to add/edit/remove the information on an editor)
     */
    protected InterpreterEditor getInterpreterEditor(Composite p) {
        return new InterpreterEditor (getInterpretersTitle(), p, PydevPlugin.getInterpreterManager());
    }

    /**
     * @param defaultSelectedInterpreter this is the path to the default selected file (interpreter)
     * @param monitor a monitor to display the progress to the user.
     */
    protected void doRestore(final String defaultSelectedInterpreter, IProgressMonitor monitor) {
        monitor.beginTask("Restoring PYTHONPATH", IProgressMonitor.UNKNOWN);
        IInterpreterManager iMan = PydevPlugin.getInterpreterManager();
        final InterpreterInfo info = iMan.getInterpreterInfo(defaultSelectedInterpreter, monitor);
        info.restorePythonpath(monitor); //that's it, info.modulesManager contains the SystemModulesManager
        
        monitor.done();
    }

    /**
     * @return whether this page has changed
	 */
	private boolean hasChanged(){
	    return changed || pathEditor.hasChanged();
	}
	
    public void init(IWorkbench workbench) {
	}
	
	/**
	 * Creates the editors - also provides a hook for getting a different interpreter editor
	 */
	protected void createFieldEditors() {
		Composite p = getFieldEditorParent();
		pathEditor = getInterpreterEditor(p);
		addField(pathEditor);
	}


	
    /**
     * Restores the modules. Is called when the user changed something in the editor and applies the change.
     * 
     * Gathers all the info and calls the hook that really restores things within a thread, so that the user can 
     * get information on the progress.
     * 
     * Only the information on the default interpreter is stored.
     */
    private void restoreModules() {

        if(pathEditor.getExesList().getItemCount() <= 0){
            return;

        } else{
            //this is the default interpreter
            final String item = pathEditor.getExesList().getItem(0);
	        ProgressMonitorDialog monitorDialog = new ProgressMonitorDialog(this.getShell());
	        monitorDialog.setBlockOnOpen(false);

            try {
	            IRunnableWithProgress operation = new IRunnableWithProgress(){
	
	                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
	                    doRestore(item, monitor);
	                }};
	                
	            monitorDialog.run(true, true, operation);
	            
	        }catch (Exception e) {
	            PydevPlugin.log(e);
	        }            
        }
    }

    
    /**
     * Applies changes (if any) 
     * @see org.eclipse.jface.preference.PreferencePage#performApply()
     */
    protected void performApply() {
        restoreModules();
        changed = false;
        pathEditor.changed = false;
        super.performApply();
    }
    
    /**
     * Restores the default values 
     *  
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    protected void performDefaults() {
        changed = true;
        super.performDefaults();
    }
    
    /**
     * Cancels any change
     *  
     * @see org.eclipse.jface.preference.IPreferencePage#performCancel()
     */
    public boolean performCancel() {
        changed = false;
        return super.performCancel();
    }
    
    /**
     * Applies changes (if any)
     * 
     * @see org.eclipse.jface.preference.IPreferencePage#performOk()
     */
    public boolean performOk() {
        if(hasChanged()){
            restoreModules();
        }
        return super.performOk();
    }


}
