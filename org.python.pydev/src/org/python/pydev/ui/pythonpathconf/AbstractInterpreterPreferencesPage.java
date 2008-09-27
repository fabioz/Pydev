/*
 * Created on May 23, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractInterpreterPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{

    private boolean changed = false;
    protected AbstractInterpreterEditor pathEditor;

    /**
     * Initializer sets the preference store
     */
    public AbstractInterpreterPreferencesPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        changed = false;
    }

    protected abstract AbstractInterpreterEditor getInterpreterEditor(Composite p);
    
    /**
     * @return whether this page has changed
     */
    protected boolean hasChanged(){
        return changed || isEditorChanged();
    }
    
    public void init(IWorkbench workbench) {
    }
    
    /**
     * Applies changes (if any) 
     * @see org.eclipse.jface.preference.PreferencePage#performApply()
     */
    protected void performApply() {
        //we must apply before restoring the modules (because it will need the info we're saving)
        changed = true; //force it to make the changes 
        super.performApply(); //calls performOk()
    }
    

    /**
     * Restores the default values 
     *  
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    protected void performDefaults() {
        //don't do anything on defaults...
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
        //IMPORTANT: we must call the perform before restoring the modules because this
        //info is going to be used when restoring them.
        super.performOk();
        
        if(hasChanged()){
            restoreModules();
        }
        
        //When we call performOk, the editor is going to store its values, but after actually restoring the modules, we
        //need to serialize the SystemModulesManager to be used when reloading the PydevPlugin
        this.getInterpreterManager().saveInterpretersInfoModulesManager();
        
        changed = false;
        setEditorUnchanged();
        return true;
    }

    /**
     * @param defaultSelectedInterpreter this is the path to the default selected file (interpreter)
     * @param monitor a monitor to display the progress to the user.
     */
    protected abstract void doRestore(final String defaultSelectedInterpreter, IProgressMonitor monitor);
    
    /**
     * all the information should be cleared but the related to the interpreters passed
     * @param allButTheseInterpreters
     * @param monitor
     */
    protected void doClear(final List<String> allButTheseInterpreters, IProgressMonitor monitor){
        IInterpreterManager iMan = getInterpreterManager();
        iMan.clearAllBut(allButTheseInterpreters);
    }
    
    /**
     * @return the interpreter manager associated to this page.
     */
    protected abstract IInterpreterManager getInterpreterManager();

    protected boolean isEditorChanged() {
        return pathEditor.hasChanged();
    }

    protected void setEditorUnchanged() {
        pathEditor.changed = false;
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
    protected void restoreModules() {
    
        if(pathEditor.getExesList().getItemCount() <= 0){
            doClear(new ArrayList<String>(),new NullProgressMonitor());
            return;
    
        } else{
            //this is the default interpreter
            final String item = pathEditor.getExesList().getItem(0);
            ProgressMonitorDialog monitorDialog = new ProgressMonitorDialog(this.getShell());
            monitorDialog.setBlockOnOpen(false);

            final List<String> exesToKeep = new ArrayList<String>();
            org.eclipse.swt.widgets.List exesList = pathEditor.getExesList();
            String[] items = exesList.getItems();
            for (String exeToKeep : items) {
                exesToKeep.add(exeToKeep);
            }
    
            try {
                IRunnableWithProgress operation = new IRunnableWithProgress(){
    
                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        monitor.beginTask("Restoring PYTHONPATH", IProgressMonitor.UNKNOWN);
                        //clear all but the ones that appear
                        doClear(exesToKeep,monitor);
                        
                        //restore the default
                        doRestore(item, monitor);
                        monitor.done();
                    }};
                    
                monitorDialog.run(true, true, operation);
                
            }catch (Exception e) {
                PydevPlugin.log(e);
            }            
        }
    }

    
    


}
